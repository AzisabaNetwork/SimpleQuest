package net.azisaba.lifequest.domain.quest.model

/**
 * Conditions that a player must satisfy to accept/start a quest.
 */
data class AcceptConditions(
    val pveLevel: Int? = null,
    val money: Double? = null,
    val requiredQuests: List<String>? = null,
    val permissions: List<String>? = null,
    val partyMode: Boolean = false,
)
