package com.example

import android.app.Application
import org.apache.sshd.common.util.io.PathUtils
import java.io.File

class MatrixServerApplication : Application() {

    init {
        // Set safe fallback user.home before any static SSHD classes load
        try {
            if (System.getProperty("user.home").isNullOrBlank()) {
                System.setProperty("user.home", "/data/local/tmp")
            }
        } catch (_: Throwable) {}
    }

    override fun onCreate() {
        super.onCreate()
        val homeDir = File(filesDir, "home").apply { if (!exists()) mkdirs() }

        try {
            System.setProperty("user.home", homeDir.absolutePath)
        } catch (_: Throwable) {}

        try {
            PathUtils.setUserHomeFolderResolver { homeDir.toPath() }
        } catch (_: Throwable) {}
    }
}
