package net.azisaba.lifequest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.azisaba.lifequest.domain.script.Script

class ScriptTest :
    FunSpec({

        test("Script holds all fields") {
            val s = Script(trigger = Script.Trigger.START, delay = 20L, commands = listOf("say hello", "give % minecraft:diamond"))
            s.trigger shouldBe Script.Trigger.START
            s.delay shouldBe 20L
            s.commands shouldBe listOf("say hello", "give % minecraft:diamond")
        }

        test("Script with zero delay") {
            val s = Script(trigger = Script.Trigger.COMPLETE, delay = 0L, commands = listOf("say done"))
            s.delay shouldBe 0L
            s.commands.size shouldBe 1
        }

        test("Script with delay") {
            val s = Script(trigger = Script.Trigger.START, delay = 200L, commands = listOf("say delayed"))
            s.delay shouldBe 200L
        }

        test("Script with empty commands") {
            val s = Script(trigger = Script.Trigger.CANCEL, delay = 0L, commands = emptyList())
            s.commands shouldBe emptyList()
        }

        test("different triggers") {
            val s1 = Script(trigger = Script.Trigger.START, delay = 0L, commands = emptyList())
            val s2 = Script(trigger = Script.Trigger.END, delay = 0L, commands = emptyList())
            val s3 = Script(trigger = Script.Trigger.COMPLETE, delay = 0L, commands = emptyList())
            val s4 = Script(trigger = Script.Trigger.CANCEL, delay = 0L, commands = emptyList())
            s1.trigger shouldNotBe s2.trigger
            s2.trigger shouldNotBe s3.trigger
            s3.trigger shouldNotBe s4.trigger
        }

        test("Script data class equality") {
            val a = Script(trigger = Script.Trigger.START, delay = 0L, commands = listOf("say hi"))
            val b = Script(trigger = Script.Trigger.START, delay = 0L, commands = listOf("say hi"))
            a shouldBe b
        }

        test("Script enums values") {
            Script.Trigger.entries.size shouldBe 4
            Script.Trigger.START.name shouldBe "START"
            Script.Trigger.END.name shouldBe "END"
            Script.Trigger.COMPLETE.name shouldBe "COMPLETE"
            Script.Trigger.CANCEL.name shouldBe "CANCEL"
        }
    })
