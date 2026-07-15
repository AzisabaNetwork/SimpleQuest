package net.azisaba.lifequest.di

import dagger.Component
import jakarta.inject.Singleton
import net.azisaba.lifequest.LifeQuestLoader
import net.azisaba.lifequest.application.quest.QuestService
import net.azisaba.lifequest.data.LifeQuestConfig
import net.azisaba.lifequest.database.BackupService
import net.azisaba.lifequest.database.DatabaseManager
import net.azisaba.lifequest.database.DiscordWebhook
import net.azisaba.lifequest.database.MigrationRunner
import net.azisaba.lifequest.database.SyncService
import net.azisaba.lifequest.infrastructure.di.InfrastructureModule
import net.azisaba.lifequest.quest.QuestManager

/**
 * Root Dagger component for LifeQuest.
 *
 * The plugin obtains dependencies by building this component
 * and calling the accessor methods.
 */
@Singleton
@Component(
    modules = [
        BukkitModule::class,
        ConfigModule::class,
        DatabaseModule::class,
        RegistryModule::class,
        InfrastructureModule::class,
    ],
)
interface LifeQuestComponent {
    fun configData(): LifeQuestConfig

    fun databaseManager(): DatabaseManager

    fun questManager(): QuestManager

    fun questService(): QuestService

    fun syncService(): SyncService

    fun migrationRunner(): MigrationRunner

    fun backupService(): BackupService

    fun discordWebhook(): DiscordWebhook

    fun lifeQuestLoader(): LifeQuestLoader
}
