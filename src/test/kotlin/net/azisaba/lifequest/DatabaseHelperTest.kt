package net.azisaba.lifequest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.azisaba.lifequest.database.DatabaseHelper

class DatabaseHelperTest :
    FunSpec({

        lateinit var h2: HikariDataSource
        lateinit var db: DatabaseHelper

        beforeTest {
            val config =
                HikariConfig().apply {
                    jdbcUrl = "jdbc:h2:mem:dbhelper_${java.util.UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MariaDB"
                    driverClassName = "org.h2.Driver"
                    username = "sa"
                    password = ""
                }
            h2 = HikariDataSource(config)
            db = DatabaseHelper(h2)
        }

        afterTest {
            h2.close()
        }

        test("execute creates table") {
            db.execute("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(255))")
            db.execute("INSERT INTO test_table VALUES (1, 'hello')")

            val result =
                db.query("SELECT name FROM test_table WHERE id = ?", 1) { rs ->
                    rs.next()
                    rs.getString(1)
                }
            result shouldBe "hello"
        }

        test("query with multiple params") {
            db.execute("CREATE TABLE test_multi (a INT, b VARCHAR(10), c DOUBLE)")
            db.update("INSERT INTO test_multi VALUES (?, ?, ?)", 42, "test", 3.14)

            val result =
                db.query("SELECT a, b, c FROM test_multi WHERE a = ?", 42) { rs ->
                    rs.next()
                    Triple(rs.getInt(1), rs.getString(2), rs.getDouble(3))
                }
            result shouldBe Triple(42, "test", 3.14)
        }

        test("update with no params") {
            db.execute("CREATE TABLE test_empty (id INT PRIMARY KEY)")
            db.update("INSERT INTO test_empty VALUES (1)")
            db.update("INSERT INTO test_empty VALUES (2)")

            val count =
                db.query("SELECT COUNT(*) FROM test_empty") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            count shouldBe 2
        }
    })
