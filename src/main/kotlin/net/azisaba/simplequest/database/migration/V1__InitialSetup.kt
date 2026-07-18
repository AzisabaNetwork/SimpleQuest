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
import javax.sql.DataSource

class V1__InitialSetup(
    private val dataSource: DataSource,
) : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val db = Database.connect(dataSource)
        transaction(db) {
            val statements =
                MigrationUtils.statementsRequiredForDatabaseMigration(
                    QuestProgress,
                    QuestCompletions,
                    PlayerQuestTypes,
                    QuestDefinitions,
                )
            statements.forEach { sql: String -> exec(sql) }
        }
    }
}
