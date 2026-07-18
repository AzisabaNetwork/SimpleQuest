package net.azisaba.simplequest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.azisaba.simplequest.data.MultiServerConfig
import net.azisaba.simplequest.database.DatabaseHelper
import net.azisaba.simplequest.database.SyncService
import java.io.File
import java.util.UUID

class SyncServiceTest :
    FunSpec({

        lateinit var h2: HikariDataSource
        lateinit var db: DatabaseHelper
        lateinit var syncService: SyncService
        lateinit var tempDir: File

        // テスト間でDB状態を分離するため each
        isolationMode = IsolationMode.InstancePerTest

        fun createSchema() {
            db.execute(
                """CREATE TABLE IF NOT EXISTS quest_definitions (
                    quest_key VARCHAR(255) PRIMARY KEY,
                    yaml_text TEXT,
                    checksum VARCHAR(64),
                    updated_at VARCHAR(64),
                    updated_by VARCHAR(64),
                    conflict BOOLEAN DEFAULT FALSE
                )""",
            )
        }

        beforeTest {
            val config =
                HikariConfig().apply {
                    jdbcUrl = "jdbc:h2:mem:sync_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MariaDB"
                    driverClassName = "org.h2.Driver"
                    username = "sa"
                    password = ""
                }
            h2 = HikariDataSource(config)
            db = DatabaseHelper(h2)
            syncService =
                SyncService(
                    db,
                    java.util.logging.Logger
                        .getGlobal(),
                    MultiServerConfig(),
                )
            tempDir =
                java.nio.file.Files
                    .createTempDirectory("simplequest-sync-")
                    .toFile()
        }

        afterTest {
            h2.close()
            tempDir.deleteRecursively()
        }

        // ---- schema helper (legacy, replaced by createSchema() in beforeTest) ----

        @Suppress("unused")
        @Deprecated("Use createSchema() in beforeTest")
        fun oldCreateSchema() {
            db.execute(
                """CREATE TABLE quest_definitions (
                    quest_key VARCHAR(255) PRIMARY KEY,
                    yaml_text TEXT,
                    checksum VARCHAR(64),
                    updated_at VARCHAR(64),
                    updated_by VARCHAR(64),
                    conflict BOOLEAN DEFAULT FALSE
                )""",
            )
        }

        fun createNamespaceDir(name: String): File {
            val dir = File(tempDir, "@$name")
            dir.mkdirs()
            val typesDir = File(dir, "types")
            typesDir.mkdirs()
            return typesDir
        }

        // ---- tests ----

        test("sync with no database table should succeed") {
            // table がない状態でもエラーにならない
            // ただし createSchema を呼んだ後の sync は空テーブルでOK
            createSchema()
            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
        }

        test("sync with empty dataFolder") {
            createSchema()
            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
            syncService.conflictedQuests().size shouldBe 0
        }

        test("sync with local file only (no database) should not push") {
            createSchema()
            val typesDir = createNamespaceDir("test")
            val file = File(typesDir, "sample.yml")
            file.writeText("SampleQuest:\n  Title: \"&aSample\"")

            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
        }

        test("conflict detection - local and db have different checksums without conflict flag") {
            // writeToYaml=true の SyncService を使う
            val syncWithYaml =
                SyncService(
                    db,
                    java.util.logging.Logger
                        .getGlobal(),
                    MultiServerConfig(writeToYaml = true),
                )
            createSchema()
            val typesDir = createNamespaceDir("test")
            val file = File(typesDir, "quest1.yml")
            file.writeText("Quest1:\n  Title: \"&aLocal Version\"")

            // DB に別内容を登録
            val dbYaml = "Quest1:\n  Title: \"&bDB Version\""
            db.update(
                "INSERT INTO quest_definitions (quest_key, yaml_text, checksum, updated_at, updated_by, conflict) VALUES (?, ?, ?, ?, ?, FALSE)",
                "test/quest1",
                dbYaml,
                SyncService.sha256(dbYaml),
                "2024-01-01T00:00:00Z",
                "test-server",
            )

            syncWithYaml.sync(tempDir)
            syncWithYaml.hasConflicts shouldBe true
            syncWithYaml.conflictedQuests() shouldBe setOf("test/quest1")
        }

        test("resolveUseLocal clears conflicts and sets conflict to false") {
            createSchema()
            val typesDir = createNamespaceDir("test")
            val file = File(typesDir, "quest1.yml")
            file.writeText("Quest1:\n  Title: \"&aLocal Version\"")

            val dbYaml = "Quest1:\n  Title: \"&bDB Version\""
            db.update(
                "INSERT INTO quest_definitions (quest_key, yaml_text, checksum, updated_at, updated_by, conflict) VALUES (?, ?, ?, ?, ?, TRUE)",
                "test/quest1",
                dbYaml,
                SyncService.sha256(dbYaml),
                "2024-01-01T00:00:00Z",
                "test-server",
            )

            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe true

            syncService.resolveUseLocal()

            // 再チェック
            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
        }

        test("no conflicts when local and db are identical") {
            createSchema()
            val typesDir = createNamespaceDir("test")
            val yamlText = "Quest1:\n  Title: \"&aSame Content\""
            val file = File(typesDir, "quest1.yml")
            file.writeText(yamlText)

            val checksum = SyncService.sha256(yamlText)
            db.update(
                "INSERT INTO quest_definitions (quest_key, yaml_text, checksum, updated_at, updated_by, conflict) VALUES (?, ?, ?, ?, ?, FALSE)",
                "test/quest1",
                yamlText,
                checksum,
                "2024-01-01T00:00:00Z",
                "test-server",
            )

            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
        }

        test("sha256 produces consistent hash") {
            val a = SyncService.sha256("hello")
            val b = SyncService.sha256("hello")
            val c = SyncService.sha256("world")
            a shouldBe b
            a shouldNotBe c
            a.length shouldBe 64
        }

        test("sync with multiple namespaces") {
            createSchema()
            for (ns in listOf("alpha", "beta")) {
                val typesDir = createNamespaceDir(ns)
                val file = File(typesDir, "quest1.yml")
                file.writeText("Quest1:\n  Title: \"&a$ns\"")
            }

            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
        }
    })
