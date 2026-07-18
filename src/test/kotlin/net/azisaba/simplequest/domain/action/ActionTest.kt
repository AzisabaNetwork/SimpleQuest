package net.azisaba.simplequest.domain.action

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ActionTest :
    FunSpec({

        context("ActionType") {
            test("all four action types exist") {
                ActionType.entries.size shouldBe 4
                ActionType.COMMAND.name shouldBe "COMMAND"
                ActionType.ITEM_GIVE.name shouldBe "ITEM_GIVE"
                ActionType.MYTHIC_ITEM_GIVE.name shouldBe "MYTHIC_ITEM_GIVE"
                ActionType.PVELEVEL_EXP.name shouldBe "PVELEVEL_EXP"
            }

            test("valueOf works for all types") {
                ActionType.valueOf("COMMAND") shouldBe ActionType.COMMAND
                ActionType.valueOf("ITEM_GIVE") shouldBe ActionType.ITEM_GIVE
                ActionType.valueOf("MYTHIC_ITEM_GIVE") shouldBe ActionType.MYTHIC_ITEM_GIVE
                ActionType.valueOf("PVELEVEL_EXP") shouldBe ActionType.PVELEVEL_EXP
            }
        }

        context("Action data class") {
            test("construct command action with all fields") {
                val a =
                    Action(
                        type = ActionType.COMMAND,
                        command = "give % minecraft:diamond 1",
                        material = null,
                        amount = null,
                        item = null,
                    )
                a.type shouldBe ActionType.COMMAND
                a.command shouldBe "give % minecraft:diamond 1"
                a.material.shouldBeNull()
                a.amount.shouldBeNull()
                a.item.shouldBeNull()
            }

            test("construct item give action") {
                val a =
                    Action(
                        type = ActionType.ITEM_GIVE,
                        material = "minecraft:diamond",
                        amount = 5,
                    )
                a.type shouldBe ActionType.ITEM_GIVE
                a.material shouldBe "minecraft:diamond"
                a.amount shouldBe 5
                a.command.shouldBeNull()
            }

            test("construct mythic item action") {
                val a =
                    Action(
                        type = ActionType.MYTHIC_ITEM_GIVE,
                        item = "MMOre",
                        amount = 3,
                    )
                a.type shouldBe ActionType.MYTHIC_ITEM_GIVE
                a.item shouldBe "MMOre"
                a.amount shouldBe 3
            }

            test("construct PvE level exp action") {
                val a = Action(type = ActionType.PVELEVEL_EXP, amount = 100)
                a.type shouldBe ActionType.PVELEVEL_EXP
                a.amount shouldBe 100
            }

            test("action equality") {
                val a1 = Action(type = ActionType.COMMAND, command = "say hi", amount = 1)
                val a2 = Action(type = ActionType.COMMAND, command = "say hi", amount = 1)
                a1 shouldBe a2
            }

            test("action inequality by type") {
                val a1 = Action(type = ActionType.COMMAND, command = "say hi")
                val a2 = Action(type = ActionType.ITEM_GIVE, material = "STONE", amount = 1)
                (a1 == a2) shouldBe false
            }

            test("action inequality by command") {
                val a1 = Action(type = ActionType.COMMAND, command = "say hi")
                val a2 = Action(type = ActionType.COMMAND, command = "say bye")
                (a1 == a2) shouldBe false
            }

            test("large amount values") {
                val a = Action(type = ActionType.PVELEVEL_EXP, amount = 999_999)
                a.amount shouldBe 999_999
            }

            test("zero amount is valid") {
                val a = Action(type = ActionType.ITEM_GIVE, material = "AIR", amount = 0)
                a.amount shouldBe 0
            }
        }

        context("ActionSet data class") {
            test("construct with both action lists") {
                val set =
                    ActionSet(
                        onFirstComplete = listOf(Action(ActionType.COMMAND, command = "say first")),
                        onComplete =
                            listOf(
                                Action(ActionType.ITEM_GIVE, material = "DIAMOND", amount = 1),
                                Action(ActionType.PVELEVEL_EXP, amount = 50),
                            ),
                    )
                set.onFirstComplete.size shouldBe 1
                set.onComplete.size shouldBe 2
            }

            test("default empty lists") {
                val set = ActionSet()
                set.onFirstComplete shouldBe emptyList()
                set.onComplete shouldBe emptyList()
            }

            test("only onFirstComplete set") {
                val set = ActionSet(onFirstComplete = listOf(Action(ActionType.COMMAND, command = "say")))
                set.onFirstComplete.size shouldBe 1
                set.onComplete shouldBe emptyList()
            }

            test("only onComplete set") {
                val set = ActionSet(onComplete = listOf(Action(ActionType.MYTHIC_ITEM_GIVE, item = "Sword", amount = 1)))
                set.onFirstComplete shouldBe emptyList()
                set.onComplete.size shouldBe 1
            }

            test("equality") {
                val s1 = ActionSet(onFirstComplete = listOf(Action(ActionType.COMMAND, command = "test")))
                val s2 = ActionSet(onFirstComplete = listOf(Action(ActionType.COMMAND, command = "test")))
                s1 shouldBe s2
            }
        }
    })
