package net.azisaba.lifequest.database

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.File
import java.security.MessageDigest
import java.util.logging.Logger

/**
 * Synchronizes quest definitions between local YAML files and MySQL database.
 */
@Singleton
class SyncService
    @Inject
    constructor(
        private val databaseHelper: DatabaseHelper,
        private val logger: Logger,
        private val multiServerConfig: net.azisaba.lifequest.data.MultiServerConfig,
    ) {
        private val writeToMysql: Boolean = multiServerConfig.writeToMysql
        private val writeToYaml: Boolean = multiServerConfig.writeToYaml
        private val serverId: String = System.getProperty("lifequest.server-id", "local")
        private val conflictedKeys = mutableSetOf<String>()

        // These fields are set by the ServiceModule @Provides method
        // to maintain backward compatibility with existing callers.
        // Once full DI migration is complete, inject them via constructor.
        fun conflictedQuests(): Set<String> = conflictedKeys.toSet()

        val hasConflicts: Boolean get() = conflictedKeys.isNotEmpty()

        fun sync(dataFolder: File) {
            conflictedKeys.clear()
            val localFiles = scanYamlFiles(dataFolder)

            if (writeToMysql) pushLocalToMysql(localFiles)
            if (writeToYaml) pullMysqlToLocal(localFiles, dataFolder)

            databaseHelper.query("SELECT quest_key, conflict FROM quest_definitions") { rs ->
                while (rs.next()) {
                    if (rs.getBoolean("conflict")) conflictedKeys.add(rs.getString("quest_key"))
                }
            }

            logger.info("Sync completed. ${conflictedKeys.size} conflict(s) detected.")
        }

        fun resolveUseLocal() {
            databaseHelper.query("SELECT quest_key FROM quest_definitions WHERE conflict = TRUE") { rs ->
                while (rs.next()) {
                    val key = rs.getString("quest_key")
                    databaseHelper.update(
                        "UPDATE quest_definitions SET conflict = FALSE, updated_at = ?, updated_by = ? WHERE quest_key = ?",
                        java.time.Instant
                            .now()
                            .toString(),
                        serverId,
                        key,
                    )
                }
            }
            conflictedKeys.clear()
        }

        fun resolveUseMySql(dataFolder: File) {
            databaseHelper.query("SELECT quest_key, yaml_text FROM quest_definitions WHERE conflict = TRUE") { rs ->
                while (rs.next()) {
                    val key = rs.getString("quest_key")
                    val yamlText = rs.getString("yaml_text")
                    writeYamlFile(dataFolder, key, yamlText)
                    databaseHelper.update("UPDATE quest_definitions SET conflict = FALSE WHERE quest_key = ?", key)
                }
            }
            conflictedKeys.clear()
        }

        private fun scanYamlFiles(dataFolder: File): Map<String, File> {
            if (!dataFolder.exists()) return emptyMap()
            val result = mutableMapOf<String, File>()
            dataFolder
                .listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("@") }
                ?.forEach { nsDir ->
                    val namespace = nsDir.name.removePrefix("@")
                    val typesDir = File(nsDir, "types")
                    if (!typesDir.exists()) return@forEach
                    typesDir
                        .walkTopDown()
                        .filter { it.isFile && it.extension in setOf("yml", "yaml") }
                        .forEach { file ->
                            result["$namespace/${file.nameWithoutExtension}"] = file
                        }
                }
            return result
        }

        private fun pushLocalToMysql(localFiles: Map<String, File>) {
            localFiles.forEach { (key, file) ->
                val yamlText = file.readText()
                val checksum = sha256(yamlText)
                databaseHelper.update(
                    """INSERT INTO quest_definitions (quest_key, yaml_text, checksum, updated_at, updated_by, conflict)
                   VALUES (?, ?, ?, ?, ?, FALSE)
                   ON DUPLICATE KEY UPDATE
                       yaml_text = VALUES(yaml_text),
                       checksum = VALUES(checksum),
                       updated_at = VALUES(updated_at),
                       updated_by = VALUES(updated_by),
                       conflict = FALSE""",
                    key,
                    yamlText,
                    checksum,
                    java.time.Instant
                        .now()
                        .toString(),
                    serverId,
                )
            }
        }

        private fun pullMysqlToLocal(
            localFiles: Map<String, File>,
            dataFolder: File,
        ) {
            databaseHelper.query(
                "SELECT quest_key, yaml_text, checksum FROM quest_definitions WHERE conflict = FALSE",
            ) { rs ->
                while (rs.next()) {
                    val key = rs.getString("quest_key")
                    val mysqlText = rs.getString("yaml_text")
                    val mysqlChecksum = rs.getString("checksum")

                    val localFile = localFiles[key]
                    if (localFile == null) {
                        writeYamlFile(dataFolder, key, mysqlText)
                    } else {
                        val localChecksum = sha256(localFile.readText())
                        if (localChecksum != mysqlChecksum) {
                            conflictedKeys.add(key)
                            databaseHelper.update("UPDATE quest_definitions SET conflict = TRUE WHERE quest_key = ?", key)
                        }
                    }
                }
            }
        }

        private fun writeYamlFile(
            dataFolder: File,
            key: String,
            content: String,
        ) {
            val parts = key.split("/", limit = 2)
            if (parts.size < 2) return
            val nsDir = File(dataFolder, "@${parts[0]}")
            nsDir.mkdirs()
            val typesDir = File(nsDir, "types")
            typesDir.mkdirs()
            File(typesDir, "${parts[1]}.yml").writeText(content)
        }

        companion object {
            fun sha256(text: String): String {
                val digest = MessageDigest.getInstance("SHA-256")
                return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
            }
        }
    }
