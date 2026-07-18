package net.azisaba.simplequest.gui

import com.tksimeji.kunectron.ChestGui
import com.tksimeji.kunectron.builder.GuiBuilder
import com.tksimeji.kunectron.element.Element
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

object ConfirmGui {
    fun open(
        player: Player,
        title: String,
        onAccept: () -> Unit,
        onReject: () -> Unit = {},
    ) {
        GuiBuilder
            .chest()
            .size(ChestGui.ChestSize.SIZE_27)
            .title(Component.text("Confirm"))
            .build(player)
            .also { hooks ->
                // 緑色 (Yes)
                hooks.useElement(
                    11,
                    Element
                        .item(Material.GREEN_TERRACOTTA)
                        .title(Component.text("§a§lYes"))
                        .lore(Component.text("§7$title"), Component.text(""), Component.text("§eClick to confirm"))
                        .handler { _ ->
                            player.closeInventory()
                            onAccept()
                        },
                )
                // 赤色 (No)
                hooks.useElement(
                    15,
                    Element
                        .item(Material.RED_TERRACOTTA)
                        .title(Component.text("§c§lNo"))
                        .lore(Component.text("§7$title"), Component.text(""), Component.text("§eClick to cancel"))
                        .handler { _ ->
                            player.closeInventory()
                            onReject()
                        },
                )
            }
    }
}
