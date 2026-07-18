package net.azisaba.simplequest.quest

data class PlayLimits(
    val weekly: Int? = null,
    val monthly: Int? = null,
    val yearly: Int? = null,
    val lifetime: Int? = null,
)
