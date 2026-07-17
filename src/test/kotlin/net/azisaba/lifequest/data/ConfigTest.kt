package net.azisaba.lifequest.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ConfigTest :
    FunSpec({

        context("DatabaseConfig") {
            test("defaults") {
                val c = DatabaseConfig()
                c.host shouldBe "localhost"
                c.port shouldBe 3306
                c.name shouldBe "lifequest"
                c.user shouldBe "root"
                c.password shouldBe ""
            }

            test("custom values") {
                val c = DatabaseConfig(host = "db.example.com", port = 3307, name = "prod", user = "admin", password = "secret")
                c.host shouldBe "db.example.com"
                c.port shouldBe 3307
                c.name shouldBe "prod"
                c.user shouldBe "admin"
                c.password shouldBe "secret"
            }

            test("equality") {
                val a = DatabaseConfig(host = "h1", port = 1, name = "n1", user = "u1", password = "p1")
                val b = DatabaseConfig(host = "h1", port = 1, name = "n1", user = "u1", password = "p1")
                a shouldBe b
            }
        }

        context("RedisConfig") {
            test("defaults") {
                val c = RedisConfig()
                c.host shouldBe "localhost"
                c.port shouldBe 6379
                c.password shouldBe ""
            }

            test("custom values") {
                val c = RedisConfig(host = "redis.local", port = 6380, password = "redis-pass")
                c.host shouldBe "redis.local"
                c.port shouldBe 6380
                c.password shouldBe "redis-pass"
            }
        }

        context("PanelConfig") {
            test("defaults") {
                val c = PanelConfig()
                c.title shouldBe "&dLifeQuest"
                c.footer shouldBe "&7azisaba.net"
            }

            test("custom values") {
                val c = PanelConfig(title = "&aMy Title", footer = "&cCustom")
                c.title shouldBe "&aMy Title"
                c.footer shouldBe "&cCustom"
            }
        }

        context("BackupConfig") {
            test("defaults") {
                val c = BackupConfig()
                c.enabled shouldBe false
                c.intervalHours shouldBe 24
                c.retentionDays shouldBe 30
                c.directory shouldBe "plugins/LifeQuest/backups/"
            }

            test("custom values") {
                val c = BackupConfig(enabled = true, intervalHours = 6, retentionDays = 7, directory = "/backups/")
                c.enabled shouldBe true
                c.intervalHours shouldBe 6
                c.retentionDays shouldBe 7
                c.directory shouldBe "/backups/"
            }
        }

        context("MultiServerConfig") {
            test("defaults") {
                val c = MultiServerConfig()
                c.writeToMysql shouldBe false
                c.writeToYaml shouldBe false
                c.conflictMode shouldBe "LOCAL"
                c.backup.enabled shouldBe false
            }

            test("custom values") {
                val c =
                    MultiServerConfig(
                        writeToMysql = true,
                        writeToYaml = true,
                        conflictMode = "MYSQL",
                        backup = BackupConfig(enabled = true),
                    )
                c.writeToMysql shouldBe true
                c.writeToYaml shouldBe true
                c.conflictMode shouldBe "MYSQL"
                c.backup.enabled shouldBe true
            }
        }

        context("DiscordConfig") {
            test("defaults") {
                val c = DiscordConfig()
                c.webhookUrl shouldBe ""
            }

            test("custom webhook url") {
                val c = DiscordConfig(webhookUrl = "https://discord.com/api/webhooks/123/abc")
                c.webhookUrl shouldBe "https://discord.com/api/webhooks/123/abc"
            }
        }

        context("LifeQuestConfig") {
            test("defaults") {
                val c = LifeQuestConfig()
                c.database.host shouldBe "localhost"
                c.redis.host shouldBe "localhost"
                c.maxPartySize shouldBe 8
                c.partyInviteLimit shouldBe 1200
                c.panel.title shouldBe "&dLifeQuest"
                c.multiServer.writeToMysql shouldBe false
                c.discord.webhookUrl shouldBe ""
            }

            test("custom maxPartySize") {
                val c = LifeQuestConfig(maxPartySize = 16)
                c.maxPartySize shouldBe 16
            }

            test("custom partyInviteLimit") {
                val c = LifeQuestConfig(partyInviteLimit = 600)
                c.partyInviteLimit shouldBe 600
            }
        }
    })
