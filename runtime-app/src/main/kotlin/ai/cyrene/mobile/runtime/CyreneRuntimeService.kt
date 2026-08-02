package ai.cyrene.mobile.runtime

import android.app.Service
import android.content.Intent
import android.os.IBinder
import ai.cyrene.mobile.runtime.protocol.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

class CyreneRuntimeService : Service() {
    private val executor = Executors.newCachedThreadPool()
    private val pending = ConcurrentHashMap<String, Future<*>>()
    private lateinit var runtime: QemuRuntimeManager

    override fun onCreate() {
        super.onCreate()
        runtime = QemuRuntimeManager(this)
    }

    private val binder = object : IRuntimeService.Stub() {
        override fun submit(requestJson: String, callback: IRuntimeCallback) {
            val requestId = runCatching { JSONObject(requestJson).optString("request_id") }.getOrDefault("")
            val future = executor.submit {
                val response = try {
                    val request = GuestRequest.parse(requestJson)
                    runtime.handle(request)
                } catch (failure: GuestProtocolException) {
                    GuestResponse(requestId, "error", errorType = failure.code, message = failure.message)
                } catch (error: Throwable) {
                    GuestResponse(requestId, "error", errorType = "runtime_internal_error", message = error.message ?: "Runtime failed")
                }
                runCatching { callback.onResult(response.toJson()) }
                pending.remove(requestId)
            }
            if (requestId.isNotBlank()) pending[requestId] = future
        }

        override fun cancel(requestId: String) {
            runtime.cancel(requestId)
            pending.remove(requestId)?.cancel(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? =
        if (intent?.action == ACTION_BIND) binder else null

    override fun onDestroy() {
        runtime.shutdown()
        pending.values.forEach { it.cancel(true) }
        executor.shutdownNow()
        super.onDestroy()
    }

    companion object { const val ACTION_BIND = "ai.cyrene.mobile.runtime.BIND" }
}
