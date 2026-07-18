package net.azisaba.simplequest.integration

import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Integration tests that run against a live Paper server instance.
 *
 * Prerequisites (handled by CI workflow):
 *   - 1 Paper server running (master:25565)
 *   - MariaDB on localhost:3306
 *   - Redis on localhost:6379
 *   - RCON enabled with password "test"
 */
class SimpleQuestIntegrationTest : FunSpec() {
    private val master =
        paperServerExtension("master", "MASTER_PORT", "MASTER_RCON_PORT")

    override suspend fun beforeSpec(spec: Spec) {
        master.connect()
    }

    override suspend fun afterSpec(spec: Spec) {
        master.disconnect()
    }

    init {
        // S1: Basic lifecycle
        context("Basic lifecycle") {
            test("server logs 'SimpleQuest enabled'") {
                master.logContains("SimpleQuest enabled") shouldBe true
            }

            test("no ERROR or FATAL in server startup logs") {
                master.assertNoErrors()
            }
        }

        // S2: Multi-server sync (verified via MariaDB)
        context("Multi-server sync") {
            test("quest definitions exist in MariaDB") {
                ServerAssertions.assertQuestDefinitionExists(
                    "test/TestQuest%",
                )
            }
        }

        // S6: Fault tolerance
        context("Fault tolerance") {
            test("server started without unexpected exceptions") {
                val pattern = Regex("(?i)(Exception|Caused by)")
                val exceptions =
                    master.server
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
                        "Log has exceptions:\n" +
                            unexpected.take(10).joinToString("\n"),
                    )
                }
            }
        }
    }
}
