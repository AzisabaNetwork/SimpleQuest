package net.azisaba.simplequest.listener

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent
import net.azisaba.simplequest.application.quest.QuestService
import net.azisaba.simplequest.domain.quest.model.EndReason
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

/**
 * Listens for MythicMobs death events and updates quest progress.
 *
 * Objective key convention:
 *   `KillMM<mmid>` where `<mmid>` is the MythicMob internal name.
 *   Example: `KillMM_SkeletonKing` matches a MythicMob with id `SkeletonKing`.
 *
 * Multiple mobs can be listed as separate objectives:
 *   `KillMM_SkeletonKing`, `KillMM_ZombieLord`, etc.
 *
 * Progress increment per kill is 1 (same as standard Kill objectives).
 * For weighted progress, use the [QuestRequirement.amount] field.
 */
class MythicMobsListener(
    private val questService: QuestService,
    private val plugin: Plugin,
) : Listener {
    private var registered = false

    fun register() {
        if (registered) return
        plugin.server.pluginManager.registerEvents(this, plugin)
        registered = true
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMythicMobDeath(event: MythicMobDeathEvent) {
        val killer = event.killer as? Player ?: return

        val quest = questService.getQuestByPlayerId(killer.uniqueId.toString()) ?: return
        val mobType = event.mobType

        // Try both naming conventions: KillMM_<id> and KillMM<id>
        val reqKey =
            quest.type.requirements.keys.firstOrNull { key ->
                val keyUpper = key.uppercase()
                keyUpper == "KILLMM_${mobType.internalName.uppercase()}" ||
                    keyUpper == "KILLMM${mobType.internalName.uppercase().replace("_", "")}"
            } ?: return

        questService.updateProgress(quest, reqKey, 1)

        if (quest.progresses.isComplete) {
            questService.endQuest(quest, EndReason.COMPLETE)
        }
    }
}
