package net.azisaba.simplequest.party

import org.bukkit.entity.Player
import java.util.UUID

/**
 * Represents a pending party invitation.
 */
class Invite(
    val party: Party,
    val sender: Player,
    val target: Player,
    private val expiresAtTick: Long,
) {
    val id: UUID = UUID.randomUUID()
    var accepted: Boolean = false
        private set

    fun accept(currentTick: Long = expiresAtTick + 1): Boolean {
        if (isExpired(currentTick)) return false
        if (accepted) return false
        accepted = true
        party.addMember(target)
        return true
    }

    fun isExpired(currentTick: Long): Boolean = currentTick > expiresAtTick
}
