package net.azisaba.simplequest.di

import dagger.Module
import dagger.Provides
import jakarta.inject.Singleton
import net.azisaba.simplequest.registry.QuestCategories
import net.azisaba.simplequest.registry.QuestTypes

/**
 * Provides singleton registries for quest definitions and categories.
 */
@Module
object RegistryModule {
    @Provides
    @Singleton
    fun provideQuestTypesRegistry() = QuestTypes

    @Provides
    @Singleton
    fun provideQuestCategoriesRegistry() = QuestCategories
}
