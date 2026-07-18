package net.azisaba.simplequest.domain.quest.model

/**
 * Defines repeat-play limits for a quest type.
 * All values are nullable — null means "no limit".
 */
data class PlayLimits(
    val daily: Int? = null,
    val weekly: Int? = null,
    val monthly: Int? = null,
    val yearly: Int? = null,
    val lifetime: Int? = null,
) {
    /** Returns true when no limits are configured. */
    val isUnlimited: Boolean
        get() = daily == null && weekly == null && monthly == null && yearly == null && lifetime == null
}
