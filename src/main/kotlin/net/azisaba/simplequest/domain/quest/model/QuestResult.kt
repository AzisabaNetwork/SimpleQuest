package net.azisaba.simplequest.domain.quest.model

/**
 * Result of attempting to start a quest.
 */
sealed class QuestResult {
    data class Success(
        val quest: Quest,
    ) : QuestResult()

    data class Failure(
        val reason: String,
    ) : QuestResult()
}
