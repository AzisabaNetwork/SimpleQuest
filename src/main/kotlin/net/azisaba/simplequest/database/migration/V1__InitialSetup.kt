package net.azisaba.simplequest.database.migration

import net.azisaba.simplequest.database.table.PlayerQuestTypes
import net.azisaba.simplequest.database.table.QuestCompletions
import net.azisaba.simplequest.database.table.QuestDefinitions
import net.azisaba.simplequest.database.table.QuestProgress
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

/**
 * V1: Initial schema setup.
 * Generates DDL using Exposed's MigrationUtils within an Exposed transaction.
 */
class V1__InitialSetup : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val db = Database.connect(getNewConnection = { context.connection })
        transaction(db) {
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
}
