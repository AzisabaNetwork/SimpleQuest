package net.azisaba.simplequest.quest

enum class QuestState {
    ACTIVE,
    COMPLETED,
    CANCELLED,
    FAILED,
}

enum class EndReason {
    COMPLETE,
    CANCEL,
    DEATH_LIMIT,
    PLUGIN,
    RELOAD,
    OTHER,
}
