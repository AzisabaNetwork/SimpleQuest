package net.azisaba.simplequest.infrastructure.bukkit

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.domain.quest.port.QuestNotifier
import net.azisaba.simplequest.gui.QuestDetailGui
import net.azisaba.simplequest.gui.QuestPanelGui
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
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
        private val plugin: Plugin,
        private val questPanelGui: QuestPanelGui,
    ) : QuestNotifier {
        override fun showQuestPanel(
            playerId: String,
            questKey: String,
        ) {
            val player = Bukkit.getPlayer(UUID.fromString(playerId)) ?: return
            val quest = SimpleQuest.plugin.questManager.getQuestByPlayer(player)
            if (quest != null) {
                questPanelGui.show(player, quest)
                QuestDetailGui.mount(player, quest)
            } else {
                player.sendMessage("§aQuest started: §e$questKey")
            }
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

/**
 * Bridge to access SimpleQuest.plugin from the notifier package.
 * TODO: replace with proper DI injection.
 */
private object SimpleQuest {
    val plugin: net.azisaba.simplequest.SimpleQuest
        get() = net.azisaba.simplequest.SimpleQuest.plugin
}
