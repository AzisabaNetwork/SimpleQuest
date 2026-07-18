package net.azisaba.simplequest.di

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dagger.Module
import dagger.Provides
import jakarta.inject.Singleton
import net.azisaba.simplequest.data.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.ExposedConnectionImpl
import javax.sql.DataSource

/**
 * Provides database connectivity: HikariCP connection pool, Exposed database, and migrations.
 * DatabaseManager, MigrationRunner are auto-wired via @Inject constructors.
 */
@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideHikariDataSource(config: DatabaseConfig): HikariDataSource {
        val hikariConfig =
            HikariConfig().apply {
                jdbcUrl = "jdbc:mariadb://${config.host}:${config.port}/${config.name}"
                username = config.user
                password = config.password
                maximumPoolSize = 10
                minimumIdle = 2
                idleTimeout = 30000
                maxLifetime = 600000
                connectionTimeout = 5000
                validate()
            }
        return HikariDataSource(hikariConfig)
    }

    @Provides
    @Singleton
    fun provideExposedDatabase(dataSource: HikariDataSource): Database =
        Database.connect(
            datasource = dataSource,
            connectionAutoRegistration = ExposedConnectionImpl(),
        )

    @Provides
    @Singleton
    fun provideDataSource(dataSource: HikariDataSource): DataSource = dataSource
}
