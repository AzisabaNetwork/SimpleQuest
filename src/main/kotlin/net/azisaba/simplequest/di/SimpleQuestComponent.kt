package net.azisaba.simplequest.di

import dagger.Component
import jakarta.inject.Singleton
import net.azisaba.simplequest.SimpleQuestLoader
import net.azisaba.simplequest.application.quest.QuestService
import net.azisaba.simplequest.data.SimpleQuestConfig
import net.azisaba.simplequest.database.BackupService
import net.azisaba.simplequest.database.DatabaseManager
import net.azisaba.simplequest.database.DiscordWebhook
import net.azisaba.simplequest.database.MigrationRunner
import net.azisaba.simplequest.database.SyncService
import net.azisaba.simplequest.infrastructure.di.InfrastructureModule
import net.azisaba.simplequest.quest.QuestManager

/**
 * Root Dagger component for SimpleQuest.
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
interface SimpleQuestComponent {
    fun configData(): SimpleQuestConfig

    fun databaseManager(): DatabaseManager

    fun questManager(): QuestManager

    fun questService(): QuestService

    fun syncService(): SyncService

    fun migrationRunner(): MigrationRunner

    fun backupService(): BackupService

    fun discordWebhook(): DiscordWebhook

    fun simpleQuestLoader(): SimpleQuestLoader
}
