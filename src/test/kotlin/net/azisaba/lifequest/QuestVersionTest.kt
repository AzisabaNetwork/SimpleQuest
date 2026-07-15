package net.azisaba.lifequest

import com.charleskorn.kaml.Yaml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.serializer
import net.azisaba.lifequest.data.QuestVersion
import net.azisaba.lifequest.data.yaml.*
import java.security.MessageDigest

class QuestVersionTest :
    FunSpec({

        test("compute generates non-empty checksum") {
            val v = QuestVersion.compute("Test YAML content")
            v.checksum shouldNotBe ""
            v.checksum.length shouldBe 64 // SHA-256 hex
            v.updatedAt shouldNotBe ""
            v.updatedBy shouldNotBe ""
        }

        test("same input produces same checksum") {
            val content = "Same content every time"
            val v1 = QuestVersion.compute(content)
            val v2 = QuestVersion.compute(content)
            v1.checksum shouldBe v2.checksum
        }

        test("different input produces different checksum") {
            val v1 = QuestVersion.compute("Content A")
            val v2 = QuestVersion.compute("Content B")
            v1.checksum shouldNotBe v2.checksum
        }

        test("checksum is SHA-256 hex") {
            val v = QuestVersion.compute("test")
            v.checksum shouldMatch Regex("^[a-f0-9]{64}$")
        }

        test("updatedBy uses system property when set") {
            System.setProperty("lifequest.server-id", "custom-server")
            val v = QuestVersion.compute("test")
            v.updatedBy shouldBe "custom-server"
            System.clearProperty("lifequest.server-id")
        }
    })
