package net.azisaba.simplequest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.azisaba.simplequest.database.DatabaseHelper

class DatabaseHelperExtTest :
    FunSpec({

        lateinit var h2: HikariDataSource
        lateinit var db: DatabaseHelper

        beforeTest {
            val config =
                HikariConfig().apply {
                    jdbcUrl = "jdbc:h2:mem:db_ext_${java.util.UUID.randomUUID()};DB_CLOSE_DELAY=-1;MODE=MariaDB"
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

        test("execute SQL with no return") {
            db.execute("CREATE TABLE sample (id INT PRIMARY KEY)")
            db.execute("INSERT INTO sample VALUES (1)")
            val count =
                db.query("SELECT COUNT(*) FROM sample") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            count shouldBe 1
        }

        test("update executes without error") {
            db.execute("CREATE TABLE retval (id INT PRIMARY KEY, name VARCHAR(50))")
            db.update("INSERT INTO retval VALUES (1, 'test')")
            val result =
                db.query("SELECT COUNT(*) FROM retval") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            result shouldBe 1
        }

        test("update multiple rows") {
            db.execute("CREATE TABLE multi (id INT PRIMARY KEY)")
            db.update("INSERT INTO multi VALUES (1)")
            db.update("INSERT INTO multi VALUES (2)")
            db.update("INSERT INTO multi VALUES (3)")
            val count =
                db.query("SELECT COUNT(*) FROM multi") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            count shouldBe 3
        }

        test("query with null params works") {
            db.execute("CREATE TABLE nullable (id INT, name VARCHAR(50))")
            db.execute("INSERT INTO nullable VALUES (1, 'hello')")

            val result =
                db.query("SELECT id, name FROM nullable WHERE id = ?", 1) { rs ->
                    rs.next()
                    rs.getInt(1) to rs.getString(2)
                }
            result shouldBe (1 to "hello")
        }

        test("query returns null when no rows match") {
            db.execute("CREATE TABLE empty_table (id INT)")
            db.execute("INSERT INTO empty_table VALUES (1)")

            val result =
                db.query("SELECT id FROM empty_table WHERE id = ?", 999) { rs ->
                    if (rs.next()) rs.getInt(1) else null
                }
            result shouldBe null
        }

        test("execute supports DDL statements") {
            db.execute("CREATE TABLE ddl (col1 INT, col2 VARCHAR(100))")
            db.execute("ALTER TABLE ddl ADD COLUMN col3 DOUBLE")
            db.execute("INSERT INTO ddl VALUES (1, 'a', 1.5)")

            val result =
                db.query("SELECT col3 FROM ddl WHERE col1 = 1") { rs ->
                    rs.next()
                    rs.getDouble(1)
                }
            result shouldBe 1.5
        }

        test("big number of params") {
            db.execute("CREATE TABLE params (a INT, b INT, c INT)")
            db.update("INSERT INTO params VALUES (?, ?, ?)", 1, 2, 3)
            val result =
                db.query("SELECT a, b, c FROM params WHERE a = ? AND b = ?", 1, 2) { rs ->
                    rs.next()
                    Triple(rs.getInt(1), rs.getInt(2), rs.getInt(3))
                }
            result shouldBe Triple(1, 2, 3)
        }

        test("VARCHAR length test") {
            db.execute("CREATE TABLE varchars (data VARCHAR(255))")
            val longStr = "A".repeat(255)
            db.update("INSERT INTO varchars VALUES (?)", longStr)
            val result =
                db.query("SELECT data FROM varchars") { rs ->
                    rs.next()
                    rs.getString(1)
                }
            result?.length shouldBe 255
        }

        test("INTEGER negative values") {
            db.execute("CREATE TABLE neg (val INT)")
            db.update("INSERT INTO neg VALUES (?)", -42)
            val result =
                db.query("SELECT val FROM neg") { rs ->
                    rs.next()
                    rs.getInt(1)
                }
            result shouldBe -42
        }

        test("BOOLEAN column") {
            db.execute("CREATE TABLE bools (flag BOOLEAN)")
            db.update("INSERT INTO bools VALUES (?)", true)
            db.update("INSERT INTO bools VALUES (?)", false)
            val results =
                db.query("SELECT flag FROM bools ORDER BY flag") { rs ->
                    val list = mutableListOf<Boolean>()
                    while (rs.next()) list.add(rs.getBoolean(1))
                    list
                }
            results shouldBe listOf(false, true)
        }
    })
