package net.azisaba.simplequest.domain.quest.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.azisaba.simplequest.domain.data.Location

class GameGuideTest :
    FunSpec({

        context("GameGuide construction") {
            test("minimal guide with only location") {
                val guide = GameGuide(location = Location("world", 100.0, 64.0, 100.0))
                guide.title.shouldBeNull()
                guide.location.world shouldBe "world"
                guide.requirements shouldBe emptyMap()
            }

            test("full guide with all fields") {
                val guide =
                    GameGuide(
                        title = "&eBoss Gate",
                        location = Location("world_nether", 10.0, 80.0, 20.0, 90.0f, 0.0f),
                        requirements = mapOf("level" to 30, "kill" to 5),
                    )
                guide.title shouldBe "&eBoss Gate"
                guide.location.world shouldBe "world_nether"
                guide.location.yaw shouldBe 90.0f
                guide.requirements.size shouldBe 2
            }

            test("equality by value") {
                val a = GameGuide(location = Location("world", 0.0, 0.0, 0.0))
                val b = GameGuide(location = Location("world", 0.0, 0.0, 0.0))
                a shouldBe b
            }

            test("inequality by different location") {
                val a = GameGuide(location = Location("world", 0.0, 0.0, 0.0))
                val b = GameGuide(location = Location("world", 1.0, 0.0, 0.0))
                (a == b) shouldBe false
            }

            test("inequality by different title") {
                val a = GameGuide(title = "A", location = Location("world"))
                val b = GameGuide(title = "B", location = Location("world"))
                (a == b) shouldBe false
            }
        }

        context("QuestType guides") {
            test("quest type with multiple guides") {
                val qt =
                    QuestType(
                        key = "test:guides",
                        title = "Multi Guide",
                        icon = Icon("STONE"),
                        guides =
                            listOf(
                                GameGuide(title = "Start", location = Location("world", 0.0, 64.0, 0.0)),
                                GameGuide(title = "Halfway", location = Location("world", 500.0, 64.0, 500.0)),
                                GameGuide(title = "Boss", location = Location("world", 1000.0, 64.0, 1000.0)),
                            ),
                    )
                qt.guides.size shouldBe 3
                qt.guides[0].title shouldBe "Start"
                qt.guides[1].title shouldBe "Halfway"
                qt.guides[2].title shouldBe "Boss"
            }

            test("default guides is empty") {
                val qt = QuestType(key = "test:empty", title = "Empty", icon = Icon("STONE"))
                qt.guides shouldBe emptyList()
            }
        }
    })
