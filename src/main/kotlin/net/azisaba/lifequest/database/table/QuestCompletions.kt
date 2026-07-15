package net.azisaba.lifequest.database.table

import org.jetbrains.exposed.v1.core.*

object QuestCompletions : Table("quest_completions") {
    val id = long("id").autoIncrement()
    val playerUuid = binary("player_uuid", 16)
    val questKey = varchar("quest_key", 255)
    val completedAt = varchar("completed_at", 32)

    override val primaryKey = PrimaryKey(id)
}
