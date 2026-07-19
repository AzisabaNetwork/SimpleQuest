package net.azisaba.simplequest.gui

import net.azisaba.simplequest.SimpleQuest
import net.azisaba.simplequest.quest.Quest
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.UUID

/**
 * Hijacks the player's 2x2 crafting grid for quest interaction.
 *
 * When a player has an active quest and opens their inventory,
 * the 2x2 crafting grid (slots 1-4) is populated with quest items:
 *
 *   ┌──────────┬──────────┐
 *   │ Slot 1   │ Slot 2   │  ← Current progress / Requirements
 *   │ Progress │ Reqs     │
 *   ├──────────┼──────────┤
 *   │ Slot 3   │ Slot 4   │  ← Party info / Cancel quest
 *   │ Party    │ Cancel   │
 *   └──────────┴──────────┘
 *
 * Clicking Cancel (slot 4) opens a confirmation GUI.
 * The items are cleared when the inventory is closed.
 * Progress is updated every 5 ticks while the inventory is open.
 */
object QuestDetailGui {
    // player UUID -> update task ID
    private val updateTasks = mutableMapOf<UUID, Int>()

    private val listener = CraftingGridListener()

    private var registered = false

    /** Starts hijacking the crafting grid for [player]'s active [quest]. */
    fun mount(
        player: Player,
        quest: Quest,
    ) {
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(listener, SimpleQuest.plugin)
            registered = true
        }
        // Items are set on InventoryOpenEvent
    }

    /** Stops hijacking for [player]. Clears crafting slots. */
    fun unmount(player: Player) {
        updateTasks.remove(player.uniqueId)?.let { Bukkit.getScheduler().cancelTask(it) }
        clearCraftingSlots(player)
        if (updateTasks.isEmpty() && registered) {
            HandlerList.unregisterAll(listener)
            registered = false
        }
    }

    /** Updates crafting slots with current quest state. */
    private fun updateSlots(
        player: Player,
        quest: Quest,
    ) {
        val inv = player.inventory

        // Slot 1: Progress
        inv.setItem(1, progressItem(quest))

        // Slot 2: Requirements
        inv.setItem(2, requirementsItem(quest))

        // Slot 3: Party info
        inv.setItem(3, partyItem(quest))

        // Slot 4: Cancel
        inv.setItem(4, cancelItem(quest))
    }

    private fun scheduleUpdates(
        player: Player,
        quest: Quest,
    ) {
        updateTasks[player.uniqueId]?.let { Bukkit.getScheduler().cancelTask(it) }
        val taskId =
            Bukkit
                .getScheduler()
                .runTaskTimer(
                    SimpleQuest.plugin,
                    Runnable {
                        if (!player.isOnline || player.openInventory.type != InventoryType.CRAFTING) {
                            updateTasks.remove(player.uniqueId)?.let { Bukkit.getScheduler().cancelTask(it) }
                            return@Runnable
                        }
                        val currentQuest = SimpleQuest.plugin.questManager.getQuestByPlayer(player)
                        if (currentQuest != null) {
                            updateSlots(player, currentQuest)
                        }
                    },
                    5L,
                    5L,
                ).taskId
        updateTasks[player.uniqueId] = taskId
    }

    internal fun handleCraftingClick(
        player: Player,
        slot: Int,
    ) {
        val quest = SimpleQuest.plugin.questManager.getQuestByPlayer(player) ?: return
        when (slot) {
            4 -> {
                // Cancel quest
                player.closeInventory()
                ConfirmGui.open(
                    player,
                    "Cancel '${quest.type.title}'?",
                    onAccept = {
                        SimpleQuest.plugin.questManager.endQuest(quest, net.azisaba.simplequest.quest.EndReason.CANCEL)
                        clearCraftingSlots(player)
                    },
                )
            }
        }
    }

    internal fun onInventoryOpen(player: Player) {
        val quest = SimpleQuest.plugin.questManager.getQuestByPlayer(player) ?: return
        if (player.openInventory.type == InventoryType.CRAFTING) {
            updateSlots(player, quest)
            scheduleUpdates(player, quest)
        }
    }

    internal fun onInventoryClose(player: Player) {
        updateTasks.remove(player.uniqueId)?.let { Bukkit.getScheduler().cancelTask(it) }
        clearCraftingSlots(player)
    }

    private fun clearCraftingSlots(player: Player) {
        for (i in 1..4) {
            player.inventory.setItem(i, null)
        }
    }

    // ---- Item factories ----

    private fun progressItem(quest: Quest): ItemStack {
        val item = ItemStack(Material.KNOWLEDGE_BOOK)
        val meta: ItemMeta = item.itemMeta
        meta.displayName(Component.text("§eQuest: §f${quest.type.title}"))
        val lore = mutableListOf<Component>()
        lore.add(Component.text(""))
        lore.add(Component.text("§7Progress: §f${quest.progresses.totalProgress} §7/ §f${quest.progresses.totalRequired}"))
        lore.add(Component.text(""))
        quest.type.requirements.forEach { (key, req) ->
            val current = quest.progresses[key] ?: 0
            val bar = progressBar(current, req.amount, 20)
            lore.add(Component.text("§7$key: §f$bar §7($current/${req.amount})"))
        }
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun requirementsItem(quest: Quest): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta: ItemMeta = item.itemMeta
        meta.displayName(Component.text("§6Requirements"))
        val lore = mutableListOf<Component>()
        quest.type.requirements.forEach { (key, req) ->
            val done = (quest.progresses[key] ?: 0) >= req.amount
            val icon = if (done) "§a✔" else "§7☐"
            lore.add(Component.text("$icon §f${req.amount}x §7$key"))
        }
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun partyItem(quest: Quest): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val meta: ItemMeta = item.itemMeta
        meta.displayName(Component.text("§dParty §7(${quest.players.size})"))
        val lore =
            quest.players.map { p ->
                Component.text("§7  ${p.name} §e♥${p.health.toInt()}")
            }
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun cancelItem(quest: Quest): ItemStack {
        val item = ItemStack(Material.BARRIER)
        val meta: ItemMeta = item.itemMeta
        meta.displayName(Component.text("§c§lCancel Quest"))
        meta.lore(
            listOf(
                Component.text("§7Click to cancel"),
                Component.text("§7'${quest.type.title}'"),
            ),
        )
        item.itemMeta = meta
        return item
    }

    private fun progressBar(
        current: Int,
        max: Int,
        length: Int,
    ): String {
        val filled = (current.toDouble() / max * length).toInt().coerceIn(0, length)
        return "§a${"█".repeat(filled)}§7${"░".repeat(length - filled)}"
    }

    // ---- Listener ----

    private class CraftingGridListener : Listener {
        @EventHandler
        fun onOpen(event: InventoryOpenEvent) {
            val player = event.player as? Player ?: return
            if (event.inventory.type == InventoryType.CRAFTING) {
                onInventoryOpen(player)
            }
        }

        @EventHandler
        fun onClose(event: InventoryCloseEvent) {
            val player = event.player as? Player ?: return
            if (event.inventory.type == InventoryType.CRAFTING) {
                onInventoryClose(player)
            }
        }

        @EventHandler
        fun onClick(event: InventoryClickEvent) {
            val player = event.whoClicked as? Player ?: return
            if (event.inventory.type != InventoryType.CRAFTING) return
            // Only handle clicks in the crafting grid (slots 1-4)
            val rawSlot = event.rawSlot
            if (rawSlot in 1..4) {
                event.isCancelled = true // Prevent taking items
                handleCraftingClick(player, rawSlot)
            }
        }

        @EventHandler
        fun onQuit(event: PlayerQuitEvent) {
            clearCraftingSlots(event.player)
            updateTasks.remove(event.player.uniqueId)?.let { Bukkit.getScheduler().cancelTask(it) }
        }
    }
}
