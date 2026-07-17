package net.azisaba.lifequest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import net.azisaba.lifequest.data.yaml.*
import net.azisaba.lifequest.domain.data.Location
import net.azisaba.lifequest.domain.action.ActionType as DomainActionType

class QuestConverterEdgeCaseTest :
    FunSpec({

        val converter = QuestConverter

        fun convert(
            def: QuestDef,
            keySuffix: String = "test",
        ) = converter.toQuestType("lq/$keySuffix", def)

        context("Icon parsing") {
            // Test name uses backtick to show the value being parsed
            data class IconCase(
                val input: String,
                val expectedType: String,
                val expectedCmd: Int?,
                val aura: Boolean,
            )

            val cases =
                listOf(
                    IconCase("STONE", "STONE", null, false),
                    IconCase("STONE:42", "STONE", 42, false),
                    IconCase("DIAMOND_SWORD:5001", "DIAMOND_SWORD", 5001, true),
                    IconCase("PLAYER_HEAD:12345", "PLAYER_HEAD", 12345, false),
                    IconCase("CHEST:0", "CHEST", 0, false),
                    IconCase("PAPER", "PAPER", null, false),
                    IconCase("COMPLEX_TYPE_NAME:999999", "COMPLEX_TYPE_NAME", 999999, false),
                    IconCase("SKULL:1", "SKULL", 1, false),
                    IconCase("WOODEN_SWORD", "WOODEN_SWORD", null, false),
                    IconCase("AIR", "AIR", null, false),
                )

            for ((input, expectedType, expectedCmd, aura) in cases) {
                test("parse icon: $input") {
                    val qt = convert(QuestDef(title = "Icon", icon = input, aura = aura))
                    qt.icon.type shouldBe expectedType
                    qt.icon.customModelData shouldBe expectedCmd
                    qt.icon.aura shouldBe aura
                }
            }

            test("icon with model parsing") {
                val iconParsed = converter.hashCode() // just ensure object exists
                val qt = convert(QuestDef(title = "ModelIcon", icon = "DIAMOND:1"))
                qt.icon.type shouldBe "DIAMOND"
                qt.icon.customModelData shouldBe 1
            }
        }

        context("Location parsing") {
            test("parse full location with yaw and pitch") {
                val qt = convert(QuestDef(title = "Loc", location = "world_nether,100.5,64.0,-200.25,90.0,45.0"))
                qt.location?.world shouldBe "world_nether"
                qt.location?.x shouldBe 100.5
                qt.location?.y shouldBe 64.0
                qt.location?.z shouldBe -200.25
                qt.location?.yaw shouldBe 90.0f
                qt.location?.pitch shouldBe 45.0f
            }

            test("parse location with only three coordinates") {
                val qt = convert(QuestDef(title = "SimpleLoc", location = "world,1,2,3"))
                qt.location?.let {
                    it.x shouldBe 1.0
                    it.y shouldBe 2.0
                    it.z shouldBe 3.0
                    it.yaw shouldBe 0.0f
                    it.pitch shouldBe 0.0f
                }
            }

            test("parse location with partial coordinates (only 2)") {
                val qt = convert(QuestDef(title = "PartialLoc", location = "world,1,2"))
                qt.location?.x shouldBe 1.0
                qt.location?.y shouldBe 2.0
                qt.location?.z shouldBe 0.0 // defaults
            }

            test("parse location with non-numeric values") {
                val qt = convert(QuestDef(title = "BadLoc", location = "world,abc,def,ghi"))
                qt.location?.x shouldBe 0.0
                qt.location?.y shouldBe 0.0
                qt.location?.z shouldBe 0.0
            }

            test("null location returns null") {
                val qt = convert(QuestDef(title = "NoLoc"))
                qt.location.shouldBeNull()
            }

            test("empty string location") {
                val qt = convert(QuestDef(title = "EmptyLoc", location = ""))
                qt.location?.world shouldBe "" // empty world name
            }
        }

        context("MaxParty parsing") {
            test("range format 1-8") {
                val qt = convert(QuestDef(title = "Range", options = QuestOptionsDef(maxParty = "1-8")))
                qt.minPlayers shouldBe 1
                qt.maxPlayers shouldBe 8
            }

            test("single number format") {
                val qt = convert(QuestDef(title = "Single", options = QuestOptionsDef(maxParty = "4")))
                qt.minPlayers.shouldBeNull()
                qt.maxPlayers shouldBe 4
            }

            test("null maxParty") {
                val qt = convert(QuestDef(title = "NullParty"))
                qt.minPlayers.shouldBeNull()
                qt.maxPlayers.shouldBeNull()
            }

            test("range with spaces") {
                val qt = convert(QuestDef(title = "Spaced", options = QuestOptionsDef(maxParty = " 2 - 6 ")))
                qt.minPlayers shouldBe 2
                qt.maxPlayers shouldBe 6
            }

            test("invalid range returns null") {
                val qt = convert(QuestDef(title = "InvalidRange", options = QuestOptionsDef(maxParty = "abc-def")))
                qt.minPlayers.shouldBeNull()
                qt.maxPlayers.shouldBeNull()
            }
        }

        context("Objectives parsing") {
            test("numeric objective") {
                val qt = convert(QuestDef(title = "NumObj", objectives = mapOf("kill" to "10")))
                qt.requirements["kill"]?.amount shouldBe 10
                qt.requirements["kill"]?.key shouldBe "kill"
            }

            test("material*amount objective") {
                val qt = convert(QuestDef(title = "MatObj", objectives = mapOf("collect" to "minecraft:diamond*5")))
                qt.requirements["collect"]?.amount shouldBe 5
            }

            test("material*amount with spaces (spaces prevent parsing)") {
                val qt = convert(QuestDef(title = "SpacedObj", objectives = mapOf("craft" to "minecraft:stick * 10")))
                // substringAfter("*") returns " 10", toIntOrNull on " 10" = null, fallback 1
                qt.requirements["craft"]?.amount shouldBe 1
            }

            test("invalid objective defaults to 1") {
                val qt = convert(QuestDef(title = "InvalidObj", objectives = mapOf("x" to "not_a_number")))
                qt.requirements["x"]?.amount shouldBe 1
            }

            test("multiple objectives") {
                val qt =
                    convert(
                        QuestDef(
                            title = "MultiObj",
                            objectives = mapOf("a" to "5", "b" to "stone*3", "c" to "dirt*7"),
                        ),
                    )
                qt.requirements.size shouldBe 3
                qt.requirements["a"]?.amount shouldBe 5
                qt.requirements["b"]?.amount shouldBe 3
                qt.requirements["c"]?.amount shouldBe 7
            }

            test("zero objective") {
                val qt = convert(QuestDef(title = "ZeroObj", objectives = mapOf("auto" to "0")))
                qt.requirements["auto"]?.amount shouldBe 0
            }

            test("negative objective parses as negative (no clamp)") {
                val qt = convert(QuestDef(title = "NegObj", objectives = mapOf("neg" to "-5")))
                // "-5".toIntOrNull() = -5, no validation in converter
                qt.requirements["neg"]?.amount shouldBe -5
            }

            test("empty objectives") {
                val qt = convert(QuestDef(title = "EmptyObj"))
                qt.requirements shouldBe emptyMap()
            }
        }

        context("Action parsing") {
            test("Command action") {
                val qt =
                    convert(
                        QuestDef(
                            title = "CmdAction",
                            actions = ActionsDef(onComplete = listOf(ActionEntry("Command", "say hello"))),
                        ),
                    )
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.type shouldBe DomainActionType.COMMAND
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.command shouldBe "say hello"
            }

            test("Item action") {
                val qt =
                    convert(
                        QuestDef(
                            title = "ItemAction",
                            actions = ActionsDef(onComplete = listOf(ActionEntry("Item", "minecraft:diamond,3"))),
                        ),
                    )
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.type shouldBe DomainActionType.ITEM_GIVE
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.material shouldBe "minecraft:diamond"
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.amount shouldBe 3
            }

            test("Item action with default amount") {
                val qt =
                    convert(
                        QuestDef(
                            title = "ItemDefault",
                            actions = ActionsDef(onComplete = listOf(ActionEntry("Item", "minecraft:stone"))),
                        ),
                    )
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.amount shouldBe 1
            }

            test("MythicItem action") {
                val qt =
                    convert(
                        QuestDef(
                            title = "MythicAction",
                            actions = ActionsDef(onComplete = listOf(ActionEntry("MythicItem", "Sword,1"))),
                        ),
                    )
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.type shouldBe DomainActionType.MYTHIC_ITEM_GIVE
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.item shouldBe "Sword"
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.amount shouldBe 1
            }

            test("PvELevel action") {
                val qt =
                    convert(
                        QuestDef(
                            title = "PveAction",
                            actions = ActionsDef(onComplete = listOf(ActionEntry("PvELevel", "100"))),
                        ),
                    )
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.type shouldBe DomainActionType.PVELEVEL_EXP
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.amount shouldBe 100
            }

            test("unknown action type falls back to Command") {
                val qt =
                    convert(
                        QuestDef(
                            title = "UnknownAction",
                            actions = ActionsDef(onComplete = listOf(ActionEntry("UnknownType", "some params"))),
                        ),
                    )
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.type shouldBe DomainActionType.COMMAND
                qt.actions
                    ?.onComplete
                    ?.first()
                    ?.command shouldBe "some params"
            }

            test("onFirstComplete and onComplete together") {
                val qt =
                    convert(
                        QuestDef(
                            title = "BothActions",
                            actions =
                                ActionsDef(
                                    onFirstComplete = listOf(ActionEntry("Command", "say first")),
                                    onComplete = listOf(ActionEntry("Item", "stone,1")),
                                ),
                        ),
                    )
                qt.actions?.onFirstComplete?.size shouldBe 1
                qt.actions?.onComplete?.size shouldBe 1
            }

            test("null actions") {
                val qt = convert(QuestDef(title = "NoActions"))
                qt.actions.shouldBeNull()
            }
        }

        context("Script parsing") {
            test("OnStart script") {
                val qt =
                    convert(
                        QuestDef(
                            title = "OnStartScript",
                            scripts = mapOf("OnStart" to listOf("say started")),
                        ),
                    )
                qt.scripts.size shouldBe 1
                qt.scripts
                    .first()
                    .trigger.name shouldBe "START"
                qt.scripts.first().delay shouldBe 0L
            }

            test("OnStart with delay") {
                val qt =
                    convert(
                        QuestDef(
                            title = "DelayedScript",
                            scripts = mapOf("OnStart+40" to listOf("say delayed")),
                        ),
                    )
                qt.scripts.size shouldBe 1
                qt.scripts
                    .first()
                    .trigger.name shouldBe "START"
                qt.scripts.first().delay shouldBe 40L
            }

            test("OnComplete script") {
                val qt =
                    convert(
                        QuestDef(
                            title = "CompleteScript",
                            scripts = mapOf("OnComplete" to listOf("say done")),
                        ),
                    )
                qt.scripts
                    .first()
                    .trigger.name shouldBe "COMPLETE"
            }

            test("OnCancel script") {
                val qt =
                    convert(
                        QuestDef(
                            title = "CancelScript",
                            scripts = mapOf("OnCancel" to listOf("say cancelled")),
                        ),
                    )
                qt.scripts
                    .first()
                    .trigger.name shouldBe "CANCEL"
            }

            test("OnEnd script") {
                val qt =
                    convert(
                        QuestDef(
                            title = "EndScript",
                            scripts = mapOf("OnEnd" to listOf("say ended")),
                        ),
                    )
                qt.scripts
                    .first()
                    .trigger.name shouldBe "END"
            }

            test("unknown trigger produces empty scripts") {
                val qt =
                    convert(
                        QuestDef(
                            title = "BadTrigger",
                            scripts = mapOf("OnUnknown" to listOf("test")),
                        ),
                    )
                qt.scripts shouldBe emptyList()
            }

            test("multiple script triggers") {
                val qt =
                    convert(
                        QuestDef(
                            title = "MultiScripts",
                            scripts =
                                mapOf(
                                    "OnStart" to listOf("say started"),
                                    "OnStart+20" to listOf("say delayed"),
                                    "OnComplete" to listOf("say done", "give % diamond 1"),
                                    "OnCancel" to listOf("say cancelled"),
                                ),
                        ),
                    )
                qt.scripts.size shouldBe 4
            }

            test("script with empty commands") {
                val qt =
                    convert(
                        QuestDef(
                            title = "EmptyScript",
                            scripts = mapOf("OnStart" to emptyList()),
                        ),
                    )
                qt.scripts.size shouldBe 1
                qt.scripts.first().commands shouldBe emptyList()
            }

            test("null scripts") {
                val qt = convert(QuestDef(title = "NoScripts"))
                qt.scripts shouldBe emptyList()
            }
        }

        context("Guide parsing") {
            test("guide with condition") {
                val qt =
                    convert(
                        QuestDef(
                            title = "GuideCond",
                            location = "world,0,0,0",
                            guides = listOf(GuideDef("Point", "100,64,200", "kill=5")),
                        ),
                    )
                qt.guides.first().requirements["kill"] shouldBe 5
            }

            test("guide without condition") {
                val qt =
                    convert(
                        QuestDef(
                            title = "GuideNoCond",
                            location = "world,0,0,0",
                            guides = listOf(GuideDef("Point", "100,64,200")),
                        ),
                    )
                qt.guides.first().requirements shouldBe emptyMap()
            }

            test("guide with malformed condition defaults to 1") {
                val qt =
                    convert(
                        QuestDef(
                            title = "BadCond",
                            location = "world,0,0,0",
                            guides = listOf(GuideDef("Point", "100,64,200", "kill=abc")),
                        ),
                    )
                qt.guides.first().requirements["kill"] shouldBe 1
            }

            test("guide with short location inherits world from quest") {
                val qt =
                    convert(
                        QuestDef(
                            title = "ShortLoc",
                            location = "world_nether,10,20,30",
                            guides = listOf(GuideDef("Point", "100,64,200")),
                        ),
                    )
                qt.guides
                    .first()
                    .location.world shouldBe "world_nether"
                qt.guides
                    .first()
                    .location.x shouldBe 100.0
                qt.guides
                    .first()
                    .location.y shouldBe 64.0
                qt.guides
                    .first()
                    .location.z shouldBe 200.0
            }

            test("guide with full world location uses own world") {
                val qt =
                    convert(
                        QuestDef(
                            title = "FullLoc",
                            location = "world_nether,10,20,30",
                            guides = listOf(GuideDef("Point", "world_the_end,500,100,0")),
                        ),
                    )
                qt.guides
                    .first()
                    .location.world shouldBe "world_the_end"
            }

            test("multiple guides") {
                val qt =
                    convert(
                        QuestDef(
                            title = "MultiGuides",
                            location = "world,0,0,0",
                            guides =
                                listOf(
                                    GuideDef("Point A", "100,64,0"),
                                    GuideDef("Point B", "200,64,0", "progress=10"),
                                    GuideDef("Point C", "300,64,0"),
                                ),
                        ),
                    )
                qt.guides.size shouldBe 3
            }
        }

        context("Full converter edge cases") {
            test("empty description list") {
                val qt = convert(QuestDef(title = "NoDesc"))
                qt.description shouldBe emptyList()
            }

            test("giver is null") {
                val qt = convert(QuestDef(title = "NoGiver"))
                qt.giver.shouldBeNull()
            }

            test("category passthrough") {
                val qt = convert(QuestDef(title = "Cat", category = "lq:weekly"))
                qt.category shouldBe "lq:weekly"
            }

            test("death limit is null when not specified") {
                val qt = convert(QuestDef(title = "NoDeath"))
                qt.deathLimit.shouldBeNull()
            }

            test("death limit when specified") {
                val qt = convert(QuestDef(title = "Death3", options = QuestOptionsDef(deathLimit = 3)))
                qt.deathLimit shouldBe 3
            }

            test("accept conditions with money") {
                val qt =
                    convert(
                        QuestDef(
                            title = "MoneyReq",
                            requirements = RequirementDef(money = 250.0),
                        ),
                    )
                qt.acceptConditions.money shouldBe 250.0
            }
        }
    })
