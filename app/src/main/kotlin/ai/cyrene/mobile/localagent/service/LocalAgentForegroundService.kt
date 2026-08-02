package ai.cyrene.mobile.localagent.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ai.cyrene.mobile.MainActivity
import ai.cyrene.mobile.R
import ai.cyrene.mobile.data.SecureStore
import ai.cyrene.mobile.localagent.database.DatabaseRuntimeStore
import ai.cyrene.mobile.localagent.database.LocalAgentDatabase
import ai.cyrene.mobile.localagent.runtime.*
import ai.cyrene.mobile.localagent.tooling.LocalHarnessToolInvoker
import kotlinx.coroutines.*
import org.json.JSONObject

class LocalAgentForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.local_agent_notification_channel), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                val runId = intent.getStringExtra(EXTRA_RUN_ID).orEmpty()
                LocalRunSupervisor.cancel(runId)
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val runId = intent.getStringExtra(EXTRA_RUN_ID).orEmpty()
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                startForeground(NOTIFICATION_ID, notification(runId, title, getString(R.string.local_agent_status_running)))
                if (runId.isNotBlank() && !LocalRunSupervisor.isRunning(runId)) {
                    val job = scope.launch { runLocalAgent(requireNotNull(intent), runId) }
                    LocalRunSupervisor.register(runId, job)
                    job.invokeOnCompletion {
                        LocalRunSupervisor.finished(runId)
                        if (!LocalRunSupervisor.hasRuns()) stopSelf()
                    }
                }
            }
        }
        // A Provider turn or shell side effect cannot be replayed safely from
        // an old Intent. Durable state remains visible and the user can start
        // a fresh Run explicitly after process death.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private suspend fun runLocalAgent(intent: Intent, runId: String) {
        val sessionId = requireNotNull(intent.getStringExtra(EXTRA_SESSION_ID))
        val message = requireNotNull(intent.getStringExtra(EXTRA_MESSAGE)).take(50_000)
        val generation = intent.getLongExtra(EXTRA_GENERATION, 0)
        require(sessionId.startsWith("ls_") && generation > 0 && message.isNotBlank())
        val secure = SecureStore(this)
        require(secure.hasRunnableLocalModel()) { "请先在设置中从桌面端复制模型配置，并确认 API Key 已保存到本机" }
        val dao = LocalAgentDatabase.open(this).dao()
        val sink = DatabaseRuntimeStore(dao, sessionId)
        val runtime = RuntimeCompanionClient(this)
        val runBudget = RunBudget(20, 50, 30 * 60 * 1000L, 120_000, 32_768, Long.MAX_VALUE, 4, 2)
        sink.checkpoint(
            ai.cyrene.mobile.localagent.runtime.RunSnapshot(
                runId, sessionId, generation, ai.cyrene.mobile.localagent.model.RunState.READY, runBudget
            )
        )
        try {
            val contextItems = materializedContext(sessionId)
            dao.saveMessage(sessionId, "user", message)
            val outcome = LocalAgentOrchestrator(
                MobileProviderClient(secure),
                LocalHarnessToolInvoker(sessionId, generation, runtime),
                sink,
            ).run(
                FrozenRunInputs(
                    sessionId, runId, generation, "local-prompt-v1", "local-tools-v1", message, contextItems,
                    runBudget,
                )
            )
            if (outcome.finalAnswer.isNotBlank()) dao.saveMessage(sessionId, "assistant", outcome.finalAnswer)
        } catch (cancelled: CancellationException) {
            dao.run(runId)?.let { current ->
                dao.saveRun(current.copy(state = ai.cyrene.mobile.localagent.model.RunState.COMPLETED, stopReason = ai.cyrene.mobile.localagent.model.StopReason.CANCELLED))
            }
            sink.event("run_cancelled", JSONObject().put("reason", "user_or_os_cancelled"))
            throw cancelled
        } catch (error: Throwable) {
            dao.run(runId)?.let { current ->
                dao.saveRun(current.copy(state = ai.cyrene.mobile.localagent.model.RunState.FAILED, stopReason = ai.cyrene.mobile.localagent.model.StopReason.FATAL_ERROR))
            }
            sink.event("run_failed", JSONObject().put("error_type", error::class.java.simpleName).put("message", error.message ?: "Local Run failed"))
        } finally {
            runtime.close()
        }
    }

    private suspend fun materializedContext(sessionId: String): List<JSONObject> {
        val dao = LocalAgentDatabase.open(this).dao()
        return dao.messages(sessionId, 80).map { message ->
            JSONObject().put("role", message.role).put("content", message.content)
        }
    }

    private fun notification(runId: String, title: String, status: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, LocalAgentForegroundService::class.java).setAction(ACTION_STOP).putExtra(EXTRA_RUN_ID, runId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.ifBlank { getString(R.string.local_agent_notification_title) })
            .setContentText(status)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "local_agent_runs"
        private const val NOTIFICATION_ID = 4102
        const val ACTION_START = "ai.cyrene.mobile.localagent.START"
        const val ACTION_STOP = "ai.cyrene.mobile.localagent.STOP"
        const val EXTRA_RUN_ID = "run_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_GENERATION = "generation"

        fun startRun(context: Context, runId: String, title: String, sessionId: String, generation: Long, message: String) {
            context.startForegroundService(
                Intent(context, LocalAgentForegroundService::class.java)
                    .setAction(ACTION_START).putExtra(EXTRA_RUN_ID, runId).putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_SESSION_ID, sessionId)
                    .putExtra(EXTRA_GENERATION, generation).putExtra(EXTRA_MESSAGE, message)
            )
        }

        fun stopRun(context: Context, runId: String) {
            context.startService(
                Intent(context, LocalAgentForegroundService::class.java).setAction(ACTION_STOP).putExtra(EXTRA_RUN_ID, runId)
            )
        }
    }
}

object LocalRunSupervisor {
    private val jobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    fun register(runId: String, job: Job) { jobs[runId] = job }
    fun cancel(runId: String) { jobs.remove(runId)?.cancel(CancellationException("User stopped Local Run")) }
    fun finished(runId: String) { jobs.remove(runId) }
    fun isRunning(runId: String) = jobs[runId]?.isActive == true
    fun hasRuns() = jobs.values.any(Job::isActive)
}
