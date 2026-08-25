package net.azisaba.simplequest.domain.quest.port

import net.azisaba.simplequest.domain.quest.model.Quest

/**
 * Port for sending quest-related UI notifications to players.
 */
interface QuestNotifier {
    fun showQuestPanel(
        playerId: String,
        quest: Quest,
    )

    fun hideQuestPanel(playerId: String)

    fun sendMessage(
        playerId: String,
        message: String,
    )
}
