package net.azisaba.simplequest.domain.quest.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ProgressesTest :
    FunSpec({

        context("empty requirements") {
            test("completes vacuously") {
                val p = Progresses(emptyMap())
                p.isComplete shouldBe true
            }

            test("totalProgress and totalRequired are zero") {
                val p = Progresses(emptyMap())
                p.totalProgress shouldBe 0
                p.totalRequired shouldBe 0
            }
        }

        context("single requirement") {
            test("initial values are zero") {
                val reqs = mapOf("kill" to QuestRequirement("kill", 10))
                val p = Progresses(reqs)
                p["kill"] shouldBe 0
                p.totalProgress shouldBe 0
                p.totalRequired shouldBe 10
                p.isComplete shouldBe false
            }

            test("progress tracking") {
                val reqs = mapOf("kill" to QuestRequirement("kill", 10))
                val p = Progresses(reqs)
                p["kill"] = 5
                p["kill"] shouldBe 5
                p.totalProgress shouldBe 5
                p.isComplete shouldBe false

                p["kill"] = 10
                p.isComplete shouldBe true
            }

            test("exceeding required amount completes") {
                val reqs = mapOf("kill" to QuestRequirement("kill", 5))
                val p = Progresses(reqs)
                p["kill"] = 10
                p.isComplete shouldBe true
            }
        }

        context("multiple requirements") {
            test("all must be completed") {
                val reqs =
                    mapOf(
                        "a" to QuestRequirement("a", 3),
                        "b" to QuestRequirement("b", 5),
                    )
                val p = Progresses(reqs)

                p["a"] = 3
                p.isComplete shouldBe false

                p["b"] = 5
                p.isComplete shouldBe true
            }
        }

        context("loadFrom") {
            test("overrides saved progress") {
                val reqs = mapOf("x" to QuestRequirement("x", 10))
                val p = Progresses(reqs)
                p.loadFrom(mapOf("x" to 7))
                p["x"] shouldBe 7
            }

            test("lower value overrides") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 10)))
                p.loadFrom(mapOf("x" to 3))
                p["x"] shouldBe 3
            }

            test("partial keys preserve others") {
                val reqs =
                    mapOf(
                        "a" to QuestRequirement("a", 1),
                        "b" to QuestRequirement("b", 1),
                    )
                val p = Progresses(reqs)
                p.loadFrom(mapOf("a" to 1))
                p["a"] shouldBe 1
                p["b"] shouldBe 0
                p.isComplete shouldBe false
            }
        }

        context("clamping") {
            test("value is clamped to minimum zero") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 1)))
                p["x"] = -5
                p["x"] shouldBe 0
            }
        }

        context("zero-amount requirement") {
            test("auto-completes") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 0)))
                p["x"] = 0
                p.isComplete shouldBe true
            }
        }

        context("totalProgress and totalRequired") {
            test("composite progress") {
                val reqs =
                    mapOf(
                        "a" to QuestRequirement("a", 3),
                        "b" to QuestRequirement("b", 7),
                    )
                val p = Progresses(reqs)
                p.totalRequired shouldBe 10
                p.totalProgress shouldBe 0

                p["a"] = 2
                p["b"] = 3
                p.totalProgress shouldBe 5
            }
        }

        context("snapshot") {
            test("returns copy of current progress") {
                val reqs =
                    mapOf(
                        "ore" to QuestRequirement("ore", 10),
                        "kill" to QuestRequirement("kill", 5),
                    )
                val p = Progresses(reqs)
                p["ore"] = 7
                p["kill"] = 3

                val snap = p.snapshot()
                snap shouldBe mapOf("ore" to 7, "kill" to 3)

                // snapshot is immutable copy
                p["ore"] = 10
                snap["ore"] shouldBe 7
            }
        }

        context("addDelta") {
            test("increments progress") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 10)))
                p.addDelta("x", 3)
                p["x"] shouldBe 3
                p.addDelta("x", 2)
                p["x"] shouldBe 5
            }

            test("negative delta is clamped") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 10)))
                p["x"] = 5
                p.addDelta("x", -10)
                p["x"] shouldBe 0
            }

            test("unknown key is ignored") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 10)))
                p.addDelta("unknown", 5)
                p["x"] shouldBe 0
            }
        }

        context("reset") {
            test("resets all progress to zero") {
                val reqs =
                    mapOf(
                        "a" to QuestRequirement("a", 3),
                        "b" to QuestRequirement("b", 5),
                    )
                val p = Progresses(reqs)
                p["a"] = 3
                p["b"] = 2
                p.reset()
                p["a"] shouldBe 0
                p["b"] shouldBe 0
                p.isComplete shouldBe false
            }
        }
    })
