package net.azisaba.simplequest.di

import com.charleskorn.kaml.Yaml
import dagger.Module
import dagger.Provides
import jakarta.inject.Singleton
import net.azisaba.simplequest.data.DatabaseConfig
import net.azisaba.simplequest.data.DiscordConfig
import net.azisaba.simplequest.data.MultiServerConfig
import net.azisaba.simplequest.data.PanelConfig
import net.azisaba.simplequest.data.SimpleQuestConfig
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
    fun provideSimpleQuestConfig(
        dataFolder: File,
        logger: Logger,
    ): SimpleQuestConfig {
        val configFile = File(dataFolder, "config.yml")

        // Generate default config if the file does not exist
        if (!configFile.exists()) {
            dataFolder.mkdirs()
            val defaultConfig = SimpleQuestConfig()
            try {
                val yamlText =
                    Yaml.default.encodeToString(
                        SimpleQuestConfig.serializer(),
                        defaultConfig,
                    )
                configFile.writeText(yamlText)
                logger.info("Generated default config.yml")
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Failed to generate default config.yml", e)
            }
        }

        return try {
            Yaml.default.decodeFromString(SimpleQuestConfig.serializer(), configFile.readText())
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to load config.yml, using defaults", e)
            SimpleQuestConfig()
        }
    }

    @Provides
    fun provideDatabaseConfig(config: SimpleQuestConfig): DatabaseConfig = config.database

    @Provides
    fun provideMultiServerConfig(config: SimpleQuestConfig): MultiServerConfig = config.multiServer

    @Provides
    fun provideDiscordConfig(config: SimpleQuestConfig): DiscordConfig = config.discord

    @Provides
    fun providePanelConfig(config: SimpleQuestConfig): PanelConfig = config.panel
}
