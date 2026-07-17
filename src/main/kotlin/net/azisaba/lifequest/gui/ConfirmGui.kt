// Original: https://github.com/AzisabaNetwork/quem by tksimeji
// Adapted for LifeQuest

package net.azisaba.lifequest.gui

import com.tksimeji.kunectron.ChestGui
import com.tksimeji.kunectron.element.Element
import com.tksimeji.kunectron.element.ItemElement
import com.tksimeji.kunectron.hooks.ChestGuiHooks
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemType

@ChestGui
class ConfirmGui(
    @ChestGui.Player private val player: Player,
    onAccept: Runnable?,
    onReject: Runnable?,
) : ChestGuiHooks {
    @ChestGui.Title
    private val title = Component.text("Confirm")

    @ChestGui.Size
    private val size = ChestGui.ChestSize.SIZE_27

    @ChestGui.Element(index = [11])
    private val accept =
        Element
            .item(ItemType.GREEN_TERRACOTTA)
            .title(Component.text("Accept").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
            .handler(
                ItemElement.Handler1 {
                    useClose()
                    onAccept?.run()
                },
            )

    @ChestGui.Element(index = [15])
    private val reject =
        Element
            .item(ItemType.RED_TERRACOTTA)
            .title(Component.text("Reject").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
            .handler(
                ItemElement.Handler1 {
                    useClose()
                    onReject?.run()
                },
            )
}
