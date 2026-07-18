package net.azisaba.simplequest.domain.quest.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import net.azisaba.simplequest.domain.action.Action
import net.azisaba.simplequest.domain.action.ActionSet
import net.azisaba.simplequest.domain.action.ActionType
import net.azisaba.simplequest.domain.data.Icon
import net.azisaba.simplequest.domain.data.Location
import net.azisaba.simplequest.domain.script.Script

class QuestTypeTest :
    FunSpec({

        context("QuestType construction") {
            test("minimal constructor") {
                val qt = QuestType(key = "test:minimal", title = "Minimal", icon = Icon(type = "STONE"))
                qt.key shouldBe "test:minimal"
                qt.title shouldBe "Minimal"
                qt.icon.type shouldBe "STONE"
                qt.description shouldBe emptyList()
                qt.category shouldBe "lq:general"
                qt.location.shouldBeNull()
                qt.giver.shouldBeNull()
                qt.playLimits.isUnlimited shouldBe true
                qt.acceptConditions.partyMode shouldBe false
                qt.maxPlayers.shouldBeNull()
                qt.minPlayers.shouldBeNull()
                qt.deathLimit.shouldBeNull()
                qt.guides shouldBe emptyList()
                qt.requirements shouldBe emptyMap()
                qt.actions.shouldBeNull()
                qt.scripts shouldBe emptyList()
            }

            test("full constructor with all fields") {
                val qt =
                    QuestType(
                        key = "test:full",
                        title = "&6Full Quest",
                        icon = Icon(type = "DIAMOND_SWORD", customModelData = 5001, aura = true),
                        description = listOf("&7Line 1", "&7Line 2"),
                        category = "lq:daily",
                        location = Location(world = "world_nether", x = 10.0, y = 64.0, z = 20.0),
                        giver = "&eQuest Master",
                        playLimits = PlayLimits(daily = 5, weekly = 10, lifetime = 100),
                        acceptConditions = AcceptConditions(pveLevel = 10, money = 500.0, partyMode = true),
                        maxPlayers = 4,
                        minPlayers = 2,
                        deathLimit = 3,
                        guides = listOf(GameGuide(title = "Point A", location = Location(world = "world", x = 100.0, y = 64.0, z = 200.0))),
                        requirements = mapOf("kill" to QuestRequirement("kill", 10)),
                        actions = ActionSet(onComplete = listOf(Action(ActionType.COMMAND, command = "say done"))),
                        scripts = listOf(Script(trigger = Script.Trigger.START, commands = listOf("say started"))),
                    )
                qt.title shouldBe "&6Full Quest"
                qt.description.size shouldBe 2
                qt.giver shouldBe "&eQuest Master"
                qt.category shouldBe "lq:daily"
                qt.location?.world shouldBe "world_nether"
                qt.playLimits.daily shouldBe 5
                qt.playLimits.lifetime shouldBe 100
                qt.acceptConditions.pveLevel shouldBe 10
                qt.acceptConditions.money shouldBe 500.0
                qt.maxPlayers shouldBe 4
                qt.minPlayers shouldBe 2
                qt.deathLimit shouldBe 3
                qt.guides.size shouldBe 1
                qt.requirements.size shouldBe 1
                qt.actions?.onComplete?.size shouldBe 1
                qt.scripts.size shouldBe 1
            }

            test("QuestType equality") {
                val a = QuestType(key = "a", title = "A", icon = Icon(type = "STONE"))
                val b = QuestType(key = "a", title = "A", icon = Icon(type = "STONE"))
                a shouldBe b
            }

            test("QuestType inequality by key") {
                val a = QuestType(key = "a", title = "A", icon = Icon(type = "STONE"))
                val b = QuestType(key = "b", title = "A", icon = Icon(type = "STONE"))
                (a == b) shouldBe false
            }

            test("QuestType copy maintains fields") {
                val qt = QuestType(key = "test", title = "Test", icon = Icon(type = "STONE"), maxPlayers = 4)
                val copy = qt.copy(maxPlayers = 8)
                copy.maxPlayers shouldBe 8
                copy.key shouldBe "test"
                copy.title shouldBe "Test"
            }
        }

        context("QuestCategory") {
            test("holds key and display name") {
                val cat = QuestCategory(key = "lq:daily", displayName = "Daily Quests")
                cat.key shouldBe "lq:daily"
                cat.displayName shouldBe "Daily Quests"
            }

            test("equality") {
                val a = QuestCategory("lq:general", "General")
                val b = QuestCategory("lq:general", "General")
                a shouldBe b
            }

            test("different categories not equal") {
                val a = QuestCategory("lq:daily", "Daily")
                val b = QuestCategory("lq:weekly", "Weekly")
                (a == b) shouldBe false
            }
        }

        context("GameGuide") {
            test("full constructor") {
                val guide =
                    GameGuide(
                        title = "&aForest Gate",
                        location = Location(world = "world", x = 100.0, y = 64.0, z = 200.0),
                        requirements = mapOf("kill" to 5),
                    )
                guide.title shouldBe "&aForest Gate"
                guide.location.x shouldBe 100.0
                guide.requirements["kill"] shouldBe 5
            }

            test("null title is valid") {
                val guide = GameGuide(location = Location("world"))
                guide.title.shouldBeNull()
            }

            test("empty requirements") {
                val guide = GameGuide(title = "Point", location = Location("world"))
                guide.requirements shouldBe emptyMap()
            }

            test("equality") {
                val a = GameGuide(title = "A", location = Location("world", x = 1.0, y = 2.0, z = 3.0), requirements = mapOf("x" to 1))
                val b = GameGuide(title = "A", location = Location("world", x = 1.0, y = 2.0, z = 3.0), requirements = mapOf("x" to 1))
                a shouldBe b
            }
        }

        context("QuestResult sealed class") {
            test("Success holds quest") {
                val qt = QuestType(key = "test:result", title = "Test", icon = Icon(type = "STONE"))
                val quest = createFakeQuest(qt)
                val result = QuestResult.Success(quest)
                val q = result.quest
                q.type.key shouldBe "test:result"
            }

            test("Failure holds reason") {
                val result = QuestResult.Failure("Party does not meet requirements")
                result.reason shouldBe "Party does not meet requirements"
            }

            test("Failure with different reasons") {
                val r1 = QuestResult.Failure("Requirement not met")
                val r2 = QuestResult.Failure("Limit exceeded")
                val r3 = QuestResult.Failure("")
                r1.reason shouldBe "Requirement not met"
                r2.reason shouldBe "Limit exceeded"
                r3.reason shouldBe ""
            }
        }
    })

private fun createFakeQuest(type: QuestType): Quest =
    object : Quest {
        override val type: QuestType = type
        override val state: QuestState = QuestState.ACTIVE
        override val progresses: Progresses = Progresses(type.requirements)

        override fun start() {}

        override fun end(reason: EndReason) {}

        override fun updateProgress(
            reqKey: String,
            delta: Int,
        ) {}
    }
