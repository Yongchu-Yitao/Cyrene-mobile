package ai.cyrene.mobile.runtime

import android.app.Activity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import ai.cyrene.mobile.runtime.protocol.GuestOperation
import ai.cyrene.mobile.runtime.protocol.GuestRequest
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Debug-only, emulator-driven proof that commands execute in the QEMU guest. */
class RuntimeProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread {
            val result = runCatching {
                val manager = QemuRuntimeManager(this)
                val sessionId = "ls_runtime_probe"
                val deadline = System.currentTimeMillis() + 180_000
                val mount = manager.handle(request(sessionId, GuestOperation.SESSION_MOUNT, JSONObject(), deadline))
                check(mount.status == "success") { mount.message ?: "mount failed" }
                val sharedContent = "shared-by-$sessionId"
                val write = manager.handle(
                    request(
                        sessionId,
                        GuestOperation.FS_WRITE,
                        JSONObject().put("path", "cross-session-proof.txt").put("content", sharedContent),
                        deadline,
                    )
                )
                check(write.status == "success") { write.message ?: "shared write failed" }
                val secondSessionId = "ls_runtime_probe_second"
                val secondMount = manager.handle(
                    request(secondSessionId, GuestOperation.SESSION_MOUNT, JSONObject(), deadline)
                )
                check(secondMount.status == "success") { secondMount.message ?: "second mount failed" }
                val sharedRead = manager.handle(
                    request(
                        secondSessionId,
                        GuestOperation.FS_READ,
                        JSONObject().put("path", "cross-session-proof.txt"),
                        deadline,
                    )
                )
                check(sharedRead.status == "success") { sharedRead.message ?: "shared read failed" }
                check(sharedRead.payload.getString("content") == sharedContent) {
                    "sessions did not observe the same workspace"
                }
                val command = intent.getStringExtra("command_b64")?.let {
                    Base64.decode(it, Base64.DEFAULT).toString(Charsets.UTF_8)
                } ?: intent.getStringExtra("command") ?: DEFAULT_COMMAND
                val execution = manager.handle(
                    request(sessionId, GuestOperation.EXEC_START, JSONObject().put("command", command), deadline)
                )
                check(execution.status == "success") { execution.message ?: "execution failed" }
                val health = manager.handle(request(sessionId, GuestOperation.HEALTH_CHECK, JSONObject(), deadline))
                JSONObject()
                    .put("mount", mount.payload)
                    .put(
                        "cross_session",
                        JSONObject()
                            .put("writer_session", sessionId)
                            .put("reader_session", secondSessionId)
                            .put("content", sharedRead.payload.getString("content"))
                            .put("same_vm_workspace", true),
                    )
                    .put("execution", execution.payload)
                    .put("health", health.payload)
                    .toString()
            }.fold({ it }, { JSONObject().put("error", it.stackTraceToString()).toString() })
            File(filesDir, "probe-result.json").writeText(result)
            Log.i(TAG, result)
            runOnUiThread { finish() }
        }.start()
    }

    private fun request(
        sessionId: String,
        operation: GuestOperation,
        payload: JSONObject,
        deadline: Long,
    ) = GuestRequest(
        requestId = "probe_${UUID.randomUUID().toString().replace("-", "")}",
        sessionId = sessionId,
        subagentId = null,
        operation = operation,
        deadlineEpochMs = deadline,
        sequence = System.nanoTime(),
        payload = payload,
    )

    companion object {
        private const val TAG = "CyreneRuntimeProbe"
        private const val DEFAULT_COMMAND =
            "uname -a; cat /etc/alpine-release; id; pwd; " +
                "echo SHARED_VM_OK > shared-proof.txt; " +
                "apk update >/tmp/apk-update.log && echo NETWORK_OK; tail -n 2 /tmp/apk-update.log"
    }
}
