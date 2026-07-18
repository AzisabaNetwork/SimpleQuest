package net.azisaba.simplequest.domain.party.model

/**
 * Represents a pending party invitation.
 */
data class Invite(
    val id: String,
    val partyId: String,
    val inviterId: String,
    val targetId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + 120_000L,
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
}
