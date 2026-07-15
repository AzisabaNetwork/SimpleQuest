package net.azisaba.lifequest.database.table

import org.jetbrains.exposed.v1.core.*

object PlayerQuestTypes : Table("player_quest_types") {
    val playerUuid = binary("player_uuid", 16)
    val questKey = varchar("quest_key", 255)
    val plays = integer("plays").default(0)

    override val primaryKey = PrimaryKey(playerUuid, questKey)
}
