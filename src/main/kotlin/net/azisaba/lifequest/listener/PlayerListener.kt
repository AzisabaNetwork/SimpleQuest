package net.azisaba.lifequest.listener

import net.azisaba.lifequest.LifeQuest
import net.azisaba.lifequest.database.SyncService
import net.azisaba.lifequest.party.PartyManager
import net.azisaba.lifequest.quest.QuestManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener for player lifecycle events related to quests and parties.
 * Injectable via Dagger; also works without injection via [LifeQuest.plugin] fallback.
 */
class PlayerListener(
    private val questManager: QuestManager,
    private val syncService: SyncService,
) : Listener {
    private val deathCounts = mutableMapOf<Pair<java.util.UUID, String>, Int>()

    // Backward-compat no-arg constructor for non-DI callers
    constructor() : this(
        LifeQuest.plugin.questManager,
        LifeQuest.plugin.syncService,
    )

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!player.hasPermission("lifequest.reload")) return
        if (syncService.hasConflicts) {
            syncService.conflictedQuests().forEach { key ->
                player.sendMessage("§c[LifeQuest] Quest conflict detected: §e$key")
            }
            player.sendMessage("§cUse §e/lifequest reload --use-local §cor §e--use-mysql §cto resolve")
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val quest = questManager.getQuestByPlayer(player)
        if (quest != null) {
            quest.removePlayer(player)
        }
        val party = PartyManager.getParty(player)
        if (party is net.azisaba.lifequest.party.PartyImpl) {
            party.removeMember(player)
        } else {
            PartyManager.removePlayer(player)
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        val quest = questManager.getQuestByPlayer(player) ?: return
        val type = quest.type
        val deathLimit = type.deathLimit ?: return

        val key = player.uniqueId to type.key.asString()
        val count = (deathCounts[key] ?: 0) + 1
        deathCounts[key] = count

        if (count >= deathLimit) {
            quest.removePlayer(player)
            deathCounts.remove(key)
            player.sendMessage("§cYou have died too many times and left the quest.")
        }
    }
}
