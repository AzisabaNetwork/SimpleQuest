package net.azisaba.lifequest.domain.action

/**
 * A single action/reward to be executed when a quest completes.
 */
data class Action(
    val type: ActionType,
    val material: String? = null,
    val amount: Int? = null,
    val item: String? = null,
    val command: String? = null,
)
