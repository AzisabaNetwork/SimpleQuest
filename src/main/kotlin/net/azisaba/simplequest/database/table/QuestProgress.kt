package net.azisaba.simplequest.database.table

import org.jetbrains.exposed.v1.core.*

object QuestProgress : Table("quest_progress") {
    val playerUuid = binary("player_uuid", 16)
    val questKey = varchar("quest_key", 255)
    val reqKey = varchar("req_key", 255)
    val progress = integer("progress").default(0)

    override val primaryKey = PrimaryKey(playerUuid, questKey, reqKey)
}
