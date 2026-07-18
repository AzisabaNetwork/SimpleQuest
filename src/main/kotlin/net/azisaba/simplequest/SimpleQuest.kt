package net.azisaba.simplequest

import net.azisaba.simplequest.application.quest.QuestService
import net.azisaba.simplequest.data.SimpleQuestConfig
import net.azisaba.simplequest.database.BackupService
import net.azisaba.simplequest.database.DatabaseManager
import net.azisaba.simplequest.database.DiscordWebhook
import net.azisaba.simplequest.database.MigrationRunner
import net.azisaba.simplequest.database.SyncService
import net.azisaba.simplequest.di.BukkitModule
import net.azisaba.simplequest.di.DaggerSimpleQuestComponent
import net.azisaba.simplequest.di.SimpleQuestComponent
import net.azisaba.simplequest.listener.PlayerListener
import net.azisaba.simplequest.listener.QuestProgressListener
import net.azisaba.simplequest.quest.QuestManager
import net.azisaba.simplequest.registry.DomainQuestTypes
import net.azisaba.simplequest.registry.QuestCategories
import org.bukkit.plugin.java.JavaPlugin

/**
 * Main plugin entry point.
 * Initializes the Dagger DI component and delegates to injected services.
 */
class SimpleQuest : JavaPlugin() {
    lateinit var diComponent: SimpleQuestComponent
        private set
    lateinit var configData: SimpleQuestConfig
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var questManager: QuestManager
        private set
    lateinit var questService: QuestService
        private set
    lateinit var syncService: SyncService
        private set
    lateinit var migrationRunner: MigrationRunner
        private set
    lateinit var backupService: BackupService
        private set
    lateinit var discordWebhook: DiscordWebhook
        private set
    lateinit var simpleQuestLoader: SimpleQuestLoader
        private set
    lateinit var questProgressListener: QuestProgressListener
        private set

    override fun onEnable() {
        plugin = this
        saveDefaultConfig()

        // Initialize Dagger DI
        diComponent =
            DaggerSimpleQuestComponent
                .builder()
                .bukkitModule(BukkitModule(this))
                .build()

        configData = diComponent.configData()
        databaseManager = diComponent.databaseManager()
        questManager = diComponent.questManager()
        questService = diComponent.questService()
        syncService = diComponent.syncService()
        migrationRunner = diComponent.migrationRunner()
        backupService = diComponent.backupService()
        discordWebhook = diComponent.discordWebhook()
        simpleQuestLoader = diComponent.simpleQuestLoader()
        questProgressListener = diComponent.questProgressListener()

        runDatabaseDependentSetup()
        registerBuiltInCategories()
        loadQuestDefinitions()
        registerListeners()
        startBackupIfConnected()
        logger.info("SimpleQuest enabled.")
    }

    override fun onDisable() {
        if (::backupService.isInitialized) backupService.stop()
        if (::discordWebhook.isInitialized) discordWebhook.shutdown()
        if (::databaseManager.isInitialized) databaseManager.disconnect()
        logger.info("SimpleQuest disabled.")
    }

    private fun registerBuiltInCategories() {
        QuestCategories.entries.size // force init
    }

    fun reloadPlugin(
        useLocal: Boolean = false,
        useMySql: Boolean = false,
    ) {
        DomainQuestTypes.clear()
        if (useLocal) syncService.resolveUseLocal()
        if (useMySql) syncService.resolveUseMySql(dataFolder)
        loadQuestDefinitions()
        val conflicts = syncService.conflictedQuests()
        if (conflicts.isNotEmpty()) {
            logger.severe("Conflicts remain: $conflicts. Use --use-local or --use-mysql.")
        } else {
            logger.info("SimpleQuest reloaded.")
        }
    }

    private fun loadQuestDefinitions() {
        syncService.sync(dataFolder)
        if (syncService.hasConflicts) {
            val msg = "Quest conflicts detected: ${syncService.conflictedQuests()}"
            logger.warning(msg)
            discordWebhook.sendError("Quest Conflict", "```$msg```", 0xFFA500)
            return
        }
        simpleQuestLoader.loadAll(dataFolder)
    }

    /**
     * Runs DB-dependent setup (migrations).
     * If the database is unavailable, these steps are skipped and the plugin
     * continues with limited functionality (quests loaded from YAML only).
     */
    private fun runDatabaseDependentSetup() {
        try {
            migrationRunner.run()
        } catch (e: Exception) {
            logger.warning("Database migration skipped (DB unavailable): ${e.message}")
        }
    }

    private fun startBackupIfConnected() {
        if (databaseManager.isConnected) {
            backupService.start()
        } else {
            logger.info("Backup service skipped (DB unavailable).")
        }
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(
            PlayerListener(questManager, syncService),
            this,
        )
        server.pluginManager.registerEvents(
            questProgressListener,
            this,
        )
    }

    companion object {
        lateinit var plugin: SimpleQuest
            private set
    }
}
