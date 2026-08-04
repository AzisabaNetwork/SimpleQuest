package net.azisaba.simplequest.listener

import net.azisaba.simplequest.SimpleQuest
import net.azisaba.simplequest.application.quest.QuestService
import net.azisaba.simplequest.database.SyncService
import net.azisaba.simplequest.domain.quest.model.EndReason
import net.azisaba.simplequest.party.PartyManager
import net.azisaba.simplequest.quest.QuestManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listener for player lifecycle events related to quests and parties.
 * Injectable via Dagger; also works without injection via [SimpleQuest.plugin] fallback.
 */
class PlayerListener(
    private val questManager: QuestManager,
    private val syncService: SyncService,
    private val questService: QuestService,
) : Listener {
    private val deathCounts = mutableMapOf<Pair<java.util.UUID, String>, Int>()

    // Backward-compat no-arg constructor for non-DI callers
    constructor() : this(
        SimpleQuest.plugin.questManager,
        SimpleQuest.plugin.syncService,
        SimpleQuest.plugin.questService,
    )

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!player.hasPermission("simplequest.reload")) return
        if (syncService.hasConflicts) {
            syncService.conflictedQuests().forEach { key ->
                player.sendMessage("§c[SimpleQuest] Quest conflict detected: §e$key")
            }
            player.sendMessage("§cUse §e/simplequest reload --use-local §cor §e--use-mysql §cto resolve")
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val quest = questManager.getQuestByPlayer(player)
        if (quest != null) {
            // Check if disconnect should fail the quest
            val domainQuest = questService.getQuestByPlayerId(player.uniqueId.toString())
            if (domainQuest != null && domainQuest.type.failConditions.onDisconnect) {
                questService.endQuest(domainQuest, EndReason.CANCEL)
                player.sendMessage("§cYour quest was cancelled because you disconnected.")
            } else {
                quest.removePlayer(player)
            }
        }
        val party = PartyManager.getParty(player)
        if (party is net.azisaba.simplequest.party.PartyImpl) {
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
