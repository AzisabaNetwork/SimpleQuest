package net.azisaba.simplequest.database.migration

import net.azisaba.simplequest.database.table.PlayerQuestTypes
import net.azisaba.simplequest.database.table.QuestCompletions
import net.azisaba.simplequest.database.table.QuestDefinitions
import net.azisaba.simplequest.database.table.QuestProgress
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

class V1__InitialSetup : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val tables =
            arrayOf(
                QuestProgress,
                QuestCompletions,
                PlayerQuestTypes,
                QuestDefinitions,
            )
        val statements: List<String> =
            MigrationUtils.statementsRequiredForDatabaseMigration(
                *tables,
            )
        statements.forEach { sql: String ->
            context.connection.createStatement().use { it.execute(sql) }
        }
    }
}
