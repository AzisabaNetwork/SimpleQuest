package net.azisaba.simplequest.gui

import com.tksimeji.kunectron.ChestGui
import com.tksimeji.kunectron.builder.GuiBuilder
import com.tksimeji.kunectron.element.Element
import com.tksimeji.kunectron.event.ItemContainerClickEvent
import com.tksimeji.kunectron.hooks.ChestGuiHooks
import net.azisaba.simplequest.SimpleQuest
import net.azisaba.simplequest.domain.quest.model.QuestType
import net.azisaba.simplequest.party.Party
import net.azisaba.simplequest.party.PartyManager
import net.azisaba.simplequest.registry.DomainQuestTypes
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object QuestGui : SearchableGui {
    private var lastQuery = ""

    fun open(player: Player) {
        GuiBuilder
            .chest()
            .size(ChestGui.ChestSize.SIZE_54)
            .title(Component.text("Quests"))
            .handler(ItemContainerClickEvent::class.java) { event, _ ->
                val index = event.index
                val types = filteredTypes()
                val questIndex = index - 18
                if (questIndex in types.indices) {
                    val questType = types[questIndex]
                    openDetail(player, questType)
                }
                if (index == 53) {
                    player.closeInventory()
                    SearchGui.openFor(player) { query ->
                        lastQuery = query
                        open(player)
                    }
                }
            }.build(player)
            .also { hooks -> useLayout(hooks) }
    }

    private fun openDetail(
        player: Player,
        questType: QuestType,
    ) {
        GuiBuilder
            .chest()
            .size(ChestGui.ChestSize.SIZE_27)
            .title(Component.text(questType.title))
            .build(player)
            .also { hooks ->
                val mat = Material.matchMaterial(questType.icon.type) ?: Material.PAPER
                hooks.useElement(
                    13,
                    Element
                        .item(mat)
                        .title(Component.text("§e${questType.title}"))
                        .lore(
                            questType.description.map { Component.text(it) } +
                                Component.text("") +
                                Component.text("§7Requirements:") +
                                questType.requirements.map { (k, v) ->
                                    Component.text("§7  $k: §f${v.amount}")
                                } +
                                Component.text("") +
                                Component.text("§eClick to accept"),
                        ).handler { _ ->
                            player.closeInventory()
                            acceptQuest(player, questType)
                        },
                )
                hooks.useElement(
                    11,
                    Element
                        .item(Material.GREEN_TERRACOTTA)
                        .title(Component.text("§a§lAccept"))
                        .lore(Component.text("§7Start this quest"), Component.text("§eClick to accept"))
                        .handler { _ ->
                            player.closeInventory()
                            acceptQuest(player, questType)
                        },
                )
                hooks.useElement(
                    15,
                    Element
                        .item(Material.RED_TERRACOTTA)
                        .title(Component.text("§c§lBack"))
                        .handler { _ ->
                            player.closeInventory()
                            open(player)
                        },
                )
            }
    }

    private fun acceptQuest(
        player: Player,
        questType: QuestType,
    ) {
        val party = PartyManager.getParty(player) ?: Party.solo(player)
        val playerIds = party.map { it.uniqueId.toString() }

        val domainParty =
            object : net.azisaba.simplequest.domain.party.model.Party {
                override val leaderId = party.leader.uniqueId.toString()
                override val memberIds = party.members.map { it.uniqueId.toString() }.toSet()
                override val size = party.size
                override val invitationSetting = net.azisaba.simplequest.domain.party.model.InvitationSetting.LEADER

                override fun hasPermission(t: QuestType): Boolean {
                    if (t.maxPlayers != null && size > t.maxPlayers) return false
                    if (t.minPlayers != null && size < t.minPlayers) return false
                    return true
                }
            }

        val result = SimpleQuest.plugin.questService.startQuest(questType, domainParty, playerIds)
        when (result) {
            is net.azisaba.simplequest.domain.quest.model.QuestResult.Success -> {
                player.sendMessage(Component.text("§aQuest '${questType.title}' started!"))
            }

            is net.azisaba.simplequest.domain.quest.model.QuestResult.Failure -> {
                player.sendMessage(Component.text("§cFailed: ${result.reason}"))
            }
        }
    }

    private fun useLayout(hooks: ChestGuiHooks) {
        hooks.useElement(0, Element.item(Material.CHEST).title(Component.text("§6All")))
        hooks.useElement(1, Element.item(Material.CLOCK).title(Component.text("§eDaily")))
        hooks.useElement(2, Element.item(Material.ENCHANTED_BOOK).title(Component.text("§dStory")))
        hooks.useElement(3, Element.item(Material.FIREWORK_STAR).title(Component.text("§aEvent")))
        hooks.useElement(
            53,
            Element.item(Material.COMPASS).title(Component.text("§bSearch")).lore(
                Component.text("§7Click to search"),
            ),
        )

        val types = filteredTypes()
        types.take(21).forEachIndexed { index, questType ->
            hooks.useElement(18 + index, Element.item(createQuestItem(questType)))
        }
    }

    private fun filteredTypes(): List<QuestType> {
        val all = DomainQuestTypes.entries.toList()
        return if (lastQuery.isBlank()) {
            all
        } else {
            all.filter { it.title.contains(lastQuery, ignoreCase = true) || it.key.contains(lastQuery, ignoreCase = true) }
        }
    }

    override fun search(query: String) {
        lastQuery = query
    }

    private fun createQuestItem(questType: QuestType): ItemStack {
        val material = Material.matchMaterial(questType.icon.type) ?: Material.PAPER
        val item = ItemStack(material)
        val meta: ItemMeta = item.itemMeta ?: return item
        meta.displayName(Component.text(questType.title))
        val lore =
            questType.description.map { Component.text(it) } +
                Component.text("") +
                Component.text("§eClick for details")
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }
}
