package net.azisaba.lifequest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.azisaba.lifequest.data.MultiServerConfig
import net.azisaba.lifequest.database.DatabaseHelper
import net.azisaba.lifequest.database.SyncService
import java.io.File
import java.util.UUID

class SyncServiceExtTest :
    FunSpec({

        lateinit var h2: HikariDataSource
        lateinit var db: DatabaseHelper
        lateinit var syncService: SyncService
        lateinit var tempDir: File

        isolationMode = IsolationMode.InstancePerTest

        beforeTest {
            val config =
                HikariConfig().apply {
                    jdbcUrl = "jdbc:h2:mem:sync_ext_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MariaDB"
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
                    .createTempDirectory("lifequest-sync-ext-")
                    .toFile()
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

        afterTest {
            h2.close()
            tempDir.deleteRecursively()
        }

        test("empty dataFolder with no directories has no conflicts") {
            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
        }

        test("namespace without types directory is skipped") {
            File(tempDir, "@ns").mkdirs()
            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
        }

        test("types directory with no yaml files") {
            File(tempDir, "@empty/types").mkdirs()
            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
        }

        test("sha256 of different inputs are different") {
            val a = SyncService.sha256("content-A")
            val b = SyncService.sha256("content-B")
            a shouldBe SyncService.sha256("content-A")
            b shouldBe SyncService.sha256("content-B")
            (a != b) shouldBe true
        }

        test("sha256 length is always 64") {
            SyncService.sha256("").length shouldBe 64
            SyncService.sha256("a").length shouldBe 64
            SyncService.sha256("a".repeat(1000)).length shouldBe 64
        }

        test("have no conflicts when DB matches local files") {
            File(tempDir, "@seed/types").mkdirs()
            File(tempDir, "@seed/types/test.yml").writeText("Test:\n  Title: \"T\"")
            db.update(
                "INSERT INTO quest_definitions (quest_key, yaml_text, checksum, updated_at, updated_by, conflict) VALUES (?, ?, ?, ?, ?, FALSE)",
                "seed/test",
                "Test:\n  Title: \"T\"",
                SyncService.sha256("Test:\n  Title: \"T\""),
                "now",
                "test-server",
            )
            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
        }

        test("resolveUseLocal with no conflicts is no-op") {
            syncService.resolveUseLocal()
            syncService.hasConflicts shouldBe false
        }

        test("multiple yaml files in same namespace") {
            File(tempDir, "@multi/types").mkdirs()
            File(tempDir, "@multi/types/quest1.yml").writeText("Q1:\n  Title: \"One\"")
            File(tempDir, "@multi/types/quest2.yml").writeText("Q2:\n  Title: \"Two\"")
            File(tempDir, "@multi/types/quest3.yml").writeText("Q3:\n  Title: \"Three\"")
            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe false
        }

        test("explicit conflict entry detected") {
            db.update(
                "INSERT INTO quest_definitions (quest_key, yaml_text, checksum, updated_at, updated_by, conflict) VALUES (?, ?, ?, ?, ?, TRUE)",
                "ns/conflict",
                "Q:\n  Title: \"T\"",
                SyncService.sha256("Q:\n  Title: \"T\""),
                "now",
                "server",
            )
            syncService.sync(tempDir)
            syncService.hasConflicts shouldBe true
            syncService.conflictedQuests() shouldBe setOf("ns/conflict")
        }
    })
