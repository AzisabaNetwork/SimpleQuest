// Original: https://github.com/AzisabaNetwork/quem by tksimeji
// Adapted for LifeQuest

package net.azisaba.lifequest.gui

import com.tksimeji.kunectron.ChestGui
import com.tksimeji.kunectron.Kunectron
import com.tksimeji.kunectron.element.Element
import com.tksimeji.kunectron.element.ItemElement
import com.tksimeji.kunectron.event.ChestGuiEvents
import com.tksimeji.kunectron.event.GuiHandler
import com.tksimeji.kunectron.hooks.ChestGuiHooks
import net.azisaba.lifequest.LifeQuest
import net.azisaba.lifequest.domain.quest.model.QuestCategory
import net.azisaba.lifequest.domain.quest.model.QuestType
import net.azisaba.lifequest.party.Party
import net.azisaba.lifequest.party.PartyManager
import net.azisaba.lifequest.quest.QuestResult
import net.azisaba.lifequest.registry.DomainQuestTypes
import net.azisaba.lifequest.registry.QuestCategories
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.net.URI
import kotlin.math.max
import kotlin.math.min

@ChestGui
class QuestGui(
    @ChestGui.Player private val player: Player,
    private val page: Int = 0,
    private val category: QuestCategory? = null,
    private val query: String = "",
) : ChestGuiHooks,
    SearchableGui {
    private val currentChunk = CHUNKED_CATEGORIES.indexOfFirst { it.contains(category) }

    private val currentCategories =
        CHUNKED_CATEGORIES.firstOrNull { it.contains(category) }
            ?: throw IllegalStateException("No categories found")

    private val questTypes =
        DomainQuestTypes.entries
            .filter {
                (category == null || it.category == category.key) &&
                    it.title.contains(query, ignoreCase = true)
            }.toList()

    @ChestGui.Title
    private val title = Component.text("Quests")

    @ChestGui.Element(index = [9])
    private val previousTab =
        Element
            .playerHead(
                URI
                    .create("http://textures.minecraft.net/texture/76ebaa41d1d405eb6b60845bb9ac724af70e85eac8a96a5544b9e23ad6c96c62")
                    .toURL(),
            ).title(Component.text("Previous").color(NamedTextColor.GREEN))
            .handler(
                ItemElement.Handler1 {
                    if (currentChunk <= 0) return@Handler1
                    val previousChunk = CHUNKED_CATEGORIES[currentChunk - 1]
                    Kunectron.create(QuestGui(player, category = previousChunk[currentChunk - 1]))
                },
            )

    @ChestGui.Element(index = [17])
    private val nextTab =
        Element
            .playerHead(
                URI
                    .create("http://textures.minecraft.net/texture/8399e5da82ef7765fd5e472f3147ed118d981887730ea7bb80d7a1bed98d5ba")
                    .toURL(),
            ).title(Component.text("Next").color(NamedTextColor.GREEN))
            .handler(
                ItemElement.Handler1 {
                    if (currentChunk >= CHUNKED_CATEGORIES.size - 1) return@Handler1
                    val nextChunk = CHUNKED_CATEGORIES[currentChunk + 1]
                    Kunectron.create(QuestGui(player, category = nextChunk.first()))
                },
            )

    @ChestGui.Element(index = [45])
    private val previous =
        Element
            .item(Material.ARROW)
            .title(Component.text("Previous").color(NamedTextColor.GREEN))
            .handler(
                ItemElement.Handler1 {
                    Kunectron.create(QuestGui(player, max(page - 1, 0), category, query))
                },
            )

    @ChestGui.Element(index = [49])
    private val close =
        Element
            .item(Material.BARRIER)
            .title(Component.text("Close").color(NamedTextColor.RED))
            .handler(ItemElement.Handler1(this::useClose))

    @ChestGui.Element(index = [50])
    private val search =
        Element
            .item(Material.COMPASS)
            .title(Component.text("Search").color(NamedTextColor.GREEN))
            .handler(
                ItemElement.Handler1 {
                    Kunectron.create(SearchGui(player, this))
                },
            )

    @ChestGui.Element(index = [53])
    private val next =
        Element
            .item(Material.ARROW)
            .title(Component.text("Next").color(NamedTextColor.GREEN))
            .handler(
                ItemElement.Handler1 {
                    Kunectron.create(
                        QuestGui(player, min(page + 1, questTypes.size / QUEST_TYPE_INDEXES.size), category, query),
                    )
                },
            )

    override fun search(query: String) {
        Kunectron.create(QuestGui(player, query = query))
    }

    @GuiHandler
    private fun onInit(event: ChestGuiEvents.InitEvent) {
        for ((index, cat) in currentCategories.withIndex()) {
            val categoryIndex = CATEGORY_INDEXES[index]
            if (cat != null) {
                useElement(
                    categoryIndex,
                    Element
                        .item(
                            Material.matchMaterial(
                                cat.key
                                    .split(":")
                                    .last()
                                    .uppercase(),
                            ) ?: Material.CHEST,
                        ).handler(
                            ItemElement.Handler1 {
                                Kunectron.create(QuestGui(player, category = cat))
                            },
                        ),
                )
            } else {
                useElement(
                    categoryIndex,
                    Element
                        .item(Material.NETHER_STAR)
                        .title(Component.text("All"))
                        .handler(
                            ItemElement.Handler1 {
                                Kunectron.create(QuestGui(player, category = null))
                            },
                        ),
                )
            }
            useElement(
                categoryIndex + 9,
                Element.item(
                    if (cat == this.category) {
                        Material.GREEN_STAINED_GLASS_PANE
                    } else {
                        Material.GRAY_STAINED_GLASS_PANE
                    },
                ),
            )
        }

        for (questTypeIndex in QUEST_TYPE_INDEXES) {
            useElement(questTypeIndex, Element.item(Material.GRAY_DYE))
        }

        for (
        (index, questType) in questTypes
            .subList(
                page * QUEST_TYPE_INDEXES.size,
                min((page + 1) * QUEST_TYPE_INDEXES.size, questTypes.size),
            ).withIndex()
        ) {
            val questTypeIndex = QUEST_TYPE_INDEXES[index]
            useElement(
                questTypeIndex,
                Element
                    .item(createQuestItem(questType))
                    .handler(
                        ItemElement.Handler1 {
                            val party =
                                PartyManager.getParty(player)
                                    ?: Party.solo(player)

                            if (party.leader != player) {
                                player.sendMessage(Component.text("Only the party leader can start quests.").color(NamedTextColor.RED))
                                player.playSound(player, Sound.ENTITY_PLAYER_TELEPORT, 1.0f, 0.1f)
                                return@Handler1
                            }

                            if (party.quest != null) {
                                player.sendMessage(Component.text("Party already has an active quest.").color(NamedTextColor.RED))
                                player.playSound(player, Sound.ENTITY_PLAYER_TELEPORT, 1.0f, 0.1f)
                                return@Handler1
                            }

                            val qm = LifeQuest.plugin.questManager
                            val legacyType = toLegacyQuestType(questType)
                            val result = qm.startQuest(legacyType, party)
                            if (result is QuestResult.Failure) {
                                player.sendMessage(
                                    Component
                                        .text(result.reason)
                                        .color(NamedTextColor.RED),
                                )
                                player.playSound(player, Sound.ENTITY_PLAYER_TELEPORT, 1.0f, 0.1f)
                            }
                        },
                    ),
            )
        }
    }

    private fun createQuestItem(questType: QuestType): ItemStack {
        val material = Material.matchMaterial(questType.icon.type) ?: Material.PAPER
        val item = ItemStack(material)
        val meta: ItemMeta = item.itemMeta ?: return item
        meta.displayName(Component.text(questType.title))
        val lore = questType.description.map { Component.text(it) }
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun toLegacyQuestType(qt: QuestType): net.azisaba.lifequest.quest.QuestType =
        net.azisaba.lifequest.quest.QuestType(
            key =
                net.kyori.adventure.key.Key
                    .key(qt.key),
            title = qt.title,
            icon = qt.icon,
            description = qt.description,
            category =
                net.azisaba.lifequest.quest.QuestCategory(
                    net.kyori.adventure.key.Key
                        .key(qt.category),
                    qt.category,
                ),
            location = qt.location,
            giver = qt.giver,
            playLimits =
                net.azisaba.lifequest.quest.PlayLimits(
                    weekly = qt.playLimits.weekly,
                    monthly = qt.playLimits.monthly,
                    yearly = qt.playLimits.yearly,
                    lifetime = qt.playLimits.lifetime,
                ),
            acceptConditions =
                net.azisaba.lifequest.quest.AcceptConditions(
                    pveLevel = qt.acceptConditions.pveLevel,
                    partyMode = qt.acceptConditions.partyMode,
                ),
            maxPlayers = qt.maxPlayers,
            minPlayers = qt.minPlayers,
            deathLimit = qt.deathLimit,
            guides = emptyList(),
            requirements =
                qt.requirements.mapValues { (_, v) ->
                    net.azisaba.lifequest.quest
                        .QuestRequirement(v.key, v.amount)
                },
            actions =
                qt.actions?.let {
                    net.azisaba.lifequest.action.ActionSet(
                        it.onFirstComplete.map { a ->
                            net.azisaba.lifequest.action.Action(
                                type =
                                    net.azisaba.lifequest.action.ActionType
                                        .valueOf(a.type.name),
                                material = a.material,
                                amount = a.amount,
                                item = a.item,
                                command = a.command,
                            )
                        },
                        it.onComplete.map { a ->
                            net.azisaba.lifequest.action.Action(
                                type =
                                    net.azisaba.lifequest.action.ActionType
                                        .valueOf(a.type.name),
                                material = a.material,
                                amount = a.amount,
                                item = a.item,
                                command = a.command,
                            )
                        },
                    )
                },
        )

    companion object {
        private val CATEGORY_INDEXES = listOf(1, 2, 3, 4, 5, 6, 7)
        private val QUEST_TYPE_INDEXES =
            listOf(19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43)

        @Suppress("UNCHECKED_CAST")
        private val CHUNKED_CATEGORIES: List<List<QuestCategory?>>
            get() =
                (
                    listOf<QuestCategory?>(
                        null,
                    ) + QuestCategories.entries.toList()
                ).chunked(CATEGORY_INDEXES.size) as List<List<QuestCategory?>>
    }
}
