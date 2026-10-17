package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ServerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServerPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("matrix_server_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<ServerConfig> = _configFlow.asStateFlow()

    fun loadConfig(): ServerConfig {
        return ServerConfig(
            port = prefs.getInt(KEY_PORT, 2222),
            username = prefs.getString(KEY_USERNAME, "matrix") ?: "matrix",
            passwordAuthEnabled = prefs.getBoolean(KEY_PASS_AUTH, true),
            password = prefs.getString(KEY_PASSWORD, "matrix1337") ?: "matrix1337",
            publicKeyAuthEnabled = prefs.getBoolean(KEY_KEY_AUTH, true),
            wakeLockEnabled = prefs.getBoolean(KEY_WAKELOCK, true),
            autoStartOnBoot = prefs.getBoolean(KEY_BOOT_START, false),
            lowPowerMode = prefs.getBoolean(KEY_LOW_POWER, false),
            setupCompleted = prefs.getBoolean(KEY_SETUP_COMPLETED, false)
        )
    }

    fun updateConfig(config: ServerConfig) {
        prefs.edit()
            .putInt(KEY_PORT, config.port)
            .putString(KEY_USERNAME, config.username)
            .putBoolean(KEY_PASS_AUTH, config.passwordAuthEnabled)
            .putString(KEY_PASSWORD, config.password)
            .putBoolean(KEY_KEY_AUTH, config.publicKeyAuthEnabled)
            .putBoolean(KEY_WAKELOCK, config.wakeLockEnabled)
            .putBoolean(KEY_BOOT_START, config.autoStartOnBoot)
            .putBoolean(KEY_LOW_POWER, config.lowPowerMode)
            .putBoolean(KEY_SETUP_COMPLETED, config.setupCompleted)
            .apply()

        _configFlow.value = config
    }

    fun setSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply()
        _configFlow.value = _configFlow.value.copy(setupCompleted = completed)
    }

    companion object {
        private const val KEY_PORT = "ssh_port"
        private const val KEY_USERNAME = "ssh_username"
        private const val KEY_PASS_AUTH = "password_auth_enabled"
        private const val KEY_PASSWORD = "ssh_password"
        private const val KEY_KEY_AUTH = "public_key_auth_enabled"
        private const val KEY_WAKELOCK = "wakelock_enabled"
        private const val KEY_BOOT_START = "auto_start_on_boot"
        private const val KEY_LOW_POWER = "low_power_mode"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
    }
}
