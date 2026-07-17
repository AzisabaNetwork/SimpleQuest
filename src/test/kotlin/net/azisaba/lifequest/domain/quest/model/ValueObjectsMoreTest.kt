package net.azisaba.lifequest.domain.quest.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.azisaba.lifequest.domain.data.Icon
import net.azisaba.lifequest.domain.data.Location

class ValueObjectsMoreTest :
    FunSpec({

        context("PlayLimits additional") {
            test("isUnlimited with all null") {
                PlayLimits().isUnlimited shouldBe true
            }

            test("isUnlimited with daily set is false") {
                PlayLimits(daily = 1).isUnlimited shouldBe false
            }

            test("isUnlimited with weekly set is false") {
                PlayLimits(weekly = 1).isUnlimited shouldBe false
            }

            test("isUnlimited with monthly set is false") {
                PlayLimits(monthly = 1).isUnlimited shouldBe false
            }

            test("isUnlimited with yearly set is false") {
                PlayLimits(yearly = 1).isUnlimited shouldBe false
            }

            test("isUnlimited with lifetime set is false") {
                PlayLimits(lifetime = 1).isUnlimited shouldBe false
            }

            test("limits equality") {
                PlayLimits(daily = 5, weekly = 10) shouldBe PlayLimits(daily = 5, weekly = 10)
                PlayLimits(daily = 5) shouldNotBe PlayLimits(daily = 10)
            }

            test("null vs zero daily") {
                PlayLimits().daily.shouldBeNull()
                PlayLimits(daily = 0).daily shouldBe 0
            }
        }

        context("AcceptConditions additional") {
            test("empty conditions equality") {
                AcceptConditions() shouldBe AcceptConditions()
            }

            test("different money values") {
                val a = AcceptConditions(money = 100.0)
                val b = AcceptConditions(money = 200.0)
                (a == b) shouldBe false
            }

            test("different pveLevel values") {
                val a = AcceptConditions(pveLevel = 10)
                val b = AcceptConditions(pveLevel = 20)
                (a == b) shouldBe false
            }

            test("copy maintains fields") {
                val c = AcceptConditions(pveLevel = 5, money = 50.0, partyMode = true)
                val copy = c.copy(pveLevel = 10)
                copy.pveLevel shouldBe 10
                copy.money shouldBe 50.0
                copy.partyMode shouldBe true
            }

            test("requiredQuests and permissions fields") {
                val c =
                    AcceptConditions(
                        requiredQuests = listOf("lq:tutorial", "lq:prologue"),
                        permissions = listOf("lifequest.vip"),
                    )
                c.requiredQuests shouldBe listOf("lq:tutorial", "lq:prologue")
                c.permissions shouldBe listOf("lifequest.vip")
            }
        }

        context("QuestRequirement additional") {
            test("requirement equality") {
                val a = QuestRequirement("kill", 10)
                val b = QuestRequirement("kill", 10)
                a shouldBe b
            }

            test("requirement inequality by key") {
                QuestRequirement("kill", 10) shouldNotBe QuestRequirement("collect", 10)
            }

            test("requirement inequality by amount") {
                QuestRequirement("kill", 10) shouldNotBe QuestRequirement("kill", 20)
            }

            test("maximum Int amount") {
                val r = QuestRequirement("grind", Int.MAX_VALUE)
                r.amount shouldBe Int.MAX_VALUE
            }

            test("zero is valid") {
                val r = QuestRequirement("auto", 0)
                r.amount shouldBe 0
            }
        }

        context("QuestState transition mapping") {
            test("ACTIVE is not terminal") {
                QuestState.ACTIVE.isTerminal shouldBe false
            }

            test("COMPLETED is terminal") {
                QuestState.COMPLETED.isTerminal shouldBe true
            }

            test("CANCELLED is terminal") {
                QuestState.CANCELLED.isTerminal shouldBe true
            }

            test("FAILED is terminal") {
                QuestState.FAILED.isTerminal shouldBe true
            }

            test("all terminal states should not be ACTIVE") {
                listOf(QuestState.COMPLETED, QuestState.CANCELLED, QuestState.FAILED).forEach {
                    it shouldNotBe QuestState.ACTIVE
                }
            }
        }

        context("EndReason to QuestState mapping") {
            test("COMPLETE -> COMPLETED") {
                EndReason.COMPLETE.toQuestState() shouldBe QuestState.COMPLETED
            }

            test("CANCEL -> CANCELLED") {
                EndReason.CANCEL.toQuestState() shouldBe QuestState.CANCELLED
            }

            test("DEATH_LIMIT -> CANCELLED") {
                EndReason.DEATH_LIMIT.toQuestState() shouldBe QuestState.CANCELLED
            }

            test("PLUGIN -> FAILED") {
                EndReason.PLUGIN.toQuestState() shouldBe QuestState.FAILED
            }

            test("RELOAD -> FAILED") {
                EndReason.RELOAD.toQuestState() shouldBe QuestState.FAILED
            }

            test("OTHER -> FAILED") {
                EndReason.OTHER.toQuestState() shouldBe QuestState.FAILED
            }
        }

        context("Icon model field") {
            test("model can be set independently of CMD") {
                val icon = Icon(type = "PAPER", customModelData = null, aura = false, model = "custom:model")
                icon.customModelData.shouldBeNull()
                icon.model shouldBe "custom:model"
            }

            test("model and CMD together") {
                val icon = Icon(type = "DIAMOND", customModelData = 1, model = "override")
                icon.customModelData shouldBe 1
                icon.model shouldBe "override"
            }
        }

        context("Location coordinate extremes") {
            test("maximum double coordinates") {
                val loc = Location("world", Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
                loc.x shouldBe Double.MAX_VALUE
            }

            test("minimum double coordinates") {
                val loc = Location("world", Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE)
                loc.x shouldBe Double.MIN_VALUE
            }

            test("NaN y coordinate") {
                val loc = Location("world", 0.0, Double.NaN, 0.0)
                loc.y.isNaN() shouldBe true
            }
        }

        context("GameGuide with complex requirements") {
            test("multiple requirements in guide") {
                val guide =
                    GameGuide(
                        title = "Boss Room",
                        location = Location("world", 100.0, 64.0, 100.0),
                        requirements = mapOf("kill" to 10, "level" to 20),
                    )
                guide.requirements.size shouldBe 2
                guide.requirements["kill"] shouldBe 10
                guide.requirements["level"] shouldBe 20
            }
        }
    })
