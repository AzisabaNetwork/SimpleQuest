package net.azisaba.simplequest.database

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.data.MultiServerConfig
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Logger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Periodic quest data backup.
 * Injectable via Dagger; backward-compat [start]/[stop] helpers provided.
 */
@Singleton
class BackupService
    @Inject
    constructor(
        private val server: Server,
        private val plugin: Plugin,
        private val logger: Logger,
        private val dataFolder: File,
        private val multiServerConfig: MultiServerConfig,
        private val discordWebhook: DiscordWebhook,
    ) {
        private var taskId: Int? = null

        fun start() {
            val cfg = multiServerConfig.backup
            if (!cfg.enabled || !multiServerConfig.writeToMysql) return

            val interval = cfg.intervalHours.toLong() * 72000L

            taskId =
                server.scheduler
                    .runTaskTimerAsynchronously(
                        plugin,
                        Runnable { runBackup() },
                        interval,
                        interval,
                    ).taskId

            logger.info("Backup scheduled every ${cfg.intervalHours}h")
        }

        fun stop() {
            taskId?.let { server.scheduler.cancelTask(it) }
            taskId = null
        }

        private fun runBackup() {
            try {
                val cfg = multiServerConfig.backup
                val backupDir = File(cfg.directory)
                backupDir.mkdirs()

                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"))
                val backupFile = File(backupDir, "$timestamp.zip")

                ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                    dataFolder
                        .listFiles()
                        ?.filter { it.isDirectory && it.name.startsWith("@") }
                        ?.forEach { nsDir ->
                            nsDir
                                .walkTopDown()
                                .filter { it.isFile && it.extension in setOf("yml", "yaml") }
                                .forEach { file ->
                                    val entryName = nsDir.toURI().relativize(file.toURI()).path
                                    zos.putNextEntry(ZipEntry(entryName))
                                    file.inputStream().use { it.copyTo(zos) }
                                    zos.closeEntry()
                                }
                        }

                    val configFile = File(dataFolder, "config.yml")
                    if (configFile.exists()) {
                        zos.putNextEntry(ZipEntry("config.yml"))
                        configFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                logger.info("Backup created: ${backupFile.name}")
                cleanupOldBackups(backupDir, cfg.retentionDays)
            } catch (e: Exception) {
                logger.warning("Backup failed: ${e.message}")
                discordWebhook.sendError("Backup Failed", "```${e.message}```", 0xFFA500)
            }
        }

        private fun cleanupOldBackups(
            backupDir: File,
            retentionDays: Int,
        ) {
            val cutoff = System.currentTimeMillis() - retentionDays * 86400000L
            backupDir
                .listFiles()
                ?.filter { it.name.endsWith(".zip") }
                ?.filter { it.lastModified() < cutoff }
                ?.forEach { file ->
                    file.delete()
                    logger.info("Deleted old backup: ${file.name}")
                }
        }

        companion object {
            private var fallback: BackupService? = null

            fun start() {
                // Backward compat — replaced by DI-managed instance
            }

            fun stop() {
                fallback?.stop()
                fallback = null
            }
        }
    }
