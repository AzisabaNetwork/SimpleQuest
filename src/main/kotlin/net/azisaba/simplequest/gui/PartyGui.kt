// Original: https://github.com/AzisabaNetwork/quem by tksimeji
// Adapted for SimpleQuest

package net.azisaba.simplequest.gui

import com.tksimeji.kunectron.ChestGui
import com.tksimeji.kunectron.hooks.ChestGuiHooks
import net.azisaba.simplequest.party.Party
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

abstract class PartyGui(
    player: Player,
    val party: Party,
) : ChestGuiHooks,
    UpdatableGui {
    @ChestGui.Title
    private val title = Component.text("Party")
}
