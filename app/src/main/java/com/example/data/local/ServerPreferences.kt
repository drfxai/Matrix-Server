package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.data.model.ServerConfig
import com.example.server.SshAuthPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ServerPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("matrix_server_prefs", Context.MODE_PRIVATE)
    private val secrets = KeystorePasswordCipher()

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<ServerConfig> = _configFlow.asStateFlow()

    fun loadConfig(): ServerConfig {
        val password = readPassword()
        return ServerConfig(
            port = prefs.getInt(KEY_PORT, 2222),
            username = prefs.getString(KEY_USERNAME, "matrix") ?: "matrix",
            // Never restore a permissive legacy default, even after an app upgrade.
            passwordAuthEnabled = prefs.getBoolean(KEY_PASS_AUTH, false) &&
                SshAuthPolicy.validPassword(password),
            password = password,
            publicKeyAuthEnabled = prefs.getBoolean(KEY_KEY_AUTH, true),
            wakeLockEnabled = prefs.getBoolean(KEY_WAKELOCK, true),
            autoStartOnBoot = prefs.getBoolean(KEY_BOOT_START, false),
            lowPowerMode = prefs.getBoolean(KEY_LOW_POWER, false),
            vpnTcpForwardingEnabled = prefs.getBoolean(KEY_VPN_TCP_FORWARD, false),
            setupCompleted = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        )
    }

    fun updateConfig(config: ServerConfig) {
        require(!config.passwordAuthEnabled || SshAuthPolicy.validPassword(config.password)) {
            "Password authentication requires a password of 12 to 128 characters"
        }
        val encrypted = if (config.password.isNotEmpty()) secrets.encrypt(config.password) else null
        val written = prefs.edit()
            .putInt(KEY_PORT, config.port)
            .putString(KEY_USERNAME, config.username)
            .putBoolean(KEY_PASS_AUTH, config.passwordAuthEnabled)
            .remove(KEY_PASSWORD) // Purge plaintext stored by older builds.
            .putBoolean(KEY_KEY_AUTH, config.publicKeyAuthEnabled)
            .putBoolean(KEY_WAKELOCK, config.wakeLockEnabled)
            .putBoolean(KEY_BOOT_START, config.autoStartOnBoot)
            .putBoolean(KEY_LOW_POWER, config.lowPowerMode)
            .putBoolean(KEY_VPN_TCP_FORWARD, config.vpnTcpForwardingEnabled)
            .putBoolean(KEY_SETUP_COMPLETED, config.setupCompleted)
            .apply {
                if (encrypted == null) remove(KEY_ENCRYPTED_PASSWORD)
                else putString(KEY_ENCRYPTED_PASSWORD, encrypted)
            }
            .commit()
        check(written) { "Unable to persist SSH settings" }
        _configFlow.value = config
    }

    fun setSetupCompleted(completed: Boolean) {
        check(prefs.edit().putBoolean(KEY_SETUP_COMPLETED, completed).commit())
        _configFlow.value = _configFlow.value.copy(setupCompleted = completed)
    }

    private fun readPassword(): String {
        // One-time migration: reject weak/known legacy credentials, and ALWAYS purge plaintext.
        val plaintext = prefs.getString(KEY_PASSWORD, null)
        if (plaintext != null) {
            val encrypted = if (SshAuthPolicy.validPassword(plaintext)) {
                runCatching { secrets.encrypt(plaintext) }.getOrNull()
            } else null
            val editor = prefs.edit().remove(KEY_PASSWORD)
            if (encrypted != null) editor.putString(KEY_ENCRYPTED_PASSWORD, encrypted)
            check(editor.commit()) { "Unable to remove plaintext SSH credentials" }
        }

        val encrypted = prefs.getString(KEY_ENCRYPTED_PASSWORD, null) ?: return ""
        // Keystore loss or ciphertext tampering must not restore password authentication.
        return runCatching { secrets.decrypt(encrypted) }.getOrDefault("")
    }

    companion object {
        private const val KEY_PORT = "ssh_port"
        private const val KEY_USERNAME = "ssh_username"
        private const val KEY_PASS_AUTH = "password_auth_enabled"
        private const val KEY_PASSWORD = "ssh_password" // Legacy key, never written.
        private const val KEY_ENCRYPTED_PASSWORD = "ssh_password_encrypted_v1"
        private const val KEY_KEY_AUTH = "public_key_auth_enabled"
        private const val KEY_WAKELOCK = "wakelock_enabled"
        private const val KEY_BOOT_START = "auto_start_on_boot"
        private const val KEY_LOW_POWER = "low_power_mode"
        private const val KEY_VPN_TCP_FORWARD = "vpn_tcp_forwarding_enabled"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
    }
}

/** AES-GCM key material remains in the Android Keystore, not the app's preferences. */
internal class KeystorePasswordCipher {
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(password: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(password.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > 12 + 16) { "Invalid SSH password payload" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
        return String(cipher.doFinal(payload.copyOfRange(12, payload.size)), StandardCharsets.UTF_8)
    }

    private companion object {
        const val ALIAS = "matrix_server_ssh_password_aes_v1"
    }
}
