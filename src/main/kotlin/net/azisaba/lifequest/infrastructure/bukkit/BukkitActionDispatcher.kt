package net.azisaba.lifequest.infrastructure.bukkit

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.lifequest.domain.action.Action
import net.azisaba.lifequest.domain.action.ActionType
import net.azisaba.lifequest.domain.action.port.ActionDispatcher
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.logging.Logger

/**
 * Bukkit implementation of [ActionDispatcher].
 * Executes quest reward actions using Paper API.
 */
@Singleton
class BukkitActionDispatcher
    @Inject
    constructor(
        private val logger: Logger,
    ) : ActionDispatcher {
        override fun dispatch(
            action: Action,
            playerId: String,
        ) {
            val player = Bukkit.getPlayer(java.util.UUID.fromString(playerId)) ?: return
            when (action.type) {
                ActionType.COMMAND -> dispatchCommand(action, player)
                ActionType.ITEM_GIVE -> giveItem(action, player)
                ActionType.MYTHIC_ITEM_GIVE -> logger.warning("MythicItemGive not implemented: ${action.item}")
                ActionType.PVELEVEL_EXP -> logger.warning("PvELevelExp not implemented: ${action.amount}")
            }
        }

        override fun dispatchAll(
            actions: List<Action>,
            playerIds: List<String>,
        ) {
            playerIds.forEach { playerId ->
                actions.forEach { action -> dispatch(action, playerId) }
            }
        }

        private fun dispatchCommand(
            action: Action,
            player: Player,
        ) {
            val cmd = action.command?.replace("%player%", player.name) ?: return
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
        }

        private fun giveItem(
            action: Action,
            player: Player,
        ) {
            val material = Material.matchMaterial(action.material ?: return) ?: return
            val item = ItemStack(material, (action.amount ?: 1).coerceAtLeast(1))
            player.inventory.addItem(item).values.forEach { leftover ->
                player.world.dropItem(player.location, leftover)
            }
        }
    }
