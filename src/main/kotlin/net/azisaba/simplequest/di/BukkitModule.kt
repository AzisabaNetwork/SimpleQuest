package net.azisaba.simplequest.di

import dagger.Module
import dagger.Provides
import jakarta.inject.Singleton
import net.azisaba.simplequest.SimpleQuest
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Logger

/**
 * Dagger module that provides Bukkit/Paper environment dependencies.
 * These are available at plugin enable time and must be set before component creation.
 */
@Module
class BukkitModule(
    private val plugin: SimpleQuest,
) {
    @Provides
    @Singleton
    fun providePlugin(): SimpleQuest = plugin

    @Provides
    @Singleton
    fun provideJavaPlugin(): JavaPlugin = plugin

    @Provides
    @Singleton
    fun providePluginInterface(): Plugin = plugin

    @Provides
    @Singleton
    fun provideServer(): Server = plugin.server

    @Provides
    @Singleton
    fun provideLogger(): Logger = plugin.logger

    @Provides
    @Singleton
    fun provideDataFolder(): File = plugin.dataFolder
}
