package net.azisaba.simplequest.domain.quest.model

/**
 * Lifecycle state of a quest instance.
 */
enum class QuestState {
    ACTIVE,
    COMPLETED,
    CANCELLED,
    FAILED,
    ;

    /** Returns true if this state is a terminal (non-active) state. */
    val isTerminal: Boolean
        get() = this != ACTIVE
}
