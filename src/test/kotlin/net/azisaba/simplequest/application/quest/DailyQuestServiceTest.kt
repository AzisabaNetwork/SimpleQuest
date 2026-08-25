package net.azisaba.simplequest.application.quest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.azisaba.simplequest.data.DailyQuestConfig
import net.azisaba.simplequest.database.DatabaseHelper
import net.azisaba.simplequest.domain.data.Icon
import net.azisaba.simplequest.domain.quest.model.QuestType
import net.azisaba.simplequest.registry.DomainQuestTypes
import java.util.UUID

class DailyQuestServiceTest :
    FunSpec({

        lateinit var h2: HikariDataSource
        lateinit var db: DatabaseHelper
        lateinit var service: DailyQuestService

        val questTypes =
            listOf(
                QuestType(
                    key = "lq:daily_kill",
                    title = "Daily Kill",
                    icon = Icon("STONE"),
                    category = "lq:daily",
                ),
                QuestType(
                    key = "lq:daily_gather",
                    title = "Daily Gather",
                    icon = Icon("STONE"),
                    category = "lq:daily",
                ),
                QuestType(
                    key = "lq:daily_craft",
                    title = "Daily Craft",
                    icon = Icon("STONE"),
                    category = "lq:daily",
                ),
                QuestType(
                    key = "lq:daily_explore",
                    title = "Daily Explore",
                    icon = Icon("STONE"),
                    category = "lq:daily",
                ),
                QuestType(
                    key = "lq:daily_boss",
                    title = "Daily Boss",
                    icon = Icon("STONE"),
                    category = "lq:daily",
                ),
                // Non-daily quest — should NOT be selected
                QuestType(
                    key = "lq:story_main",
                    title = "Main Story",
                    icon = Icon("STONE"),
                    category = "lq:story",
                ),
            )

        beforeSpec {
            val config =
                HikariConfig().apply {
                    jdbcUrl = "jdbc:h2:mem:daily_test_${UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MariaDB"
                    driverClassName = "org.h2.Driver"
                    username = "sa"
                    password = ""
                }
            h2 = HikariDataSource(config)
            db = DatabaseHelper(h2)
            db.execute(
                """
                CREATE TABLE IF NOT EXISTS daily_quest_assignments (
                    player_uuid BINARY(16) NOT NULL,
                    quest_key VARCHAR(255) NOT NULL,
                    assigned_date VARCHAR(10) NOT NULL,
                    PRIMARY KEY (player_uuid, quest_key, assigned_date)
                )
                """.trimIndent(),
            )

            DomainQuestTypes.clear()
            questTypes.forEach { DomainQuestTypes.register(it) }
        }

        afterSpec {
            DomainQuestTypes.clear()
            h2.close()
        }

        beforeEach {
            db.execute("DELETE FROM daily_quest_assignments")
        }

        context("getDailyQuests") {
            test("returns count quests from daily category only") {
                service = DailyQuestService(db, DailyQuestConfig(enabled = true, count = 3))

                val playerId = UUID.randomUUID()
                val quests = service.getDailyQuests(playerId)

                quests shouldHaveSize 3
                quests.forEach {
                    (it in listOf("lq:daily_kill", "lq:daily_gather", "lq:daily_craft", "lq:daily_explore", "lq:daily_boss")) shouldBe
                        true
                }
                // Non-daily quests must never be selected
                quests.none { it == "lq:story_main" } shouldBe true
            }

            test("returns consistent results for the same player on the same day") {
                service = DailyQuestService(db, DailyQuestConfig(enabled = true, count = 2))

                val playerId = UUID.randomUUID()
                val first = service.getDailyQuests(playerId)
                val second = service.getDailyQuests(playerId)

                first shouldBe second
            }

            test("returns empty when no daily quests exist") {
                DomainQuestTypes.clear()
                // Only register a non-daily quest
                DomainQuestTypes.register(
                    QuestType(
                        key = "lq:story_only",
                        title = "Story Only",
                        icon = Icon("STONE"),
                        category = "lq:story",
                    ),
                )

                service = DailyQuestService(db, DailyQuestConfig(enabled = true, count = 3))

                val quests = service.getDailyQuests(UUID.randomUUID())
                quests shouldHaveSize 0

                // Restore
                DomainQuestTypes.clear()
                questTypes.forEach { DomainQuestTypes.register(it) }
            }

            test("different players get (potentially) different assignments") {
                service = DailyQuestService(db, DailyQuestConfig(enabled = true, count = 3))

                val p1 = UUID.randomUUID()
                val p2 = UUID.randomUUID()
                service.getDailyQuests(p1)
                service.getDailyQuests(p2)

                // Both should have 3 quests each in DB
                val p1Count =
                    db.query(
                        "SELECT COUNT(*) as cnt FROM daily_quest_assignments WHERE player_uuid = ?",
                        uuidToBytes(p1),
                    ) { rs ->
                        rs.next()
                        rs.getInt("cnt")
                    }
                val p2Count =
                    db.query(
                        "SELECT COUNT(*) as cnt FROM daily_quest_assignments WHERE player_uuid = ?",
                        uuidToBytes(p2),
                    ) { rs ->
                        rs.next()
                        rs.getInt("cnt")
                    }

                p1Count shouldBe 3
                p2Count shouldBe 3
            }

            test("respects count=1 config") {
                service = DailyQuestService(db, DailyQuestConfig(enabled = true, count = 1))

                val quests = service.getDailyQuests(UUID.randomUUID())
                quests shouldHaveSize 1
                (quests[0] in listOf("lq:daily_kill", "lq:daily_gather", "lq:daily_craft", "lq:daily_explore", "lq:daily_boss")) shouldBe
                    true
            }
        }

        context("todayDate") {
            test("returns ISO-8601 date string") {
                service = DailyQuestService(db, DailyQuestConfig())
                val date = service.todayDate()
                // Should match YYYY-MM-DD
                date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) shouldBe true
            }
        }
    })

private fun uuidToBytes(uuid: UUID): ByteArray {
    val most = uuid.mostSignificantBits
    val least = uuid.leastSignificantBits
    return ByteArray(16).also {
        for (i in 0..7) it[i] = (most shr ((7 - i) * 8)).toByte()
        for (i in 0..7) it[i + 8] = (least shr ((7 - i) * 8)).toByte()
    }
}
