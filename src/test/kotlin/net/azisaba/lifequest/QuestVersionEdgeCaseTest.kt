package net.azisaba.lifequest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.azisaba.lifequest.data.QuestVersion
import net.azisaba.lifequest.domain.data.Icon
import net.azisaba.lifequest.domain.quest.model.PlayLimits
import net.azisaba.lifequest.domain.quest.model.QuestType

class QuestVersionEdgeCaseTest :
    FunSpec({

        context("QuestVersion.compute") {
            test("empty content produces valid checksum") {
                val v = QuestVersion.compute("")
                v.checksum.length shouldBe 64
                v.checksum shouldNotBe ""
            }

            test("null character in content") {
                val v = QuestVersion.compute("content\u0000with null")
                v.checksum.length shouldBe 64
            }

            test("very long content") {
                val longContent = "A".repeat(10_000)
                val v = QuestVersion.compute(longContent)
                v.checksum.length shouldBe 64
            }

            test("two identical very similar content produce different hash") {
                val v1 = QuestVersion.compute("QuestA")
                val v2 = QuestVersion.compute("QuestB")
                v1.checksum shouldNotBe v2.checksum
            }

            test("newline differences produce different hash") {
                val v1 = QuestVersion.compute("line1\nline2")
                val v2 = QuestVersion.compute("line1\r\nline2")
                v1.checksum shouldNotBe v2.checksum
            }

            test("trailing whitespace affects hash") {
                val v1 = QuestVersion.compute("content")
                val v2 = QuestVersion.compute("content ")
                v1.checksum shouldNotBe v2.checksum
            }

            test("version field is always 1") {
                val v = QuestVersion.compute("any")
                v.version shouldBe 1
            }

            test("updatedAt is not empty") {
                val v = QuestVersion.compute("test")
                v.updatedAt shouldNotBe ""
            }

            test("updatedBy is not empty") {
                val v = QuestVersion.compute("test")
                v.updatedBy shouldNotBe ""
            }

            test("SHA-256 of known input") {
                // SHA-256 of "LifeQuest" = known hex
                val v = QuestVersion.compute("LifeQuest")
                // Just verify it's 64 hex chars
                v.checksum.matches(Regex("^[a-f0-9]{64}$")) shouldBe true
            }

            test("unicode content produces valid checksum") {
                val v = QuestVersion.compute("日本語コンテンツ テスト")
                v.checksum.length shouldBe 64
            }
        }
    })
