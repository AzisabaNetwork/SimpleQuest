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
 */
class SimpleQuestIntegrationTest : FunSpec() {
    private val logPath =
        Path.of(
            System.getenv("SERVER_LOG")
                ?: "servers/master/logs/latest.log",
        )

    private fun logLines(): List<String> =
        if (logPath.toFile().exists()) {
            logPath.toFile().readLines()
        } else {
            emptyList()
        }

    private fun logContains(pattern: Regex): Boolean = logLines().any { pattern.containsMatchIn(it) }

    init {
        test("server logs 'SimpleQuest enabled'") {
            logContains(Regex("SimpleQuest enabled")) shouldBe true
        }

        test("plugin remapping completed successfully") {
            logContains(
                Regex("Done remapping plugin.*SimpleQuest"),
            ) shouldBe true
        }

        test("plugin loaded without fatal errors") {
            // Exclude known non-fatal messages:
            // - DB connection failures (expected without proper DB setup)
            // - Table missing warnings (migration may not have run)
            // - Nag messages about System.err usage
            val fatalPattern =
                Regex(
                    "(?i)(FATAL|CrashReport|OutOfMemory|" +
                        "java\\.lang\\.IllegalStateException|" +
                        "Could not load plugin)",
                )
            val fatalErrors =
                logLines().filter { fatalPattern.containsMatchIn(it) }
            if (fatalErrors.isNotEmpty()) {
                throw AssertionError(
                    "Log has fatal errors:\n" +
                        fatalErrors.take(10).joinToString("\n"),
                )
            }
        }
    }
}
