package ai.cyrene.mobile.data

import android.content.Context
import android.os.Build
import ai.cyrene.mobile.PermissionMode
import ai.cyrene.mobile.protocol.MobileIdentity
import ai.cyrene.mobile.protocol.Peer
import ai.cyrene.mobile.protocol.b64Url
import ai.cyrene.mobile.protocol.b64UrlDecode
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class SecureStore(context: Context) {
    private val preferences = context.getSharedPreferences("cyrene_mobile", Context.MODE_PRIVATE)

    fun identity(): MobileIdentity {
        val encrypted = preferences.getString("identity_cipher", null)
        val iv = preferences.getString("identity_iv", null)
        if (encrypted != null && iv != null) {
            return runCatching {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, b64UrlDecode(iv)))
                val raw = JSONObject(cipher.doFinal(b64UrlDecode(encrypted)).toString(Charsets.UTF_8))
                MobileIdentity(
                    b64UrlDecode(raw.getString("signing")),
                    b64UrlDecode(raw.getString("exchange")),
                )
            }.getOrElse {
                // Restoring an emulator/device snapshot can roll SharedPreferences
                // back without rolling Android Keystore back to the matching key.
                // The old peers and copied provider credentials are then no longer
                // cryptographically usable and must not be presented as connected.
                preferences.edit()
                    .remove("identity_cipher")
                    .remove("identity_iv")
                    .remove("peers")
                    .remove("peer")
                    .remove("active_peer_id")
                    .remove(LOCAL_MODELS_CIPHER)
                    .remove(LOCAL_MODELS_IV)
                    .apply()
                createIdentity()
            }
        }
        return createIdentity()
    }

    private fun createIdentity(): MobileIdentity {
        val identity = MobileIdentity.generate()
        val raw = JSONObject()
            .put("signing", b64Url(identity.signingPrivate))
            .put("exchange", b64Url(identity.exchangePrivate))
            .toString().toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        preferences.edit()
            .putString("identity_cipher", b64Url(cipher.doFinal(raw)))
            .putString("identity_iv", b64Url(cipher.iv))
            .apply()
        return identity
    }

    private fun masterKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun savePeer(peer: Peer) {
        val updated = (peers().filterNot { it.deviceId == peer.deviceId } + peer)
        preferences.edit()
            .putString("peers", JSONArray(updated.map { it.toJson() }).toString())
            .putString("active_peer_id", peer.deviceId)
            .remove("peer")
            .apply()
    }

    fun peers(): List<Peer> {
        val stored = preferences.getString("peers", null)
        if (stored != null) {
            return runCatching {
                val array = JSONArray(stored)
                (0 until array.length()).mapNotNull { index ->
                    runCatching { array.getJSONObject(index).toPeer() }.getOrNull()
                }.distinctBy(Peer::deviceId)
            }.getOrDefault(emptyList())
        }
        return preferences.getString("peer", null)?.let {
            runCatching { listOf(JSONObject(it).toPeer()) }.getOrNull()
        }.orEmpty()
    }

    fun peer(): Peer? {
        val savedPeers = peers()
        val activeId = preferences.getString("active_peer_id", null)
        return savedPeers.firstOrNull { it.deviceId == activeId } ?: savedPeers.firstOrNull()
    }

    fun selectPeer(deviceId: String) {
        require(peers().any { it.deviceId == deviceId })
        preferences.edit().putString("active_peer_id", deviceId).apply()
    }

    fun clearPeer(deviceId: String) {
        val remaining = peers().filterNot { it.deviceId == deviceId }
        val editor = preferences.edit()
            .putString("peers", JSONArray(remaining.map { it.toJson() }).toString())
            .remove("peer")
        val currentActiveId = preferences.getString("active_peer_id", null)
        if (currentActiveId == deviceId || remaining.none { it.deviceId == currentActiveId }) {
            remaining.firstOrNull()?.let { editor.putString("active_peer_id", it.deviceId) }
                ?: editor.remove("active_peer_id")
        }
        editor.apply()
    }

    fun uiTheme(): String = preferences.getString("ui_theme", "system")
        .orEmpty().takeIf { it in setOf("system", "light", "dark") } ?: "system"

    fun saveUiTheme(value: String) {
        require(value in setOf("system", "light", "dark"))
        preferences.edit().putString("ui_theme", value).apply()
    }

    fun uiLanguage(): String = preferences.getString("ui_language", "")
        .orEmpty().takeIf { it in setOf("", "en", "zh-CN") } ?: ""

    fun saveUiLanguage(value: String) {
        require(value in setOf("", "en", "zh-CN"))
        preferences.edit().putString("ui_language", value).apply()
    }

    fun permissionMode(): PermissionMode = PermissionMode.fromWireValue(
        preferences.getString("permission_mode", PermissionMode.AUTO.wireValue)
    )

    fun savePermissionMode(value: PermissionMode) {
        preferences.edit().putString("permission_mode", value.wireValue).apply()
    }

    /**
     * Stores the complete Provider configuration used by local conversations.
     * The payload can contain API credentials and is therefore always wrapped by
     * an Android Keystore-backed AES-GCM key. It must never be copied to the
     * unencrypted desktop cache or UI state.
     */
    fun saveLocalModelConfiguration(value: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        val plaintext = value.toString().toByteArray(Charsets.UTF_8)
        preferences.edit()
            .putString(LOCAL_MODELS_CIPHER, b64Url(cipher.doFinal(plaintext)))
            .putString(LOCAL_MODELS_IV, b64Url(cipher.iv))
            .apply()
        plaintext.fill(0)
    }

    fun localModelConfiguration(): JSONObject? {
        val encrypted = preferences.getString(LOCAL_MODELS_CIPHER, null) ?: return null
        val iv = preferences.getString(LOCAL_MODELS_IV, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, b64UrlDecode(iv)))
            val plaintext = cipher.doFinal(b64UrlDecode(encrypted))
            try {
                JSONObject(plaintext.toString(Charsets.UTF_8))
            } finally {
                plaintext.fill(0)
            }
        }.getOrNull()
    }

    fun localModelConfigurationPublic(): JSONObject? = localModelConfiguration()?.let(::redactModelSecrets)

    fun hasRunnableLocalModel(): Boolean {
        val models = localModelConfiguration() ?: return false
        if (models.optString("source", "custom") != "custom") return false
        val custom = models.optJSONArray("custom_models") ?: return false
        if (custom.length() == 0) return false
        val primary = custom.optJSONObject(0) ?: return false
        return primary.optString("model").isNotBlank() &&
            primary.optString("base_url").startsWith("http") &&
            primary.optString("api_key").isNotBlank()
    }

    companion object {
        private const val KEY_ALIAS = "cyrene_mobile_identity_master_v1"
        private const val LOCAL_MODELS_CIPHER = "local_models_cipher"
        private const val LOCAL_MODELS_IV = "local_models_iv"
    }
}

