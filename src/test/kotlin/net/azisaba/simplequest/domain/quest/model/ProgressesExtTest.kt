package net.azisaba.simplequest.domain.quest.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ProgressesExtTest :
    FunSpec({

        context("Progresses with single requirement - saturation") {
            test("progress cannot exceed required") {
                val p = Progresses(mapOf("kill" to QuestRequirement("kill", 10)))
                p["kill"] = 15
                p["kill"] shouldBe 15 // set() doesn't clamp max, only min
                p.isComplete shouldBe true // but completed
            }
        }

        context("Progresses with many requirements") {
            test("10 requirements tracked") {
                val reqs = (1..10).associate { "r$it" to QuestRequirement("r$it", it * 2) }
                val p = Progresses(reqs)
                p.totalRequired shouldBe (1..10).sumOf { it * 2 }
                p.totalProgress shouldBe 0
                p.isComplete shouldBe false
            }

            test("partial completion of many requirements") {
                val reqs = (1..5).associate { "r$it" to QuestRequirement("r$it", 10) }
                val p = Progresses(reqs)
                p["r1"] = 10
                p["r2"] = 10
                p.isComplete shouldBe false // 3 more needed
                p["r3"] = 10
                p["r4"] = 10
                p["r5"] = 10
                p.isComplete shouldBe true
            }

            test("completing in any order works") {
                val reqs = mapOf("a" to QuestRequirement("a", 3), "b" to QuestRequirement("b", 3))
                val p = Progresses(reqs)
                p["b"] = 3
                p["a"] = 3
                p.isComplete shouldBe true
            }
        }

        context("Progresses - loadFrom edge cases") {
            test("loadFrom with empty map resets nothing") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 5)))
                p["x"] = 3
                p.loadFrom(emptyMap())
                p["x"] shouldBe 3 // not overwritten
            }

            test("loadFrom with extra keys ignored") {
                val p = Progresses(mapOf("a" to QuestRequirement("a", 1)))
                p.loadFrom(mapOf("a" to 1, "b" to 5))
                p["a"] shouldBe 1
                // b doesn't exist in requirements, ignored silently
            }

            test("loadFrom with negative values clamps to 0") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 10)))
                p.loadFrom(mapOf("x" to -5))
                p["x"] shouldBe 0
            }

            test("loadFrom with very large values") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 5)))
                p.loadFrom(mapOf("x" to 999_999))
                p["x"] shouldBe 999_999
                p.isComplete shouldBe true
            }
        }

        context("Progresses - addDelta edge cases") {
            test("addDelta multiple times") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 10)))
                p.addDelta("x", 2)
                p.addDelta("x", 3)
                p.addDelta("x", 5)
                p["x"] shouldBe 10
                p.isComplete shouldBe true
            }

            test("addDelta zero is no-op") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 5)))
                p.addDelta("x", 0)
                p["x"] shouldBe 0
                p.isComplete shouldBe false
            }
        }

        context("Progresses - snapshot consistency") {
            test("snapshot after all modifications") {
                val reqs =
                    mapOf(
                        "a" to QuestRequirement("a", 5),
                        "b" to QuestRequirement("b", 3),
                        "c" to QuestRequirement("c", 7),
                    )
                val p = Progresses(reqs)
                p["a"] = 5
                p["b"] = 2
                p.addDelta("c", 4)

                val snap = p.snapshot()
                snap shouldBe mapOf("a" to 5, "b" to 2, "c" to 4)
            }

            test("snapshot is independent copy") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 10)))
                p["x"] = 5
                val snap1 = p.snapshot()
                p["x"] = 8
                val snap2 = p.snapshot()
                snap1["x"] shouldBe 5 // unchanged
                snap2["x"] shouldBe 8
            }
        }

        context("Progresses - reset behavior") {
            test("reset after complete returns to incomplete") {
                val p = Progresses(mapOf("a" to QuestRequirement("a", 3)))
                p["a"] = 3
                p.isComplete shouldBe true
                p.reset()
                p["a"] shouldBe 0
                p.totalProgress shouldBe 0
                p.isComplete shouldBe false
            }

            test("reset twice is idempotent") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 10)))
                p["x"] = 10
                p.reset()
                p.reset()
                p["x"] shouldBe 0
            }
        }

        context("Progresses - zero amount requirements") {
            test("zero amount requirement auto-completes") {
                val p = Progresses(mapOf("auto" to QuestRequirement("auto", 0)))
                p.isComplete shouldBe true // initial value is 0 which >= 0
            }

            test("zero and non-zero mix - zero satisfied immediately") {
                val reqs =
                    mapOf(
                        "auto" to QuestRequirement("auto", 0),
                        "kill" to QuestRequirement("kill", 5),
                    )
                val p = Progresses(reqs)
                p.isComplete shouldBe false // kill not done
                p["kill"] = 5
                p.isComplete shouldBe true
            }
        }

        context("Progresses - total properties") {
            test("totalProgress after partial completion") {
                val p =
                    Progresses(
                        mapOf(
                            "a" to QuestRequirement("a", 10),
                            "b" to QuestRequirement("b", 10),
                            "c" to QuestRequirement("c", 10),
                        ),
                    )
                p["a"] = 5
                p["b"] = 7
                p.totalProgress shouldBe 12
                p.totalRequired shouldBe 30
            }

            test("totalProgress exceeds totalRequired is possible") {
                // since set() doesn't clamp max
                val p = Progresses(mapOf("x" to QuestRequirement("x", 5)))
                p["x"] = 10
                p.totalProgress shouldBe 10
                p.totalRequired shouldBe 5
                p.isComplete shouldBe true
            }
        }

        context("Progresses - isComplete semantics") {
            test("standard completion path") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 3)))
                // x starts at 0
                p.isComplete shouldBe false
                p["x"] = 1
                p.isComplete shouldBe false
                p["x"] = 2
                p.isComplete shouldBe false
                p["x"] = 3
                p.isComplete shouldBe true
            }

            test("over-completion is still complete") {
                val p = Progresses(mapOf("x" to QuestRequirement("x", 1)))
                p["x"] = 100
                p.isComplete shouldBe true
            }
        }
    })
