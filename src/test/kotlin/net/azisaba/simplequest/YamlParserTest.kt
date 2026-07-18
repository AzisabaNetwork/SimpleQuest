package net.azisaba.simplequest

import com.charleskorn.kaml.Yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.serializer
import net.azisaba.simplequest.data.yaml.*
import net.kyori.adventure.key.Key

class YamlParserTest :
    FunSpec({

        val yaml = Yaml.default
        val mapSerializer = MapSerializer(serializer<String>(), QuestDef.serializer())

        fun parse(raw: String): Map<String, QuestDef> = yaml.decodeFromString(mapSerializer, raw.trimIndent())

        test("minimal quest: title only") {
            val parsed =
                parse(
                    """
            Minimal:
              Title: "&aMinimal"
        """,
                )
            parsed.size shouldBe 1
            parsed["Minimal"]?.title shouldBe "&aMinimal"
        }

        test("all default values for optional fields") {
            val parsed =
                parse(
                    """
            Defaults:
              Title: "Test"
        """,
                )
            val def = parsed["Defaults"]!!
            def.description shouldBe emptyList()
            def.icon shouldBe "STONE"
            def.aura shouldBe false
            def.giver shouldBe null
            def.category shouldBe "lq:general"
            def.location shouldBe null
            def.options shouldBe null
            def.requirements shouldBe null
            def.objectives shouldBe emptyMap()
            def.actions shouldBe null
            def.guides shouldBe null
            def.scripts shouldBe null
            def.unlock shouldBe null
        }

        test("parse WolfSlayer YAML") {
            val raw =
                """
WolfSlayer:
  Title: "&cWolf Slayer"
  Description:
    - "&7Kill 10 wolves!"
  Icon: "STONE_SWORD:3001"
  Giver: "&eHunter"
  Category: "lq:general"
  Location: "world,100,64,100"
  Options:
    MaxParty: 1-4
    Limits:
      Daily: 3
  Requirements:
    PvELevel: 5
  Objectives:
    KillWolf: 10
  Actions:
    OnFirstComplete:
      - Type: MythicItem
        Params: "WolfFang,5"
    OnComplete:
      - Type: Item
        Params: "minecraft:diamond,3"
  Scripts:
    OnStart:
      - "say started"
  Guides:
    - Title: "&aForest Gate"
      Location: "100,64,100"
      Condition: "KillWolf=1"
                """.trimIndent()
            val parsed: Map<String, QuestDef> = yaml.decodeFromString(mapSerializer, raw)

            parsed.size shouldBe 1
            val def = parsed["WolfSlayer"]!!
            def.title shouldBe "&cWolf Slayer"
            def.description shouldBe listOf("&7Kill 10 wolves!")
            def.icon shouldBe "STONE_SWORD:3001"
            def.giver shouldBe "&eHunter"
            def.category shouldBe "lq:general"
            def.location shouldBe "world,100,64,100"
            def.options?.maxParty shouldBe "1-4"
            def.options?.limits?.daily shouldBe 3
            def.requirements?.pveLevel shouldBe 5
            def.objectives["KillWolf"] shouldBe "10"
            def.actions?.onFirstComplete?.size shouldBe 1
            def.actions
                ?.onFirstComplete
                ?.first()
                ?.type shouldBe "MythicItem"
            def.actions
                ?.onComplete
                ?.first()
                ?.params shouldBe "minecraft:diamond,3"
            def.scripts?.size shouldBe 1
            def.guides?.size shouldBe 1
            def.guides?.first()?.let {
                it.title shouldBe "&aForest Gate"
                it.location shouldBe "100,64,100"
                it.condition shouldBe "KillWolf=1"
            }
        }

        test("parse SupplyProcurement YAML") {
            val raw =
                """
SupplyProcurement:
  Title: "&6Supply Procurement"
  Description:
    - "&7Collect materials"
  Icon: "CHEST:4001"
  Category: "lq:general"
  Location: "world,200,64,200"
  Options:
    MaxParty: 1-4
  Requirements:
    PvELevel: 3
    Money: 100.0
  Objectives:
    IronIngot: "minecraft:iron_ingot*5"
    GoldIngot: "minecraft:gold_ingot*5"
    Diamond: "minecraft:diamond*5"
  Actions:
    OnFirstComplete:
      - Type: Item
        Params: "minecraft:tripwire_hook,1"
        DisplayName: "&6Special Key"
      - Type: Command
        Params: "lp user %player% parent set supplier"
    OnComplete:
      - Type: Item
        Params: "minecraft:emerald_block,3"
                """.trimIndent()
            val parsed: Map<String, QuestDef> = yaml.decodeFromString(mapSerializer, raw)
            val def = parsed["SupplyProcurement"]!!

            def.title shouldBe "&6Supply Procurement"
            def.icon shouldBe "CHEST:4001"
            def.requirements?.money shouldBe 100.0
            def.objectives["IronIngot"] shouldBe "minecraft:iron_ingot*5"
            def.objectives["GoldIngot"] shouldBe "minecraft:gold_ingot*5"
            def.objectives["Diamond"] shouldBe "minecraft:diamond*5"

            val firstActions = def.actions?.onFirstComplete
            firstActions?.size shouldBe 2
            firstActions?.first()?.let {
                it.type shouldBe "Item"
                it.params shouldBe "minecraft:tripwire_hook,1"
                it.displayName shouldBe "&6Special Key"
            }
            firstActions?.get(1)?.type shouldBe "Command"
        }

        test("quest with only Scripts section") {
            val parsed =
                parse(
                    """
            ScriptOnly:
              Title: "Script Test"
              Scripts:
                OnStart:
                  - "say started"
                OnStart+20:
                  - "say delayed"
                OnComplete:
                  - "say done"
                  - "give % minecraft:diamond 1"
        """,
                )
            val def = parsed["ScriptOnly"]!!
            def.scripts?.size shouldBe 3
            def.scripts?.containsKey("OnStart") shouldBe true
            def.scripts?.containsKey("OnStart+20") shouldBe true
            def.scripts?.containsKey("OnComplete") shouldBe true
            def.scripts?.get("OnComplete")?.size shouldBe 2
        }

        test("quest with all Options fields") {
            val parsed =
                parse(
                    """
            FullOptions:
              Title: "Full Options"
              Options:
                MaxParty: 6
                Limits:
                  Daily: 5
                  Weekly: 10
                  Monthly: 30
                  Yearly: 365
                  Lifetime: 100
                DeathLimit: 3
        """,
                )
            val opt = parsed["FullOptions"]!!.options!!
            opt.maxParty shouldBe "6"
            opt.limits?.daily shouldBe 5
            opt.limits?.weekly shouldBe 10
            opt.limits?.monthly shouldBe 30
            opt.limits?.yearly shouldBe 365
            opt.limits?.lifetime shouldBe 100
            opt.deathLimit shouldBe 3
        }

        test("quest with multiple descriptions") {
            val parsed =
                parse(
                    """
            MultiLine:
              Title: "Multi"
              Description:
                - "line1"
                - "line2"
                - "line3"
        """,
                )
            parsed["MultiLine"]!!.description shouldBe listOf("line1", "line2", "line3")
        }

        test("quest with Unlock section") {
            val parsed =
                parse(
                    """
            UnlockQuest:
              Title: "Unlock"
              Unlock:
                - EnterArea: "world,150,150,250,250"
        """,
                )
            parsed["UnlockQuest"]!!.unlock?.size shouldBe 1
            parsed["UnlockQuest"]!!.unlock?.first()?.enterArea shouldBe "world,150,150,250,250"
        }

        test("quest with aura icon") {
            val parsed =
                parse(
                    """
            AuraItem:
              Title: "Aura"
              Icon: "DIAMOND"
              Aura: true
        """,
                )
            parsed["AuraItem"]!!.icon shouldBe "DIAMOND"
            parsed["AuraItem"]!!.aura shouldBe true
        }

        test("quest with giver") {
            val parsed =
                parse(
                    """
            WithGiver:
              Title: "Giver Test"
              Giver: "&eQuest Master"
        """,
                )
            parsed["WithGiver"]!!.giver shouldBe "&eQuest Master"
        }

        test("quest with PartyMode requirement") {
            val parsed =
                parse(
                    """
            PartyQuest:
              Title: "Party"
              Requirements:
                PartyMode: true
        """,
                )
            parsed["PartyQuest"]!!.requirements?.partyMode shouldBe true
        }

        test("quest with Money requirement") {
            val parsed =
                parse(
                    """
            PaidQuest:
              Title: "Paid"
              Requirements:
                Money: 500.50
        """,
                )
            parsed["PaidQuest"]!!.requirements?.money shouldBe 500.50
        }

        test("multiple entries in single file") {
            val parsed =
                parse(
                    """
            QuestA:
              Title: "Quest A"
            QuestB:
              Title: "Quest B"
            QuestC:
              Title: "Quest C"
        """,
                )
            parsed.size shouldBe 3
            parsed.containsKey("QuestA") shouldBe true
            parsed.containsKey("QuestB") shouldBe true
            parsed.containsKey("QuestC") shouldBe true
        }

        test("guides without condition") {
            val parsed =
                parse(
                    """
            NoCondition:
              Title: "No Condition"
              Guides:
                - Title: "Point A"
                  Location: "100,64,100"
        """,
                )
            val guide = parsed["NoCondition"]!!.guides?.first()!!
            guide.title shouldBe "Point A"
            guide.location shouldBe "100,64,100"
            guide.condition shouldBe null
        }

        test("empty actions") {
            val parsed =
                parse(
                    """
            EmptyActions:
              Title: "Empty"
              Actions:
                OnFirstComplete: []
                OnComplete: []
        """,
                )
            parsed["EmptyActions"]!!.actions?.onFirstComplete shouldBe emptyList()
            parsed["EmptyActions"]!!.actions?.onComplete shouldBe emptyList()
        }

        test("multiple actions in OnComplete") {
            val parsed =
                parse(
                    """
            MultiAction:
              Title: "Multi"
              Actions:
                OnComplete:
                  - Type: Command
                    Params: "say first"
                  - Type: Command
                    Params: "say second"
                  - Type: Command
                    Params: "say third"
        """,
                )
            parsed["MultiAction"]!!.actions?.onComplete?.size shouldBe 3
        }

        // --- 追加エッジケース ---

        test("special characters in title") {
            val parsed =
                parse(
                    """
                Special:
                  Title: "&a§lBold &c&lRed &nUnderline"
            """,
                )
            parsed["Special"]!!.title shouldBe "&a§lBold &c&lRed &nUnderline"
        }

        test("long description lines") {
            val longLine = "A".repeat(200)
            val parsed =
                parse(
                    """
                LongDesc:
                  Title: "Long"
                  Description:
                    - "$longLine"
                    - "short"
            """,
                )
            parsed["LongDesc"]!!.description.size shouldBe 2
            parsed["LongDesc"]!!.description.first().length shouldBe 200
        }

        test("unicode in values") {
            val parsed =
                parse(
                    """
                Unicode:
                  Title: "日本語タイトル"
                  Giver: "&e村長さん"
                  Description:
                    - "\u3042\u3044\u3046\u3048\u304a"
            """,
                )
            parsed["Unicode"]!!.title shouldBe "日本語タイトル"
            parsed["Unicode"]!!.giver shouldBe "&e村長さん"
        }

        test("script with multiple commands in one trigger") {
            val parsed =
                parse(
                    """
                MultiCmd:
                  Title: "MultiCmd"
                  Scripts:
                    OnStart:
                      - "say first"
                      - "say second"
                      - "give % minecraft:diamond 1"
                      - "broadcast Quest started!"
                    OnComplete:
                      - "say done"
            """,
                )
            parsed["MultiCmd"]!!.scripts?.get("OnStart")?.size shouldBe 4
            parsed["MultiCmd"]!!.scripts?.get("OnComplete")?.size shouldBe 1
        }

        test("all sections present in single file") {
            val parsed =
                parse(
                    """
                Full:
                  Title: "Complete"
                  Description:
                    - "desc"
                  Icon: "STONE:42"
                  Aura: true
                  Giver: "&eNPC"
                  Category: "lq:daily"
                  Location: "world,1,2,3"
                  Options:
                    MaxParty: "2-4"
                    Limits:
                      Daily: 5
                    DeathLimit: 2
                  Requirements:
                    PvELevel: 5
                    Money: 50.0
                    PartyMode: true
                  Objectives:
                    kill: "10"
                    collect: "minecraft:diamond*3"
                  Actions:
                    OnFirstComplete:
                      - Type: Item
                        Params: "minecraft:diamond,5"
                    OnComplete:
                      - Type: Command
                        Params: "say done"
                  Guides:
                    - Title: "Point"
                      Location: "100,64,200"
                      Condition: "kill=1"
                  Scripts:
                    OnStart:
                      - "say hi"
                  Unlock:
                    - EnterArea: "world,10,10,100,100"
            """,
                )
            val def = parsed["Full"]!!
            def.title shouldBe "Complete"
            def.icon shouldBe "STONE:42"
            def.aura shouldBe true
            def.giver shouldBe "&eNPC"
            def.category shouldBe "lq:daily"
            def.location shouldBe "world,1,2,3"
            def.options?.maxParty shouldBe "2-4"
            def.options?.limits?.daily shouldBe 5
            def.options?.deathLimit shouldBe 2
            def.requirements?.pveLevel shouldBe 5
            def.requirements?.money shouldBe 50.0
            def.requirements?.partyMode shouldBe true
            def.objectives.size shouldBe 2
            def.actions?.onFirstComplete?.size shouldBe 1
            def.actions?.onComplete?.size shouldBe 1
            def.guides?.size shouldBe 1
            def.scripts?.size shouldBe 1
            def.unlock?.size shouldBe 1
        }

        test("multiple empty lines and indentation") {
            val parsed =
                parse(
                    """
                Indent:
                  Title: "Indent Test"


                  Description:
                    - "line1"


                    - "line2"

                  Icon: "DIAMOND"
            """,
                )
            parsed["Indent"]!!.title shouldBe "Indent Test"
            parsed["Indent"]!!.description shouldBe listOf("line1", "line2")
        }

        test("single-quoted strings") {
            val parsed =
                parse(
                    """
                SingleQuote:
                  Title: 'Single Quoted Title'
                  Giver: '&eQuest Master'
            """,
                )
            parsed["SingleQuote"]!!.title shouldBe "Single Quoted Title"
            parsed["SingleQuote"]!!.giver shouldBe "&eQuest Master"
        }

        test("numeric string values preserved") {
            val parsed =
                parse(
                    """
                Numeric:
                  Title: "Numeric"
                  Location: "100,200,300"
                  Options:
                    MaxParty: "4"
            """,
                )
            parsed["Numeric"]!!.location shouldBe "100,200,300"
            parsed["Numeric"]!!.options?.maxParty shouldBe "4"
        }

        test("quest name with hyphens and underscores") {
            val parsed =
                parse(
                    """
                my-quest_name:
                  Title: "HyphenUnderscore"
            """,
                )
            parsed["my-quest_name"]!!.title shouldBe "HyphenUnderscore"
        }

        test("comments in YAML are ignored") {
            val parsed =
                parse(
                    """
                Commented:
                  Title: "Visible"
                  # Icon: "HIDDEN"
                  Description:
                    - "visible"
                    # - "hidden"
            """,
                )
            parsed["Commented"]!!.title shouldBe "Visible"
            parsed["Commented"]!!.description shouldBe listOf("visible")
            parsed["Commented"]!!.icon shouldBe "STONE" // default
        }
    })
