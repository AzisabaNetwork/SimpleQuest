package net.azisaba.simplequest.database.migration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.azisaba.simplequest.database.table.PlayerQuestTypes
import net.azisaba.simplequest.database.table.QuestCompletions
import net.azisaba.simplequest.database.table.QuestDefinitions
import net.azisaba.simplequest.database.table.QuestProgress
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

class V1__InitialSetup : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val meta = context.connection.metaData
        val ds =
            HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = meta.url
                    username = meta.userName
                    driverClassName = "org.mariadb.jdbc.Driver"
                    maximumPoolSize = 1
                },
            )
        try {
            val db = Database.connect(ds)
            transaction(db) {
                val statements =
                    MigrationUtils.statementsRequiredForDatabaseMigration(
                        QuestProgress,
                        QuestCompletions,
                        PlayerQuestTypes,
                        QuestDefinitions,
                    )
                statements.forEach { sql: String ->
                    exec(sql)
                }
            }
        } finally {
            ds.close()
        }
    }
}
