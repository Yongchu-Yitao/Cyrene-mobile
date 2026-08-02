package ai.cyrene.mobile

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.cyrene.mobile.data.CachedProjectData
import ai.cyrene.mobile.data.DesktopDataCache
import ai.cyrene.mobile.data.DesktopDataSnapshot
import ai.cyrene.mobile.data.SecureStore
import ai.cyrene.mobile.localagent.database.LocalAgentDatabase
import ai.cyrene.mobile.localagent.database.LocalSessionEntity
import ai.cyrene.mobile.localagent.model.RunState
import ai.cyrene.mobile.localagent.runtime.RunSnapshot
import ai.cyrene.mobile.localagent.service.LocalAgentForegroundService
import ai.cyrene.mobile.localagent.service.LocalRunSupervisor
import ai.cyrene.mobile.network.CyreneClient
import ai.cyrene.mobile.network.PairingOffer
import ai.cyrene.mobile.protocol.Peer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.Base64
import java.util.UUID

internal const val LOCAL_PROJECT_ID = "mobile:local"

data class MobileUiState(
    val peer: Peer? = null,
    val peers: List<Peer> = emptyList(),
    val pairingOffer: PairingOffer? = null,
    val busy: Boolean = false,
    val creatingChat: Boolean = false,
    val backgroundSyncing: Boolean = false,
    val backgroundSyncProgress: Float = 0f,
    val desktopConnected: Boolean = false,
    val error: String? = null,
    val status: String = "",
    val projects: List<JSONObject> = emptyList(),
    val selectedProject: JSONObject? = null,
    val projectChats: Map<String, List<JSONObject>> = emptyMap(),
    val projectTasks: Map<String, List<JSONObject>> = emptyMap(),
    val chats: List<JSONObject> = emptyList(),
    val selectedChat: JSONObject? = null,
    val tasks: List<JSONObject> = emptyList(),
    val selectedTask: JSONObject? = null,
    val artifacts: List<JSONObject> = emptyList(),
    val selectedChangeDiff: JSONObject? = null,
    val changeDiffLoading: Boolean = false,
    val viewerFile: JSONObject? = null,
    val viewerFilePath: String? = null,
    val viewerMimeType: String? = null,
    val viewerLoading: Boolean = false,
    val inlineAttachmentPaths: Map<String, String> = emptyMap(),
    val inlineAttachmentLoading: Set<String> = emptySet(),
    val inlineAttachmentErrors: Set<String> = emptySet(),
    val fullImagePaths: Map<String, String> = emptyMap(),
    val fullImageLoading: Set<String> = emptySet(),
    val fullImageErrors: Set<String> = emptySet(),
    val downloadProgress: Map<String, Float> = emptyMap(),
    val downloadedFiles: List<String> = emptyList(),
    val runEvents: List<JSONObject> = emptyList(),
    val activeRunId: String? = null,
    val terminalLines: List<String> = emptyList(),
    val terminalPrompt: String = "$",
    val terminalCwd: String = ".",
    val terminalBusy: Boolean = false,
    val terminalSessionId: String? = null,
    val terminalSessionStatus: String = "disconnected",
    val uiTheme: String = "system",
    val uiLanguage: String = "",
    val permissionMode: PermissionMode = PermissionMode.AUTO,
    val desktopSettings: JSONObject? = null,
    val desktopSettingsSchema: JSONObject? = null,
    val desktopModels: JSONObject? = null,
    val desktopOpenAiOAuth: JSONObject? = null,
    val desktopOpenAiOAuthLoading: Boolean = false,
    val desktopOpenAiOAuthAuthUrl: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SecureStore(application)
    private val desktopDataCache = DesktopDataCache(application)
    private val localAgentDao = LocalAgentDatabase.open(application).dao()
    private val identity = store.identity()
    private val client = CyreneClient(
        identity,
        "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
        application,
    )
    private fun text(id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    private fun localProject() = JSONObject()
        .put("id", LOCAL_PROJECT_ID)
        .put("name", text(R.string.local_project_name))
        .put("status", "local")
        .put("local", true)

    private fun isLocalProject(project: JSONObject?) =
        project?.optString("id") == LOCAL_PROJECT_ID

    private fun withLocalProject(projects: List<JSONObject>): List<JSONObject> =
        listOf(localProject()) + projects.filterNot(::isLocalProject)

    private fun initialState(
        peer: Peer? = null,
        peers: List<Peer> = store.peers(),
    ) = MobileUiState(
        peer = peer,
        peers = peers,
        status = text(R.string.status_not_connected),
        terminalLines = emptyList(),
        uiTheme = store.uiTheme(),
        uiLanguage = store.uiLanguage(),
        permissionMode = store.permissionMode(),
        desktopModels = store.localModelConfigurationPublic(),
        projects = listOf(localProject()),
        selectedProject = if (peer == null) localProject() else null,
        projectChats = mapOf(LOCAL_PROJECT_ID to emptyList()),
    )

    private val _state = MutableStateFlow(initialState(store.peer()))
    val state = _state.asStateFlow()
    private var terminalPollJob: Job? = null
    private var openAiOAuthPollJob: Job? = null
    private var terminalProjectId = ""
    private var terminalCursor = 0
    private var terminalCommandSent = false
    private var terminalOutputStarted = false
    private var terminalPrompt = "$"
    private var deviceLoadJob: Job? = null
    private var backgroundSyncJob: Job? = null
    private var localChatObservationJob: Job? = null
    private val cacheMutex = Mutex()
    @Volatile
    private var cachedData = DesktopDataSnapshot()

    init {
        refreshLocalSessions()
        _state.value.peer?.let(::restoreCacheAndRefresh)
    }

    private fun refreshLocalSessions(selectLocalWhenUnpaired: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val sessions = localAgentDao.sessions()
                val chats = sessions.map { localSessionJson(it, includeMessages = false) }
                val current = _state.value
                val selectedLocal = isLocalProject(current.selectedProject)
                val selectedProject = when {
                    selectedLocal -> localProject()
                    current.selectedProject == null && current.peer == null && selectLocalWhenUnpaired -> localProject()
                    else -> current.selectedProject
                }
                _state.value = current.copy(
                    projects = withLocalProject(current.projects),
                    selectedProject = selectedProject,
                    projectChats = current.projectChats + (LOCAL_PROJECT_ID to chats),
                    chats = if (selectedLocal || selectedProject?.optString("id") == LOCAL_PROJECT_ID) {
                        chats
                    } else {
                        current.chats
                    },
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    error = error.message ?: text(R.string.local_agent_unavailable),
                )
            }
        }
    }

    private suspend fun localSessionJson(
        session: LocalSessionEntity,
        includeMessages: Boolean,
    ): JSONObject {
        val messages = localAgentDao.messages(session.sessionId, if (includeMessages) 500 else 1)
        val latestRun = localAgentDao.latestRun(session.sessionId)
        val updatedAt = session.updatedAt.toLongOrNull() ?: 0L
        val createdAt = session.createdAt.toLongOrNull() ?: updatedAt
        return JSONObject()
            .put("id", session.sessionId)
            .put("title", session.title)
            .put("project_id", LOCAL_PROJECT_ID)
            .put("local", true)
            .put("status", localRunStatus(latestRun))
            .put("message_count", if (includeMessages) messages.size else localAgentDao.messageCount(session.sessionId))
            .put("preview", messages.lastOrNull()?.content.orEmpty())
            .put("createdAt", epochMillisAsIso(createdAt))
            .put("updatedAt", epochMillisAsIso(updatedAt))
            .put("execution_target", session.executionTarget)
            .also { chat ->
                if (includeMessages) chat.put(
                    "messages",
                    JSONArray(localConversationTimeline(session.sessionId, messages, latestRun)),
                )
            }
    }

    private suspend fun localConversationTimeline(
        sessionId: String,
        messages: List<ai.cyrene.mobile.localagent.database.LocalMessageEntity>,
        latestRun: RunSnapshot?,
    ): List<JSONObject> {
        val timeline = messages.map { message ->
            JSONObject()
                .put("id", message.messageId)
                .put("role", message.role)
                .put("content", message.content)
                .put("createdAt", epochMillisAsIso(message.createdAt.toLongOrNull() ?: 0L))
                .put("timelineEpochMs", message.createdAt.toLongOrNull() ?: 0L)
        }.toMutableList()
        val traces = localAgentDao.traces(sessionId, 500).asReversed()
        val activities = linkedMapOf<String, JSONObject>()
        val callTurns = mutableMapOf<String, String>()
        val intermediate = mutableListOf<JSONObject>()
        fun activity(turnId: String, trace: ai.cyrene.mobile.localagent.database.LocalTraceEntity) =
            activities.getOrPut(turnId) {
                JSONObject()
                    .put("id", "activity_$turnId")
                    .put("role", "assistant")
                    .put("content", "")
                    .put("activityCard", true)
                    .put("trace", JSONArray())
                    .put("reasoning", "")
                    .put("provider", localProviderName())
                    .put("runId", trace.runId.orEmpty())
                    .put("createdAt", epochMillisAsIso(trace.createdAt.toLongOrNull() ?: 0L))
                    .put("timelineEpochMs", trace.createdAt.toLongOrNull() ?: 0L)
            }
        traces.forEach { trace ->
            val raw = runCatching { JSONObject(trace.payloadJson) }.getOrNull() ?: return@forEach
            if (trace.type == "model_event") {
                val turnId = raw.optString("model_turn_id").ifBlank { return@forEach }
                val eventType = raw.optString("type")
                val payload = raw.optJSONObject("payload") ?: JSONObject()
                val card = activity(turnId, trace)
                when (eventType) {
                    "reasoning.status" -> {
                        val delta = payload.optString("delta").ifBlank {
                            payload.optString("content").ifBlank { payload.optString("text") }
                        }
                        if (delta.isNotBlank()) card.put("reasoning", card.optString("reasoning") + delta)
                    }
                    "tool_call.started", "tool_call.completed" -> {
                        val callId = payload.optString("call_id")
                        if (callId.isNotBlank()) callTurns[callId] = turnId
                        localTraceEntry(trace)?.let { card.getJSONArray("trace").put(it) }
                    }
                    "message.completed" -> if (card.getJSONArray("trace").length() > 0) {
                        val content = payload.optString("content")
                        if (content.isNotBlank()) intermediate += JSONObject()
                            .put("id", "intermediate_${turnId}_${trace.createdAt}")
                            .put("role", "assistant")
                            .put("content", content)
                            .put("createdAt", epochMillisAsIso(trace.createdAt.toLongOrNull() ?: 0L))
                            .put("timelineEpochMs", trace.createdAt.toLongOrNull() ?: 0L)
                    }
                }
            } else if (trace.type == "tool_result") {
                val callId = raw.optString("call_id")
                val turnId = callTurns[callId] ?: return@forEach
                localTraceEntry(trace)?.let { activities[turnId]?.getJSONArray("trace")?.put(it) }
            }
        }
        activities.values.forEach { card ->
            if (card.getJSONArray("trace").length() == 0 && card.optString("reasoning").isBlank()) return@forEach
            card.put(
                "runtimeActivityActive",
                card.optString("runId") == latestRun?.runId && isLocalRunActive(latestRun),
            ).remove("runId")
            timeline += card
        }
        timeline += intermediate
        return timeline.sortedWith(
            compareBy<JSONObject> { it.optLong("timelineEpochMs") }
                .thenBy { if (it.optBoolean("activityCard")) 1 else 0 },
        ).onEach { it.remove("timelineEpochMs") }
    }

    private fun localProviderName(): String = store.localModelConfigurationPublic()
        ?.optJSONArray("custom_models")?.optJSONObject(0)
        ?.optString("provider").orEmpty()

    private fun localTraceEntry(trace: ai.cyrene.mobile.localagent.database.LocalTraceEntity): JSONObject? {
        val raw = runCatching { JSONObject(trace.payloadJson) }.getOrNull() ?: return null
        if (trace.type == "model_event") {
            val eventType = raw.optString("type")
            val payload = raw.optJSONObject("payload") ?: JSONObject()
            if (eventType !in setOf("tool_call.started", "tool_call.completed")) return null
            if (payload.optString("name") in setOf(
                    "use_tools", "quit", "send_message", "update_plan_progress",
                )
            ) return null
            val entry = JSONObject()
                .put("type", eventType)
                .put("toolCallId", payload.optString("call_id"))
                .put("tool", payload.optString("name"))
                .put("status", if (eventType.endsWith("started")) "running" else "completed")
            payload.optString("arguments_json").takeIf(String::isNotBlank)?.let { arguments ->
                runCatching { JSONObject(arguments) }.getOrNull()?.let { entry.put("args", it) }
            }
            return entry
        }
        if (trace.type == "tool_result") {
            val inline = raw.optString("inline_payload")
            val detail = runCatching { JSONObject(inline) }.getOrNull()
            val preview = detail?.optString("command").orEmpty().ifBlank {
                detail?.optString("stdout").orEmpty().lineSequence().firstOrNull().orEmpty()
            }.ifBlank { raw.optString("summary") }
            return JSONObject()
                .put("type", "tool_result")
                .put("toolCallId", raw.optString("call_id"))
                .put("status", raw.optString("status"))
                .put("result_preview", preview.take(240))
                .put("failed", raw.optString("status") !in setOf("success", "completed"))
        }
        return null
    }

    private fun localRunStatus(run: RunSnapshot?): String = when {
        run == null -> "ready"
        run.state == RunState.COMPLETED -> {
            if (run.stopReason?.wireName == "cancelled") "cancelled" else "completed"
        }
        run.state == RunState.FAILED -> "failed"
        !LocalRunSupervisor.isRunning(run.runId) -> "paused"
        else -> "running"
    }

    private fun isLocalRunActive(run: RunSnapshot?) =
        run != null &&
            run.state !in setOf(RunState.COMPLETED, RunState.FAILED) &&
            LocalRunSupervisor.isRunning(run.runId)

    private fun epochMillisAsIso(value: Long): String =
        Instant.ofEpochMilli(value.coerceAtLeast(0L)).toString()

    private suspend fun loadLocalProject(resetSelection: Boolean) {
        localChatObservationJob?.cancel()
        val chats = localAgentDao.sessions().map { localSessionJson(it, includeMessages = false) }
        val current = _state.value
        val selectedChat = current.selectedChat?.takeIf { selected ->
            !resetSelection && chats.any { it.optString("id") == selected.optString("id") }
        }
        _state.value = current.copy(
            projects = withLocalProject(current.projects),
            selectedProject = localProject(),
            projectChats = current.projectChats + (LOCAL_PROJECT_ID to chats),
            projectTasks = current.projectTasks + (LOCAL_PROJECT_ID to emptyList()),
            chats = chats,
            tasks = emptyList(),
            selectedChat = selectedChat,
            selectedTask = null,
            runEvents = if (resetSelection) emptyList() else current.runEvents,
            activeRunId = if (selectedChat == null) null else current.activeRunId,
            status = text(R.string.local_agent_device_only),
        )
        selectedChat?.optString("id")?.takeIf(String::isNotBlank)?.let { sessionId ->
            loadLocalChat(sessionId, clearEvents = false)
            observeLocalChat(sessionId)
        }
    }

    private suspend fun loadLocalChat(sessionId: String, clearEvents: Boolean) {
        val session = localAgentDao.session(sessionId) ?: return
        val detail = localSessionJson(session, includeMessages = true)
        val run = localAgentDao.latestRun(sessionId)
        val traces = localAgentDao.traces(sessionId, 50)
        val runEvents = if (run?.state == RunState.FAILED) {
            val failure = traces.firstOrNull { it.type == "run_failed" }
            listOf(
                JSONObject()
                    .put("type", "error")
                    .put(
                        "message",
                        failure?.let {
                            runCatching { JSONObject(it.payloadJson).optString("message") }.getOrNull()
                        }.orEmpty().ifBlank { text(R.string.chat_run_failed) },
                    ),
            )
        } else if (clearEvents) {
            emptyList()
        } else {
            _state.value.runEvents.filterNot { it.optString("type") == "error" }
        }
        val summaries = localAgentDao.sessions().map { localSessionJson(it, includeMessages = false) }
        val current = _state.value
        if (!isLocalProject(current.selectedProject) || current.selectedChat?.optString("id")
                ?.takeIf(String::isNotBlank)?.let { it != sessionId } == true
        ) return
        val activeRunId = when {
            isLocalRunActive(run) -> run?.runId
            run == null -> current.activeRunId
            else -> null
        }
        _state.value = current.copy(
            selectedChat = detail,
            projectChats = current.projectChats + (LOCAL_PROJECT_ID to summaries),
            chats = summaries,
            activeRunId = activeRunId,
            runEvents = runEvents,
            status = when {
                run?.state == RunState.FAILED -> text(R.string.status_run_failed)
                activeRunId != null -> text(R.string.status_agent_running)
                run?.state == RunState.COMPLETED -> text(R.string.status_run_completed)
                else -> text(R.string.local_agent_device_only)
            },
        )
    }

    private fun observeLocalChat(sessionId: String) {
        localChatObservationJob?.cancel()
        localChatObservationJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive && isLocalProject(_state.value.selectedProject) &&
                _state.value.selectedChat?.optString("id") == sessionId
            ) {
                loadLocalChat(sessionId, clearEvents = false)
                delay(500)
            }
        }
    }

    private fun localExecutionTarget() =
        "mobile:${identity.fingerprint.replace(" ", "")}"

    private fun restoreCacheAndRefresh(peer: Peer) {
        deviceLoadJob = viewModelScope.launch(Dispatchers.IO) {
            desktopDataCache.load(peer.deviceId)?.let { snapshot ->
                ensureCurrentPeer(peer)
                cachedData = snapshot
                applyCachedSnapshot(snapshot)
            }
            ensureCurrentPeer(peer)
            _state.value = _state.value.copy(
                busy = true,
                desktopConnected = false,
                error = null,
                status = text(R.string.status_syncing_projects),
            )
            var activePeer = peer
            runCatching {
                activePeer = adoptRecoveredPeer(peer, client.recoverEndpoint(peer))
                loadProjects(activePeer)
            }
                .onSuccess { startBackgroundSync(activePeer) }
                .onFailure { error ->
                    if (error !is CancellationException &&
                        _state.value.peer?.deviceId == peer.deviceId
                    ) {
                        _state.value = _state.value.copy(
                            // Automatic desktop refresh must not block the local conversation UI.
                            // The cached remote projects remain available and Device shows the
                            // connection state when the user wants to troubleshoot it.
                            error = null,
                            desktopConnected = false,
                            status = if (cachedData.projects.isEmpty()) {
                                text(R.string.status_need_attention)
                            } else {
                                text(R.string.status_cached_projects, cachedData.projects.size)
                            },
                        )
                    }
                }
            if (_state.value.peer?.deviceId == peer.deviceId) {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }

    fun beginPairing(host: String, port: String, key: String) =
        launchBusy(text(R.string.status_checking_pair)) {
        val offer = client.claim(host.trim(), port.toIntOrNull() ?: 37841, key)
        _state.value = _state.value.copy(
            pairingOffer = offer,
            status = text(R.string.status_verify_fingerprint),
        )
    }

    fun confirmPairing() = launchBusy(text(R.string.status_pairing)) {
        val offer = requireNotNull(_state.value.pairingOffer)
        val peer = client.complete(offer)
        store.savePeer(peer)
        cancelActiveDeviceWork()
        cachedData = DesktopDataSnapshot()
        _state.value = initialState(peer, store.peers()).copy(
            status = text(R.string.status_connected, peer.name),
            busy = true,
        )
        loadProjects(peer)
        startBackgroundSync(peer)
    }

    fun cancelPairing() {
        _state.value = _state.value.copy(
            pairingOffer = null,
            status = _state.value.peer?.let { text(R.string.status_connected, it.name) }
                ?: text(R.string.status_not_connected),
        )
    }

    fun forgetDevice() {
        val peerId = _state.value.peer?.deviceId
        cancelActiveDeviceWork()
        if (peerId != null) {
            store.clearPeer(peerId)
        }
        cachedData = DesktopDataSnapshot()
        if (peerId != null) {
            viewModelScope.launch(Dispatchers.IO) { desktopDataCache.clear(peerId) }
        }
        val nextPeer = store.peer()
        _state.value = initialState(nextPeer, store.peers())
        nextPeer?.let(::restoreCacheAndRefresh)
    }

    fun selectDevice(peer: Peer) {
        if (_state.value.peer?.deviceId == peer.deviceId) return
        cancelActiveDeviceWork()
        store.selectPeer(peer.deviceId)
        cachedData = DesktopDataSnapshot()
        _state.value = initialState(peer, store.peers()).copy(
            status = text(R.string.status_connected, peer.name),
        )
        restoreCacheAndRefresh(peer)
    }

    private fun cancelActiveDeviceWork() {
        deviceLoadJob?.cancel()
        deviceLoadJob = null
        backgroundSyncJob?.cancel()
        backgroundSyncJob = null
        terminalPollJob?.cancel()
        terminalPollJob = null
        openAiOAuthPollJob?.cancel()
        openAiOAuthPollJob = null
        terminalProjectId = ""
        terminalCursor = 0
        terminalCommandSent = false
        terminalOutputStarted = false
        terminalPrompt = "$"
    }

    fun setUiTheme(value: String) {
        store.saveUiTheme(value)
        syncApplicationNightMode(getApplication(), value)
        _state.value = _state.value.copy(uiTheme = value)
    }

    fun setUiLanguage(value: String) {
        store.saveUiLanguage(value)
        _state.value = _state.value.copy(uiLanguage = value)
    }

    fun setPermissionMode(value: PermissionMode) {
        store.savePermissionMode(value)
        _state.value = _state.value.copy(permissionMode = value)
    }

    fun loadDesktopSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                busy = true,
                desktopSettings = null,
                desktopSettingsSchema = null,
                desktopModels = null,
                status = text(R.string.status_loading_settings),
            )
            runCatching {
                val peer = requirePeer()
                val result = client.command(peer, "settings.read")
                copyDesktopModelsToLocal(peer)
                result
            }.onSuccess { result ->
                _state.value = _state.value.copy(
                    desktopSettings = result.optJSONObject("settings") ?: JSONObject(),
                    desktopSettingsSchema = result.optJSONObject("schema"),
                    desktopModels = store.localModelConfigurationPublic()
                        ?: result.optJSONObject("models"),
                    status = text(R.string.status_settings_loaded),
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    desktopSettings = null,
                    desktopSettingsSchema = null,
                    desktopModels = store.localModelConfigurationPublic(),
                    status = text(R.string.status_settings_unavailable),
                )
            }
            _state.value = _state.value.copy(busy = false)
        }
    }

    fun updateDesktopSetting(key: String, value: Any) =
        launchBusy(text(R.string.status_saving_settings)) {
            val result = client.command(
                requirePeer(),
                "settings.update",
                payload = JSONObject().put(key, value),
            )
            _state.value = _state.value.copy(
                desktopSettings = result.optJSONObject("settings") ?: _state.value.desktopSettings,
                desktopSettingsSchema = result.optJSONObject("schema")
                    ?: _state.value.desktopSettingsSchema,
                status = text(R.string.status_settings_saved),
            )
        }

    fun updateDesktopModels(models: JSONObject) =
        launchBusy(text(R.string.status_saving_models)) {
            val localModels = mergeLocalModelSecrets(models, store.localModelConfiguration())
            store.saveLocalModelConfiguration(localModels)
            _state.value = _state.value.copy(
                desktopModels = store.localModelConfigurationPublic(),
                status = text(R.string.status_local_models_saved),
            )
            val peer = _state.value.peer ?: return@launchBusy
            val result = client.command(
                peer,
                "settings.update",
                payload = JSONObject().put("models", localModels),
            )
            _state.value = _state.value.copy(
                desktopSettings = result.optJSONObject("settings")
                    ?: _state.value.desktopSettings,
                desktopSettingsSchema = result.optJSONObject("schema")
                    ?: _state.value.desktopSettingsSchema,
                desktopModels = store.localModelConfigurationPublic()
                    ?: result.optJSONObject("models")
                    ?: _state.value.desktopModels,
                status = text(R.string.status_models_saved),
            )
        }

    fun loadDesktopOpenAiOAuth() {
        if (_state.value.desktopOpenAiOAuthLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(desktopOpenAiOAuthLoading = true)
            runCatching {
                client.command(requirePeer(), "settings.openai_oauth.read")
            }.onSuccess { result ->
                _state.value = _state.value.copy(desktopOpenAiOAuth = result)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    desktopOpenAiOAuth = JSONObject()
                        .put("available", false)
                        .put("connected", false)
                        .put("models", JSONArray())
                        .put("error", error.message ?: text(R.string.error_generic)),
                )
            }
            _state.value = _state.value.copy(desktopOpenAiOAuthLoading = false)
        }
    }

    fun startDesktopOpenAiOAuthLogin() {
        openAiOAuthPollJob?.cancel()
        openAiOAuthPollJob = viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                desktopOpenAiOAuthLoading = true,
                desktopOpenAiOAuthAuthUrl = null,
                error = null,
            )
            runCatching {
                client.command(requirePeer(), "settings.openai_oauth.login")
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    desktopOpenAiOAuthLoading = false,
                    error = error.message ?: text(R.string.error_generic),
                    status = text(R.string.status_need_attention),
                )
            }.onSuccess { result ->
                val authUrl = result.optString("authUrl")
                    .ifBlank { result.optString("auth_url") }
                    .ifBlank { result.optString("url") }
                _state.value = _state.value.copy(
                    desktopOpenAiOAuthAuthUrl = authUrl.ifBlank { null },
                )

                var attempts = 0
                while (isActive && attempts < 40) {
                    delay(1_500)
                    attempts += 1
                    val snapshot = runCatching {
                        client.command(requirePeer(), "settings.openai_oauth.read")
                    }.getOrNull() ?: continue
                    _state.value = _state.value.copy(desktopOpenAiOAuth = snapshot)
                    if (snapshot.optBoolean("connected")) break
                }
                _state.value = _state.value.copy(desktopOpenAiOAuthLoading = false)
            }
        }
    }

    fun clearDesktopOpenAiOAuthAuthUrl() {
        _state.value = _state.value.copy(desktopOpenAiOAuthAuthUrl = null)
    }

    fun logoutDesktopOpenAiOAuth() {
        openAiOAuthPollJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                desktopOpenAiOAuthLoading = true,
                error = null,
            )
            runCatching {
                client.command(requirePeer(), "settings.openai_oauth.logout")
                client.command(requirePeer(), "settings.openai_oauth.read")
            }.onSuccess { result ->
                _state.value = _state.value.copy(desktopOpenAiOAuth = result)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    error = error.message ?: text(R.string.error_generic),
                    status = text(R.string.status_need_attention),
                )
            }
            _state.value = _state.value.copy(desktopOpenAiOAuthLoading = false)
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    fun refreshProjects() = launchBusy(text(R.string.status_syncing_projects)) {
        _state.value = _state.value.copy(desktopConnected = false)
        val current = requirePeer()
        val peer = adoptRecoveredPeer(current, client.recoverEndpoint(current))
        loadProjects(peer)
        startBackgroundSync(peer)
    }

    private fun adoptRecoveredPeer(expected: Peer, recovered: Peer): Peer {
        ensureCurrentPeer(expected)
        if (expected.host == recovered.host && expected.port == recovered.port) return expected
        store.savePeer(recovered)
        _state.value = _state.value.copy(
            peer = recovered,
            peers = store.peers(),
        )
        return recovered
    }

    private suspend fun loadProjects(peer: Peer) {
        val result = client.command(peer, "projects.list")
        ensureCurrentPeer(peer)
        // A successful desktop connection is also the hand-off point for the
        // provider configuration used by device-local conversations. Keep this
        // best-effort so an older desktop can still serve remote projects, but
        // never require the user to visit Settings just to trigger the copy.
        copyDesktopModelsToLocal(peer)
        val remoteProjects = result.optJSONArray("projects").objects()
        val projects = withLocalProject(remoteProjects)
        updateCache(peer.deviceId) { previous -> previous.copy(projects = remoteProjects) }
        val previousProjectId = _state.value.selectedProject?.optString("id").orEmpty()
        val selectedProject = projects.firstOrNull {
            it.optString("id") == previousProjectId
        } ?: remoteProjects.firstOrNull() ?: projects.firstOrNull()
        val projectChanged = selectedProject?.optString("id").orEmpty() != previousProjectId
        _state.value = _state.value.copy(
            projects = projects,
            desktopConnected = true,
            selectedProject = selectedProject,
            selectedChat = if (projectChanged) null else _state.value.selectedChat,
            selectedTask = if (projectChanged) null else _state.value.selectedTask,
            chats = if (selectedProject == null) emptyList() else _state.value.chats,
            tasks = if (selectedProject == null) emptyList() else _state.value.tasks,
            status = text(R.string.status_online_projects, remoteProjects.size),
        )
        if (selectedProject != null) {
            if (isLocalProject(selectedProject)) {
                loadLocalProject(resetSelection = projectChanged)
            } else {
                loadProjectContent(peer, selectedProject, resetSelection = projectChanged)
            }
        }
    }

    private suspend fun copyDesktopModelsToLocal(peer: Peer): Boolean {
        ensureCurrentPeer(peer)
        val models = runCatching {
            client.command(peer, "settings.models.copy").optJSONObject("models")
        }.getOrNull() ?: return false
        ensureCurrentPeer(peer)
        store.saveLocalModelConfiguration(models)
        _state.value = _state.value.copy(
            desktopModels = store.localModelConfigurationPublic(),
        )
        return true
    }

    fun selectProject(project: JSONObject) {
        if (isLocalProject(project)) {
            viewModelScope.launch(Dispatchers.IO) {
                loadLocalProject(resetSelection = true)
            }
            return
        }
        localChatObservationJob?.cancel()
        val projectId = project.optString("id")
        val cached = cachedData.projectData[projectId]
        if (cached != null) {
            applyProjectData(project, cached, resetSelection = true)
            refreshProjectInBackground(project)
            return
        }
        launchBusy(text(R.string.status_loading_project)) {
            loadProjectContent(requirePeer(), project, resetSelection = true)
        }
    }

    fun refreshProjectContent() {
        val project = _state.value.selectedProject ?: return
        if (isLocalProject(project)) {
            viewModelScope.launch(Dispatchers.IO) {
                loadLocalProject(resetSelection = false)
            }
            return
        }
        launchBusy(text(R.string.status_refreshing_project)) {
            loadProjectContent(requirePeer(), project, resetSelection = false)
        }
    }

    private suspend fun loadProjectContent(
        peer: Peer,
        project: JSONObject,
        resetSelection: Boolean,
    ) {
        val projectId = project.getString("id")
        val chats = client.command(peer, "chats.list", projectId)
            .optJSONArray("chats").objects()
        val tasks = client.command(peer, "tasks.list", projectId)
            .optJSONArray("tasks").objects()
        ensureCurrentPeer(peer)
        updateCache(peer.deviceId) { previous ->
            previous.copy(
                projectData = previous.projectData + (
                    projectId to CachedProjectData(chats = chats, tasks = tasks)
                ),
            )
        }
        applyProjectData(project, CachedProjectData(chats, tasks), resetSelection)
    }

    private fun applyProjectData(
        project: JSONObject,
        data: CachedProjectData,
        resetSelection: Boolean,
    ) {
        val chats = data.chats
        val tasks = data.tasks
        val selectedChatId = _state.value.selectedChat?.optString("id").orEmpty()
        val selectedTaskId = _state.value.selectedTask?.optString("id").orEmpty()
        _state.value = _state.value.copy(
            selectedProject = project,
            projectChats = _state.value.projectChats + (project.getString("id") to chats),
            projectTasks = _state.value.projectTasks + (project.getString("id") to tasks),
            chats = chats,
            tasks = tasks,
            selectedChat = if (resetSelection) null else _state.value.selectedChat?.takeIf {
                chats.any { chat -> chat.optString("id") == selectedChatId }
            },
            selectedTask = if (resetSelection) null else _state.value.selectedTask?.takeIf {
                tasks.any { task -> task.optString("id") == selectedTaskId }
            },
            runEvents = if (resetSelection) emptyList() else _state.value.runEvents,
            status = project.optString("name"),
        )
    }

    private fun refreshProjectInBackground(project: JSONObject) {
        if (isLocalProject(project)) return
        val peer = _state.value.peer ?: return
        launchTrackedRefresh(peer) {
            loadProjectContent(peer, project, resetSelection = false)
        }
    }

    fun showChatList() {
        localChatObservationJob?.cancel()
        _state.value = _state.value.copy(
            selectedChat = null,
            runEvents = emptyList(),
            activeRunId = null,
            selectedChangeDiff = null,
            viewerFile = null,
            viewerFilePath = null,
            viewerMimeType = null,
            viewerLoading = false,
        )
    }

    fun sendNewChatMessage(
        message: String,
        permissionMode: PermissionMode,
        attachments: List<PendingAttachment> = emptyList(),
    ): Boolean {
        if (_state.value.creatingChat) return false
        val content = message.trim()
        if (content.isBlank()) return false
        val projectId = _state.value.selectedProject?.optString("id").orEmpty()
        if (projectId.isBlank()) return false
        if (projectId == LOCAL_PROJECT_ID) {
            return createLocalChatAndSend(content, attachments)
        }
        val peer = _state.value.peer ?: return false
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                creatingChat = true,
                error = null,
                status = text(R.string.status_sending_agent),
            )
            runCatching {
                val result = client.command(
                    peer, "chats.create", projectId,
                    JSONObject().put("title", ""),
                )
                reloadChats(peer, projectId)
                val chat = requireNotNull(result.optJSONObject("chat")) {
                    text(R.string.error_generic)
                }
                ensureCurrentPeer(peer)
                check(_state.value.selectedProject?.optString("id") == projectId)
                _state.value = _state.value.copy(
                    selectedChat = chat,
                    runEvents = emptyList(),
                    activeRunId = null,
                    selectedChangeDiff = null,
                    viewerFile = null,
                    viewerFilePath = null,
                    viewerMimeType = null,
                    viewerLoading = false,
                )
                sendMessageCommand(
                    peer = peer,
                    projectId = projectId,
                    chat = chat,
                    content = content,
                    permissionMode = permissionMode,
                    attachments = attachments,
                )
            }.onFailure { error ->
                if (_state.value.peer?.deviceId == peer.deviceId) {
                    _state.value = _state.value.copy(
                        error = error.message ?: text(R.string.error_generic),
                        status = text(R.string.status_need_attention),
                    )
                }
            }
            if (_state.value.peer?.deviceId == peer.deviceId) {
                _state.value = _state.value.copy(creatingChat = false)
            }
        }
        return true
    }

    private fun createLocalChatAndSend(
        content: String,
        attachments: List<PendingAttachment>,
    ): Boolean {
        if (attachments.isNotEmpty()) {
            _state.value = _state.value.copy(error = text(R.string.local_agent_attachments_unsupported))
            return false
        }
        if (!store.hasRunnableLocalModel()) {
            _state.value = _state.value.copy(error = text(R.string.local_agent_model_required))
            return false
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                creatingChat = true,
                error = null,
                status = text(R.string.status_sending_agent),
            )
            runCatching {
                val sessionId = "ls_${UUID.randomUUID().toString().replace("-", "")}"
                val title = content.lineSequence().firstOrNull()
                    ?.trim()?.take(40).orEmpty()
                    .ifBlank { text(R.string.local_agent_default_title) }
                localAgentDao.putSession(
                    LocalSessionEntity(
                        sessionId = sessionId,
                        title = title,
                        executionTarget = localExecutionTarget(),
                    ),
                )
                loadLocalProject(resetSelection = true)
                loadLocalChat(sessionId, clearEvents = true)
                val runId = "run_${UUID.randomUUID().toString().replace("-", "")}"
                updateLocalUserMessage(
                    chatId = sessionId,
                    messageId = "pending_$runId",
                    content = content,
                    deliveryState = "sending",
                )
                _state.value = _state.value.copy(
                    activeRunId = runId,
                    status = text(R.string.status_agent_running),
                )
                LocalAgentForegroundService.startRun(
                    getApplication(), runId, title, sessionId, 1L, content,
                )
                observeLocalChat(sessionId)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    error = error.message ?: text(R.string.local_agent_unavailable),
                    activeRunId = null,
                    status = text(R.string.status_need_attention),
                )
            }
            _state.value = _state.value.copy(creatingChat = false)
        }
        return true
    }

    private fun sendLocalMessage(
        chat: JSONObject,
        content: String,
        attachments: List<PendingAttachment>,
    ) {
        if (content.isBlank()) return
        if (attachments.isNotEmpty()) {
            _state.value = _state.value.copy(error = text(R.string.local_agent_attachments_unsupported))
            return
        }
        if (!store.hasRunnableLocalModel()) {
            _state.value = _state.value.copy(error = text(R.string.local_agent_model_required))
            return
        }
        if (_state.value.activeRunId != null) return
        val sessionId = chat.optString("id")
        val runId = "run_${UUID.randomUUID().toString().replace("-", "")}"
        updateLocalUserMessage(
            chatId = sessionId,
            messageId = "pending_$runId",
            content = content,
            deliveryState = "sending",
        )
        _state.value = _state.value.copy(
            activeRunId = runId,
            runEvents = emptyList(),
            error = null,
            status = text(R.string.status_agent_running),
        )
        LocalAgentForegroundService.startRun(
            getApplication(), runId, chat.optString("title"), sessionId, 1L, content,
        )
        observeLocalChat(sessionId)
    }

    fun renameChat(project: JSONObject, chat: JSONObject, requestedTitle: String) {
        val projectId = project.optString("id")
        val chatId = chat.optString("id")
        val title = requestedTitle.trim()
        if (projectId.isBlank() || chatId.isBlank() || title.isBlank()) return
        if (projectId == LOCAL_PROJECT_ID) {
            viewModelScope.launch(Dispatchers.IO) {
                _state.value = _state.value.copy(busy = true, error = null)
                runCatching {
                    check(localAgentDao.renameSession(chatId, title) == 1)
                    loadLocalProject(resetSelection = false)
                }.onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: text(R.string.error_generic),
                    )
                }
                _state.value = _state.value.copy(busy = false)
            }
            return
        }
        val peer = _state.value.peer ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching {
                val result = client.command(
                    peer,
                    "chats.update",
                    projectId,
                    JSONObject()
                        .put("chat_id", chatId)
                        .put("title", title),
                )
                val updatedChat = result.optJSONObject("chat")
                reloadChats(peer, projectId)
                updatedChat
            }.onSuccess { updatedChat ->
                if (
                    updatedChat != null &&
                    _state.value.selectedProject?.optString("id") == projectId &&
                    _state.value.selectedChat?.optString("id") == chatId
                ) {
                    updateCache(peer.deviceId) { previous ->
                        previous.copy(
                            chatDetails = previous.chatDetails + (chatId to updatedChat),
                        )
                    }
                    _state.value = _state.value.copy(selectedChat = updatedChat)
                }
            }.onFailure { error ->
                if (_state.value.peer?.deviceId == peer.deviceId) {
                    _state.value = _state.value.copy(
                        error = error.message ?: text(R.string.error_generic),
                        status = text(R.string.status_need_attention),
                    )
                }
            }
            if (_state.value.peer?.deviceId == peer.deviceId) {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }

    fun deleteChat(project: JSONObject, chat: JSONObject) {
        val projectId = project.optString("id")
        val chatId = chat.optString("id")
        if (projectId.isBlank() || chatId.isBlank()) return
        if (projectId == LOCAL_PROJECT_ID) {
            viewModelScope.launch(Dispatchers.IO) {
                _state.value = _state.value.copy(busy = true, error = null)
                runCatching {
                    localChatObservationJob?.cancel()
                    check(localAgentDao.deleteSession(chatId) == 1)
                    loadLocalProject(resetSelection = true)
                }.onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: text(R.string.error_generic),
                    )
                }
                _state.value = _state.value.copy(busy = false)
            }
            return
        }
        val peer = _state.value.peer ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching {
                client.command(
                    peer,
                    "chats.delete",
                    projectId,
                    JSONObject().put("chat_id", chatId),
                )
                updateCache(peer.deviceId) { previous ->
                    previous.copy(chatDetails = previous.chatDetails - chatId)
                }
                reloadChats(peer, projectId)
                if (
                    _state.value.selectedProject?.optString("id") == projectId &&
                    _state.value.selectedChat?.optString("id") == chatId
                ) {
                    val nextChat = _state.value.chats.firstOrNull()
                    _state.value = _state.value.copy(
                        selectedChat = null,
                        runEvents = emptyList(),
                        activeRunId = null,
                        selectedChangeDiff = null,
                        viewerFile = null,
                        viewerFilePath = null,
                        viewerMimeType = null,
                        viewerLoading = false,
                    )
                    if (nextChat != null) {
                        loadChat(peer, projectId, nextChat, clearEvents = true)
                    }
                }
            }.onFailure { error ->
                if (_state.value.peer?.deviceId == peer.deviceId) {
                    _state.value = _state.value.copy(
                        error = error.message ?: text(R.string.error_generic),
                        status = text(R.string.status_need_attention),
                    )
                }
            }
            if (_state.value.peer?.deviceId == peer.deviceId) {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }

    fun openChat(chat: JSONObject) {
        if (chat.optBoolean("local") || isLocalProject(_state.value.selectedProject)) {
            val chatId = chat.optString("id")
            if (chatId.isBlank()) return
            viewModelScope.launch(Dispatchers.IO) {
                _state.value = _state.value.copy(busy = true, error = null)
                loadLocalChat(chatId, clearEvents = true)
                _state.value = _state.value.copy(busy = false)
                observeLocalChat(chatId)
            }
            return
        }
        val chatId = chat.optString("id")
        val cached = cachedData.chatDetails[chatId]
        if (cached != null) {
            _state.value = _state.value.copy(
                selectedChat = cached,
                runEvents = emptyList(),
                activeRunId = null,
                selectedChangeDiff = null,
                viewerFile = null,
                viewerFilePath = null,
                viewerMimeType = null,
                viewerLoading = false,
            )
            refreshChatInBackground(chat)
            return
        }
        projectCommand(text(R.string.status_loading_chat)) { peer, projectId ->
            loadChat(peer, projectId, chat, clearEvents = true)
        }
    }

    fun openChat(project: JSONObject, chat: JSONObject) {
        if (isLocalProject(project)) {
            viewModelScope.launch(Dispatchers.IO) {
                loadLocalProject(resetSelection = true)
                loadLocalChat(chat.optString("id"), clearEvents = true)
                observeLocalChat(chat.optString("id"))
            }
            return
        }
        selectProjectForSession(project)
        openChat(chat)
    }

    fun sendMessage(
        message: String,
        permissionMode: PermissionMode,
        attachments: List<PendingAttachment> = emptyList(),
    ) {
        val content = message.trim()
        val chat = _state.value.selectedChat ?: return
        val chatId = chat.optString("id")
        if (chatId.isBlank()) return
        if (chat.optBoolean("local") || isLocalProject(_state.value.selectedProject)) {
            sendLocalMessage(chat, content, attachments)
            return
        }
        projectCommand(text(R.string.status_sending_agent)) { peer, projectId ->
            sendMessageCommand(
                peer = peer,
                projectId = projectId,
                chat = chat,
                content = content,
                permissionMode = permissionMode,
                attachments = attachments,
            )
        }
    }

    private suspend fun sendMessageCommand(
        peer: Peer,
        projectId: String,
        chat: JSONObject,
        content: String,
        permissionMode: PermissionMode,
        attachments: List<PendingAttachment>,
    ) {
        val chatId = chat.getString("id")
        val localMessageId = "local_" + UUID.randomUUID().toString().replace("-", "")
        if (content.isNotBlank()) {
            updateLocalUserMessage(
                chatId = chatId,
                messageId = localMessageId,
                content = content,
                deliveryState = "sending",
            )
        }
        _state.value = _state.value.copy(
            runEvents = emptyList(),
            activeRunId = null,
            error = null,
        )
        val encodedAttachments = encodeAttachments(attachments)
        val result = try {
            client.command(
                peer, "chats.send", projectId,
                JSONObject()
                    .put("chat_id", chatId)
                    .put("message", content)
                    .put("attachments", encodedAttachments)
                    .put("permission_mode", permissionMode.wireValue)
                    .put("language", "zh"),
                timeoutSeconds = 50,
            )
        } catch (error: Exception) {
            markLocalUserMessageDelivery(chatId, localMessageId, "failed")
            throw error
        }
        markLocalUserMessageDelivery(chatId, localMessageId, "sent")
        val runId = result.optString("run_id").ifBlank {
            result.optJSONObject("run")?.optString("run_id").orEmpty()
        }
        _state.value = _state.value.copy(activeRunId = runId.ifBlank { null })
        loadChat(peer, projectId, chat, clearEvents = false)
        if (runId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val monitorCompleted = runCatching { pollRun(peer, projectId, runId) }
                    .onFailure { error ->
                        _state.value = _state.value.copy(
                            error = error.message ?: text(R.string.error_run_monitor_interrupted),
                            activeRunId = null,
                            status = text(R.string.status_run_monitor_interrupted),
                        )
                    }
                // Workbench drops its live runtime only after the saved
                // transcript becomes authoritative. Do the same here: the
                // durable assistant reply/activity cards replace reply_done
                // and live tool events instead of being rendered beside them.
                runCatching {
                    loadChat(peer, projectId, chat, clearEvents = monitorCompleted.isSuccess)
                }
            }
        }
    }

    private suspend fun pollRun(peer: Peer, projectId: String, runId: String) {
        var cursor = 0
        var consecutiveFailures = 0
        while (_state.value.activeRunId == runId) {
            val result = try {
                client.command(
                    peer, "runs.wait", projectId,
                    JSONObject()
                        .put("run_id", runId)
                        .put("cursor", cursor)
                        .put("limit", 200)
                        .put("timeout_seconds", 25),
                    timeoutSeconds = 50,
                )
            } catch (error: Exception) {
                consecutiveFailures += 1
                val backoffSeconds = minOf(
                    30,
                    1 shl minOf(consecutiveFailures - 1, 5),
                )
                _state.value = _state.value.copy(
                    status = text(R.string.status_retry_run, backoffSeconds),
                )
                delay(backoffSeconds * 1_000L)
                continue
            }
            consecutiveFailures = 0
            val events = result.optJSONArray("events").objects()
            cursor = result.optInt("next_cursor", cursor)
            val mergedEvents = (_state.value.runEvents + events)
                .distinctBy { "${it.optInt("cursor")}:${it.optString("type")}" }
            val runError = mergedEvents.lastOrNull { it.optString("type") == "error" }
            val completed = result.optBoolean("completed")
            _state.value = _state.value.copy(
                runEvents = mergedEvents,
                status = when {
                    runError != null -> text(R.string.status_run_failed)
                    completed -> text(R.string.status_run_completed)
                    else -> text(R.string.status_agent_running)
                },
            )
            if (completed) {
                _state.value = _state.value.copy(activeRunId = null)
                return
            }
        }
    }

    private fun updateLocalUserMessage(
        chatId: String,
        messageId: String,
        content: String,
        deliveryState: String,
    ) {
        val current = _state.value.selectedChat ?: return
        if (current.optString("id") != chatId) return
        val updated = JSONObject(current.toString())
        val messages = updated.optJSONArray("messages") ?: JSONArray().also {
            updated.put("messages", it)
        }
        messages.put(
            JSONObject()
                .put("id", messageId)
                .put("role", "user")
                .put("content", content)
                .put("createdAt", Instant.now().toString())
                .put("deliveryState", deliveryState),
        )
        updated.put("updatedAt", Instant.now().toString())
        updated.put("preview", content)
        _state.value = _state.value.copy(selectedChat = updated)
    }

    private fun markLocalUserMessageDelivery(
        chatId: String,
        messageId: String,
        deliveryState: String,
    ) {
        val current = _state.value.selectedChat ?: return
        if (current.optString("id") != chatId) return
        val updated = JSONObject(current.toString())
        val messages = updated.optJSONArray("messages") ?: return
        for (index in 0 until messages.length()) {
            val item = messages.optJSONObject(index) ?: continue
            if (item.optString("id") == messageId) {
                item.put("deliveryState", deliveryState)
                break
            }
        }
        _state.value = _state.value.copy(selectedChat = updated)
    }

    fun guideRun(message: String) {
        if (isLocalProject(_state.value.selectedProject)) {
            _state.value = _state.value.copy(error = text(R.string.local_agent_guidance_unsupported))
            return
        }
        projectCommand(text(R.string.status_guiding)) { peer, projectId ->
            val chat = requireNotNull(_state.value.selectedChat)
            client.command(
                peer, "runs.guide", projectId,
                JSONObject()
                    .put("chat_id", chat.getString("id"))
                    .put("message", message.trim())
                    .put("request_id", "guide_" + System.currentTimeMillis()),
            )
        }
    }

    fun interruptRun() {
        if (isLocalProject(_state.value.selectedProject)) {
            val runId = _state.value.activeRunId ?: return
            LocalAgentForegroundService.stopRun(getApplication(), runId)
            _state.value = _state.value.copy(status = text(R.string.status_stop_requested))
            return
        }
        projectCommand(text(R.string.status_stopping)) { peer, projectId ->
            val chat = requireNotNull(_state.value.selectedChat)
            client.command(
                peer, "runs.interrupt", projectId,
                JSONObject().put("chat_id", chat.getString("id")),
            )
            _state.value = _state.value.copy(
                activeRunId = null,
                status = text(R.string.status_stop_requested),
            )
        }
    }

    fun answerChat(questionId: String, answer: String) =
        projectCommand(text(R.string.status_answering)) { peer, projectId ->
            val chat = requireNotNull(_state.value.selectedChat)
            client.command(
                peer, "approvals.respond", projectId,
                JSONObject()
                    .put("chat_id", chat.getString("id"))
                    .put("question_id", questionId)
                    .put("answer", answer.trim())
                    .put("permission_mode", _state.value.permissionMode.approvalWireValue()),
                timeoutSeconds = 55,
            )
            _state.value = _state.value.copy(
                runEvents = _state.value.runEvents.filterNot {
                    it.optString("type") == "awaiting_user"
                },
            )
            loadChat(peer, projectId, chat, clearEvents = false)
        }

    fun createTask(title: String, goal: String) =
        projectCommand(text(R.string.status_creating_task)) { peer, projectId ->
        client.command(
            peer, "tasks.create", projectId,
            JSONObject()
                .put("title", title.trim().ifBlank { text(R.string.default_mobile_task) })
                .put("goal", goal.trim())
                .put("priority", "medium"),
        )
        reloadTasks(peer, projectId)
    }

    fun openTask(task: JSONObject) {
        val taskId = task.optString("id")
        val cached = cachedData.taskDetails[taskId]
        if (cached != null) {
            _state.value = _state.value.copy(
                selectedTask = cached,
                artifacts = cachedData.taskArtifacts[taskId].orEmpty(),
            )
            refreshTaskInBackground(task)
            return
        }
        projectCommand(text(R.string.status_loading_task)) { peer, projectId ->
            loadTask(peer, projectId, task)
        }
    }

    fun openTask(project: JSONObject, task: JSONObject) {
        selectProjectForSession(project)
        openTask(task)
    }

    private fun selectProjectForSession(project: JSONObject) {
        val projectId = project.optString("id")
        val data = cachedData.projectData[projectId] ?: CachedProjectData(
            chats = _state.value.projectChats[projectId].orEmpty(),
            tasks = _state.value.projectTasks[projectId].orEmpty(),
        )
        applyProjectData(project, data, resetSelection = true)
    }

    private suspend fun loadTask(peer: Peer, projectId: String, task: JSONObject) {
        val result = client.command(
            peer, "tasks.read", projectId,
            JSONObject().put("task_id", task.getString("id")),
        )
        val detail = result.optJSONObject("task") ?: task
        val artifacts = client.command(
            peer, "artifacts.list", projectId,
            JSONObject().put("task_id", detail.getString("id")),
        ).optJSONArray("artifacts").objects()
        val taskId = detail.getString("id")
        updateCache(peer.deviceId) { previous ->
            val projectData = previous.projectData[projectId] ?: CachedProjectData()
            previous.copy(
                projectData = previous.projectData + (
                    projectId to projectData.copy(
                        tasks = replaceSession(projectData.tasks, detail),
                    )
                ),
                taskDetails = previous.taskDetails + (taskId to detail),
                taskArtifacts = previous.taskArtifacts + (taskId to artifacts),
            )
        }
        val projectTasks = replaceSession(
            _state.value.projectTasks[projectId].orEmpty(),
            detail,
        )
        _state.value = _state.value.copy(
            selectedTask = detail,
            projectTasks = _state.value.projectTasks + (projectId to projectTasks),
            tasks = if (_state.value.selectedProject?.optString("id") == projectId) {
                projectTasks
            } else {
                _state.value.tasks
            },
            artifacts = artifacts,
        )
    }

    fun taskAction(
        command: String,
        message: String = "",
        attachments: List<PendingAttachment> = emptyList(),
    ) =
        projectCommand(text(R.string.status_task_action)) { peer, projectId ->
            val task = requireNotNull(_state.value.selectedTask)
            val payload = JSONObject().put("task_id", task.getString("id"))
            if (command == "tasks.dispatch") {
                payload.put("message", message.trim())
                    .put("permission_mode", _state.value.permissionMode.taskWireValue())
                    .put("attachments", encodeAttachments(attachments))
            }
            client.command(peer, command, projectId, payload, timeoutSeconds = 50)
            loadTask(peer, projectId, task)
        }

    private fun encodeAttachments(attachments: List<PendingAttachment>): JSONArray {
        val encodedAttachments = JSONArray()
        var totalAttachmentBytes = 0
        attachments.take(5).forEach { attachment ->
            val bytes = getApplication<Application>().contentResolver
                .openInputStream(attachment.uri)
                ?.use { input -> input.readBytes() }
                ?: throw IllegalStateException(
                    text(R.string.error_attachment_read, attachment.name)
                )
            require(bytes.size <= 8 * 1024 * 1024) {
                text(R.string.error_attachment_too_large, attachment.name)
            }
            totalAttachmentBytes += bytes.size
            require(totalAttachmentBytes <= 8 * 1024 * 1024) {
                text(R.string.error_attachments_too_large)
            }
            encodedAttachments.put(
                JSONObject()
                    .put("name", attachment.name)
                    .put(
                        "content_type",
                        getApplication<Application>().contentResolver
                            .getType(attachment.uri)
                            ?: "application/octet-stream",
                    )
                    .put("content_base64", Base64.getEncoder().encodeToString(bytes))
            )
        }
        return encodedAttachments
    }

    fun runTaskStep(stepId: String, message: String) =
        projectCommand(text(R.string.status_running_step)) { peer, projectId ->
            val task = requireNotNull(_state.value.selectedTask)
            client.command(
                peer, "tasks.run_step", projectId,
                JSONObject()
                    .put("task_id", task.getString("id"))
                    .put("step_id", stepId)
                    .put("message", message.trim().ifBlank { text(R.string.default_run_step_message) })
                    .put("permission_mode", _state.value.permissionMode.taskWireValue()),
                timeoutSeconds = 55,
            )
            loadTask(peer, projectId, task)
        }

    fun answerTask(questionId: String, answer: String) =
        projectCommand(text(R.string.status_answering_task)) { peer, projectId ->
            val task = requireNotNull(_state.value.selectedTask)
            client.command(
                peer, "approvals.respond", projectId,
                JSONObject()
                    .put("task_id", task.getString("id"))
                    .put("question_id", questionId)
                    .put("answer", answer.trim())
                    .put("permission_mode", _state.value.permissionMode.approvalWireValue()),
                timeoutSeconds = 55,
            )
            loadTask(peer, projectId, task)
        }

    fun downloadArtifact(artifact: JSONObject) =
        projectCommand(text(R.string.status_downloading, artifact.optString("name"))) { peer, projectId ->
            val taskId = requireNotNull(_state.value.selectedTask).getString("id")
            val artifactId = artifact.getString("id")
            val safeName = artifact.optString("name", "artifact")
                .replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "_")
                .take(160).ifBlank { "artifact" }
            val directory = File(getApplication<Application>().filesDir, "downloads")
                .apply { mkdirs() }
            val target = File(directory, "${artifactId.takeLast(10)}-$safeName")
            val part = File(directory, "${target.name}.part")
            var offset = part.takeIf(File::exists)?.length() ?: 0L
            var total = Long.MAX_VALUE
            FileOutputStream(part, true).use { output ->
                while (offset < total) {
                    val chunk = client.command(
                        peer, "artifacts.read", projectId,
                        JSONObject()
                            .put("task_id", taskId)
                            .put("artifact_id", artifactId)
                            .put("offset", offset)
                            .put("limit", 512 * 1024),
                        timeoutSeconds = 55,
                    )
                    val chunkOffset = chunk.getLong("offset")
                    val chunkSize = chunk.getInt("chunk_size")
                    val nextOffset = chunk.getLong("next_offset")
                    total = chunk.getLong("size")
                    require(chunkOffset == offset) { text(R.string.error_download_offset) }
                    val bytes = Base64.getDecoder().decode(chunk.getString("content_base64"))
                    require(bytes.size == chunkSize) { text(R.string.error_download_chunk_length) }
                    require(nextOffset == offset + chunkSize && nextOffset <= total) {
                        text(R.string.error_download_bounds)
                    }
                    output.write(bytes)
                    offset = nextOffset
                    _state.value = _state.value.copy(
                        downloadProgress = _state.value.downloadProgress +
                            (artifactId to if (total == 0L) 1f else offset.toFloat() / total),
                    )
                    if (chunk.optBoolean("eof")) {
                        require(offset == total) { text(R.string.error_download_end) }
                        break
                    }
                }
                output.fd.sync()
            }
            part.renameTo(target).also {
                require(it) { text(R.string.error_publish_download) }
            }
            _state.value = _state.value.copy(
                downloadedFiles = (_state.value.downloadedFiles + target.absolutePath)
                    .distinct(),
                status = text(R.string.status_file_saved),
            )
        }

    fun loadChangeDiff(changeSet: JSONObject, file: JSONObject) =
        projectCommand(text(R.string.status_loading_change)) { peer, projectId ->
            _state.value = _state.value.copy(
                changeDiffLoading = true,
                selectedChangeDiff = null,
            )
            try {
                val result = client.command(
                    peer,
                    "changes.read",
                    projectId,
                    JSONObject()
                        .put("chat_id", requireNotNull(_state.value.selectedChat).getString("id"))
                        .put("change_set_id", changeSet.getString("id"))
                        .put("file_path", file.getString("path")),
                )
                _state.value = _state.value.copy(
                    selectedChangeDiff = result.optJSONObject("change"),
                )
            } finally {
                _state.value = _state.value.copy(changeDiffLoading = false)
            }
        }

    fun previewChatAttachment(file: JSONObject) =
        projectCommand(text(R.string.status_loading_preview)) { peer, projectId ->
            val chatId = requireNotNull(_state.value.selectedChat).getString("id")
            val attachmentId = file.getString("id")
            val safeName = file.optString("name", "file")
                .replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "_")
                .take(160).ifBlank { "file" }
            val directory = File(getApplication<Application>().filesDir, "viewer")
                .apply { mkdirs() }
            val target = File(directory, "${attachmentId.takeLast(10)}-$safeName")
            val part = File(directory, "${target.name}.part")
            _state.value = _state.value.copy(
                viewerFile = file,
                viewerFilePath = null,
                viewerMimeType = file.optString("content_type"),
                viewerLoading = true,
            )
            try {
                var offset = 0L
                var total = Long.MAX_VALUE
                var mediaType = file.optString("content_type")
                FileOutputStream(part, false).use { output ->
                    while (offset < total) {
                        val chunk = client.command(
                            peer,
                            "attachments.read",
                            projectId,
                            JSONObject()
                                .put("chat_id", chatId)
                                .put("attachment_id", attachmentId)
                                .put("offset", offset)
                                .put("limit", 512 * 1024),
                            timeoutSeconds = 55,
                        )
                        val chunkOffset = chunk.getLong("offset")
                        val chunkSize = chunk.getInt("chunk_size")
                        val nextOffset = chunk.getLong("next_offset")
                        total = chunk.getLong("size")
                        require(chunkOffset == offset) { text(R.string.error_download_offset) }
                        val bytes = Base64.getDecoder().decode(chunk.getString("content_base64"))
                        require(bytes.size == chunkSize) { text(R.string.error_download_chunk_length) }
                        require(nextOffset == offset + chunkSize && nextOffset <= total) {
                            text(R.string.error_download_bounds)
                        }
                        output.write(bytes)
                        offset = nextOffset
                        mediaType = chunk.optString("media_type", mediaType)
                        if (chunk.optBoolean("eof")) break
                    }
                    output.fd.sync()
                }
                if (target.exists()) target.delete()
                require(part.renameTo(target)) { text(R.string.error_publish_download) }
                _state.value = _state.value.copy(
                    viewerFilePath = target.absolutePath,
                    viewerMimeType = mediaType,
                )
            } finally {
                _state.value = _state.value.copy(viewerLoading = false)
            }
        }

    fun loadInlineChatImage(file: JSONObject) = loadChatImage(file, thumbnail = true)

    fun loadFullChatImage(file: JSONObject) = loadChatImage(file, thumbnail = false)

    private fun loadChatImage(file: JSONObject, thumbnail: Boolean) {
        val attachmentId = file.optString("id")
        val peer = _state.value.peer ?: return
        val projectId = _state.value.selectedProject?.optString("id").orEmpty()
        val chatId = _state.value.selectedChat?.optString("id").orEmpty()
        val cacheKey = "$chatId::$attachmentId"
        val paths = if (thumbnail) {
            _state.value.inlineAttachmentPaths
        } else {
            _state.value.fullImagePaths
        }
        val loading = if (thumbnail) {
            _state.value.inlineAttachmentLoading
        } else {
            _state.value.fullImageLoading
        }
        if (
            attachmentId.isBlank() || projectId.isBlank() || chatId.isBlank() ||
            cacheKey in loading ||
            paths[cacheKey]?.let(::File)?.exists() == true
        ) {
            return
        }
        _state.value = if (thumbnail) {
            _state.value.copy(
                inlineAttachmentLoading =
                    _state.value.inlineAttachmentLoading + cacheKey,
                inlineAttachmentErrors =
                    _state.value.inlineAttachmentErrors - cacheKey,
            )
        } else {
            _state.value.copy(
                fullImageLoading = _state.value.fullImageLoading + cacheKey,
                fullImageErrors = _state.value.fullImageErrors - cacheKey,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val safeName = file.optString("name", "image")
                .replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "_")
                .take(160).ifBlank { "image" }
            val cacheRoot = File(
                getApplication<Application>().filesDir,
                if (thumbnail) "inline-thumbnails" else "inline-images",
            )
            val safePeerId = peer.deviceId.replace(Regex("[^A-Za-z0-9_-]"), "_")
                .take(80).ifBlank { "peer" }
            val safeChatId = chatId.replace(Regex("[^A-Za-z0-9_-]"), "_")
                .take(80).ifBlank { "chat" }
            val directory = File(File(cacheRoot, safePeerId), safeChatId).apply { mkdirs() }
            val target = File(
                directory,
                "${attachmentId.takeLast(18)}-${if (thumbnail) "thumb-" else ""}$safeName",
            )
            runCatching {
                if (!target.exists() || target.length() == 0L) {
                    downloadChatAttachment(
                        peer = peer,
                        projectId = projectId,
                        chatId = chatId,
                        attachmentId = attachmentId,
                        target = target,
                        variant = if (thumbnail) "thumbnail" else "original",
                    )
                }
                require(target.length() > 0L) { text(R.string.error_publish_download) }
                target.absolutePath
            }.onSuccess { path ->
                _state.value = if (thumbnail) {
                    _state.value.copy(
                        inlineAttachmentPaths =
                            _state.value.inlineAttachmentPaths + (cacheKey to path),
                        inlineAttachmentLoading =
                            _state.value.inlineAttachmentLoading - cacheKey,
                        inlineAttachmentErrors =
                            _state.value.inlineAttachmentErrors - cacheKey,
                    )
                } else {
                    _state.value.copy(
                        fullImagePaths =
                            _state.value.fullImagePaths + (cacheKey to path),
                        fullImageLoading =
                            _state.value.fullImageLoading - cacheKey,
                        fullImageErrors =
                            _state.value.fullImageErrors - cacheKey,
                    )
                }
            }.onFailure {
                _state.value = if (thumbnail) {
                    _state.value.copy(
                        inlineAttachmentLoading =
                            _state.value.inlineAttachmentLoading - cacheKey,
                        inlineAttachmentErrors =
                            _state.value.inlineAttachmentErrors + cacheKey,
                    )
                } else {
                    _state.value.copy(
                        fullImageLoading =
                            _state.value.fullImageLoading - cacheKey,
                        fullImageErrors =
                            _state.value.fullImageErrors + cacheKey,
                    )
                }
            }
        }
    }

    private suspend fun downloadChatAttachment(
        peer: Peer,
        projectId: String,
        chatId: String,
        attachmentId: String,
        target: File,
        variant: String,
    ) {
        val part = File(target.parentFile, "${target.name}.part")
        var offset = 0L
        var total = Long.MAX_VALUE
        FileOutputStream(part, false).use { output ->
            while (offset < total) {
                val chunk = client.command(
                    peer,
                    "attachments.read",
                    projectId,
                    JSONObject()
                        .put("chat_id", chatId)
                        .put("attachment_id", attachmentId)
                        .put("variant", variant)
                        .put("offset", offset)
                        .put("limit", 512 * 1024),
                    timeoutSeconds = 55,
                )
                val chunkOffset = chunk.getLong("offset")
                val chunkSize = chunk.getInt("chunk_size")
                val nextOffset = chunk.getLong("next_offset")
                total = chunk.getLong("size")
                require(chunkOffset == offset) { text(R.string.error_download_offset) }
                val bytes = Base64.getDecoder().decode(chunk.getString("content_base64"))
                require(bytes.size == chunkSize) { text(R.string.error_download_chunk_length) }
                require(nextOffset == offset + chunkSize && nextOffset <= total) {
                    text(R.string.error_download_bounds)
                }
                output.write(bytes)
                offset = nextOffset
                if (chunk.optBoolean("eof")) {
                    require(offset == total) { text(R.string.error_download_end) }
                    break
                }
            }
            output.fd.sync()
        }
        if (target.exists()) target.delete()
        require(part.renameTo(target)) { text(R.string.error_publish_download) }
    }

    fun clearViewer() {
        _state.value = _state.value.copy(
            viewerFile = null,
            viewerFilePath = null,
            viewerMimeType = null,
            viewerLoading = false,
        )
    }

    fun ensureTerminalShell(force: Boolean = false) {
        val peer = _state.value.peer ?: return
        val project = _state.value.selectedProject ?: return
        val projectId = project.optString("id")
        if (projectId.isBlank() || _state.value.terminalBusy) return
        if (
            !force &&
            terminalProjectId == projectId &&
            _state.value.terminalSessionId != null &&
            _state.value.terminalSessionStatus == "running"
        ) {
            startTerminalPolling(peer, projectId)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                terminalBusy = true,
                terminalSessionStatus = "connecting",
                error = null,
                status = text(R.string.status_terminal_connecting),
            )
            runCatching {
                closeCurrentTerminalBestEffort()
                terminalProjectId = projectId
                terminalCursor = 0
                terminalCommandSent = false
                terminalOutputStarted = false
                _state.value = _state.value.copy(
                    terminalLines = emptyList(),
                    terminalSessionId = null,
                )
                val result = client.command(
                    peer,
                    "shell.open",
                    projectId,
                    timeoutSeconds = 20,
                )
                terminalPrompt = result.optString("prompt", "$")
                _state.value = _state.value.copy(
                    terminalLines = emptyList(),
                    terminalPrompt = terminalPrompt,
                    terminalCwd = result.optString("cwd", "."),
                    terminalSessionId = result.getString("shell_id"),
                    terminalSessionStatus = result.optString("status", "running"),
                    status = text(R.string.status_terminal_connected),
                )
                applyTerminalSnapshot(result)
                startTerminalPolling(peer, projectId)
            }.onFailure { error ->
                appendTerminalError(error)
                _state.value = _state.value.copy(
                    terminalSessionId = null,
                    terminalSessionStatus = "error",
                    error = error.message ?: text(R.string.error_remote_execution),
                    status = text(R.string.status_terminal_failed),
                )
            }
            _state.value = _state.value.copy(terminalBusy = false)
        }
    }

    fun sendTerminalCommand(input: String) {
        val command = input.trimEnd()
        if (command.isBlank() || _state.value.terminalBusy) return
        if (command.equals("clear", ignoreCase = true)) {
            _state.value = _state.value.copy(
                terminalLines = emptyList(),
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                terminalBusy = true,
                error = null,
                status = text(R.string.status_terminal_sending),
            )
            runCatching {
                val peer = requirePeer()
                val project = requireNotNull(_state.value.selectedProject) {
                    text(R.string.error_select_project_first)
                }
                val projectId = project.getString("id")
                require(terminalProjectId == projectId) {
                    text(R.string.error_terminal_not_connected)
                }
                val shellId = requireNotNull(_state.value.terminalSessionId) {
                    text(R.string.error_terminal_not_connected)
                }
                terminalCommandSent = true
                val result = client.command(
                    peer,
                    "shell.write",
                    projectId,
                    JSONObject()
                        .put("shell_id", shellId)
                        .put("input", command)
                        .put("cursor", terminalCursor)
                        .put("wait_ms", 300),
                    timeoutSeconds = 20,
                )
                applyTerminalSnapshot(result)
                _state.value = _state.value.copy(
                    terminalSessionStatus = result.optString("status", "running"),
                    status = text(R.string.status_terminal_connected),
                )
            }.onFailure { error ->
                appendTerminalError(error)
                _state.value = _state.value.copy(
                    error = error.message ?: text(R.string.error_remote_execution),
                    terminalSessionStatus = "error",
                    status = text(R.string.status_terminal_failed),
                )
            }
            _state.value = _state.value.copy(terminalBusy = false)
        }
    }

    fun clearTerminalOutput() {
        _state.value = _state.value.copy(terminalLines = emptyList())
    }

    fun interruptTerminal() {
        val peer = _state.value.peer ?: return
        val projectId = _state.value.selectedProject?.optString("id").orEmpty()
        val shellId = _state.value.terminalSessionId ?: return
        if (
            projectId.isBlank() ||
            terminalProjectId != projectId ||
            _state.value.terminalSessionStatus != "running"
        ) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val result = client.command(
                    peer,
                    "shell.interrupt",
                    projectId,
                    JSONObject()
                        .put("shell_id", shellId)
                        .put("cursor", terminalCursor),
                    timeoutSeconds = 12,
                )
                applyTerminalSnapshot(result)
                _state.value = _state.value.copy(
                    terminalSessionStatus = result.optString("status", "running"),
                    status = text(R.string.status_terminal_connected),
                )
            }.onFailure { error ->
                appendTerminalError(error)
                _state.value = _state.value.copy(
                    error = error.message ?: text(R.string.error_remote_execution),
                    status = text(R.string.status_terminal_failed),
                )
            }
        }
    }

    private fun startTerminalPolling(peer: Peer, projectId: String) {
        if (terminalPollJob?.isActive == true) return
        terminalPollJob = viewModelScope.launch(Dispatchers.IO) {
            var consecutiveFailures = 0
            while (isActive) {
                delay(800)
                val shellId = _state.value.terminalSessionId ?: return@launch
                if (terminalProjectId != projectId) return@launch
                val result = try {
                    client.command(
                        peer,
                        "shell.read",
                        projectId,
                        JSONObject()
                            .put("shell_id", shellId)
                            .put("cursor", terminalCursor),
                        timeoutSeconds = 12,
                    )
                } catch (_: Exception) {
                    consecutiveFailures += 1
                    if (consecutiveFailures >= 3) {
                        _state.value = _state.value.copy(
                            terminalSessionStatus = "error",
                            status = text(R.string.status_terminal_failed),
                        )
                    }
                    delay(minOf(8_000L, consecutiveFailures * 1_000L))
                    null
                }
                if (result == null) continue
                consecutiveFailures = 0
                applyTerminalSnapshot(result)
                val status = result.optString("status", "running")
                _state.value = _state.value.copy(
                    terminalSessionStatus = status,
                    status = if (status == "running") {
                        text(R.string.status_terminal_connected)
                    } else {
                        text(R.string.status_terminal_closed)
                    },
                )
                if (status != "running") return@launch
            }
        }
    }

    private fun applyTerminalSnapshot(result: JSONObject) {
        val currentCursor = terminalCursor
        val freshLines = result.optJSONArray("lines").objects()
            .filter { it.optInt("seq") > currentCursor }
            .sortedBy { it.optInt("seq") }
        terminalCursor = maxOf(terminalCursor, result.optInt("next_cursor", terminalCursor))
        val visibleLines = if (!terminalCommandSent) {
            emptyList()
        } else if (!terminalOutputStarted) {
            val promptIndex = freshLines.indexOfFirst { it.optString("kind") == "prompt" }
            if (promptIndex < 0) {
                emptyList()
            } else {
                terminalOutputStarted = true
                freshLines.drop(promptIndex)
            }
        } else {
            freshLines
        }
        val additions = visibleLines.map { line ->
            if (line.optString("kind") == "prompt") {
                "$terminalPrompt ${line.optString("text").removePrefix("$ ")}"
            } else {
                line.optString("text")
            }
        }.map(::cleanTerminalText)
        _state.value = _state.value.copy(
            terminalCwd = result.optString("cwd", _state.value.terminalCwd),
        )
        if (terminalCommandSent && additions.isNotEmpty()) {
            _state.value = _state.value.copy(
                terminalLines = (_state.value.terminalLines + additions).takeLast(500),
            )
        }
    }

    private fun cleanTerminalText(value: String): String =
        value
            .replace(Regex("\\x1B\\[[0-?]*[ -/]*[@-~]"), "")
            .replace('\r', ' ')
            .trimEnd()

    private fun appendTerminalError(error: Throwable) {
        val message = "[${text(R.string.terminal_error)}] " +
            (error.message ?: text(R.string.error_remote_execution))
        _state.value = _state.value.copy(
            terminalLines = (_state.value.terminalLines + message).takeLast(500),
        )
    }

    private suspend fun closeCurrentTerminalBestEffort() {
        terminalPollJob?.cancel()
        terminalPollJob = null
        val peer = _state.value.peer
        val shellId = _state.value.terminalSessionId
        if (peer != null && shellId != null && terminalProjectId.isNotBlank()) {
            runCatching {
                client.command(
                    peer,
                    "shell.close",
                    terminalProjectId,
                    JSONObject()
                        .put("shell_id", shellId)
                        .put("cursor", terminalCursor),
                    timeoutSeconds = 10,
                )
            }
        }
        terminalCursor = 0
        terminalCommandSent = false
        terminalOutputStarted = false
        terminalPrompt = "$"
        _state.value = _state.value.copy(
            terminalSessionId = null,
            terminalSessionStatus = "disconnected",
            terminalPrompt = "$",
            terminalCwd = ".",
        )
    }

    private suspend fun reloadChats(peer: Peer, projectId: String) {
        val chats = client.command(peer, "chats.list", projectId)
            .optJSONArray("chats").objects()
        updateCache(peer.deviceId) { previous ->
            val existing = previous.projectData[projectId] ?: CachedProjectData()
            previous.copy(
                projectData = previous.projectData + (projectId to existing.copy(chats = chats)),
            )
        }
        _state.value = _state.value.copy(
            projectChats = _state.value.projectChats + (projectId to chats),
            chats = if (_state.value.selectedProject?.optString("id") == projectId) {
                chats
            } else {
                _state.value.chats
            },
        )
    }

    private suspend fun loadChat(
        peer: Peer,
        projectId: String,
        chat: JSONObject,
        clearEvents: Boolean,
    ) {
        val result = client.command(
            peer, "chats.read", projectId,
            JSONObject().put("chat_id", chat.getString("id")),
        )
        val detail = result.optJSONObject("chat") ?: chat
        val chatId = detail.optString("id").ifBlank { chat.getString("id") }
        updateCache(peer.deviceId) { previous ->
            val projectData = previous.projectData[projectId] ?: CachedProjectData()
            previous.copy(
                projectData = previous.projectData + (
                    projectId to projectData.copy(
                        chats = replaceSession(projectData.chats, detail),
                    )
                ),
                chatDetails = previous.chatDetails + (chatId to detail),
            )
        }
        val projectChats = replaceSession(
            _state.value.projectChats[projectId].orEmpty(),
            detail,
        )
        _state.value = _state.value.copy(
            selectedChat = detail,
            projectChats = _state.value.projectChats + (projectId to projectChats),
            chats = if (_state.value.selectedProject?.optString("id") == projectId) {
                projectChats
            } else {
                _state.value.chats
            },
            runEvents = if (clearEvents) emptyList() else _state.value.runEvents,
        )
    }

    private suspend fun reloadTasks(peer: Peer, projectId: String) {
        val tasks = client.command(peer, "tasks.list", projectId)
            .optJSONArray("tasks").objects()
        updateCache(peer.deviceId) { previous ->
            val existing = previous.projectData[projectId] ?: CachedProjectData()
            previous.copy(
                projectData = previous.projectData + (projectId to existing.copy(tasks = tasks)),
            )
        }
        _state.value = _state.value.copy(
            projectTasks = _state.value.projectTasks + (projectId to tasks),
            tasks = if (_state.value.selectedProject?.optString("id") == projectId) {
                tasks
            } else {
                _state.value.tasks
            },
        )
    }

    private fun refreshChatInBackground(chat: JSONObject) {
        val peer = _state.value.peer ?: return
        val projectId = _state.value.selectedProject?.optString("id").orEmpty()
        if (projectId.isBlank()) return
        launchTrackedRefresh(peer) {
            loadChat(peer, projectId, chat, clearEvents = false)
        }
    }

    private fun refreshTaskInBackground(task: JSONObject) {
        val peer = _state.value.peer ?: return
        val projectId = _state.value.selectedProject?.optString("id").orEmpty()
        if (projectId.isBlank()) return
        launchTrackedRefresh(peer) {
            loadTask(peer, projectId, task)
        }
    }

    private fun launchTrackedRefresh(peer: Peer, block: suspend () -> Unit) {
        if (backgroundSyncJob?.isActive == true) {
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { block() }
            }
            return
        }
        backgroundSyncJob = viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                backgroundSyncing = true,
                backgroundSyncProgress = 0f,
            )
            try {
                block()
                setBackgroundProgress(1f)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Cached data remains visible when a silent refresh cannot complete.
            } finally {
                if (_state.value.peer?.deviceId == peer.deviceId) {
                    _state.value = _state.value.copy(backgroundSyncing = false)
                }
            }
        }
    }

    private fun applyCachedSnapshot(snapshot: DesktopDataSnapshot) {
        val remoteProjects = snapshot.projects
        if (remoteProjects.isEmpty()) return
        val projects = withLocalProject(remoteProjects)
        val currentProjectId = _state.value.selectedProject?.optString("id").orEmpty()
        val selectedProject = projects.firstOrNull {
            it.optString("id") == currentProjectId
        } ?: remoteProjects.first()
        val projectData = snapshot.projectData[selectedProject.optString("id")]
            ?: CachedProjectData()
        val localChats = _state.value.projectChats[LOCAL_PROJECT_ID].orEmpty()
        _state.value = _state.value.copy(
            projects = projects,
            selectedProject = selectedProject,
            projectChats = snapshot.projectData.mapValues { it.value.chats } +
                (LOCAL_PROJECT_ID to localChats),
            projectTasks = snapshot.projectData.mapValues { it.value.tasks } +
                (LOCAL_PROJECT_ID to emptyList()),
            chats = if (isLocalProject(selectedProject)) localChats else projectData.chats,
            tasks = if (isLocalProject(selectedProject)) emptyList() else projectData.tasks,
            status = text(R.string.status_cached_projects, remoteProjects.size),
        )
    }

    private fun startBackgroundSync(peer: Peer) {
        backgroundSyncJob?.cancel()
        backgroundSyncJob = viewModelScope.launch(Dispatchers.IO) {
            val projects = _state.value.projects.filterNot(::isLocalProject)
            if (projects.isEmpty()) {
                _state.value = _state.value.copy(
                    backgroundSyncing = false,
                    backgroundSyncProgress = 1f,
                )
                return@launch
            }
            _state.value = _state.value.copy(
                backgroundSyncing = true,
                backgroundSyncProgress = 0f,
            )
            try {
                val indexedProjects = mutableListOf<Pair<JSONObject, CachedProjectData>>()
                projects.forEachIndexed { index, project ->
                    ensureCurrentPeer(peer)
                    val projectId = project.getString("id")
                    val previous = cachedData.projectData[projectId] ?: CachedProjectData()
                    val chats = fetchOrPrevious(previous.chats) {
                        client.command(peer, "chats.list", projectId)
                            .optJSONArray("chats").objects()
                    }
                    val tasks = fetchOrPrevious(previous.tasks) {
                        client.command(peer, "tasks.list", projectId)
                            .optJSONArray("tasks").objects()
                    }
                    val data = CachedProjectData(chats, tasks)
                    indexedProjects += project to data
                    updateCache(peer.deviceId) { snapshot ->
                        snapshot.copy(
                            projects = projects,
                            projectData = snapshot.projectData + (projectId to data),
                        )
                    }
                    publishProjectLists(project, data)
                    setBackgroundProgress(0.2f * (index + 1) / projects.size)
                }

                val totalDetails = indexedProjects.sumOf { (_, data) ->
                    data.chats.size + data.tasks.size * 2
                }
                var completedDetails = 0
                val progressMutex = Mutex()
                val requestSemaphore = Semaphore(4)
                suspend fun completeDetailStep() {
                    progressMutex.withLock {
                        completedDetails += 1
                        val detailFraction = if (totalDetails == 0) {
                            1f
                        } else {
                            completedDetails.toFloat() / totalDetails
                        }
                        setBackgroundProgress(0.2f + 0.8f * detailFraction)
                    }
                }

                indexedProjects.forEach { (project, data) ->
                    ensureCurrentPeer(peer)
                    val projectId = project.getString("id")
                    coroutineScope {
                        val chatJobs = data.chats.map { chat ->
                            async {
                                requestSemaphore.withPermit {
                                    val chatId = chat.getString("id")
                                    val detail = fetchOrPrevious(
                                        cachedData.chatDetails[chatId] ?: chat,
                                    ) {
                                        client.command(
                                            peer,
                                            "chats.read",
                                            projectId,
                                            JSONObject().put("chat_id", chatId),
                                        ).optJSONObject("chat") ?: chat
                                    }
                                    updateCache(peer.deviceId, persist = false) { snapshot ->
                                        snapshot.copy(
                                            chatDetails = snapshot.chatDetails + (chatId to detail),
                                        )
                                    }
                                    publishChatIfSelected(detail)
                                    completeDetailStep()
                                }
                            }
                        }
                        val taskJobs = data.tasks.map { task ->
                            async {
                                requestSemaphore.withPermit {
                                    val taskId = task.getString("id")
                                    val detail = fetchOrPrevious(
                                        cachedData.taskDetails[taskId] ?: task,
                                    ) {
                                        client.command(
                                            peer,
                                            "tasks.read",
                                            projectId,
                                            JSONObject().put("task_id", taskId),
                                        ).optJSONObject("task") ?: task
                                    }
                                    updateCache(peer.deviceId, persist = false) { snapshot ->
                                        snapshot.copy(
                                            taskDetails = snapshot.taskDetails + (taskId to detail),
                                        )
                                    }
                                    publishTaskIfSelected(detail, null)
                                    completeDetailStep()

                                    val artifacts = fetchOrPrevious(
                                        cachedData.taskArtifacts[taskId].orEmpty(),
                                    ) {
                                        client.command(
                                            peer,
                                            "artifacts.list",
                                            projectId,
                                            JSONObject().put("task_id", taskId),
                                        ).optJSONArray("artifacts").objects()
                                    }
                                    updateCache(peer.deviceId, persist = false) { snapshot ->
                                        snapshot.copy(
                                            taskArtifacts = snapshot.taskArtifacts +
                                                (taskId to artifacts),
                                        )
                                    }
                                    publishTaskIfSelected(detail, artifacts)
                                    completeDetailStep()
                                }
                            }
                        }
                        (chatJobs + taskJobs).awaitAll()
                    }
                    persistCache(peer.deviceId)
                }
                setBackgroundProgress(1f)
            } finally {
                if (_state.value.peer?.deviceId == peer.deviceId) {
                    _state.value = _state.value.copy(backgroundSyncing = false)
                }
            }
        }
    }

    private suspend fun <T> fetchOrPrevious(previous: T, block: suspend () -> T): T =
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            previous
        }

    private fun ensureCurrentPeer(peer: Peer) {
        if (_state.value.peer?.deviceId != peer.deviceId) throw CancellationException()
    }

    private fun setBackgroundProgress(value: Float) {
        _state.value = _state.value.copy(backgroundSyncProgress = value.coerceIn(0f, 1f))
    }

    private fun publishProjectLists(
        project: JSONObject,
        data: CachedProjectData,
    ) {
        val projectId = project.optString("id")
        _state.value = _state.value.copy(
            projectChats = _state.value.projectChats + (projectId to data.chats),
            projectTasks = _state.value.projectTasks + (projectId to data.tasks),
        )
        if (_state.value.selectedProject?.optString("id") == projectId) {
            applyProjectData(project, data, resetSelection = false)
        }
    }

    private fun publishChatIfSelected(detail: JSONObject) {
        if (_state.value.selectedChat?.optString("id") == detail.optString("id")) {
            _state.value = _state.value.copy(selectedChat = detail)
        }
    }

    private fun publishTaskIfSelected(detail: JSONObject, artifacts: List<JSONObject>?) {
        if (_state.value.selectedTask?.optString("id") == detail.optString("id")) {
            _state.value = _state.value.copy(
                selectedTask = detail,
                artifacts = artifacts ?: _state.value.artifacts,
            )
        }
    }

    private suspend fun updateCache(
        peerId: String,
        persist: Boolean = true,
        transform: (DesktopDataSnapshot) -> DesktopDataSnapshot,
    ) {
        cacheMutex.withLock {
            if (_state.value.peer?.deviceId != peerId) return
            cachedData = transform(cachedData)
            if (persist) desktopDataCache.save(peerId, cachedData)
        }
    }

    private suspend fun persistCache(peerId: String) {
        cacheMutex.withLock {
            if (_state.value.peer?.deviceId == peerId) {
                desktopDataCache.save(peerId, cachedData)
            }
        }
    }

    private fun requirePeer(): Peer = requireNotNull(_state.value.peer) {
        text(R.string.error_pair_device_first)
    }

    private fun projectCommand(
        label: String,
        block: suspend (Peer, String) -> Unit,
    ) = launchBusy(label) {
        val projectId = requireNotNull(_state.value.selectedProject) {
            text(R.string.error_select_project_first)
        }
            .getString("id")
        block(requirePeer(), projectId)
    }

    private fun launchBusy(label: String, block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null, status = label)
            runCatching { block() }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        error = error.message ?: text(R.string.error_generic),
                        status = text(R.string.status_need_attention),
                    )
                }
            _state.value = _state.value.copy(busy = false)
        }
    }

    override fun onCleared() {
        deviceLoadJob?.cancel()
        backgroundSyncJob?.cancel()
        terminalPollJob?.cancel()
        openAiOAuthPollJob?.cancel()
        client.close()
        super.onCleared()
    }
}

