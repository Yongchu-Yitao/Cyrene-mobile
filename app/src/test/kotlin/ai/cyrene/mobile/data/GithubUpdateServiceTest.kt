package ai.cyrene.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GithubUpdateServiceTest {
    @Test
    fun comparesReleaseVersions() {
        assertTrue(GithubUpdateService.isNewerVersion("v0.2.0", "0.1.9"))
        assertTrue(GithubUpdateService.isNewerVersion("v1.0.1", "1.0"))
        assertFalse(GithubUpdateService.isNewerVersion("v1.0.0", "1.0"))
        assertFalse(GithubUpdateService.isNewerVersion("v1.0.0-beta.2", "1.0.0"))
        assertTrue(GithubUpdateService.isNewerVersion("v1.0.0-beta.2", "1.0.0-beta.1"))
    }

    @Test
    fun prefersApkAssetAndKeepsReleaseNotes() {
        val release = GithubUpdateService.parseRelease(
            """
            {
              "tag_name": "v0.2.0",
              "html_url": "https://github.com/example/releases/v0.2.0",
              "body": "What changed",
              "published_at": "2026-07-31T00:00:00Z",
              "assets": [
                {
                  "name": "cyrene-mobile-v0.2.0.apk",
                  "browser_download_url": "https://github.com/example/cyrene.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("v0.2.0", release.version)
        assertEquals("https://github.com/example/cyrene.apk", release.apkUrl)
        assertEquals("cyrene-mobile-v0.2.0.apk", release.apkName)
        assertEquals("What changed", release.notes)
        assertEquals("2026-07-31T00:00:00Z", release.publishedAt)
    }

    @Test
    fun keepsApkMissingSoUiCanFallBackToReleasePage() {
        val release = GithubUpdateService.parseRelease(
            """
            {
              "tag_name": "v0.2.0",
              "html_url": "https://github.com/example/releases/v0.2.0",
              "assets": []
            }
            """.trimIndent(),
        )

        assertEquals(null, release.apkUrl)
        assertEquals("https://github.com/example/releases/v0.2.0", release.releaseUrl)
    }
}
