package net.azisaba.lifequest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.azisaba.lifequest.domain.data.Icon
import net.azisaba.lifequest.domain.data.Location
import net.azisaba.lifequest.quest.*
import net.kyori.adventure.key.Key

class QuestTypeTest :
    FunSpec({

        context("Progresses") {
            test("empty requirements completes vacuously") {
                val p = Progresses(emptyMap()) { }
                // empty requirements = nothing to track, so it's already complete
                p.isComplete shouldBe true
            }

            test("single requirement tracking") {
                val reqs = mapOf("kill" to QuestRequirement("kill", 10))
                var completed = false
                val p = Progresses(reqs) { completed = true }

                p["kill"] shouldBe 0
                p.totalProgress shouldBe 0
                p.totalRequired shouldBe 10
                p.isComplete shouldBe false

                p["kill"] = 5
                p["kill"] shouldBe 5
                p.totalProgress shouldBe 5
                p.isComplete shouldBe false

                p["kill"] = 10
                p.isComplete shouldBe true
                completed shouldBe true
            }

            test("multiple requirements: all must be met") {
                val reqs =
                    mapOf(
                        "a" to QuestRequirement("a", 3),
                        "b" to QuestRequirement("b", 5),
                    )
                var completed = false
                val p = Progresses(reqs) { completed = true }

                p["a"] = 3
                p.isComplete shouldBe false // b is not done

                p["b"] = 5
                p.isComplete shouldBe true
            }

            test("loadFrom overrides saved progress") {
                val reqs = mapOf("x" to QuestRequirement("x", 10))
                val p = Progresses(reqs) {}
                p.loadFrom(mapOf("x" to 7))
                p["x"] shouldBe 7

                p.loadFrom(mapOf("x" to 3)) // lower value — should override
                p["x"] shouldBe 3
            }

            test("loadFrom with partial keys") {
                val reqs =
                    mapOf(
                        "a" to QuestRequirement("a", 1),
                        "b" to QuestRequirement("b", 1),
                    )
                val p = Progresses(reqs) {}
                p.loadFrom(mapOf("a" to 1))
                p["a"] shouldBe 1
                p["b"] shouldBe 0 // not in saved map
                p.isComplete shouldBe false
            }

            test("clamp value to minimum zero") {
                val reqs = mapOf("x" to QuestRequirement("x", 1))
                val p = Progresses(reqs) {}
                p["x"] = -5
                p["x"] shouldBe 0
                p["x"] = -1
                p["x"] shouldBe 0
            }

            test("zero is valid initial value") {
                val reqs = mapOf("x" to QuestRequirement("x", 0))
                var completed = false
                val p = Progresses(reqs) { completed = true }
                p["x"] = 0
                p.isComplete shouldBe true // amount=0 means auto-complete
                completed shouldBe true
            }

            test("totalProgress and totalRequired") {
                val reqs =
                    mapOf(
                        "a" to QuestRequirement("a", 3),
                        "b" to QuestRequirement("b", 7),
                    )
                val p = Progresses(reqs) {}
                p.totalRequired shouldBe 10
                p.totalProgress shouldBe 0

                p["a"] = 2
                p["b"] = 3
                p.totalProgress shouldBe 5
            }
        }

        context("QuestRequirement") {
            test("holds key and amount") {
                val r = QuestRequirement("kill_zombie", 10)
                r.key shouldBe "kill_zombie"
                r.amount shouldBe 10
            }

            test("amount can be zero") {
                val r = QuestRequirement("auto_complete", 0)
                r.amount shouldBe 0
            }

            test("amount can be large") {
                val r = QuestRequirement("grind", 10_000)
                r.amount shouldBe 10000
            }
        }

        context("PlayLimits") {
            test("all fields default to null") {
                val limits = PlayLimits()
                limits.weekly shouldBe null
                limits.monthly shouldBe null
                limits.yearly shouldBe null
                limits.lifetime shouldBe null
            }

            test("partial fields set") {
                val limits = PlayLimits(weekly = 3, lifetime = 1)
                limits.weekly shouldBe 3
                limits.lifetime shouldBe 1
                limits.monthly shouldBe null
                limits.yearly shouldBe null
            }

            test("all fields set") {
                val limits = PlayLimits(weekly = 7, monthly = 30, yearly = 365, lifetime = 100)
                limits.weekly shouldBe 7
                limits.monthly shouldBe 30
                limits.yearly shouldBe 365
                limits.lifetime shouldBe 100
            }
        }

        context("AcceptConditions") {
            test("all defaults") {
                val c = AcceptConditions()
                c.pveLevel shouldBe null
                c.requiredQuests shouldBe null
                c.permissions shouldBe null
                c.partyMode shouldBe false
            }

            test("all fields set") {
                val c =
                    AcceptConditions(
                        pveLevel = 10,
                        requiredQuests = listOf(Key.key("lq", "prologue")),
                        permissions = listOf("group.vip"),
                        partyMode = true,
                    )
                c.pveLevel shouldBe 10
                c.requiredQuests shouldBe listOf(Key.key("lq", "prologue"))
                c.permissions shouldBe listOf("group.vip")
                c.partyMode shouldBe true
            }

            test("partyMode only") {
                val c = AcceptConditions(partyMode = true)
                c.partyMode shouldBe true
                c.pveLevel shouldBe null
            }
        }

        context("QuestState") {
            test("values are correct") {
                QuestState.entries.size shouldBe 4
                QuestState.ACTIVE.name shouldBe "ACTIVE"
                QuestState.COMPLETED.name shouldBe "COMPLETED"
                QuestState.CANCELLED.name shouldBe "CANCELLED"
                QuestState.FAILED.name shouldBe "FAILED"
            }
        }

        context("EndReason") {
            test("all values present") {
                EndReason.entries.size shouldBe 6
                EndReason.COMPLETE.name shouldBe "COMPLETE"
                EndReason.CANCEL.name shouldBe "CANCEL"
                EndReason.DEATH_LIMIT.name shouldBe "DEATH_LIMIT"
                EndReason.PLUGIN.name shouldBe "PLUGIN"
                EndReason.RELOAD.name shouldBe "RELOAD"
                EndReason.OTHER.name shouldBe "OTHER"
            }
        }

        context("Icon") {
            test("full constructor") {
                val icon = Icon(type = "STONE_SWORD", customModelData = 3001, aura = true)
                icon.type shouldBe "STONE_SWORD"
                icon.customModelData shouldBe 3001
                icon.aura shouldBe true
                icon.model shouldBe null
            }

            test("minimal constructor") {
                val icon = Icon(type = "PAPER")
                icon.type shouldBe "PAPER"
                icon.customModelData shouldBe null
                icon.aura shouldBe false
                icon.model shouldBe null
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
                val loc = Location(world = "world_nether", x = 1.0, y = 2.0, z = 3.0, yaw = 90.0f, pitch = 45.0f)
                loc.world shouldBe "world_nether"
                loc.x shouldBe 1.0
                loc.y shouldBe 2.0
                loc.z shouldBe 3.0
                loc.yaw shouldBe 90.0f
                loc.pitch shouldBe 45.0f
            }

            test("minimal constructor") {
                val loc = Location(world = "world", x = 0.0, y = 64.0, z = 0.0)
                loc.world shouldBe "world"
                loc.x shouldBe 0.0
                loc.y shouldBe 64.0
                loc.z shouldBe 0.0
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
