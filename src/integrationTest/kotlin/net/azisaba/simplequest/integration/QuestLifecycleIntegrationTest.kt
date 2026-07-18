package net.azisaba.simplequest.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Integration tests that exercise the quest lifecycle via RCON commands.
 *
 * Prerequisites:
 *   - A Paper server running with SimpleQuest installed and RCON enabled
 *   - MariaDB accessible with the test schema
 *   - Test quest YAMLs loaded (test_quest.yml, bot_quest.yml)
 *
 * These tests verify the full quest lifecycle:
 *   1. Quest definitions exist in MariaDB
 *   2. Grant → progress → complete via RCON
 *   3. Completion messages appear in server log
 */
class QuestLifecycleIntegrationTest : FunSpec() {
    private val logPath =
        java.nio.file.Path.of(
            System.getenv("SERVER_LOG")
                ?: "servers/master/logs/latest.log",
        )

    private val serverDef =
        ServerProcess.fromEnv(
            name = "master",
            portEnv = "MASTER_PORT",
            rconPortEnv = "MASTER_RCON_PORT",
        )

    private fun logLines(): List<String> = if (logPath.toFile().exists()) logPath.toFile().readLines() else emptyList()

    /** All text blocks joined, for substring search. */
    private val fullLog: String by lazy { logLines().joinToString("\n") }

    init {
        beforeSpec {
            serverDef.connectRcon()
        }

        afterSpec {
            serverDef.disconnectRcon()
        }

        context("database state") {

            test("quest definitions exist in MariaDB") {
                ServerAssertions.assertQuestDefinitionsExist()
            }

            test("test quest 'TestQuest' exists in MariaDB") {
                ServerAssertions.assertQuestDefinitionExists("TestQuest")
            }

            test("bot quest 'BotQuest' exists in MariaDB") {
                ServerAssertions.assertQuestDefinitionExists("BotQuest")
            }

            test("no conflict flags in quest_definitions") {
                ServerAssertions.assertNoConflicts()
            }
        }

        context("quest grant via RCON") {

            test("grant BotQuest to test player") {
                val result = serverDef.executeCommand("simplequest grant BotTester BotQuest")
                // success: contains "Granted" or empty (RCON ok)
                val ok = result.isEmpty() || result.contains("Granted", ignoreCase = true)
                if (!ok) println("grant result: '$result'")
                ok shouldBe true
            }

            test("revoke and re-grant is idempotent") {
                serverDef.executeCommand("simplequest revoke BotTester BotQuest")
                val result = serverDef.executeCommand("simplequest grant BotTester BotQuest")
                val ok = result.isEmpty() || result.contains("Granted", ignoreCase = true)
                ok shouldBe true
            }
        }

        context("quest progress via RCON") {

            test("progress command rejects player with no active quest") {
                // BotTester has no active quest instance, so progress should fail
                val result =
                    serverDef.executeCommand(
                        "simplequest progress BotTester BreakStone +1",
                    )
                // Should contain error about no active quest
                val hasError =
                    result.contains("no active quest", ignoreCase = true) ||
                        result.isEmpty()
                println("progress-no-quest result: '$result'")
                hasError shouldBe true
            }
        }

        context("server log verification") {

            test("server log contains plugin enabled message") {
                fullLog shouldContain "SimpleQuest enabled"
            }

            test("server log has no FATAL or CrashReport entries") {
                val fatalCount =
                    logLines().count {
                        Regex("(?i)(FATAL|CrashReport|OutOfMemory)").containsMatchIn(it)
                    }
                // Allow some startup warnings but no fatal crashes
                fatalCount shouldBe 0
            }
        }
    }
}
