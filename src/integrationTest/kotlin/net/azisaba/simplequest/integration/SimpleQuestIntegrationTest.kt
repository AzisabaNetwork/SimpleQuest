package net.azisaba.simplequest.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

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
        test("plugin loaded and enabled") {
            logContains(Regex("SimpleQuest enabled")) shouldBe true
        }

        test("plugin remapping completed") {
            logContains(
                Regex("Done remapping plugin.*SimpleQuest"),
            ) shouldBe true
        }

        test("database migration applied") {
            logContains(
                Regex("Flyway migration completed.*\\d+ migration"),
            ) shouldBe true
        }

        test("MariaDB quest_definitions table exists") {
            ServerAssertions.assertQuestDefinitionTableExists()
        }

        test("no fatal errors in server log") {
            val fatalPattern =
                Regex(
                    "(?i)(FATAL|CrashReport|OutOfMemory|" +
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
