package net.azisaba.simplequest.action

data class Action(
    val type: ActionType,
    val material: String? = null,
    val amount: Int? = null,
    val item: String? = null,
    val command: String? = null,
)

data class ActionSet(
    val onFirstComplete: List<Action> = emptyList(),
    val onComplete: List<Action> = emptyList(),
)
