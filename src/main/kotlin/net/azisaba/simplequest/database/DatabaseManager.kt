package net.azisaba.simplequest.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.data.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.ExposedConnectionImpl

@Singleton
class DatabaseManager
    @Inject
    constructor(
        private val config: DatabaseConfig,
    ) {
        val hikariDataSource: HikariDataSource?
            get() = hikari
        private var hikari: HikariDataSource? = null
        private var exposedDb: Database? = null

        init {
            try {
                connect()
            } catch (e: Exception) {
                // Plugin can still load without a database connection.
                // DB-dependent features (sync, migrations, progress persistence) will be skipped.
                System.err.println("[SimpleQuest] Failed to connect to database: ${e.message}")
                System.err.println("[SimpleQuest] DB-dependent features are disabled.")
            }
        }

        fun connect(): Database? =
            try {
                val hikariConfig =
                    HikariConfig().apply {
                        jdbcUrl = "jdbc:mariadb://${config.host}:${config.port}/${config.name}"
                        driverClassName = "org.mariadb.jdbc.Driver"
                        username = config.user
                        password = config.password
                        maximumPoolSize = 10
                        minimumIdle = 2
                        idleTimeout = 30000
                        maxLifetime = 600000
                        connectionTimeout = 5000
                        initializationFailTimeout = -1
                        validate()
                    }
                hikari = HikariDataSource(hikariConfig)
                // Pass ExposedConnectionImpl directly to bypass ServiceLoader.
                // ServiceLoader can fail in Paper PluginClassLoader when Exposed is relocated.
                exposedDb =
                    Database.connect(
                        datasource = hikari!!,
                        connectionAutoRegistration = ExposedConnectionImpl(),
                    )
                exposedDb
            } catch (e: Exception) {
                hikari?.close()
                hikari = null
                exposedDb = null
                throw e
            }

        fun disconnect() {
            hikari?.close()
            hikari = null
            exposedDb = null
        }

        val isConnected: Boolean get() = hikari?.isRunning == true
    }
