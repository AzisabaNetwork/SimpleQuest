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
import net.azisaba.lifequest.party.InvitationSetting
import net.azisaba.lifequest.party.Party
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemType
import java.net.URI
import kotlin.math.max
import kotlin.math.min

@ChestGui
class PartyMenuGui(
    @ChestGui.Player private val player: Player,
    party: Party,
    private val page: Int = 0,
) : PartyGui(player, party),
    ChestGuiHooks {
    private val members = party.members.toList()

    @ChestGui.Element(index = [18])
    private val previous =
        Element
            .item(ItemType.ARROW)
            .title(Component.text("Previous").color(NamedTextColor.GREEN))
            .handler(
                ItemElement.Handler1 {
                    Kunectron.create(PartyMenuGui(player, party, max(page - 1, 0)))
                },
            )

    @ChestGui.Element(index = [26])
    private val next =
        Element
            .item(ItemType.ARROW)
            .title(Component.text("Next").color(NamedTextColor.GREEN))
            .handler(
                ItemElement.Handler1 {
                    Kunectron.create(
                        PartyMenuGui(player, party, min(page + 1, members.size / MEMBER_INDEXES.size)),
                    )
                },
            )

    @ChestGui.Element(index = [47])
    private val invitationSetting =
        Element
            .item(ItemType.COMPARATOR)
            .title(Component.text("Invitation Setting").color(NamedTextColor.GREEN))
            .lore(
                Component.text("Change who can invite members").color(NamedTextColor.GRAY),
                Component.empty(),
                Component
                    .text("Leader Only")
                    .color(if (party.invitationSetting == InvitationSetting.LEADER) NamedTextColor.GREEN else NamedTextColor.DARK_GRAY),
                Component
                    .text("All Members")
                    .color(if (party.invitationSetting == InvitationSetting.ALL) NamedTextColor.GREEN else NamedTextColor.DARK_GRAY),
            ).handler(
                ItemElement.Handler1 {
                    if (player != party.leader) {
                        return@Handler1
                    }
                    party.invitationSetting =
                        when (party.invitationSetting) {
                            InvitationSetting.LEADER -> InvitationSetting.ALL
                            InvitationSetting.ALL -> InvitationSetting.LEADER
                        }
                },
            )

    @ChestGui.Element(index = [48])
    private val quest =
        Element
            .item(ItemType.ENCHANTING_TABLE)
            .title(Component.text("Quest").color(NamedTextColor.LIGHT_PURPLE))
            .handler(
                ItemElement.Handler1 {
                    if (party.quest == null) {
                        Kunectron.create(QuestGui(player))
                    }
                },
            )

    @ChestGui.Element(index = [49])
    private val close =
        Element
            .item(ItemType.BARRIER)
            .title(Component.text("Close").color(NamedTextColor.RED))
            .handler(ItemElement.Handler1(this::useClose))

    @ChestGui.Element(index = [50])
    private val quit =
        Element
            .item(ItemType.TNT_MINECART)
            .title(
                Component
                    .text(if (player != party.leader) "Quit" else "Disband")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD),
            ).handler(
                ItemElement.Handler1 {
                    Kunectron.create(
                        ConfirmGui(
                            player,
                            { party.removeMember(player) },
                            null,
                        ),
                    )
                },
            )

    @GuiHandler
    fun onInit(event: ChestGuiEvents.InitEvent) {
        var lastIndex = 0

        for (
        (index, member) in members
            .subList(page * MEMBER_INDEXES.size, min((page + 1) * MEMBER_INDEXES.size, members.size))
            .withIndex()
        ) {
            lastIndex = index
            val memberIndex = MEMBER_INDEXES[index]
            val el =
                Element
                    .playerHead(member)
                    .title(
                        (
                            if (member == party.leader) {
                                Component
                                    .text("👑")
                                    .color(NamedTextColor.YELLOW)
                                    .decorate(TextDecoration.BOLD)
                                    .appendSpace()
                            } else {
                                Component.empty()
                            }
                        ).append(Component.text(member.name)),
                    )

            if (player == party.leader && member != party.leader) {
                el
                    .lore(
                        Component.text("Member options").color(NamedTextColor.DARK_GRAY),
                        Component.text("Shift+Left Click: Kick").color(NamedTextColor.GRAY),
                        Component.text("Shift+Right Click: Promote to Leader").color(NamedTextColor.GRAY),
                    ).handler(
                        ItemElement.Handler2 { ev ->
                            if (!ev.isShiftClick) {
                                return@Handler2
                            }
                            if (ev.isLeftClick) {
                                party.removeMember(member)
                            } else if (ev.isRightClick) {
                                party.leader = member
                                Kunectron.create(PartyMenuGui(player, party))
                            }
                        },
                    )
            }

            useElement(memberIndex, el)
        }

        for (index in lastIndex + 1 until MEMBER_INDEXES.size) {
            useElement(MEMBER_INDEXES[index], Element.item(ItemType.CLAY_BALL))
        }

        if (party.size < Party.MAX_SIZE && party.quest == null) {
            useElement(
                MEMBER_INDEXES[party.size],
                Element
                    .playerHead(
                        URI
                            .create(
                                "http://textures.minecraft.net/texture/dd1500e5b04c8053d40c7968330887d24b073daf1e273faf4db8b62ebd99da83",
                            ).toURL(),
                    ).title(Component.text("Invite Player").color(NamedTextColor.GREEN))
                    .handler(
                        ItemElement.Handler1 {
                            Kunectron.create(PartyInviteGui(player, party))
                        },
                    ),
            )
        }

        if (party.quest != null && player == party.leader) {
            useElement(51, Element.item(ItemType.REDSTONE_TORCH))
        }
    }

    override fun update() {
        Kunectron.create(PartyMenuGui(player, party))
    }

    companion object {
        private val MEMBER_INDEXES = listOf(11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33)
    }
}
