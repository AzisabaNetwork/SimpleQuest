package net.azisaba.simplequest.domain.quest.model

/**
 * Defines a single quest objective requirement: what to track and how many are needed.
 */
data class QuestRequirement(
    val key: String,
    val amount: Int,
)
