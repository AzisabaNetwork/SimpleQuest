package net.azisaba.simplequest.party

import org.bukkit.entity.Player

class SoloPartyImpl(
    player: Player,
) : PartyImpl(player) {
    init {
        require(Party.MAX_SIZE >= 1) { "Max party size must be at least 1" }
    }

    override fun addMember(player: Player): Unit = throw UnsupportedOperationException("Solo party cannot accept members")
}
