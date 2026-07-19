package net.azisaba.simplequest.gui

import com.tksimeji.kunectron.builder.GuiBuilder
import com.tksimeji.kunectron.event.SignGuiEvents
import org.bukkit.entity.Player

/**
 * Sign-based search GUI utility.
 * Opens a sign editor and calls [onSearch] with the first line on close.
 */
object SearchGui {
    fun openFor(
        player: Player,
        onSearch: (String) -> Unit,
    ) {
        GuiBuilder
            .sign()
            .handler(SignGuiEvents.CloseEvent::class.java) { event: SignGuiEvents.CloseEvent, _ ->
                val query = event.firstLine
                if (query.isNotBlank()) {
                    onSearch(query)
                }
            }.build(player)
    }
}
