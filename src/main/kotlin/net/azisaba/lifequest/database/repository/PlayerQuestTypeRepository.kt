package net.azisaba.lifequest.database.repository

import com.zaxxer.hikari.HikariDataSource
import net.azisaba.lifequest.LifeQuest
import java.nio.ByteBuffer
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * Repository for player quest type grant/revoke and play limit queries.
 * Uses HikariCP directly since Exposed v1 API differs significantly from standard Exposed.
 */
object PlayerQuestTypeRepository {
    private val dataSource: HikariDataSource?
        get() = LifeQuest.plugin.databaseManager.hikariDataSource

    fun isGranted(
        player: UUID,
        questKey: String,
    ): Boolean =
        query { stmt ->
            stmt.prepareStatement("SELECT 1 FROM player_quest_types WHERE player_uuid = ? AND quest_key = ?").use { ps ->
                ps.setBytes(1, uuidToBytes(player))
                ps.setString(2, questKey)
                ps.executeQuery().use { rs -> rs.next() }
            }
        }

    fun grant(
        player: UUID,
        questKey: String,
    ) = update { stmt ->
        stmt.prepareStatement("INSERT IGNORE INTO player_quest_types (player_uuid, quest_key, plays) VALUES (?, ?, 0)").use { ps ->
            ps.setBytes(1, uuidToBytes(player))
            ps.setString(2, questKey)
            ps.executeUpdate()
        }
    }

    fun revoke(
        player: UUID,
        questKey: String,
    ) = update { stmt ->
        stmt.prepareStatement("DELETE FROM player_quest_types WHERE player_uuid = ? AND quest_key = ?").use { ps ->
            ps.setBytes(1, uuidToBytes(player))
            ps.setString(2, questKey)
            ps.executeUpdate()
        }
    }

    fun getPlays(
        player: UUID,
        questKey: String,
    ): Int =
        query { stmt ->
            stmt.prepareStatement("SELECT plays FROM player_quest_types WHERE player_uuid = ? AND quest_key = ?").use { ps ->
                ps.setBytes(1, uuidToBytes(player))
                ps.setString(2, questKey)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("plays") else 0 }
            }
        }

    fun getCompletionsSince(
        player: UUID,
        questKey: String,
        since: Instant,
    ): Int =
        query { stmt ->
            stmt
                .prepareStatement(
                    "SELECT COUNT(*) FROM quest_completions WHERE player_uuid = ? AND quest_key = ? AND completed_at >= ?",
                ).use { ps ->
                    ps.setBytes(1, uuidToBytes(player))
                    ps.setString(2, questKey)
                    ps.setString(3, since.toString())
                    ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 }
                }
        }

    fun isFirstCompletion(
        player: UUID,
        questKey: String,
    ): Boolean = getCompletionsSince(player, questKey, Instant.EPOCH) == 0

    fun getWeeklyCompletions(
        player: UUID,
        questKey: String,
    ): Int {
        val start =
            LocalDate
                .now(ZoneId.systemDefault())
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        return getCompletionsSince(player, questKey, start)
    }

    fun getMonthlyCompletions(
        player: UUID,
        questKey: String,
    ): Int {
        val start =
            LocalDate
                .now(ZoneId.systemDefault())
                .withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        return getCompletionsSince(player, questKey, start)
    }

    fun getYearlyCompletions(
        player: UUID,
        questKey: String,
    ): Int {
        val start =
            LocalDate
                .now(ZoneId.systemDefault())
                .withDayOfYear(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        return getCompletionsSince(player, questKey, start)
    }

    private fun <T> query(block: (Connection) -> T): T {
        val ds = dataSource ?: error("DataSource not initialized")
        return ds.connection.use(block)
    }

    private fun update(block: (Connection) -> Int) {
        val ds = dataSource ?: error("DataSource not initialized")
        ds.connection.use { block(it) }
    }

    private fun uuidToBytes(uuid: UUID): ByteArray {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }
}
