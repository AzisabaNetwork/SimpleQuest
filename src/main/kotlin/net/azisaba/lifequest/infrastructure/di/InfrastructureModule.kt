package net.azisaba.lifequest.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import jakarta.inject.Singleton
import net.azisaba.lifequest.data.DiscordConfig
import net.azisaba.lifequest.database.repository.PlayerQuestTypeRepository
import net.azisaba.lifequest.domain.action.port.ActionDispatcher
import net.azisaba.lifequest.domain.quest.port.QuestNotifier
import net.azisaba.lifequest.domain.quest.port.QuestRepository
import net.azisaba.lifequest.domain.script.port.ScriptRunner
import net.azisaba.lifequest.infrastructure.bukkit.BukkitActionDispatcher
import net.azisaba.lifequest.infrastructure.bukkit.BukkitQuestNotifier
import net.azisaba.lifequest.infrastructure.bukkit.BukkitScriptRunner
import net.azisaba.lifequest.infrastructure.persistence.QuestRepositoryImpl

/**
 * Dagger module that binds domain port interfaces to their infrastructure implementations.
 */
@Module(
    includes = [InfrastructureProvidersModule::class],
)
abstract class InfrastructureModule {
    @Binds
    @Singleton
    abstract fun bindQuestRepository(impl: QuestRepositoryImpl): QuestRepository

    @Binds
    @Singleton
    abstract fun bindActionDispatcher(impl: BukkitActionDispatcher): ActionDispatcher

    @Binds
    @Singleton
    abstract fun bindScriptRunner(impl: BukkitScriptRunner): ScriptRunner

    @Binds
    @Singleton
    abstract fun bindQuestNotifier(impl: BukkitQuestNotifier): QuestNotifier
}

/**
 * Provides concrete instances that cannot use @Inject constructors (e.g., Kotlin objects).
 */
@Module
object InfrastructureProvidersModule {
    @Provides
    @Singleton
    fun providePlayerQuestTypeRepository(): PlayerQuestTypeRepository = PlayerQuestTypeRepository

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(CIO)

    @Provides
    fun provideDiscordWebhookUrl(config: DiscordConfig): String = config.webhookUrl
}
