package ai.cyrene.mobile

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentSessionsTest {
    @Test
    fun combinesEveryProjectAndSortsByUpdatedTime() {
        val projectA = JSONObject().put("id", "project_a").put("name", "Alpha")
        val projectB = JSONObject().put("id", "project_b").put("name", "Beta")
        val chatA = JSONObject()
            .put("id", "chat_a")
            .put("updated_at", "2026-08-01T08:00:00Z")
        val taskA = JSONObject()
            .put("id", "task_a")
            .put("updatedAt", "2026-08-01T10:00:00Z")
        val chatB = JSONObject()
            .put("id", "chat_b")
            .put("updated_at", "2026-08-01T09:00:00Z")

        val sessions = recentSessionsForProjects(
            projects = listOf(projectA, projectB),
            projectChats = mapOf(
                "project_a" to listOf(chatA),
                "project_b" to listOf(chatB),
            ),
            projectTasks = mapOf("project_a" to listOf(taskA)),
        )

        assertEquals(listOf("task_a", "chat_b", "chat_a"), sessions.map {
            it.data.getString("id")
        })
        assertEquals(listOf("Alpha", "Beta", "Alpha"), sessions.map {
            it.project.getString("name")
        })
    }
}
