package ai.cyrene.mobile

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionModeTest {
    @Test
    fun unknownOrMissingModeDefaultsToAuto() {
        assertEquals(PermissionMode.AUTO, PermissionMode.fromWireValue(null))
        assertEquals(PermissionMode.AUTO, PermissionMode.fromWireValue("unknown"))
        assertEquals(PermissionMode.DEFAULT, PermissionMode.fromWireValue(" DEFAULT "))
    }

    @Test
    fun planFallsBackToDefaultWhenAnsweringOrRunningTasks() {
        assertEquals("default", PermissionMode.PLAN.approvalWireValue())
        assertEquals("default", PermissionMode.PLAN.taskWireValue())
        assertEquals("auto", PermissionMode.AUTO.approvalWireValue())
        assertEquals("auto", PermissionMode.AUTO.taskWireValue())
    }

    @Test
    fun parsesHiddenPermissionQuestionFromChatDetail() {
        val chat = JSONObject().put(
            "pending_question",
            JSONObject()
                .put("id", "question_1")
                .put("text", "Allow access outside the workspace?")
                .put("kind", "write_permission_request")
                .put("allowCustom", false)
                .put(
                    "options",
                    JSONArray()
                        .put(JSONObject().put("id", "allow_once").put("label", "Allow once"))
                        .put("Deny"),
                ),
        )

        val question = requireNotNull(pendingApprovalQuestion(chat))
        assertEquals("question_1", question.id)
        assertEquals("Allow access outside the workspace?", question.prompt)
        assertEquals(listOf("Allow once", "Deny"), question.options.map { it.label })
        assertFalse(question.allowCustom)
    }

    @Test
    fun fallsBackToAwaitingUserRunEvent() {
        val event = JSONObject()
            .put("type", "awaiting_user")
            .put(
                "pending_question",
                JSONObject()
                    .put("questionId", "question_2")
                    .put("prompt", "Continue?")
                    .put("choices", JSONArray().put("Continue").put("Stop")),
            )

        val question = requireNotNull(pendingApprovalQuestion(JSONObject(), listOf(event)))
        assertEquals("question_2", question.id)
        assertEquals("Continue?", question.prompt)
        assertTrue(question.allowCustom)
    }
}
