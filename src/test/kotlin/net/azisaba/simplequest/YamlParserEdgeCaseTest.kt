package net.azisaba.simplequest

import com.charleskorn.kaml.Yaml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.serializer
import net.azisaba.simplequest.data.yaml.*

class YamlParserEdgeCaseTest :
    FunSpec({

        val yaml = Yaml.default
        val mapSerializer = MapSerializer(serializer<String>(), QuestDef.serializer())

        fun parse(raw: String): Map<String, QuestDef> = yaml.decodeFromString(mapSerializer, raw.trimIndent())

        context("Edge case values") {
            test("title with only color codes") {
                val p = parse("ColorOnly:\n  Title: \"&a&l&n\"")
                p["ColorOnly"]!!.title shouldBe "&a&l&n"
            }

            test("json-like values in giver") {
                val p = parse("JsonGiver:\n  Title: \"Test\"\n  Giver: \"{\\\"name\\\":\\\"NPC\\\"}\"")
                p["JsonGiver"]!!.giver shouldBe "{\"name\":\"NPC\"}"
            }

            test("newline escape in description") {
                val p = parse("Newline:\n  Title: \"Test\"\n  Description:\n    - \"line1\\nline2\"")
                p["Newline"]!!.description shouldBe listOf("line1\nline2")
            }

            test("empty string title") {
                val p = parse("EmptyTitle:\n  Title: \"\"")
                p["EmptyTitle"]!!.title shouldBe ""
            }

            test("whitespace-only title") {
                val p = parse("WsTitle:\n  Title: \"   \"")
                p["WsTitle"]!!.title shouldBe "   "
            }

            test("numeric key quest names") {
                val p = parse("123Quest:\n  Title: \"Numbers\"")
                p["123Quest"]!!.title shouldBe "Numbers"
            }

            test("camelCase quest name") {
                val p = parse("camelCaseQuest:\n  Title: \"Camel\"")
                p["camelCaseQuest"]!!.title shouldBe "Camel"
            }

            test("very long quest name") {
                val longName = "Q".repeat(100)
                val p = parse("$longName:\n  Title: \"Long\"")
                p[longName]!!.title shouldBe "Long"
            }
        }

        context("Options edge cases") {
            test("zero limits") {
                val p =
                    parse(
                        """
                    ZeroLimits:
                      Title: "Zero"
                      Options:
                        Limits:
                          Daily: 0
                          Weekly: 0
                """,
                    )
                p["ZeroLimits"]!!.options?.limits?.daily shouldBe 0
                p["ZeroLimits"]!!.options?.limits?.weekly shouldBe 0
            }

            test("negative death limit") {
                val p =
                    parse(
                        """
                    NegativeDeath:
                      Title: "Neg"
                      Options:
                        DeathLimit: -1
                """,
                    )
                p["NegativeDeath"]!!.options?.deathLimit shouldBe -1
            }

            test("maxParty as empty string") {
                val p =
                    parse(
                        """
                    EmptyMaxParty:
                      Title: "Empty"
                      Options:
                        MaxParty: ""
                """,
                    )
                p["EmptyMaxParty"]!!.options?.maxParty shouldBe ""
            }

            test("limits with only lifetime") {
                val p =
                    parse(
                        """
                    LifetimeOnly:
                      Title: "Lifetime"
                      Options:
                        Limits:
                          Lifetime: 1
                """,
                    )
                p["LifetimeOnly"]!!.options?.limits?.lifetime shouldBe 1
                p["LifetimeOnly"]!!
                    .options
                    ?.limits
                    ?.daily
                    .shouldBe(null)
            }
        }

        context("Requirements edge cases") {
            test("requirement with zero pve level and zero money") {
                val p =
                    parse(
                        """
                    ZeroReqs:
                      Title: "Zero"
                      Requirements:
                        PvELevel: 0
                        Money: 0.0
                """,
                    )
                p["ZeroReqs"]!!.requirements?.pveLevel shouldBe 0
                p["ZeroReqs"]!!.requirements?.money shouldBe 0.0
            }

            test("requirement with large decimal money") {
                val p =
                    parse(
                        """
                    LargeMoney:
                      Title: "Large"
                      Requirements:
                        Money: 1234567.89
                """,
                    )
                p["LargeMoney"]!!.requirements?.money shouldBe 1234567.89
            }

            test("PartyMode false explicitly") {
                val p =
                    parse(
                        """
                    ExplicitFalse:
                      Title: "False"
                      Requirements:
                        PartyMode: false
                """,
                    )
                p["ExplicitFalse"]!!.requirements?.partyMode shouldBe false
            }
        }

        context("Unlock edge cases") {
            test("multiple unlock entries") {
                val p =
                    parse(
                        """
                    MultiUnlock:
                      Title: "Multi"
                      Unlock:
                        - EnterArea: "world,10,10,100,100"
                        - EnterArea: "world,200,10,300,100"
                        - EnterArea: "world,400,10,500,100"
                """,
                    )
                p["MultiUnlock"]!!.unlock?.size shouldBe 3
            }

            test("unlock with empty EnterArea") {
                val p =
                    parse(
                        """
                    EmptyArea:
                      Title: "Empty"
                      Unlock:
                        - EnterArea: ""
                """,
                    )
                p["EmptyArea"]!!.unlock?.first()?.enterArea shouldBe ""
            }
        }

        context("Mixed complexity") {
            test("two quests in same yaml with all sections") {
                val p =
                    parse(
                        """
                    QuestOne:
                      Title: "One"
                      Icon: "DIAMOND:1"
                      Aura: true
                      Options:
                        Limits:
                          Daily: 1
                      Objectives:
                        kill: "5"
                    QuestTwo:
                      Title: "Two"
                      Icon: "STONE"
                      Options:
                        Limits:
                          Daily: 2
                      Objectives:
                        collect: "stone*10"
                """,
                    )
                p.size shouldBe 2
                p["QuestOne"]!!.aura shouldBe true
                p["QuestTwo"]!!.aura shouldBe false
                p["QuestOne"]!!.options?.limits?.daily shouldBe 1
                p["QuestTwo"]!!.options?.limits?.daily shouldBe 2
            }

            test("quest with only Objectives and no other sections") {
                val p =
                    parse(
                        """
                    ObjectivesOnly:
                      Title: "Obj"
                      Objectives:
                        a: "1"
                        b: "2"
                        c: "3"
                        d: "4"
                        e: "5"
                """,
                    )
                p["ObjectivesOnly"]!!.objectives.size shouldBe 5
            }

            test("nested YAML values parse correctly") {
                val p =
                    parse(
                        """
                    Nested:
                      Title: "Nested"
                      Actions:
                        OnComplete:
                          - Type: Command
                            Params: 'say {\"nested\":true}'
                """,
                    )
                // YAML single-quoted string preserves backslashes literally
                val params =
                    p["Nested"]!!
                        .actions
                        ?.onComplete
                        ?.first()
                        ?.params ?: ""
                (params.length > 0) shouldBe true
            }
        }

        context("YAML encoding") {
            test("boolean true as string") {
                val p = parse("BoolTrue:\n  Title: \"Bool\"\n  Aura: true")
                p["BoolTrue"]!!.aura shouldBe true
            }

            test("boolean false as string") {
                val p = parse("BoolFalse:\n  Title: \"Bool\"\n  Aura: false")
                p["BoolFalse"]!!.aura shouldBe false
            }

            test("integer as unquoted value") {
                val p = parse("IntVal:\n  Title: Test\n  Options:\n    DeathLimit: 5")
                p["IntVal"]!!.options?.deathLimit shouldBe 5
            }
        }

        context("Missing title is a parse error") {
            test("quest without Title throws") {
                val result =
                    runCatching {
                        parse("NoTitle:\n  Icon: \"STONE\"")
                    }
                // kaml may or may not throw depending on defaults
                // Title is required (non-nullable), so it should fail
                result.isFailure shouldBe true
            }
        }

        context("Large data") {
            test("20 quests in single file") {
                val yamlLines =
                    buildString {
                        (1..20).joinTo(this, "\n") { i ->
                            "Quest$i:\n  Title: \"Quest $i\"\n  Objectives:\n    kill: \"$i\""
                        }
                    }
                val p = parse(yamlLines)
                p.size shouldBe 20
            }
        }
    })
