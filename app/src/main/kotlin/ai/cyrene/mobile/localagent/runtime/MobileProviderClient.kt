package ai.cyrene.mobile.localagent.runtime

import ai.cyrene.mobile.data.SecureStore
import ai.cyrene.mobile.localagent.model.MODEL_EVENT_SCHEMA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** Direct, on-device OpenAI-compatible Provider transport for local sessions. */
class MobileProviderClient(private val store: SecureStore) : LocalModelTransport {
    private val previousAssistantMessages = ConcurrentHashMap<String, JSONObject>()

    override suspend fun start(payload: JSONObject, idempotencyKey: String): JSONObject = complete(payload)

    override suspend fun continueTurn(payload: JSONObject, idempotencyKey: String): JSONObject = complete(payload)

    override suspend fun wait(modelTurnId: String, cursor: Long): JSONObject =
        throw IllegalStateException("Mobile Provider turns complete inline")

    private suspend fun complete(payload: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val turnId = payload.getString("model_turn_id")
        val phase = payload.getString("phase")
        val candidate = primaryCandidate()
        val request = JSONObject()
            .put("model", candidate.getString("model"))
            .put("messages", messages(payload, phase))
            .put("tools", if (phase == "decision") decisionTools() else executionTools())
            .put("max_tokens", payload.getJSONObject("limits").optInt("max_output_tokens", 8192).coerceIn(1, 32768))
            .put("stream", false)
        val response = post(candidate, request)
        val choice = response.optJSONArray("choices")?.optJSONObject(0)
            ?: throw IllegalStateException(response.optJSONObject("error")?.optString("message")
                ?: "模型服务没有返回 choices")
        val assistant = choice.optJSONObject("message") ?: JSONObject()
        previousAssistantMessages[turnId] = JSONObject(assistant.toString())
        val events = JSONArray()
        var sequence = 1
        fun event(type: String, body: JSONObject = JSONObject()) {
            events.put(JSONObject().put("schema", MODEL_EVENT_SCHEMA).put("model_turn_id", turnId)
                .put("sequence", sequence++).put("type", type).put("payload", body))
        }
        event("turn.started", JSONObject().put("phase", phase))
        assistant.optString("reasoning_content").ifBlank {
            assistant.optString("reasoning")
        }.takeIf(String::isNotBlank)?.let { reasoning ->
            event("reasoning.status", JSONObject().put("content", reasoning))
        }
        val content = assistant.optString("content")
        if (content.isNotBlank()) {
            event("message.delta", JSONObject().put("delta", content))
            event("message.completed", JSONObject().put("content", content))
        }
        val calls = assistant.optJSONArray("tool_calls") ?: JSONArray()
        for (index in 0 until calls.length()) {
            val call = calls.optJSONObject(index) ?: continue
            val function = call.optJSONObject("function") ?: continue
            val callId = call.optString("id").ifBlank { "call_${turnId}_$index" }
            val name = function.optString("name")
            val arguments = function.opt("arguments")?.toString().orEmpty().ifBlank { "{}" }
            event("tool_call.started", JSONObject().put("call_id", callId).put("name", name))
            event("tool_call.completed", JSONObject().put("call_id", callId).put("name", name)
                .put("arguments_json", arguments))
        }
        response.optJSONObject("usage")?.let { usage ->
            event("usage.updated", JSONObject().apply {
                listOf("prompt_tokens", "completion_tokens", "total_tokens").forEach { key ->
                    if (usage.has(key)) put(key, usage.get(key))
                }
            })
        }
        event("turn.completed", JSONObject().put("finish_reason", choice.optString("finish_reason", "stop")))
        JSONObject().put("model_turn_id", turnId).put("state", "COMPLETED").put("events", events)
    }

    private fun primaryCandidate(): JSONObject {
        val models = store.localModelConfiguration()
            ?: throw IllegalStateException("请先在设置中从桌面端复制模型配置")
        if (models.optString("source", "custom") != "custom") {
            throw IllegalStateException("Codex OAuth 凭据不能复制到移动端；请选择 API Key 模型")
        }
        val candidate = models.optJSONArray("custom_models")?.optJSONObject(0)
            ?: throw IllegalStateException("移动端没有可用的模型配置")
        require(candidate.optString("provider", "openai_compatible") == "openai_compatible") {
            "移动端当前仅支持 OpenAI-compatible API"
        }
        require(candidate.optString("model").isNotBlank()) { "模型标识为空" }
        require(candidate.optString("api_key").isNotBlank()) { "请在移动端模型设置中重新输入 API Key" }
        val uri = URI(candidate.optString("base_url"))
        require(uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null) {
            "模型 Base URL 无效"
        }
        return candidate
    }

