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
            connect()
        }

        fun connect(): Database {
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
            hikari = HikariDataSource(hikariConfig)
            // Pass ExposedConnectionImpl directly to bypass ServiceLoader.
            // ServiceLoader can fail in Paper PluginClassLoader when Exposed is relocated.
            exposedDb =
                Database.connect(
                    datasource = hikari!!,
                    connectionAutoRegistration = ExposedConnectionImpl(),
                )
            return exposedDb!!
        }

        fun disconnect() {
            hikari?.close()
            hikari = null
            exposedDb = null
        }

        val isConnected: Boolean get() = hikari?.isRunning == true
    }
