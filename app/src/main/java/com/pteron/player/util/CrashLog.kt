package com.pteron.player.util

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Writes the stack trace of an uncaught exception to a small file just before the process dies,
 * so the next launch can show it. Costs nothing while the app runs fine.
 */
object CrashLog {
    private const val FILE = "last_crash.txt"

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val text = buildString {
                    append("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                    append("Thread: ${thread.name}\n\n")
                    append(Log.getStackTraceString(throwable))
                }
                File(app.filesDir, FILE).writeText(text.take(20_000))
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun read(context: Context): String? =
        runCatching { File(context.filesDir, FILE).takeIf { it.exists() }?.readText() }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE).delete() }
    }
}
