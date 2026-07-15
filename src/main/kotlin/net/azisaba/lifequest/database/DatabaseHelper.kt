package net.azisaba.lifequest.database

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

/**
 * Helper for executing raw SQL queries.
 * Now injectable via DataSource instead of relying on [LifeQuest.plugin].
 */
@Singleton
class DatabaseHelper
    @Inject
    constructor(
        private val dataSource: DataSource,
    ) {
        fun <T> query(
            sql: String,
            vararg params: Any?,
            mapper: (ResultSet) -> T,
        ): T =
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    params.forEachIndexed { i, param -> ps.setObject(i + 1, param) }
                    ps.executeQuery().use { rs -> mapper(rs) }
                }
            }

        fun update(
            sql: String,
            vararg params: Any?,
        ) {
            dataSource.connection.use { conn ->
                conn.autoCommit = true
                conn.prepareStatement(sql).use { ps ->
                    params.forEachIndexed { i, param -> ps.setObject(i + 1, param) }
                    ps.executeUpdate()
                }
            }
        }

        fun execute(sql: String) {
            dataSource.connection.use { conn ->
                conn.createStatement().use { stmt -> stmt.execute(sql) }
            }
        }
    }
