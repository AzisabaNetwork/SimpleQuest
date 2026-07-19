package net.azisaba.simplequest.integration

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

/**
 * Represents a running Paper server process managed externally (by CI).
 * Provides RCON command execution and log file access.
 */
class ServerProcess(
    val name: String,
    val host: String,
    val port: Int,
    val rconPort: Int,
    val rconPassword: String,
    val logPath: Path,
) {
    private var rcon: RconClient? = null

    fun connectRcon() {
        var lastEx: Exception? = null
        repeat(10) { attempt ->
            try {
                rcon = RconClient(host, rconPort, rconPassword).also { it.connect() }
                return
            } catch (e: Exception) {
                lastEx = e
                if (attempt < 9) {
                    Thread.sleep(2000)
                }
            }
        }
        throw IllegalStateException("RCON connection failed after 10 attempts", lastEx)
    }

    fun disconnectRcon() {
        rcon?.disconnect()
        rcon = null
    }

    fun executeCommand(command: String): String {
        val client = rcon ?: throw IllegalStateException("RCON not connected")
        return client.executeCommand(command)
    }

    fun logContains(pattern: Regex): Boolean {
        if (!logPath.exists()) return false
        return logPath.readLines().any { line -> pattern.containsMatchIn(line) }
    }

    fun logLines(): List<String> {
        if (!logPath.exists()) return emptyList()
        return logPath.readLines()
    }

    companion object {
        fun fromEnv(
            name: String,
            portEnv: String,
            rconPortEnv: String,
            logPathEnv: String? = null,
        ): ServerProcess {
            val port = System.getenv(portEnv)?.toIntOrNull() ?: 25565
            val rconPort = System.getenv(rconPortEnv)?.toIntOrNull() ?: 25575
            val password = System.getenv("RCON_PASSWORD") ?: "test"

            val logPath =
                if (logPathEnv != null) {
                    Path.of(System.getenv(logPathEnv) ?: "servers/$name/server.log")
                } else {
                    Path.of("servers/$name/server.log")
                }

            return ServerProcess(
                name = name,
                host = "localhost",
                port = port,
                rconPort = rconPort,
                rconPassword = password,
                logPath = logPath,
            )
        }
    }
}
