package net.azisaba.simplequest.quest

data class PlayLimits(
    val daily: Int? = null,
    val weekly: Int? = null,
    val monthly: Int? = null,
    val yearly: Int? = null,
    val lifetime: Int? = null,
    val cooldownMinutes: Int? = null,
)
