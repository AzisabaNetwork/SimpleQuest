package net.azisaba.lifequest.domain.quest.model

/**
 * Aggregate root representing an active quest instance.
 */
interface Quest {
    val type: QuestType
    val state: QuestState
    val progresses: Progresses

    fun start()

    fun end(reason: EndReason)

    fun updateProgress(
        reqKey: String,
        delta: Int,
    )
}
