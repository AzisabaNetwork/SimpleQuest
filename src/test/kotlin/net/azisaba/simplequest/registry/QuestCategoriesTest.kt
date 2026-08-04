package net.azisaba.simplequest.registry

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class QuestCategoriesTest :
    FunSpec({

        context("isValidCategory") {
            test("accepts valid lq:general") {
                QuestCategories.isValidCategory("lq:general") shouldBe true
            }

            test("accepts valid lq:daily") {
                QuestCategories.isValidCategory("lq:daily") shouldBe true
            }

            test("accepts valid lq:story") {
                QuestCategories.isValidCategory("lq:story") shouldBe true
            }

            test("accepts valid lq:event") {
                QuestCategories.isValidCategory("lq:event") shouldBe true
            }

            test("rejects invalid category") {
                QuestCategories.isValidCategory("lq:custom") shouldBe false
                QuestCategories.isValidCategory("lq:unknown") shouldBe false
                QuestCategories.isValidCategory("something") shouldBe false
            }
        }

        context("validCategoryKeys") {
            test("returns the 4 built-in keys") {
                val keys = QuestCategories.validCategoryKeys()
                keys shouldBe setOf("general", "daily", "story", "event")
            }
        }
    })
