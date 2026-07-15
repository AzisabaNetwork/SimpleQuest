package net.azisaba.lifequest.domain.quest.model

/**
 * Reason why a quest ended.
 */
enum class EndReason {
    COMPLETE,
    CANCEL,
    DEATH_LIMIT,
    PLUGIN,
    RELOAD,
    OTHER,
    ;

    /** Maps this reason to the resulting [QuestState]. */
    fun toQuestState(): QuestState =
        when (this) {
            COMPLETE -> QuestState.COMPLETED
            CANCEL, DEATH_LIMIT -> QuestState.CANCELLED
            PLUGIN, RELOAD, OTHER -> QuestState.FAILED
        }
}
