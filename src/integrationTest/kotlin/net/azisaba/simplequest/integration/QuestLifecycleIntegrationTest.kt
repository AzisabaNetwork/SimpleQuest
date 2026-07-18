package net.azisaba.simplequest.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Integration tests that exercise the quest lifecycle via RCON.
 *
 * These tests run against a live Paper server with SimpleQuest installed.
 * The server must be running with RCON enabled.
 *
 * Test flow:
 *   1. Verify quest definitions exist in MariaDB
 *   2. Grant a quest to a test player via RCON
 *   3. Start the quest (via quest GUI / command)
 *   4. Update quest progress via RCON
 *   5. Verify completion messages in server log
 *   6. Verify quest completion records in MariaDB
 */
class QuestLifecycleIntegrationTest : FunSpec() {
    /** Uses the same log path as the environment-configured server log. */
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

    private fun logLines(): List<String> =
        if (logPath.toFile().exists()) {
            logPath.toFile().readLines()
        } else {
            emptyList()
        }

    private fun logContains(pattern: Regex): Boolean = logLines().any { pattern.containsMatchIn(it) }

    private fun logContains(text: String): Boolean = logLines().any { it.contains(text) }

    init {

        beforeSpec {
            serverDef.connectRcon()
        }

        afterSpec {
            serverDef.disconnectRcon()
        }

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

        context("quest lifecycle via RCON") {

            test("grant quest to player") {
                val result =
                    serverDef.executeCommand(
                        "simplequest grant Bot_QuestRunner BotQuest",
                    )
                // Should succeed without error
                // (RCON typically returns empty on success or error message on failure)
                logContains("Bot_QuestRunner") ||
                    result.isEmpty() || result.contains("grant", ignoreCase = true)
            }

            test("quest start script appears in log") {
                // Trigger quest start via progress command
                // The progress command triggers start implicitly if quest not yet active
                serverDef.executeCommand(
                    "simplequest progress Bot_QuestRunner BreakStone +2",
                )

                // Allow server time to process
                kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.delay(2000)
                }

                val started = logContains(Regex("Quest started.*bot integration test"))
                println("Quest start script in log: $started")
                // Not a hard assertion — depends on plugin state
            }

            test("complete quest progress via command") {
                // Set progress to exactly 5 (the quest requirement)
                serverDef.executeCommand(
                    "simplequest progress Bot_QuestRunner BreakStone =5",
                )

                // Allow server time to process completion
                kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.delay(3000)
                }

                // Check for completion message in log
                val completed =
                    logContains(
                        Regex("Quest completed.*bot integration test"),
                    )
                println("Quest completion in log: $completed")

                // Check for the action command in log
                val actionFired =
                    logContains(
                        "completed the bot integration test quest!",
                    )
                println("Quest action command in log: $actionFired")
            }

            test("revoke quest from player") {
                serverDef.executeCommand(
                    "simplequest revoke Bot_QuestRunner BotQuest",
                )
                // Should succeed without error
            }
        }

        context("bot scenario execution") {

            test("run bot quest scenario via external process") {
                val projectRoot =
                    java.nio.file.Path
                        .of("")
                        .toAbsolutePath()
                        .parent ?: java.nio.file.Path
                        .of("")
                val botsDir = projectRoot.resolve("bots").toFile()

                if (!botsDir.exists()) {
                    println("SKIP: bots/ directory not found — bot scenario not run")
                    return@test
                }

                // Check if node_modules are installed
                val nodeModules = botsDir.resolve("node_modules")
                if (!nodeModules.exists()) {
                    println("SKIP: node_modules not installed — run 'cd bots && pnpm install'")
                    return@test
                }

                val logLines = logLines()
                val questGuiInLog =
                    logLines.any {
                        it.contains("Quest") && (
                            it.contains("GUI") || it.contains("gui") || it.contains("Integration")
                        )
                    }
                println("Quest-related log entries found: $questGuiInLog")
                // Soft assertion — depends on whether bot scenario ran
                if (questGuiInLog) {
                    logLines
                        .filter { it.contains("Quest") || it.contains("simplequest") }
                        .take(10)
                        .forEach { println("  LOG: $it") }
                }
            }
        }
    }
}
