package net.azisaba.lifequest.quest

import net.azisaba.lifequest.domain.data.Location

data class GameGuide(
    val title: String? = null,
    val location: Location,
    val requirements: Map<String, Int> = emptyMap(),
)
