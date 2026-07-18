package net.azisaba.simplequest.listener

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.application.quest.QuestService
import net.azisaba.simplequest.domain.quest.model.EndReason
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.FurnaceExtractEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerShearEntityEvent
import java.util.Locale
import net.azisaba.simplequest.domain.quest.model.Quest as DomainQuest

/**
 * Bridges Bukkit events to quest objective progress.
 *
 * Listens for common player actions (block break/place, entity kill,
 * item pickup, crafting) and updates the player's active quest progress.
 *
 * ## Objective Key Convention
 *
 * Objective keys in YAML follow a `{ActionPrefix}{TargetName}` convention:
 *
 * | Prefix   | Event                | Example Key   | Meaning                  |
 * |----------|----------------------|---------------|--------------------------|
 * | `Break`   | BlockBreakEvent        | `BreakStone`    | Break STONE blocks        |
 * | `Place`   | BlockPlaceEvent        | `PlaceDirt`     | Place DIRT blocks         |
 * | `Kill`    | EntityDeathEvent       | `KillZombie`    | Kill ZOMBIE entities      |
 * | `Collect` | EntityPickupItemEvent  | `CollectDiamond`| Pick up DIAMOND items     |
 * | `Craft`   | CraftItemEvent         | `CraftStick`    | Craft STICK items         |
 * | `Consume` | PlayerItemConsumeEvent | `ConsumeApple`  | Eat/drink APPLE           |
 * | `Fish`    | PlayerFishEvent        | `FishCod`       | Catch COD (EntityType)    |
 * | `Enchant` | EnchantItemEvent       | `EnchantSword`  | Enchant IRON_SWORD        |
 * | `Smelt`   | FurnaceExtractEvent    | `SmeltIron`     | Smelt IRON_INGOT          |
 * | `Breed`   | EntityBreedEvent       | `BreedCow`      | Breed COW entities        |
 * | `Shear`   | PlayerShearEntityEvent | `ShearSheep`    | Shear SHEEP entities      |
 *
 * The TargetName must match a Bukkit [Material] or [EntityType] enum constant
 * (case-insensitive). Examples: `Stone`, `OAK_LOG`, `ZOMBIE`, `IRON_INGOT`.
 *
 * When all requirements are met, the quest is automatically completed.
 */
@Singleton
class QuestProgressListener
    @Inject
    constructor(
        private val questService: QuestService,
    ) : Listener {
        // ---- BlockBreakEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onBlockBreak(event: BlockBreakEvent) {
            updateProgressForPrefix(event.player, "Break") { key ->
                matchesMaterial(key.removePrefix("Break"), event.block.type)
            }
        }

        // ---- BlockPlaceEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onBlockPlace(event: BlockPlaceEvent) {
            updateProgressForPrefix(event.player, "Place") { key ->
                matchesMaterial(key.removePrefix("Place"), event.block.type)
            }
        }

        // ---- EntityDeathEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onEntityDeath(event: EntityDeathEvent) {
            val killer = event.entity.killer ?: return
            updateProgressForPrefix(killer, "Kill") { key ->
                matchesEntityType(key.removePrefix("Kill"), event.entityType)
            }
        }

        // ---- EntityPickupItemEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onEntityPickupItem(event: EntityPickupItemEvent) {
            val player = event.entity as? Player ?: return
            val itemStack = event.item.itemStack
            updateProgressForPrefix(player, "Collect") { key ->
                matchesMaterial(key.removePrefix("Collect"), itemStack.type)
            }
        }

        // ---- CraftItemEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onCraftItem(event: CraftItemEvent) {
            val player = event.whoClicked as? Player ?: return
            val result = event.recipe.result
            updateProgressForPrefix(player, "Craft") { key ->
                matchesMaterial(key.removePrefix("Craft"), result.type)
            }
        }

        // ---- PlayerItemConsumeEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onItemConsume(event: PlayerItemConsumeEvent) {
            updateProgressForPrefix(event.player, "Consume") { key ->
                matchesMaterial(key.removePrefix("Consume"), event.item.type)
            }
        }

        // ---- PlayerFishEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onPlayerFish(event: PlayerFishEvent) {
            // Only count successful catches (CAUGHT_FISH state)
            if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
            val caught = event.caught ?: return
            updateProgressForPrefix(event.player, "Fish") { key ->
                matchesEntityType(key.removePrefix("Fish"), caught.type)
            }
        }

        // ---- EnchantItemEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onEnchantItem(event: EnchantItemEvent) {
            updateProgressForPrefix(event.enchanter, "Enchant") { key ->
                matchesMaterial(key.removePrefix("Enchant"), event.item.type)
            }
        }

        // ---- FurnaceExtractEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onFurnaceExtract(event: FurnaceExtractEvent) {
            updateProgressForPrefix(event.player, "Smelt") { key ->
                matchesMaterial(key.removePrefix("Smelt"), event.itemType)
            }
        }

        // ---- EntityBreedEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onEntityBreed(event: EntityBreedEvent) {
            val player = event.breeder as? Player ?: return
            updateProgressForPrefix(player, "Breed") { key ->
                matchesEntityType(key.removePrefix("Breed"), event.entity.type)
            }
        }

        // ---- PlayerShearEntityEvent ----

        @EventHandler(ignoreCancelled = true)
        fun onPlayerShear(event: PlayerShearEntityEvent) {
            updateProgressForPrefix(event.player, "Shear") { key ->
                matchesEntityType(key.removePrefix("Shear"), event.entity.type)
            }
        }

        // ---- Core progress update logic ----

        /**
         * Finds the player's active quest, checks if any requirement key
         * matches the given [prefix] and [keyFilter], and increments progress by 1.
         *
         * If all requirements are met after the update, the quest is auto-completed.
         */
        private fun updateProgressForPrefix(
            player: Player,
            prefix: String,
            keyFilter: (String) -> Boolean,
        ) {
            val quest = questService.getQuestByPlayerId(player.uniqueId.toString()) ?: return
            val reqKey =
                quest.type.requirements.keys.firstOrNull { key ->
                    key.startsWith(prefix, ignoreCase = true) && keyFilter(key)
                } ?: return

            questService.updateProgress(quest, reqKey, 1)

            // Auto-complete if all requirements met
            if (quest.progresses.isComplete) {
                questService.endQuest(quest, EndReason.COMPLETE)
            }
        }

        // ---- Material/Entity matching utilities ----

        private fun matchesMaterial(
            targetName: String,
            material: Material,
        ): Boolean {
            val expected = targetName.uppercase(Locale.ROOT).replace(" ", "_")
            return material.name == expected
        }

        private fun matchesEntityType(
            targetName: String,
            entityType: EntityType,
        ): Boolean {
            val expected = targetName.uppercase(Locale.ROOT).replace(" ", "_")
            return entityType.name == expected
        }
    }
