package com.tagaev.trrcrm.updates

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Helper process: waits until app exits, runs installer, restarts app on success.
 */
object DesktopUpdaterLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) return
        val installerPath = args[0]
        val parentPid = args.getOrNull(1)?.toLongOrNull()
        val restartCommand = args.getOrNull(2)

        try {
            if (parentPid != null) {
                waitForParentExit(parentPid, timeoutSeconds = 120)
            }
            val exitCode = runInstaller(installerPath)
            if (exitCode == 0 && !restartCommand.isNullOrBlank()) {
                runCatching { ProcessBuilder(restartCommand).start() }
            }
        } catch (_: Throwable) {
            // best effort helper
        }
    }

    private fun waitForParentExit(pid: Long, timeoutSeconds: Long) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutSeconds * 1_000) {
            val alive = ProcessHandle.of(pid).map { it.isAlive }.orElse(false)
            if (!alive) return
            Thread.sleep(500)
        }
    }

    private fun runInstaller(path: String): Int {
        val installer = File(path)
        if (!installer.exists()) return -1
        val ext = installer.extension.lowercase()
        val command = when (ext) {
            "msi" -> listOf("msiexec", "/i", installer.absolutePath, "/passive", "/norestart")
            "exe" -> listOf(installer.absolutePath)
            else -> return -2
        }
        val process = ProcessBuilder(command).start()
        process.waitFor(30, TimeUnit.MINUTES)
        return process.exitValue()
    }
}

