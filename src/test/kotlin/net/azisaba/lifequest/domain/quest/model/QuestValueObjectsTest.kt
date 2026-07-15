package net.azisaba.lifequest.domain.quest.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import net.azisaba.lifequest.domain.data.Icon
import net.azisaba.lifequest.domain.data.Location

class QuestValueObjectsTest :
    FunSpec({

        context("QuestRequirement") {
            test("holds key and amount") {
                val r = QuestRequirement(key = "kill_zombie", amount = 10)
                r.key shouldBe "kill_zombie"
                r.amount shouldBe 10
            }

            test("amount can be zero") {
                val r = QuestRequirement(key = "auto_complete", amount = 0)
                r.amount shouldBe 0
            }

            test("amount can be large") {
                val r = QuestRequirement(key = "grind", amount = 10_000)
                r.amount shouldBe 10000
            }
        }

        context("PlayLimits") {
            test("all fields default to null") {
                val limits = PlayLimits()
                limits.weekly.shouldBeNull()
                limits.monthly.shouldBeNull()
                limits.yearly.shouldBeNull()
                limits.lifetime.shouldBeNull()
                limits.daily.shouldBeNull()
            }

            test("partial fields set") {
                val limits = PlayLimits(weekly = 3, lifetime = 1)
                limits.weekly shouldBe 3
                limits.lifetime shouldBe 1
                limits.monthly.shouldBeNull()
                limits.yearly.shouldBeNull()
                limits.daily.shouldBeNull()
            }

            test("all fields set") {
                val limits = PlayLimits(daily = 5, weekly = 7, monthly = 30, yearly = 365, lifetime = 100)
                limits.weekly shouldBe 7
                limits.monthly shouldBe 30
                limits.yearly shouldBe 365
                limits.lifetime shouldBe 100
                limits.daily shouldBe 5
            }

            test("no limit means unchecked") {
                val noLimit = PlayLimits()
                noLimit.isUnlimited shouldBe true

                val limited = PlayLimits(daily = 3)
                limited.isUnlimited shouldBe false
            }
        }

        context("AcceptConditions") {
            test("all defaults") {
                val c = AcceptConditions()
                c.pveLevel.shouldBeNull()
                c.requiredQuests.shouldBeNull()
                c.permissions.shouldBeNull()
                c.partyMode shouldBe false
                c.money.shouldBeNull()
            }

            test("all fields set") {
                val c =
                    AcceptConditions(
                        pveLevel = 10,
                        requiredQuests = listOf("lq:prologue"),
                        permissions = listOf("group.vip"),
                        partyMode = true,
                        money = 100.0,
                    )
                c.pveLevel shouldBe 10
                c.requiredQuests shouldBe listOf("lq:prologue")
                c.permissions shouldBe listOf("group.vip")
                c.partyMode shouldBe true
                c.money shouldBe 100.0
            }

            test("partyMode only") {
                val c = AcceptConditions(partyMode = true)
                c.partyMode shouldBe true
                c.pveLevel.shouldBeNull()
                c.money.shouldBeNull()
            }
        }

        context("QuestState") {
            test("values are correct") {
                QuestState.entries.size shouldBe 4
                QuestState.ACTIVE shouldBe QuestState.valueOf("ACTIVE")
                QuestState.COMPLETED shouldBe QuestState.valueOf("COMPLETED")
                QuestState.CANCELLED shouldBe QuestState.valueOf("CANCELLED")
                QuestState.FAILED shouldBe QuestState.valueOf("FAILED")
            }

            test("terminal states") {
                QuestState.ACTIVE.isTerminal shouldBe false
                QuestState.COMPLETED.isTerminal shouldBe true
                QuestState.CANCELLED.isTerminal shouldBe true
                QuestState.FAILED.isTerminal shouldBe true
            }
        }

        context("EndReason") {
            test("all values present") {
                EndReason.entries.size shouldBe 6
                EndReason.COMPLETE shouldBe EndReason.valueOf("COMPLETE")
                EndReason.CANCEL shouldBe EndReason.valueOf("CANCEL")
                EndReason.DEATH_LIMIT shouldBe EndReason.valueOf("DEATH_LIMIT")
                EndReason.PLUGIN shouldBe EndReason.valueOf("PLUGIN")
                EndReason.RELOAD shouldBe EndReason.valueOf("RELOAD")
                EndReason.OTHER shouldBe EndReason.valueOf("OTHER")
            }

            test("maps to QuestState") {
                EndReason.COMPLETE.toQuestState() shouldBe QuestState.COMPLETED
                EndReason.CANCEL.toQuestState() shouldBe QuestState.CANCELLED
                EndReason.DEATH_LIMIT.toQuestState() shouldBe QuestState.CANCELLED
                EndReason.PLUGIN.toQuestState() shouldBe QuestState.FAILED
                EndReason.RELOAD.toQuestState() shouldBe QuestState.FAILED
                EndReason.OTHER.toQuestState() shouldBe QuestState.FAILED
            }
        }

        context("Icon") {
            test("full constructor") {
                val icon = Icon(type = "STONE_SWORD", customModelData = 3001, aura = true)
                icon.type shouldBe "STONE_SWORD"
                icon.customModelData shouldBe 3001
                icon.aura shouldBe true
                icon.model.shouldBeNull()
            }

            test("minimal constructor") {
                val icon = Icon(type = "PAPER")
                icon.type shouldBe "PAPER"
                icon.customModelData.shouldBeNull()
                icon.aura shouldBe false
                icon.model.shouldBeNull()
            }

            test("with model") {
                val icon = Icon(type = "DIAMOND_SWORD", model = "minecraft:netherite_sword")
                icon.type shouldBe "DIAMOND_SWORD"
                icon.model shouldBe "minecraft:netherite_sword"
            }

            test("equality") {
                val a = Icon(type = "STONE", customModelData = 1, aura = true)
                val b = Icon(type = "STONE", customModelData = 1, aura = true)
                val c = Icon(type = "STONE", customModelData = 2, aura = true)
                a shouldBe b
                (a == c) shouldBe false
            }
        }

        context("Location") {
            test("full constructor") {
                val loc =
                    Location(
                        world = "world_nether",
                        x = 1.0,
                        y = 2.0,
                        z = 3.0,
                        yaw = 90.0f,
                        pitch = 45.0f,
                    )
                loc.world shouldBe "world_nether"
                loc.x shouldBe 1.0
                loc.y shouldBe 2.0
                loc.z shouldBe 3.0
                loc.yaw shouldBe 90.0f
                loc.pitch shouldBe 45.0f
            }

            test("minimal constructor") {
                val loc = Location(world = "world", x = 0.0, y = 64.0, z = 0.0)
                loc.yaw shouldBe 0.0f
                loc.pitch shouldBe 0.0f
            }

            test("negative coordinates") {
                val loc = Location(world = "world", x = -100.5, y = -10.0, z = 200.75)
                loc.x shouldBe -100.5
                loc.y shouldBe -10.0
                loc.z shouldBe 200.75
            }
        }
    })
