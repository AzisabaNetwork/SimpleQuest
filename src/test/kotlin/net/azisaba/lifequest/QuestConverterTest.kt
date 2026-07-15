package net.azisaba.lifequest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import net.azisaba.lifequest.data.yaml.*
import net.azisaba.lifequest.domain.action.ActionType as DomainActionType

class QuestConverterTest :
    FunSpec({

        val converter = QuestConverter

        fun convert(
            def: QuestDef,
            keySuffix: String = "test",
        ) = converter.toQuestType("lq/$keySuffix", def)

        test("minimal quest conversion") {
            val qt = convert(QuestDef(title = "Test", icon = "STONE"))
            qt.key shouldBe "lq/test"
            qt.title shouldBe "Test"
            qt.category shouldBe "lq:general"
            qt.icon.type shouldBe "STONE"
        }

        test("full quest conversion") {
            val def =
                QuestDef(
                    title = "&cFull",
                    description = listOf("desc"),
                    icon = "DIAMOND_SWORD:5001",
                    aura = true,
                    giver = "&eNPC",
                    category = "lq:daily",
                    location = "world,100,64,200",
                    options =
                        QuestOptionsDef(
                            maxParty = "2-4",
                            limits = PlayLimitDef(weekly = 3, lifetime = 1),
                            deathLimit = 5,
                        ),
                    requirements = RequirementDef(pveLevel = 10, money = 100.0, partyMode = true),
                    objectives = mapOf("kill" to "10", "item" to "minecraft:diamond*3"),
                    actions =
                        ActionsDef(
                            onFirstComplete = listOf(ActionEntry("MythicItem", "Sword,1")),
                            onComplete = listOf(ActionEntry("Item", "minecraft:emerald,5")),
                        ),
                    guides = listOf(GuideDef("Gate", "100,64,100", "kill=1")),
                    scripts = mapOf("OnStart" to listOf("say hello")),
                )
            val qt = convert(def, "full")

            qt.title shouldBe "&cFull"
            qt.description shouldBe listOf("desc")
            qt.icon.type shouldBe "DIAMOND_SWORD"
            qt.icon.customModelData shouldBe 5001
            qt.icon.aura shouldBe true
            qt.giver shouldBe "&eNPC"
            qt.category shouldBe "lq:daily"
            qt.location?.world shouldBe "world"
            qt.location?.x shouldBe 100.0
            qt.location?.y shouldBe 64.0
            qt.location?.z shouldBe 200.0
            qt.maxPlayers shouldBe 4
            qt.minPlayers shouldBe 2
            qt.deathLimit shouldBe 5
            qt.playLimits.weekly shouldBe 3
            qt.playLimits.lifetime shouldBe 1
            qt.acceptConditions.pveLevel shouldBe 10
            qt.acceptConditions.partyMode shouldBe true
            qt.requirements["kill"]?.amount shouldBe 10
            qt.requirements["item"]?.amount shouldBe 3
            qt.actions?.onFirstComplete?.size shouldBe 1
            qt.actions?.onComplete?.size shouldBe 1
            qt.guides.size shouldBe 1
            qt.scripts.size shouldBe 1
        }

        test("icon parsing via conversion") {
            val qt = convert(QuestDef(title = "IconTest", icon = "STONE:42"))
            qt.icon.type shouldBe "STONE"
            qt.icon.customModelData shouldBe 42
            qt.icon.aura shouldBe false
        }

        test("icon with aura") {
            val qt = convert(QuestDef(title = "AuraTest", icon = "DIAMOND", aura = true))
            qt.icon.type shouldBe "DIAMOND"
            qt.icon.aura shouldBe true
        }

        test("location parsing") {
            val qt = convert(QuestDef(title = "LocTest", location = "world_nether,10,20,30"))
            qt.location?.world shouldBe "world_nether"
            qt.location?.x shouldBe 10.0
            qt.location?.y shouldBe 20.0
            qt.location?.z shouldBe 30.0
        }

        test("null location") {
            val qt = convert(QuestDef(title = "NoLoc"))
            qt.location.shouldBeNull()
        }

        test("maxParty range → minPlayers/maxPlayers") {
            val qt = convert(QuestDef(title = "PartySizeTest", options = QuestOptionsDef(maxParty = "1-4")))
            qt.minPlayers shouldBe 1
            qt.maxPlayers shouldBe 4
        }

        test("maxParty single → maxPlayers only") {
            val qt = convert(QuestDef(title = "MaxOnly", options = QuestOptionsDef(maxParty = "8")))
            qt.minPlayers shouldBe null
            qt.maxPlayers shouldBe 8
        }

        test("play limits conversion") {
            val qt =
                convert(
                    QuestDef(
                        title = "Limits",
                        options = QuestOptionsDef(limits = PlayLimitDef(weekly = 7, monthly = 30, yearly = 365, lifetime = 100)),
                    ),
                )
            qt.playLimits.weekly shouldBe 7
            qt.playLimits.monthly shouldBe 30
            qt.playLimits.yearly shouldBe 365
            qt.playLimits.lifetime shouldBe 100
        }

        test("death limit conversion") {
            val qt =
                convert(
                    QuestDef(
                        title = "Death",
                        options = QuestOptionsDef(deathLimit = 3),
                    ),
                )
            qt.deathLimit shouldBe 3
        }

        test("all action types convert correctly") {
            val def =
                QuestDef(
                    title = "Actions",
                    actions =
                        ActionsDef(
                            onFirstComplete = listOf(ActionEntry("Command", "say first")),
                            onComplete =
                                listOf(
                                    ActionEntry("Item", "minecraft:diamond,3"),
                                    ActionEntry("MythicItem", "MMOre,5"),
                                    ActionEntry("PvELevel", "100"),
                                ),
                        ),
                )
            val qt = convert(def, "actions")

            qt.actions
                ?.onFirstComplete
                ?.first()
                ?.type shouldBe DomainActionType.COMMAND
            qt.actions?.onComplete?.size shouldBe 3
            qt.actions
                ?.onComplete
                ?.get(0)
                ?.type shouldBe DomainActionType.ITEM_GIVE
            qt.actions
                ?.onComplete
                ?.get(0)
                ?.material shouldBe "minecraft:diamond"
            qt.actions
                ?.onComplete
                ?.get(0)
                ?.amount shouldBe 3
            qt.actions
                ?.onComplete
                ?.get(1)
                ?.type shouldBe DomainActionType.MYTHIC_ITEM_GIVE
            qt.actions
                ?.onComplete
                ?.get(1)
                ?.item shouldBe "MMOre"
            qt.actions
                ?.onComplete
                ?.get(2)
                ?.type shouldBe DomainActionType.PVELEVEL_EXP
            qt.actions
                ?.onComplete
                ?.get(2)
                ?.amount shouldBe 100
        }

        test("script parsing") {
            val qt =
                convert(
                    QuestDef(
                        title = "Scripts",
                        scripts =
                            mapOf(
                                "OnStart" to listOf("say started"),
                                "OnStart+20" to listOf("say delayed"),
                                "OnComplete" to listOf("say done", "give % minecraft:diamond 1"),
                                "OnCancel" to listOf("say cancelled"),
                            ),
                    ),
                )
            qt.scripts.size shouldBe 4
            qt.scripts.count { it.trigger.name == "START" } shouldBe 2
            qt.scripts.count { it.trigger.name == "COMPLETE" } shouldBe 1
            qt.scripts.count { it.trigger.name == "CANCEL" } shouldBe 1
        }

        test("guide conversion with condition") {
            val qt =
                convert(
                    QuestDef(
                        title = "Guides",
                        location = "world,0,0,0",
                        guides = listOf(GuideDef("Point", "100,64,200", "kill=5")),
                    ),
                )
            qt.guides.size shouldBe 1
            qt.guides.first().title shouldBe "Point"
            qt.guides
                .first()
                .location.x shouldBe 100.0
            qt.guides.first().requirements["kill"] shouldBe 5
        }

        test("objectives conversion") {
            val qt =
                convert(
                    QuestDef(
                        title = "Objectives",
                        objectives = mapOf("kill_zombie" to "10", "collect_item" to "minecraft:diamond*3"),
                    ),
                )
            qt.requirements["kill_zombie"]?.amount shouldBe 10
            qt.requirements["collect_item"]?.amount shouldBe 3
        }

        test("category fallback to general") {
            val qt = convert(QuestDef(title = "UnknownCat", category = "lq:nonexistent"))
            qt.category shouldBe "lq:nonexistent" // no fallback — uses raw value
        }

        test("category resolution") {
            val qt = convert(QuestDef(title = "DailyCat", category = "lq:daily"))
            qt.category shouldBe "lq:daily"
        }
    })
