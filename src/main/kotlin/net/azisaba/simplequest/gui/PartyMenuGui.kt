package net.azisaba.simplequest.gui

import com.tksimeji.kunectron.ChestGui
import com.tksimeji.kunectron.builder.GuiBuilder
import com.tksimeji.kunectron.element.Element
import com.tksimeji.kunectron.element.ItemElement
import com.tksimeji.kunectron.event.ItemContainerClickEvent
import net.azisaba.simplequest.party.InvitationSetting
import net.azisaba.simplequest.party.PartyImpl
import net.azisaba.simplequest.party.PartyManager
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

object PartyMenuGui {
    fun open(player: Player) {
        val party =
            PartyManager.getParty(player) ?: run {
                player.sendMessage(Component.text("§cYou are not in a party."))
                return
            }

        GuiBuilder
            .chest()
            .size(ChestGui.ChestSize.SIZE_54)
            .title(Component.text("Party Menu"))
            .build(player)
            .also { hooks ->
                val allMembers = listOf(party.leader) + party.members.toList()
                allMembers.forEachIndexed { index, member ->
                    val isLeader = member == party.leader
                    val headEl =
                        Element
                            .playerHead(member)
                            .title(Component.text(if (isLeader) "§6§l${member.name} §e👑" else "§e${member.name}"))
                            .lore(
                                Component.text("§7HP: ${member.health.toInt()}"),
                                Component.text(""),
                                Component.text(if (isLeader) "§7Party Leader" else "§7Member"),
                            )
                    if (isLeader) {
                        headEl.handler(
                            ItemElement.Handler2 { event: ItemContainerClickEvent ->
                                if (event.isShiftClick && event.isLeftClick) {
                                    player.closeInventory()
                                    ConfirmGui.open(player, "Kick ${member.name}?", {
                                        if (party is PartyImpl) party.removeMember(member)
                                        open(player)
                                    })
                                }
                            },
                        )
                    }
                    hooks.useElement(index, headEl)
                }

                hooks.useElement(
                    45,
                    Element
                        .item(Material.COMPARATOR)
                        .title(Component.text("§6Setting: §e${party.invitationSetting.name}"))
                        .handler(
                            ItemElement.Handler1 {
                                party.invitationSetting =
                                    if (party.invitationSetting == InvitationSetting.LEADER) {
                                        InvitationSetting.ALL
                                    } else {
                                        InvitationSetting.LEADER
                                    }
                                open(player)
                            },
                        ),
                )

                hooks.useElement(
                    49,
                    Element
                        .item(Material.ENCHANTING_TABLE)
                        .title(Component.text("§5Quest Selection"))
                        .handler(
                            ItemElement.Handler1 {
                                if (party.quest == null) {
                                    QuestGui.open(player)
                                } else {
                                    player.sendMessage(Component.text("§cAlready in a quest."))
                                }
                            },
                        ),
                )

                hooks.useElement(
                    53,
                    Element
                        .item(Material.TNT_MINECART)
                        .title(Component.text("§c§lLeave Party"))
                        .handler(
                            ItemElement.Handler1 {
                                player.closeInventory()
                                ConfirmGui.open(player, "Leave party?", {
                                    if (party is PartyImpl) party.removeMember(player)
                                })
                            },
                        ),
                )
            }
    }
}
