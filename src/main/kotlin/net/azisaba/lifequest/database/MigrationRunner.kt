package net.azisaba.lifequest.database

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.lifequest.database.table.PlayerQuestTypes
import net.azisaba.lifequest.database.table.QuestCompletions
import net.azisaba.lifequest.database.table.QuestDefinitions
import net.azisaba.lifequest.database.table.QuestProgress
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Creates/verifies database tables on startup.
 */
@Singleton
class MigrationRunner
    @Inject
    constructor(
        private val logger: Logger,
    ) {
        fun run() {
            try {
                transaction {
                    SchemaUtils.createMissingTablesAndColumns(
                        QuestProgress,
                        QuestCompletions,
                        PlayerQuestTypes,
                        QuestDefinitions,
                    )
                }
                logger.info("Database tables verified.")
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed to initialize database tables", e)
            }
        }
    }
