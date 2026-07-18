package net.azisaba.simplequest.domain.script

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ScriptExtTest :
    FunSpec({

        context("Script.Trigger") {
            test("all trigger names") {
                Script.Trigger.entries.map { it.name } shouldBe listOf("START", "END", "COMPLETE", "CANCEL")
            }

            test("trigger inequality") {
                Script.Trigger.START shouldNotBe Script.Trigger.END
                Script.Trigger.COMPLETE shouldNotBe Script.Trigger.CANCEL
                Script.Trigger.START shouldNotBe Script.Trigger.COMPLETE
            }

            test("trigger valueOf") {
                Script.Trigger.valueOf("START") shouldBe Script.Trigger.START
                Script.Trigger.valueOf("END") shouldBe Script.Trigger.END
            }
        }

        context("Script data class variations") {
            test("Script with single command") {
                val s = Script(trigger = Script.Trigger.START, delay = 0L, commands = listOf("say hello"))
                s.trigger shouldBe Script.Trigger.START
                s.delay shouldBe 0L
                s.commands shouldBe listOf("say hello")
            }

            test("Script with many commands") {
                val cmds = (1..20).map { "command $it" }
                val s = Script(trigger = Script.Trigger.COMPLETE, delay = 0L, commands = cmds)
                s.commands.size shouldBe 20
            }

            test("Script with long delay") {
                val s = Script(trigger = Script.Trigger.START, delay = 3600L, commands = listOf("delayed"))
                s.delay shouldBe 3600L
            }

            test("Script with negative delay") {
                val s = Script(trigger = Script.Trigger.START, delay = -100L, commands = listOf("test"))
                s.delay shouldBe -100L
            }

            test("Script with empty commands list") {
                val s = Script(trigger = Script.Trigger.END, delay = 0L, commands = emptyList())
                s.commands shouldBe emptyList()
            }

            test("all trigger types with same commands") {
                val cmds = listOf("say hi")
                Script.Trigger.entries.forEach { trigger ->
                    val s = Script(trigger = trigger, delay = 0L, commands = cmds)
                    s.trigger shouldBe trigger
                    s.commands shouldBe cmds
                }
            }

            test("Script equality by value") {
                val a = Script(Script.Trigger.START, 10L, listOf("cmd1", "cmd2"))
                val b = Script(Script.Trigger.START, 10L, listOf("cmd1", "cmd2"))
                a shouldBe b
            }

            test("Script inequality by trigger") {
                val a = Script(Script.Trigger.START, 0L, listOf("cmd"))
                val b = Script(Script.Trigger.END, 0L, listOf("cmd"))
                (a == b) shouldBe false
            }

            test("Script inequality by delay") {
                val a = Script(Script.Trigger.START, 0L, listOf("cmd"))
                val b = Script(Script.Trigger.START, 10L, listOf("cmd"))
                (a == b) shouldBe false
            }

            test("Script inequality by commands") {
                val a = Script(Script.Trigger.START, 0L, listOf("cmd1"))
                val b = Script(Script.Trigger.START, 0L, listOf("cmd2"))
                (a == b) shouldBe false
            }

            test("Script copy maintains all fields") {
                val s = Script(Script.Trigger.COMPLETE, 50L, listOf("a", "b"))
                val copy = s.copy(delay = 100L)
                copy.trigger shouldBe Script.Trigger.COMPLETE
                copy.delay shouldBe 100L
                copy.commands shouldBe listOf("a", "b")
            }

            test("Script with special characters in commands") {
                val s =
                    Script(
                        Script.Trigger.START,
                        0L,
                        listOf(
                            "say &aHello &bWorld",
                            "give % minecraft:diamond{display:{Name:'\"Special\"'}} 1",
                            "broadcast <player> completed",
                        ),
                    )
                s.commands.size shouldBe 3
                s.commands[0] shouldBe "say &aHello &bWorld"
                s.commands[1] shouldBe "give % minecraft:diamond{display:{Name:'\"Special\"'}} 1"
                s.commands[2] shouldBe "broadcast <player> completed"
            }
        }

        context("Script Trigger string representations") {
            test("trigger names are case-sensitive") {
                Script.Trigger.START.name shouldBe "START"
                Script.Trigger.END.name shouldBe "END"
                Script.Trigger.COMPLETE.name shouldBe "COMPLETE"
                Script.Trigger.CANCEL.name shouldBe "CANCEL"
            }
        }
    })
