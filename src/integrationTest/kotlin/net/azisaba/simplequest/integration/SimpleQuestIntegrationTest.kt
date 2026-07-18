package net.azisaba.simplequest.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

/**
 * Integration tests that verify a live Paper server instance.
 *
 * Prerequisites (handled by CI workflow):
 *   - 1 Paper server running (master:25565)
 *   - MariaDB on localhost:3306
 *   - Test quest definitions deployed
 *
 * RCON is not used — tests verify via logs and direct DB access.
 */
class SimpleQuestIntegrationTest : FunSpec() {
    private val logPath =
        Path.of(System.getenv("SERVER_LOG") ?: "servers/master/server.log")

    private fun logLines(): List<String> =
        if (logPath.toFile().exists()) {
            logPath.toFile().readLines()
        } else {
            emptyList()
        }

    private fun logContains(pattern: Regex): Boolean = logLines().any { pattern.containsMatchIn(it) }

    init {
        // S1: Basic lifecycle
        test("server logs 'SimpleQuest enabled'") {
            logContains(Regex("SimpleQuest enabled")) shouldBe true
        }

        test("no ERROR or FATAL in server startup logs") {
            val errorPattern = Regex("(?i)(ERROR|FATAL|Exception)")
            val exceptions =
                logLines().filter { errorPattern.containsMatchIn(it) }
            // Ignore expected connect failures
            val unexpected =
                exceptions.filterNot {
                    it.contains("ConnectException") ||
                        it.contains("Connection refused") ||
                        it.contains("Connection timed out")
                }
            if (unexpected.isNotEmpty()) {
                throw AssertionError(
                    "Log has unexpected exceptions:\n" +
                        unexpected.take(10).joinToString("\n"),
                )
            }
        }

        // S2: MariaDB verification
        test("quest definitions exist in MariaDB") {
            ServerAssertions.assertQuestDefinitionExists(
                "test/TestQuest%",
            )
        }
    }
}
