package net.azisaba.lifequest.di

import com.charleskorn.kaml.Yaml
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
 * Generates default config.yml if it does not exist.
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

        // Generate default config if the file does not exist
        if (!configFile.exists()) {
            dataFolder.mkdirs()
            val defaultConfig = LifeQuestConfig()
            try {
                val yamlText =
                    Yaml.default.encodeToString(
                        LifeQuestConfig.serializer(),
                        defaultConfig,
                    )
                configFile.writeText(yamlText)
                logger.info("Generated default config.yml")
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed to generate default config.yml", e)
            }
        }

        return try {
            Yaml.default.decodeFromString(LifeQuestConfig.serializer(), configFile.readText())
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to load config.yml, using defaults", e)
            LifeQuestConfig()
        }
    }

    @Provides
    fun provideDatabaseConfig(config: LifeQuestConfig): DatabaseConfig = config.database

    @Provides
    fun provideMultiServerConfig(config: LifeQuestConfig): MultiServerConfig = config.multiServer

    @Provides
    fun provideDiscordConfig(config: LifeQuestConfig): DiscordConfig = config.discord
}
