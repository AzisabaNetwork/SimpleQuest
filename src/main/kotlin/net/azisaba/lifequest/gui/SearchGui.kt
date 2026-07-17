// Original: https://github.com/AzisabaNetwork/quem by tksimeji
// Adapted for LifeQuest

package net.azisaba.lifequest.gui

import com.tksimeji.kunectron.SignGui
import com.tksimeji.kunectron.event.GuiHandler
import com.tksimeji.kunectron.event.SignGuiEvents
import org.bukkit.entity.Player

@SignGui
class SearchGui(
    @SignGui.Player private val player: Player,
    private val searchable: SearchableGui,
) {
    @GuiHandler
    fun onClose(event: SignGuiEvents.CloseEvent) {
        searchable.search(event.firstLine)
    }
}
