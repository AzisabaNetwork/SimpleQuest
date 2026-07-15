package net.azisaba.lifequest.di

import dagger.Module
import dagger.Provides
import jakarta.inject.Singleton
import net.azisaba.lifequest.registry.QuestCategories
import net.azisaba.lifequest.registry.QuestTypes

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
