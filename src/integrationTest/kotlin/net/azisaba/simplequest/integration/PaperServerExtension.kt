package net.azisaba.simplequest.integration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Connects to a running Paper server and provides RCON command execution
 * and log verification helpers.
 *
 * Server lifecycle (start/stop) is managed externally by the CI workflow.
 * This class only handles RCON connection and log access.
 */
class PaperServerExtension(
    val server: ServerProcess,
) {
    val name: String get() = server.name

    /** Connect RCON. Call before tests. */
    fun connect() {
        server.connectRcon()
    }

    /** Disconnect RCON. Call after tests. */
    fun disconnect() {
        server.disconnectRcon()
    }

    /**
     * Execute a command on the server via RCON.
     */
    suspend fun executeCommand(command: String): String =
        withContext(Dispatchers.IO) {
            server.executeCommand(command)
        }

    /**
     * Check if the server log contains the given text (literal match).
     */
    suspend fun logContains(text: String): Boolean =
        withContext(Dispatchers.IO) {
            server.logContains(Regex.escape(text).toRegex())
        }

    /**
     * Assert that the server log contains the given text.
     */
    suspend fun assertLogContains(text: String) {
        val found = logContains(text)
        if (!found) {
            throw AssertionError(
                "$name server log should contain '$text'",
            )
        }
    }

    /**
     * Assert the server log has no ERROR/FATAL lines.
     */
    suspend fun assertNoErrors() {
        val errorPattern = Regex("(?i)(ERROR|FATAL|Exception)")
        val errors =
            withContext(Dispatchers.IO) {
                server.logLines().filter { errorPattern.containsMatchIn(it) }
            }
        if (errors.isNotEmpty()) {
            throw AssertionError(
                "$name log contains errors (${errors.size}):\n${errors.take(10).joinToString("\n")}",
            )
        }
    }
}

/**
 * Factory for creating PaperServerExtension from environment variables.
 */
fun paperServerExtension(
    name: String,
    portEnv: String,
    rconPortEnv: String,
): PaperServerExtension {
    val server = ServerProcess.fromEnv(name, portEnv, rconPortEnv)
    return PaperServerExtension(server)
}

/**
 * Creates all 3 server extensions (master, slave, readonly) from environment.
 */
fun allServerExtensions(): List<PaperServerExtension> =
    listOf(
        paperServerExtension("master", "MASTER_PORT", "MASTER_RCON_PORT"),
        paperServerExtension("slave", "SLAVE_PORT", "SLAVE_RCON_PORT"),
        paperServerExtension("readonly", "READONLY_PORT", "READONLY_RCON_PORT"),
    )