    private fun messages(payload: JSONObject, phase: String): JSONArray {
        val result = JSONArray().put(JSONObject().put("role", "system").put("content", systemPrompt(phase)))
        val input = payload.optJSONArray("input_items") ?: JSONArray()
        for (index in 0 until input.length()) {
            val item = input.optJSONObject(index) ?: continue
            val role = item.optString("role", "user").takeIf { it in setOf("user", "assistant", "tool") } ?: "user"
            result.put(JSONObject().put("role", role).put("content", item.optString("content").take(500_000)))
        }
        val previousId = payload.optString("previous_model_turn_id")
        previousAssistantMessages[previousId]?.let { result.put(JSONObject(it.toString())) }
        val toolResults = payload.optJSONArray("tool_results") ?: JSONArray()
        for (index in 0 until toolResults.length()) {
            val item = toolResults.optJSONObject(index) ?: continue
            result.put(JSONObject().put("role", "tool").put("tool_call_id", item.optString("call_id"))
                .put("content", item.optString("inline_payload").ifBlank { item.optString("summary") }.take(128_000)))
        }
        return result
    }

    private fun post(candidate: JSONObject, body: JSONObject): JSONObject {
        val base = candidate.getString("base_url").trimEnd('/')
        val endpoint = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 120_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${candidate.getString("api_key")}")
            val bytes = body.toString().toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = runCatching { JSONObject(text).optJSONObject("error")?.optString("message") }
                    .getOrNull().orEmpty().ifBlank { "HTTP $status" }
                throw IllegalStateException("模型请求失败：${detail.take(500)}")
            }
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun systemPrompt(phase: String): String = if (phase == "decision") {
        """你是运行在 Android 设备上的 Cyrene 本地 Agent。直接回答不需要工具的问题；需要本地文件或命令时调用 use_tools，信息不足时调用 ask_user，明确无法继续时调用 quit。不要声称已经执行尚未执行的操作。"""
    } else {
        """你是运行在 Android 隔离 Linux 工作区中的 Cyrene 本地 Agent。使用提供的工具完成用户请求；路径必须相对于会话工作区。完成后直接给出最终回答。不要伪造工具结果。"""
    }

    private fun function(name: String, description: String, properties: JSONObject, required: List<String> = emptyList()): JSONObject =
        JSONObject().put("type", "function").put("function", JSONObject().put("name", name)
            .put("description", description).put("parameters", JSONObject().put("type", "object")
                .put("properties", properties).put("required", JSONArray(required)).put("additionalProperties", false)))

    private fun stringProperty(description: String) = JSONObject().put("type", "string").put("description", description)

    private fun decisionTools() = JSONArray()
        .put(function("use_tools", "进入本地工具执行阶段", JSONObject().put("task", stringProperty("保持用户原始任务语义")), listOf("task")))
        .put(function("ask_user", "请求用户补充必要信息", JSONObject().put("question", stringProperty("问题")), listOf("question")))
        .put(function("quit", "无法继续并说明原因", JSONObject().put("answer", stringProperty("说明")), listOf("answer")))

    private fun executionTools() = JSONArray()
        .put(function("Read", "读取工作区文件", JSONObject().put("path", stringProperty("相对路径")), listOf("path")))
        .put(function("Write", "写入工作区文件", JSONObject().put("path", stringProperty("相对路径")).put("content", stringProperty("完整内容")), listOf("path", "content")))
        .put(function("Edit", "替换工作区文件中的一段文本", JSONObject().put("path", stringProperty("相对路径")).put("old_string", stringProperty("原文本")).put("new_string", stringProperty("新文本")), listOf("path", "old_string", "new_string")))
        .put(function("Glob", "按 glob 查找工作区文件", JSONObject().put("pattern", stringProperty("例如 **/*.kt")), listOf("pattern")))
        .put(function("Grep", "搜索工作区文本", JSONObject().put("pattern", stringProperty("正则表达式")).put("path", stringProperty("相对目录，默认 .")), listOf("pattern")))
        .put(function("Bash", "在隔离工作区执行 shell 命令", JSONObject().put("command", stringProperty("命令")), listOf("command")))
}
