package net.azisaba.simplequest.party

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PartyManager {
    private val playerParties = ConcurrentHashMap<UUID, Party>()

    fun getParty(player: Player): Party? = playerParties[player.uniqueId]

    fun setParty(
        player: Player,
        party: Party?,
    ) {
        if (party != null) {
            playerParties[player.uniqueId] = party
        } else {
            playerParties.remove(player.uniqueId)
        }
    }

    fun removePlayer(player: Player) {
        playerParties.remove(player.uniqueId)
    }

    fun clear() {
        playerParties.clear()
    }
}

val Player.party: Party? get() = PartyManager.getParty(this)
