package net.azisaba.simplequest.integration

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Integration tests that run against live Paper server instances.
 *
 * Prerequisites (handled by CI workflow):
 *   - 3 Paper servers running (master:25565, slave:25566, readonly:25567)
 *   - MariaDB on localhost:3306
 *   - Redis on localhost:6379
 *   - RCON enabled with password "test"
 */
class SimpleQuestIntegrationTest : FunSpec() {
    private val master =
        paperServerExtension("master", "MASTER_PORT", "MASTER_RCON_PORT")
    private val slave =
        paperServerExtension("slave", "SLAVE_PORT", "SLAVE_RCON_PORT")
    private val readOnly =
        paperServerExtension("readonly", "READONLY_PORT", "READONLY_RCON_PORT")

    private val allServers = listOf(master, slave, readOnly)

    override suspend fun beforeSpec(spec: Spec) {
        allServers.forEach { it.connect() }
    }

    override suspend fun afterSpec(spec: Spec) {
        allServers.forEach { it.disconnect() }
    }

    init {
        // S1: Basic lifecycle
        context("Basic lifecycle") {
            test("all 3 servers log 'SimpleQuest enabled'") {
                for (s in allServers) {
                    s.logContains("SimpleQuest enabled") shouldBe true
                }
            }

            test("no ERROR or FATAL in server startup logs") {
                for (s in allServers) {
                    s.assertNoErrors()
                }
            }
        }

        // S2: Multi-server sync
        context("Multi-server sync") {
            test("master writes quest definitions to MySQL on reload") {
                val result = master.executeCommand("simplequest reload")
                result shouldContain "reloaded"
                master.assertLogContains("Sync completed")
            }

            test("quest definitions exist in MariaDB after master reload") {
                ServerAssertions.assertQuestDefinitionsExist()
            }

            test("slave receives quest definitions from MySQL on reload") {
                val result = slave.executeCommand("simplequest reload")
                result shouldContain "reloaded"
            }
        }

        // S5: Player data persistence
        context("Player data persistence") {
            test("MariaDB quest_definitions table is reachable") {
                ServerAssertions.assertQuestDefinitionExists("test/TestQuest%")
            }
        }

        // S6: Fault tolerance
        context("Fault tolerance") {
            test("all 3 servers started without exceptions") {
                val pattern = Regex("(?i)(Exception|Caused by)")
                for (s in allServers) {
                    val exceptions =
                        s.server
                            .logLines()
                            .filter { pattern.containsMatchIn(it) }
                    val unexpected =
                        exceptions.filterNot {
                            it.contains("ConnectException") ||
                                it.contains("Connection refused") ||
                                it.contains("Connection timed out")
                        }
                    if (unexpected.isNotEmpty()) {
                        throw AssertionError(
                            "${s.name} log has exceptions:\n" +
                                unexpected.take(10).joinToString("\n"),
                        )
                    }
                }
            }

            test("readonly server can still load quest UI") {
                val result = readOnly.executeCommand("simplequest quest")
                result shouldContain "Player"
            }
        }
    }
}
