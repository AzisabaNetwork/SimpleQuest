// Original: https://github.com/AzisabaNetwork/quem by tksimeji
// Adapted for LifeQuest

package net.azisaba.lifequest.gui

import com.tksimeji.kunectron.ChestGui
import com.tksimeji.kunectron.hooks.ChestGuiHooks
import net.azisaba.lifequest.party.Party
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
