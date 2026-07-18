package net.azisaba.simplequest.integration

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.sql.Connection
import java.sql.DriverManager

/**
 * DSL-style assertions for integration test verification.
 */
class ServerAssertions(
    private val servers: List<ServerProcess>,
) {
    /** Asserts that all servers contain a specific log message. */
    suspend fun assertAllLogsContain(pattern: String) {
        val regex = Regex(pattern)
        for (server in servers) {
            server.logContains(regex) shouldBe true
        }
    }

    /** Asserts that no server has ERROR or FATAL in its log. */
    suspend fun assertNoErrors() {
        val errorPattern = Regex("(?i)(ERROR|FATAL|Exception)")
        for (server in servers) {
            val errors =
                server.logLines().filter { errorPattern.containsMatchIn(it) }
            if (errors.isNotEmpty()) {
                throw AssertionError(
                    "${server.name} log contains errors:\n${errors.joinToString("\n")}",
                )
            }
        }
    }

    /** Asserts all servers have the plugin enabled. */
    suspend fun assertAllEnabled() {
        assertAllLogsContain("SimpleQuest enabled")
    }

    companion object {
        /**
         * Creates a JDBC connection to MariaDB from environment variables.
         */
        fun connectMariaDB(): Connection {
            val url =
                System.getenv("MARIADB_URL")
                    ?: "jdbc:mariadb://localhost:3306/simplequest"
            val user = System.getenv("MARIADB_USER") ?: "root"
            val password = System.getenv("MARIADB_PASSWORD") ?: "test"
            return DriverManager.getConnection(url, user, password)
        }

        /**
         * Verifies that the quest_definitions table exists in MariaDB.
         */
        fun assertQuestDefinitionTableExists() {
            connectMariaDB().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs =
                        stmt.executeQuery(
                            "SELECT COUNT(*) FROM quest_definitions",
                        )
                    rs.next()
                    // Just check the table exists (query succeeds)
                    rs.getInt(1) shouldBeGreaterThanOrEqual 0
                }
            }
        }

        /**
         * Verifies that quest definitions exist in MariaDB.
         */
        fun assertQuestDefinitionsExist() {
            connectMariaDB().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs =
                        stmt.executeQuery(
                            "SELECT COUNT(*) FROM quest_definitions",
                        )
                    rs.next()
                    rs.getInt(1) shouldBeGreaterThan 0
                }
            }
        }

        /**
         * Verifies that a specific quest key exists in MariaDB.
         */
        fun assertQuestDefinitionExists(key: String) {
            connectMariaDB().use { conn ->
                conn
                    .prepareStatement(
                        "SELECT quest_key FROM quest_definitions WHERE quest_key = ?",
                    ).use { ps ->
                        ps.setString(1, key)
                        val rs = ps.executeQuery()
                        rs.next() shouldBe true
                        rs.getString("quest_key") shouldContain key
                    }
            }
        }

        /**
         * Verifies there are no conflict flags in quest_definitions.
         */
        fun assertNoConflicts() {
            connectMariaDB().use { conn ->
                conn.createStatement().use { stmt ->
                    val rs =
                        stmt.executeQuery(
                            "SELECT COUNT(*) FROM quest_definitions WHERE conflict = TRUE",
                        )
                    rs.next()
                    rs.getInt(1) shouldBe 0
                }
            }
        }
    }
}
