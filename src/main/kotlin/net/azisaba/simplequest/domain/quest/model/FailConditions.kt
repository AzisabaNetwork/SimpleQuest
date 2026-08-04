package net.azisaba.simplequest.domain.quest.model

/**
 * Configurable fail conditions for a quest type.
 * When any of these conditions are met, the quest is failed.
 */
data class FailConditions(
    /** Fail the quest when a quest member disconnects from the server. */
    val onDisconnect: Boolean = false,
)
