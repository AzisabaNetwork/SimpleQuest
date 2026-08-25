package net.azisaba.simplequest.database.table

import org.jetbrains.exposed.v1.core.*

/**
 * Stores per-player daily quest assignments.
 * Each player gets up to [maxDailyQuests] random daily quests per day.
 * Refreshed when a new day starts.
 */
object DailyQuestAssignments : Table("daily_quest_assignments") {
    val playerUuid = binary("player_uuid", 16)
    val questKey = varchar("quest_key", 255)
    val assignedDate = varchar("assigned_date", 10) // "YYYY-MM-DD"

    override val primaryKey = PrimaryKey(playerUuid, questKey, assignedDate)
}
