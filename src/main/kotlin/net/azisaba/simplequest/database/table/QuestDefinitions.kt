package net.azisaba.simplequest.database.table

import org.jetbrains.exposed.v1.core.*

object QuestDefinitions : Table("quest_definitions") {
    val questKey = varchar("quest_key", 255)
    val yamlText = text("yaml_text")
    val checksum = varchar("checksum", 64)
    val updatedAt = varchar("updated_at", 32)
    val updatedBy = varchar("updated_by", 36)
    val conflict = bool("conflict").default(false)
    override val primaryKey = PrimaryKey(questKey)
}
