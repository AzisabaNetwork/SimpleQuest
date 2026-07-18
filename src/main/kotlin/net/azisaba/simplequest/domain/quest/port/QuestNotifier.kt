package net.azisaba.simplequest.domain.quest.port

/**
 * Port for sending quest-related UI notifications to players.
 */
interface QuestNotifier {
    fun showQuestPanel(
        playerId: String,
        questKey: String,
    )

    fun hideQuestPanel(playerId: String)

    fun sendMessage(
        playerId: String,
        message: String,
    )
}
