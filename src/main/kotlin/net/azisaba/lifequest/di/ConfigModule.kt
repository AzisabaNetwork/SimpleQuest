package net.azisaba.lifequest.di

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlException
import dagger.Module
import dagger.Provides
import jakarta.inject.Singleton
import net.azisaba.lifequest.data.DatabaseConfig
import net.azisaba.lifequest.data.DiscordConfig
import net.azisaba.lifequest.data.LifeQuestConfig
import net.azisaba.lifequest.data.MultiServerConfig
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Provides configuration objects for the plugin.
 */
@Module
object ConfigModule {
    @Provides
    @Singleton
    fun provideLifeQuestConfig(
        dataFolder: File,
        logger: Logger,
    ): LifeQuestConfig {
        val configFile = File(dataFolder, "config.yml")
        return try {
            Yaml.default.decodeFromString(LifeQuestConfig.serializer(), configFile.readText())
        } catch (e: YamlException) {
            logger.log(Level.SEVERE, "Failed to load config.yml", e)
            LifeQuestConfig() // fallback
        }
    }

    @Provides
    fun provideDatabaseConfig(config: LifeQuestConfig): DatabaseConfig = config.database

    @Provides
    fun provideMultiServerConfig(config: LifeQuestConfig): MultiServerConfig = config.multiServer

    @Provides
    fun provideDiscordConfig(config: LifeQuestConfig): DiscordConfig = config.discord
}
