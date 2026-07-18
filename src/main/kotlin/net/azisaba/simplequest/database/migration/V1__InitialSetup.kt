package net.azisaba.simplequest.database.migration

import net.azisaba.simplequest.database.table.PlayerQuestTypes
import net.azisaba.simplequest.database.table.QuestCompletions
import net.azisaba.simplequest.database.table.QuestDefinitions
import net.azisaba.simplequest.database.table.QuestProgress
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

/**
 * V1: Initial schema setup.
 * Uses Exposed's [MigrationUtils] to generate DDL from table definitions,
 * then executes them via Flyway's JDBC connection.
 */
class V1__InitialSetup : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val statements: List<String> =
            MigrationUtils.statementsRequiredForDatabaseMigration(
                QuestProgress,
                QuestCompletions,
                PlayerQuestTypes,
                QuestDefinitions,
            )
        context.connection.createStatement().use { stmt ->
            statements.forEach { sql: String -> stmt.execute(sql) }
        }
    }
}
