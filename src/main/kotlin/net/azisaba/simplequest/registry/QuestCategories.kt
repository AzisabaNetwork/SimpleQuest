package net.azisaba.simplequest.registry

import net.azisaba.simplequest.quest.QuestCategory
import net.kyori.adventure.key.Key

object QuestCategories : Registry<QuestCategory>() {
    val GENERAL = registerBuiltIn("general", "General")
    val DAILY = registerBuiltIn("daily", "Daily")
    val STORY = registerBuiltIn("story", "Story")
    val EVENT = registerBuiltIn("event", "Event")

    private fun registerBuiltIn(
        keySuffix: String,
        title: String,
    ): QuestCategory {
        val key = Key.key("lq", keySuffix)
        val category = QuestCategory(key, title)
        register(category)
        return category
    }

    fun isBuiltIn(category: QuestCategory): Boolean = category.key in setOf(GENERAL.key, DAILY.key, STORY.key, EVENT.key)
}
