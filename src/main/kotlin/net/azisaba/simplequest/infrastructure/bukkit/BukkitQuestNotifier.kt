package net.azisaba.simplequest.infrastructure.bukkit

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.domain.quest.model.Quest
import net.azisaba.simplequest.domain.quest.port.QuestNotifier
import net.azisaba.simplequest.gui.QuestDetailGui
import net.azisaba.simplequest.gui.QuestPanelGui
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Bukkit implementation of [QuestNotifier].
 *
 * - [showQuestPanel] displays the scoreboard quest panel
 * - [hideQuestPanel] closes it
 * - [sendMessage] sends a chat message
 */
@Singleton
class BukkitQuestNotifier
    @Inject
    constructor(
        private val questPanelGui: QuestPanelGui,
    ) : QuestNotifier {
        override fun showQuestPanel(
            playerId: String,
            quest: Quest,
        ) {
            val player = Bukkit.getPlayer(UUID.fromString(playerId)) ?: return
            questPanelGui.show(player, quest)
            QuestDetailGui.mount(player)
        }

        override fun hideQuestPanel(playerId: String) {
            val player = Bukkit.getPlayer(UUID.fromString(playerId)) ?: return
            questPanelGui.hide(player)
            QuestDetailGui.unmount(player)
        }

        override fun sendMessage(
            playerId: String,
            message: String,
        ) {
            val player = Bukkit.getPlayer(UUID.fromString(playerId)) ?: return
            player.sendMessage(message)
        }
    }