private fun mergeLocalModelSecrets(incoming: JSONObject, previous: JSONObject?): JSONObject {
    val merged = JSONObject(incoming.toString())
    fun mergeArray(key: String) {
        val next = merged.optJSONArray(key) ?: return
        val old = previous?.optJSONArray(key)
        for (index in 0 until next.length()) {
            val candidate = next.optJSONObject(index) ?: continue
            if (candidate.optString("api_key").isNotBlank()) continue
            val id = candidate.optString("id")
            val model = candidate.optString("model")
            val prior = (0 until (old?.length() ?: 0)).mapNotNull { old?.optJSONObject(it) }
                .firstOrNull {
                    (id.isNotBlank() && it.optString("id") == id) ||
                        (model.isNotBlank() && it.optString("model") == model)
                }
            prior?.optString("api_key")?.takeIf(String::isNotBlank)?.let {
                candidate.put("api_key", it)
            }
            candidate.remove("api_key_configured")
        }
    }
    mergeArray("custom_models")
    mergeArray("vision_models")
    val secondary = merged.optJSONObject("secondary_model")
    if (secondary != null && secondary.optString("api_key").isBlank()) {
        previous?.optJSONObject("secondary_model")?.optString("api_key")
            ?.takeIf(String::isNotBlank)?.let { secondary.put("api_key", it) }
        secondary.remove("api_key_configured")
    }
    return merged
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun replaceSession(
    sessions: List<JSONObject>,
    replacement: JSONObject,
): List<JSONObject> {
    val replacementId = replacement.optString("id")
    if (replacementId.isBlank()) return sessions
    var replaced = false
    val updated = sessions.map { session ->
        if (session.optString("id") == replacementId) {
            replaced = true
            replacement
        } else {
            session
        }
    }
    return if (replaced) updated else updated + replacement
}
