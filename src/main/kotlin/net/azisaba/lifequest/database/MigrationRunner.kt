package net.azisaba.lifequest.database

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.lifequest.database.migration.V1__InitialSetup
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import java.util.logging.Level
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Runs Flyway migrations on startup.
 *
 * Table definitions live in Exposed table objects under [net.azisaba.lifequest.database.table].
 * V1 migration is a Java-based migration ([V1__InitialSetup]) that uses Exposed's
 * [org.jetbrains.exposed.v1.jdbc.MigrationUtils] to generate DDL from those definitions.
 */
@Singleton
class MigrationRunner
    @Inject
    constructor(
        private val dataSource: DataSource,
        private val logger: Logger,
    ) {
        fun run(): MigrateResult? =
            try {
                val flyway =
                    Flyway
                        .configure()
                        .dataSource(dataSource)
                        .locations("classpath:net/azisaba/lifequest/database/migration")
                        .load()
                val result = flyway.migrate()
                logger.info("Flyway migration completed: ${result.migrations.size} migration(s) applied.")
                result
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed to run database migrations", e)
                null
            }
    }
