package net.azisaba.lifequest.domain.action

/**
 * A set of actions triggered by quest lifecycle events.
 */
data class ActionSet(
    val onFirstComplete: List<Action> = emptyList(),
    val onComplete: List<Action> = emptyList(),
)
