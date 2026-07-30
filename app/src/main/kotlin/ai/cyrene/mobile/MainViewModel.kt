package ai.cyrene.mobile

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.cyrene.mobile.data.CachedProjectData
import ai.cyrene.mobile.data.DesktopDataCache
import ai.cyrene.mobile.data.DesktopDataSnapshot
import ai.cyrene.mobile.data.SecureStore
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

data class MobileUiState(
    val peer: Peer? = null,
    val peers: List<Peer> = emptyList(),
    val pairingOffer: PairingOffer? = null,
    val busy: Boolean = false,
    val creatingChat: Boolean = false,
    val backgroundSyncing: Boolean = false,
    val backgroundSyncProgress: Float = 0f,
    val error: String? = null,
    val status: String = "",
    val projects: List<JSONObject> = emptyList(),
    val selectedProject: JSONObject? = null,
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
    val desktopSettings: JSONObject? = null,
    val desktopSettingsSchema: JSONObject? = null,
    val desktopModels: JSONObject? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SecureStore(application)
    private val desktopDataCache = DesktopDataCache(application)
    private val identity = store.identity()
    private val client = CyreneClient(
        identity,
        "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
        application,
    )
    private fun text(id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

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
    )

    private val _state = MutableStateFlow(initialState(store.peer()))
    val state = _state.asStateFlow()
    private var terminalPollJob: Job? = null
    private var terminalProjectId = ""
    private var terminalCursor = 0
    private var terminalCommandSent = false
    private var terminalOutputStarted = false
    private var terminalPrompt = "$"
    private var deviceLoadJob: Job? = null
    private var backgroundSyncJob: Job? = null
    private val cacheMutex = Mutex()
    @Volatile
    private var cachedData = DesktopDataSnapshot()

    init {
        _state.value.peer?.let(::restoreCacheAndRefresh)
    }

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
                error = null,
                status = text(R.string.status_syncing_projects),
            )
            runCatching { loadProjects(peer) }
                .onSuccess { startBackgroundSync(peer) }
                .onFailure { error ->
                    if (error !is CancellationException &&
                        _state.value.peer?.deviceId == peer.deviceId
                    ) {
                        _state.value = _state.value.copy(
                            error = error.message ?: text(R.string.error_generic),
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
                client.command(requirePeer(), "settings.read")
            }.onSuccess { result ->
                _state.value = _state.value.copy(
                    desktopSettings = result.optJSONObject("settings") ?: JSONObject(),
                    desktopSettingsSchema = result.optJSONObject("schema"),
                    desktopModels = result.optJSONObject("models"),
                    status = text(R.string.status_settings_loaded),
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    desktopSettings = null,
                    desktopSettingsSchema = null,
                    desktopModels = null,
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
            val result = client.command(
                requirePeer(),
                "settings.update",
                payload = JSONObject().put("models", models),
            )
            _state.value = _state.value.copy(
                desktopSettings = result.optJSONObject("settings")
                    ?: _state.value.desktopSettings,
                desktopSettingsSchema = result.optJSONObject("schema")
                    ?: _state.value.desktopSettingsSchema,
                desktopModels = result.optJSONObject("models")
                    ?: _state.value.desktopModels,
                status = text(R.string.status_models_saved),
            )
        }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    fun refreshProjects() = launchBusy(text(R.string.status_syncing_projects)) {
        val peer = requirePeer()
        loadProjects(peer)
        startBackgroundSync(peer)
    }

    private suspend fun loadProjects(peer: Peer) {
        val result = client.command(peer, "projects.list")
        ensureCurrentPeer(peer)
        val projects = result.optJSONArray("projects").objects()
        updateCache(peer.deviceId) { previous -> previous.copy(projects = projects) }
        val previousProjectId = _state.value.selectedProject?.optString("id").orEmpty()
        val selectedProject = projects.firstOrNull {
            it.optString("id") == previousProjectId
        } ?: projects.firstOrNull()
        val projectChanged = selectedProject?.optString("id").orEmpty() != previousProjectId
        _state.value = _state.value.copy(
            projects = projects,
            selectedProject = selectedProject,
            selectedChat = if (projectChanged) null else _state.value.selectedChat,
            selectedTask = if (projectChanged) null else _state.value.selectedTask,
            chats = if (selectedProject == null) emptyList() else _state.value.chats,
            tasks = if (selectedProject == null) emptyList() else _state.value.tasks,
            status = text(R.string.status_online_projects, projects.size),
        )
        if (selectedProject != null) {
            loadProjectContent(peer, selectedProject, resetSelection = projectChanged)
        }
    }

    fun selectProject(project: JSONObject) {
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
        val peer = _state.value.peer ?: return
        launchTrackedRefresh(peer) {
            loadProjectContent(peer, project, resetSelection = false)
        }
    }

    fun showChatList() {
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
        planMode: Boolean,
        attachments: List<PendingAttachment> = emptyList(),
    ) {
        if (_state.value.creatingChat) return
        val content = message.trim()
        if (content.isBlank()) return
        val peer = _state.value.peer ?: return
        val projectId = _state.value.selectedProject?.optString("id").orEmpty()
        if (projectId.isBlank()) return
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
                    planMode = planMode,
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
    }

    fun renameChat(chat: JSONObject, requestedTitle: String) {
        val peer = _state.value.peer ?: return
        val projectId = _state.value.selectedProject?.optString("id").orEmpty()
        val chatId = chat.optString("id")
        val title = requestedTitle.trim()
        if (projectId.isBlank() || chatId.isBlank() || title.isBlank()) return
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

    fun deleteChat(chat: JSONObject) {
        val peer = _state.value.peer ?: return
        val projectId = _state.value.selectedProject?.optString("id").orEmpty()
        val chatId = chat.optString("id")
        if (projectId.isBlank() || chatId.isBlank()) return
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
                if (_state.value.selectedChat?.optString("id") == chatId) {
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

    fun sendMessage(
        message: String,
        planMode: Boolean,
        attachments: List<PendingAttachment> = emptyList(),
    ) {
        val content = message.trim()
        val chat = _state.value.selectedChat ?: return
        val chatId = chat.optString("id")
        if (chatId.isBlank()) return
        projectCommand(text(R.string.status_sending_agent)) { peer, projectId ->
            sendMessageCommand(
                peer = peer,
                projectId = projectId,
                chat = chat,
                content = content,
                planMode = planMode,
                attachments = attachments,
            )
        }
    }

    private suspend fun sendMessageCommand(
        peer: Peer,
        projectId: String,
        chat: JSONObject,
        content: String,
        planMode: Boolean,
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
                    .put("permission_mode", if (planMode) "plan" else "default")
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
                runCatching { pollRun(peer, projectId, runId) }
                    .onFailure { error ->
                        _state.value = _state.value.copy(
                            error = error.message ?: text(R.string.error_run_monitor_interrupted),
                            activeRunId = null,
                            status = text(R.string.status_run_monitor_interrupted),
                        )
                    }
                runCatching { loadChat(peer, projectId, chat, clearEvents = false) }
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

    fun guideRun(message: String) = projectCommand(text(R.string.status_guiding)) { peer, projectId ->
        val chat = requireNotNull(_state.value.selectedChat)
        client.command(
            peer, "runs.guide", projectId,
            JSONObject()
                .put("chat_id", chat.getString("id"))
                .put("message", message.trim())
                .put("request_id", "guide_" + System.currentTimeMillis()),
        )
    }

    fun interruptRun() = projectCommand(text(R.string.status_stopping)) { peer, projectId ->
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

    fun answerChat(questionId: String, answer: String) =
        projectCommand(text(R.string.status_answering)) { peer, projectId ->
            val chat = requireNotNull(_state.value.selectedChat)
            client.command(
                peer, "approvals.respond", projectId,
                JSONObject()
                    .put("chat_id", chat.getString("id"))
                    .put("question_id", questionId)
                    .put("answer", answer.trim())
                    .put("permission_mode", "default"),
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
            previous.copy(
                taskDetails = previous.taskDetails + (taskId to detail),
                taskArtifacts = previous.taskArtifacts + (taskId to artifacts),
            )
        }
        _state.value = _state.value.copy(selectedTask = detail, artifacts = artifacts)
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
                    .put("permission_mode", "default")
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
                    .put("permission_mode", "default"),
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
                    .put("permission_mode", "default"),
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
        _state.value = _state.value.copy(chats = chats)
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
            previous.copy(chatDetails = previous.chatDetails + (chatId to detail))
        }
        _state.value = _state.value.copy(
            selectedChat = detail,
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
        _state.value = _state.value.copy(tasks = tasks)
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
        val projects = snapshot.projects
        if (projects.isEmpty()) return
        val currentProjectId = _state.value.selectedProject?.optString("id").orEmpty()
        val selectedProject = projects.firstOrNull {
            it.optString("id") == currentProjectId
        } ?: projects.first()
        val projectData = snapshot.projectData[selectedProject.optString("id")]
            ?: CachedProjectData()
        _state.value = _state.value.copy(
            projects = projects,
            selectedProject = selectedProject,
            chats = projectData.chats,
            tasks = projectData.tasks,
            status = text(R.string.status_cached_projects, projects.size),
        )
    }

    private fun startBackgroundSync(peer: Peer) {
        backgroundSyncJob?.cancel()
        backgroundSyncJob = viewModelScope.launch(Dispatchers.IO) {
            val projects = _state.value.projects
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
                    publishProjectListsIfSelected(project, data)
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

    private fun publishProjectListsIfSelected(
        project: JSONObject,
        data: CachedProjectData,
    ) {
        if (_state.value.selectedProject?.optString("id") == project.optString("id")) {
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
        client.close()
        super.onCleared()
    }
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