private fun redactModelSecrets(value: JSONObject): JSONObject {
    val result = JSONObject(value.toString())
    listOf("custom_models", "vision_models").forEach { key ->
        val array = result.optJSONArray(key) ?: return@forEach
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.let { candidate ->
                val configured = candidate.optString("api_key").isNotBlank()
                candidate.remove("api_key")
                candidate.put("api_key_configured", configured)
            }
        }
    }
    result.optJSONObject("secondary_model")?.let { candidate ->
        val configured = candidate.optString("api_key").isNotBlank()
        candidate.remove("api_key")
        candidate.put("api_key_configured", configured)
    }
    result.optJSONObject("codex_model")?.remove("api_key")
    return result
}

fun Peer.toJson(): JSONObject = JSONObject()
    .put("device_id", deviceId)
    .put("name", name)
    .put("signing_public", signingPublic)
    .put("exchange_public", exchangePublic)
    .put("fingerprint", fingerprint)
    .put("host", host)
    .put("port", port)
    .put("capabilities", JSONArray(capabilities))
    .put("project_scopes", JSONArray(projectScopes))

fun JSONObject.toPeer(): Peer = Peer(
    deviceId = getString("device_id"),
    name = getString("name"),
    signingPublic = getString("signing_public"),
    exchangePublic = getString("exchange_public"),
    fingerprint = getString("fingerprint"),
    host = getString("host"),
    port = getInt("port"),
    capabilities = optJSONArray("capabilities").strings(),
    projectScopes = optJSONArray("project_scopes").strings(),
)

fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).map { getString(it) }
