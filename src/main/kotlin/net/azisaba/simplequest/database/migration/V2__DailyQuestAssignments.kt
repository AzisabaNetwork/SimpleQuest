package net.azisaba.simplequest.database.migration

import net.azisaba.simplequest.database.table.DailyQuestAssignments
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import javax.sql.DataSource

class V2__DailyQuestAssignments(
    private val dataSource: DataSource,
) : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val db = Database.connect(dataSource)
        transaction(db) {
            val statements =
                MigrationUtils.statementsRequiredForDatabaseMigration(
                    DailyQuestAssignments,
                )
            statements.forEach { sql: String -> exec(sql) }
        }
    }
}
