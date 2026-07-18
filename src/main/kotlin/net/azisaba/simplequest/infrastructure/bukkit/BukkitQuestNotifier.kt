package net.azisaba.simplequest.infrastructure.bukkit

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.domain.quest.port.QuestNotifier
import org.bukkit.Bukkit
import java.util.UUID

/**
 * Bukkit implementation of [QuestNotifier] using Paper API for chat messages.
 * GUI panel integration will be added after QuestPanelGui migration.
 */
@Singleton
class BukkitQuestNotifier
    @Inject
    constructor() : QuestNotifier {
        override fun showQuestPanel(
            playerId: String,
            questKey: String,
        ) {
            val player = Bukkit.getPlayer(UUID.fromString(playerId)) ?: return
            player.sendMessage("§aQuest started: §e$questKey")
        }

        override fun hideQuestPanel(playerId: String) {
            val player = Bukkit.getPlayer(UUID.fromString(playerId)) ?: return
            player.sendMessage("§cQuest ended.")
        }

        override fun sendMessage(
            playerId: String,
            message: String,
        ) {
            val player = Bukkit.getPlayer(UUID.fromString(playerId)) ?: return
            player.sendMessage(message)
        }
    }
