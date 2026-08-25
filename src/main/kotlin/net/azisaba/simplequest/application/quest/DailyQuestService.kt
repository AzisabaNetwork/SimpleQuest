package net.azisaba.simplequest.application.quest

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.data.DailyQuestConfig
import net.azisaba.simplequest.database.DatabaseHelper
import net.azisaba.simplequest.registry.DomainQuestTypes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Manages daily quest assignments per player.
 *
 * Each player gets [config.count] random quests from category "lq:daily" on each new day.
 * Assignments are stored in `daily_quest_assignments` and shared across all servers.
 */
@Singleton
class DailyQuestService
    @Inject
    constructor(
        private val db: DatabaseHelper,
        private val config: DailyQuestConfig,
    ) {
        private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        /**
         * Returns the daily quest keys assigned to [playerId] for today.
         * If assignments don't exist yet, generates and persists them.
         */
        fun getDailyQuests(playerId: UUID): List<String> {
            val today = LocalDate.now().format(dateFormatter)

            val existing = loadAssignments(playerId, today)
            if (existing.isNotEmpty()) return existing.sorted()

            return generateAndSave(playerId, today).sorted()
        }

        /**
         * Gets today's date string (for use as seed/timing reference).
         */
        fun todayDate(): String = LocalDate.now().format(dateFormatter)

        // ---- private ----

        private fun loadAssignments(
            playerId: UUID,
            date: String,
        ): List<String> {
            val result = mutableListOf<String>()
            db.query(
                "SELECT quest_key FROM daily_quest_assignments WHERE player_uuid = ? AND assigned_date = ?",
                uuidToBytes(playerId),
                date,
            ) { rs ->
                while (rs.next()) {
                    result.add(rs.getString("quest_key"))
                }
            }
            return result
        }

        private fun generateAndSave(
            playerId: UUID,
            date: String,
        ): List<String> {
            val dailyQuestCandidates =
                DomainQuestTypes.entries
                    .filter { it.category == "lq:daily" }
                    .toList()

            if (dailyQuestCandidates.isEmpty()) return emptyList()

            val selected =
                dailyQuestCandidates
                    .shuffled()
                    .take(config.count)
                    .map { it.key }

            if (selected.isEmpty()) return emptyList()

            val playerBytes = uuidToBytes(playerId)
            for (key in selected) {
                db.update(
                    "INSERT INTO daily_quest_assignments (player_uuid, quest_key, assigned_date) VALUES (?, ?, ?)",
                    playerBytes,
                    key,
                    date,
                )
            }

            return selected
        }

        private fun uuidToBytes(uuid: UUID): ByteArray {
            val most = uuid.mostSignificantBits
            val least = uuid.leastSignificantBits
            return ByteArray(16).also {
                for (i in 0..7) it[i] = (most shr ((7 - i) * 8)).toByte()
                for (i in 0..7) it[i + 8] = (least shr ((7 - i) * 8)).toByte()
            }
        }
    }
