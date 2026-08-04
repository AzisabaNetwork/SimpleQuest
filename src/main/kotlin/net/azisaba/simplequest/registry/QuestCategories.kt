package net.azisaba.simplequest.registry

import net.azisaba.simplequest.quest.QuestCategory
import net.kyori.adventure.key.Key

object QuestCategories : Registry<QuestCategory>() {
    val GENERAL = registerBuiltIn("general", "General")
    val DAILY = registerBuiltIn("daily", "Daily")
    val STORY = registerBuiltIn("story", "Story")
    val EVENT = registerBuiltIn("event", "Event")

    /** Set of valid category key suffixes for validation. */
    private val validKeys = setOf("general", "daily", "story", "event")

    /**
     * Validates that a category key string is among the registered categories.
     * Input like "lq:general" or "general" are both accepted.
     */
    fun isValidCategory(category: String): Boolean {
        val suffix = category.substringAfter("lq:")
        return suffix in validKeys
    }

    /** Returns all valid category key suffixes. */
    fun validCategoryKeys(): Set<String> = validKeys

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
