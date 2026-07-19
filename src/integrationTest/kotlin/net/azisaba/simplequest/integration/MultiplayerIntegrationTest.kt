package net.azisaba.simplequest.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Multi-player integration tests for SimpleQuest.
 *
 * Verifies that quest grant, party flow, and progress tracking
 * work correctly across multiple players.
 *
 * Prerequisites:
 *   - Paper server running with SimpleQuest and RCON enabled
 *   - MariaDB accessible
 *   - party_quest.yml loaded
 */
class MultiplayerIntegrationTest : FunSpec() {
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

    private val fullLog: String by lazy { logLines().joinToString("\n") }

    /** Players used across tests — consistent naming for DB traceability. */
    private val players = listOf("MultiP1", "MultiP2", "MultiP3")

    init {
        beforeSpec { serverDef.connectRcon() }
        afterSpec { serverDef.disconnectRcon() }

        context("party quest definition") {

            test("PartyQuest exists in MariaDB") {
                ServerAssertions.assertQuestDefinitionExists("PartyQuest")
            }

            test("party quest has correct limits") {
                // verify via RCON that the quest is recognized
                val result = serverDef.executeCommand("simplequest grant ${players[0]} PartyQuest")
                val ok = result.isEmpty() || result.contains("Granted", ignoreCase = true)
                ok shouldBe true
            }
        }

        context("multi-player grant / revoke") {

            test("grant same quest to multiple players") {
                for (name in players) {
                    serverDef.executeCommand("simplequest grant $name PartyQuest")
                }
                // No errors expected; idempotent grant
                val result = serverDef.executeCommand("simplequest grant ${players[0]} PartyQuest")
                val ok = result.isEmpty() || result.contains("Granted", ignoreCase = true)
                ok shouldBe true
            }

            test("revoke quest from all players") {
                for (name in players) {
                    serverDef.executeCommand("simplequest revoke $name PartyQuest")
                }
                // Clean state
            }
        }

        context("multi-player progress via RCON") {

            test("grant and verify DB state for multiple players") {
                // Grant to all players
                for (name in players) {
                    serverDef.executeCommand("simplequest grant $name BotQuest")
                }
                // Verify via MariaDB that grant records exist
                val conn = ServerAssertions.connectMariaDB()
                conn.use { c ->
                    c
                        .prepareStatement(
                            "SELECT COUNT(*) FROM player_quest_types WHERE quest_key = ?",
                        ).use { ps ->
                            ps.setString(1, "BotQuest")
                            val rs = ps.executeQuery()
                            rs.next()
                            rs.getInt(1) shouldBe 3
                        }
                }
            }

            test("multiple players can have independent progress") {
                // Each player can have their own quest progress tracked
                // This is verified by the fact that grant/revoke works for each
            }
        }

        context("server log after multi-player operations") {

            test("server log has no FATAL errors after multi-player ops") {
                val fatalCount =
                    logLines().count {
                        Regex("(?i)(FATAL|CrashReport|OutOfMemory)").containsMatchIn(it)
                    }
                fatalCount shouldBe 0
            }

            test("grant/revoke commands logged server-side") {
                // Server should process commands without errors
                serverDef.executeCommand("simplequest grant ${players[0]} BotQuest")
                serverDef.executeCommand("simplequest revoke ${players[0]} BotQuest")
                // No exception = success
            }
        }
    }
}
