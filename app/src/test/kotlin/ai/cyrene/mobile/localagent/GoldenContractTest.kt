package ai.cyrene.mobile.localagent

import ai.cyrene.mobile.localagent.protocol.LocalAgentProtocol
import ai.cyrene.mobile.runtime.protocol.GuestRequest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class GoldenContractTest {
    private val root = File(System.getProperty("user.dir"), "../protocol/local-agent/v1").canonicalFile

    @Test fun providerEventAndGuestRequestFixturesRemainStrictlyReadable() {
        val event = LocalAgentProtocol.parseModelEvent(JSONObject(root.resolve("model-event.example.json").readText()))
        assertEquals("mt_example", event.modelTurnId)
        val guest = GuestRequest.parse(root.resolve("guest-request.example.json").readText())
        assertEquals("ls_example", guest.sessionId)
    }

    @Test fun fixedCommandMatrixHasNoGenericExecutionEscapeHatch() {
        val value = JSONObject(root.resolve("command-matrix.json").readText())
        assertFalse(value.getBoolean("arbitrary_command"))
        val desktopCommands = value.getJSONArray("desktop_commands")
        val localCommands = value.getJSONArray("local_session_commands")
        assertEquals(1, desktopCommands.length())
        assertEquals("settings.models.copy", desktopCommands.getString(0))
        assertEquals(0, localCommands.length())
        for (index in 0 until desktopCommands.length()) {
            assertFalse(desktopCommands.getString(index).endsWith(".execute"))
        }
    }
}
