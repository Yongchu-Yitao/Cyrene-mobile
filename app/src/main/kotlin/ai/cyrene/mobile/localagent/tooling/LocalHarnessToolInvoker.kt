package ai.cyrene.mobile.localagent.tooling

import ai.cyrene.mobile.localagent.model.ToolResult
import ai.cyrene.mobile.localagent.model.ToolResultStatus
import ai.cyrene.mobile.localagent.runtime.HarnessToolInvoker
import ai.cyrene.mobile.localagent.runtime.RuntimeCompanionClient
import ai.cyrene.mobile.runtime.protocol.GuestOperation
import org.json.JSONArray
import org.json.JSONObject

/** Fixed mobile v1 executor map. It has no arbitrary route or host-command escape hatch. */
class LocalHarnessToolInvoker(
    private val sessionId: String,
    private val generation: Long,
    private val runtime: RuntimeCompanionClient,
) : HarnessToolInvoker {
    private var mounted = false

    override suspend fun invoke(callId: String, name: String, arguments: JSONObject): ToolResult = try {
        when (name) {
            "Read" -> guest(callId, GuestOperation.FS_READ, arguments)
            "Write" -> guest(callId, GuestOperation.FS_WRITE, arguments)
            "Edit" -> guest(callId, GuestOperation.FS_PATCH, arguments)
            "Glob" -> guest(callId, GuestOperation.FS_GLOB, arguments)
            "Grep" -> guest(callId, GuestOperation.FS_GREP, arguments)
            "Bash" -> guest(callId, GuestOperation.EXEC_START, arguments)
            "code_tools" -> packageGateway(callId, arguments, CODE_CAPABILITIES)
            else -> ToolResult(callId, ToolResultStatus.DENIED, "Capability is not available in the frozen mobile runtime", errorType = "mobile_incompatible")
        }
    } catch (error: Throwable) {
        ToolResult(callId, ToolResultStatus.ERROR, error.message ?: "Tool execution failed", errorType = "executor_error")
    }

    private suspend fun guest(callId: String, operation: GuestOperation, arguments: JSONObject): ToolResult {
        if (!mounted) {
            val mount = runtime.submit(
                sessionId,
                GuestOperation.SESSION_MOUNT,
                JSONObject().put("generation", generation),
                timeoutMs = 120_000,
            )
            if (mount.status != "success") return guestFailure(callId, mount.errorType, mount.message)
            mounted = true
        }
        val timeoutMs = if (operation == GuestOperation.EXEC_START) 120_000L else 30_000L
        val response = runtime.submit(sessionId, operation, arguments, timeoutMs = timeoutMs)
        return if (response.status == "success") {
            val content = response.payload.toString()
            ToolResult(callId, ToolResultStatus.SUCCESS, "Linux guest operation completed", inlinePayload = bounded(content))
        } else guestFailure(callId, response.errorType, response.message)
    }

    private fun packageGateway(callId: String, arguments: JSONObject, capabilities: List<String>): ToolResult = when (arguments.optString("operation")) {
        "discover" -> ToolResult(callId, ToolResultStatus.SUCCESS, "Compatible capabilities", inlinePayload = JSONArray(capabilities).toString())
        "describe" -> {
            val requested = buildList {
                arguments.optString("capability_id").takeIf(String::isNotBlank)?.let(::add)
                val ids = arguments.optJSONArray("capability_ids") ?: JSONArray()
                for (index in 0 until ids.length()) add(ids.getString(index))
            }
            if (requested.any { it !in capabilities }) ToolResult(callId, ToolResultStatus.ERROR, "Capability is not in this package", errorType = "unknown_capability")
            else ToolResult(callId, ToolResultStatus.SUCCESS, "Use the corresponding direct local tool; its JSON schema is in the current tool contract", inlinePayload = JSONArray(requested).toString())
        }
        "invoke" -> ToolResult(callId, ToolResultStatus.ERROR, "Invoke the corresponding direct frozen tool after describe", errorType = "direct_tool_required")
        else -> ToolResult(callId, ToolResultStatus.ERROR, "Invalid progressive gateway operation", errorType = "invalid_arguments")
    }

    private fun guestFailure(callId: String, type: String?, message: String?) = ToolResult(
        callId, ToolResultStatus.ERROR, message ?: "Linux runtime operation failed", errorType = type ?: "guest_error"
    )

    private fun bounded(value: String): String {
        val bytes = value.toByteArray()
        if (bytes.size <= 64 * 1024) return value
        return bytes.copyOf(64 * 1024).toString(Charsets.UTF_8).trimEnd('\uFFFD')
    }

    companion object {
        private val CODE_CAPABILITIES = listOf("code.read", "code.write", "code.patch", "code.glob", "code.grep", "code.exec")
    }
}
