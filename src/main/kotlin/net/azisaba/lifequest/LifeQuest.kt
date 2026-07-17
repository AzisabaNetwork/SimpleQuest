package net.azisaba.lifequest

import net.azisaba.lifequest.application.quest.QuestService
import net.azisaba.lifequest.data.LifeQuestConfig
import net.azisaba.lifequest.database.BackupService
import net.azisaba.lifequest.database.DatabaseManager
import net.azisaba.lifequest.database.DiscordWebhook
import net.azisaba.lifequest.database.MigrationRunner
import net.azisaba.lifequest.database.SyncService
import net.azisaba.lifequest.di.BukkitModule
import net.azisaba.lifequest.di.DaggerLifeQuestComponent
import net.azisaba.lifequest.di.LifeQuestComponent
import net.azisaba.lifequest.listener.PlayerListener
import net.azisaba.lifequest.quest.QuestManager
import net.azisaba.lifequest.registry.DomainQuestTypes
import net.azisaba.lifequest.registry.QuestCategories
import org.bukkit.plugin.java.JavaPlugin

/**
 * Main plugin entry point.
 * Initializes the Dagger DI component and delegates to injected services.
 */
class LifeQuest : JavaPlugin() {
    lateinit var diComponent: LifeQuestComponent
        private set
    lateinit var configData: LifeQuestConfig
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
    lateinit var lifeQuestLoader: LifeQuestLoader
        private set

    override fun onEnable() {
        plugin = this
        saveDefaultConfig()

        // Initialize Dagger DI
        diComponent =
            DaggerLifeQuestComponent
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
        lifeQuestLoader = diComponent.lifeQuestLoader()

        migrationRunner.run()
        registerBuiltInCategories()
        loadQuestDefinitions()
        registerListeners()
        backupService.start()
        logger.info("LifeQuest enabled.")
    }

    override fun onDisable() {
        if (::backupService.isInitialized) backupService.stop()
        if (::discordWebhook.isInitialized) discordWebhook.shutdown()
        if (::databaseManager.isInitialized) databaseManager.disconnect()
        logger.info("LifeQuest disabled.")
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
            logger.info("LifeQuest reloaded.")
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
        lifeQuestLoader.loadAll(dataFolder)
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(
            PlayerListener(questManager, syncService),
            this,
        )
    }

    companion object {
        lateinit var plugin: LifeQuest
            private set
    }
}
