// Original: https://github.com/AzisabaNetwork/quem by tksimeji
// Adapted for LifeQuest

package net.azisaba.simplequest.gui

import com.tksimeji.kunectron.ChestGui
import com.tksimeji.kunectron.element.Element
import com.tksimeji.kunectron.element.ItemElement
import com.tksimeji.kunectron.hooks.ChestGuiHooks
import net.azisaba.simplequest.party.Party
import net.azisaba.simplequest.party.PartyManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemType

@ChestGui
class PartyCreateGui(
    @ChestGui.Player private val player: Player,
) : ChestGuiHooks {
    @ChestGui.Title
    private val title = Component.text("Party")

    @ChestGui.Element(index = [22])
    private val create =
        Element
            .item(ItemType.CRAFTING_TABLE)
            .title(Component.text("Create Party").color(NamedTextColor.GREEN))
            .lore(Component.text("Create a new party").color(NamedTextColor.GRAY))
            .handler(
                ItemElement.Handler1 {
                    PartyManager.setParty(player, Party.create(player))
                    PartyMenuGui.open(player)
                },
            )

    @ChestGui.Element(index = [40])
    private val close =
        Element
            .item(ItemType.BARRIER)
            .title(Component.text("Close").color(NamedTextColor.RED))
            .handler(ItemElement.Handler1(this::useClose))
}
