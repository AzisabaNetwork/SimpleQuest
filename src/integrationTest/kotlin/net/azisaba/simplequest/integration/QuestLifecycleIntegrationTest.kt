package net.azisaba.simplequest.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class QuestLifecycleIntegrationTest :
    FunSpec({

        val logPath =
            java.nio.file.Path.of(
                System.getenv("SERVER_LOG")
                    ?: "servers/master/logs/latest.log",
            )

        val serverDef =
            ServerProcess.fromEnv(
                name = "master",
                portEnv = "MASTER_PORT",
                rconPortEnv = "MASTER_RCON_PORT",
            )

        fun logLines(): List<String> = if (logPath.toFile().exists()) logPath.toFile().readLines() else emptyList()

        val fullLog: String by lazy { logLines().joinToString("\n") }

        beforeSpec { serverDef.connectRcon() }
        afterSpec { serverDef.disconnectRcon() }

        context("database state") {

            test("quest definitions exist in MariaDB") {
                ServerAssertions.assertQuestDefinitionsExist()
            }

            test("test quest 'TestQuest' exists") {
                ServerAssertions.assertQuestDefinitionExists("TestQuest")
            }

            test("bot quest 'BotQuest' exists") {
                ServerAssertions.assertQuestDefinitionExists("BotQuest")
            }

            test("party quest 'PartyQuest' exists") {
                ServerAssertions.assertQuestDefinitionExists("PartyQuest")
            }

            test("no conflict flags") {
                ServerAssertions.assertNoConflicts()
            }
        }

        context("quest grant via RCON") {

            test("grant BotQuest succeeds") {
                val result = serverDef.executeCommand("simplequest grant BotTester BotQuest")
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

        context("server log verification") {

            test("plugin enabled in log") {
                fullLog shouldContain "SimpleQuest enabled"
            }

            test("no FATAL errors") {
                val fatalCount =
                    logLines().count {
                        Regex("(?i)(FATAL|CrashReport|OutOfMemory)").containsMatchIn(it)
                    }
                fatalCount shouldBe 0
            }
        }
    })
