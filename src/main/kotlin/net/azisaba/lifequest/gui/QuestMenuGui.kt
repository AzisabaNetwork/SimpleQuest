// Original: https://github.com/AzisabaNetwork/quem by tksimeji
// Adapted for LifeQuest

package net.azisaba.lifequest.gui

import com.tksimeji.kunectron.ChestGui
import com.tksimeji.kunectron.Kunectron
import com.tksimeji.kunectron.element.Element
import com.tksimeji.kunectron.element.ItemElement
import com.tksimeji.kunectron.hooks.ChestGuiHooks
import net.azisaba.lifequest.quest.Quest
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player

@ChestGui
class QuestMenuGui(
    @ChestGui.Player private val player: Player,
    private val quest: Quest,
) : ChestGuiHooks {
    @ChestGui.Title
    private val title = Component.text("Quest Menu")

    @ChestGui.Size
    private val size = ChestGui.ChestSize.SIZE_27

    @ChestGui.Element(index = [11])
    private val questInfo =
        Element
            .item(Material.PAPER)
            .title(Component.text(quest.type.title))

    @ChestGui.Element(index = [13])
    private val quit =
        Element
            .item(Material.TNT_MINECART)
            .title(
                Component
                    .text(if (player != quest.party.leader) "Quit" else "Disband")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD),
            ).handler(
                ItemElement.Handler1 {
                    Kunectron.create(
                        ConfirmGui(
                            player,
                            { quest.party.removeMember(player) },
                            null,
                        ),
                    )
                },
            )

    @ChestGui.Element(index = [15])
    private val close =
        Element
            .item(Material.BARRIER)
            .title(Component.text("Close").color(NamedTextColor.RED))
            .handler(ItemElement.Handler1(this::useClose))
}
