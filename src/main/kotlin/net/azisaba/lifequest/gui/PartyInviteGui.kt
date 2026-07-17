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
import net.azisaba.lifequest.party.Party
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemType
import kotlin.math.max
import kotlin.math.min

@ChestGui
class PartyInviteGui(
    @ChestGui.Player private val player: Player,
    party: Party,
    private val page: Int = 0,
    private val query: String = "",
) : PartyGui(player, party),
    ChestGuiHooks,
    SearchableGui {
    private val players
        get() = Bukkit.getOnlinePlayers().filter { it !in party && it.name.contains(query, ignoreCase = true) }

    @ChestGui.Title
    private val title = Component.text("Party")

    @ChestGui.Element(index = [45])
    private val previous =
        Element
            .item(ItemType.ARROW)
            .title(Component.text("Previous").color(NamedTextColor.GREEN))
            .handler(
                ItemElement.Handler1 {
                    Kunectron.create(PartyInviteGui(player, party, max(page - 1, 0)))
                },
            )

    @ChestGui.Element(index = [48])
    private val back =
        Element
            .item(ItemType.ARROW)
            .title(Component.text("Back").color(NamedTextColor.GREEN))
            .handler(
                ItemElement.Handler1 {
                    Kunectron.create(PartyMenuGui(player, party))
                },
            )

    @ChestGui.Element(index = [49])
    private val close =
        Element
            .item(ItemType.BARRIER)
            .title(Component.text("Close").color(NamedTextColor.RED))
            .handler(ItemElement.Handler1(this::useClose))

    @ChestGui.Element(index = [53])
    private val next =
        Element
            .item(ItemType.ARROW)
            .title(Component.text("Next").color(NamedTextColor.GREEN))
            .handler(
                ItemElement.Handler1 {
                    Kunectron.create(
                        PartyInviteGui(player, party, min(page + 1, players.size / PLAYER_INDEXES.size)),
                    )
                },
            )

    @GuiHandler
    private fun onInit(event: ChestGuiEvents.InitEvent) {
        for (
        (index, aPlayer) in players
            .subList(page * PLAYER_INDEXES.size, min((page + 1) * PLAYER_INDEXES.size, players.size))
            .withIndex()
        ) {
            val playerIndex = PLAYER_INDEXES[index]
            useElement(
                playerIndex,
                Element
                    .playerHead(aPlayer)
                    .title(Component.text(aPlayer.name))
                    .handler(
                        ItemElement.Handler1 {
                            if (aPlayer in party) {
                                Kunectron.create(PartyInviteGui(player, party, page, query))
                                return@Handler1
                            }
                            Bukkit.dispatchCommand(player, "party invite ${aPlayer.name}")
                        },
                    ),
            )
        }
    }

    override fun search(query: String) {
        Kunectron.create(PartyInviteGui(player, party, query = query))
    }

    override fun update() {
        Kunectron.create(PartyInviteGui(player, party, page, query))
    }

    companion object {
        private val PLAYER_INDEXES = listOf(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34)
    }
}
