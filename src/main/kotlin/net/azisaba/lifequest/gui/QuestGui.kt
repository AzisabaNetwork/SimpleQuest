package net.azisaba.lifequest.gui

import com.tksimeji.kunectron.ChestGui
import com.tksimeji.kunectron.builder.GuiBuilder
import com.tksimeji.kunectron.element.Element
import com.tksimeji.kunectron.event.ItemContainerClickEvent
import com.tksimeji.kunectron.hooks.ChestGuiHooks
import net.azisaba.lifequest.domain.data.Icon
import net.azisaba.lifequest.domain.quest.model.QuestType
import net.azisaba.lifequest.registry.DomainQuestTypes
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object QuestGui {
    fun open(player: Player) {
        GuiBuilder
            .chest()
            .size(ChestGui.ChestSize.SIZE_54)
            .title(Component.text("Quests"))
            .handler(ItemContainerClickEvent::class.java) { event, _ ->
                val index = event.index
                val types = DomainQuestTypes.entries.toList()
                val questIndex = index - 18
                if (questIndex in types.indices) {
                    val questType = types[questIndex]
                    player.sendMessage(Component.text("§e${questType.title} §achosen"))
                    player.closeInventory()
                }
            }.build(player)
            .also { hooks ->
                useLayout(hooks)
            }
    }

    private fun useLayout(hooks: ChestGuiHooks) {
        hooks.useElement(0, Element.item(Material.CHEST).title(Component.text("§6All")))
        hooks.useElement(1, Element.item(Material.CLOCK).title(Component.text("§eDaily")))
        hooks.useElement(2, Element.item(Material.ENCHANTED_BOOK).title(Component.text("§dStory")))
        hooks.useElement(3, Element.item(Material.FIREWORK_STAR).title(Component.text("§aEvent")))
        hooks.useElement(53, Element.item(Material.COMPASS).title(Component.text("§bSearch")))

        val types = DomainQuestTypes.entries.toList()
        types.take(21).forEachIndexed { index, questType ->
            hooks.useElement(18 + index, Element.item(createQuestItem(questType)))
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
}
