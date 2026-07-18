package net.azisaba.simplequest.quest

import net.azisaba.simplequest.domain.data.Location

data class GameGuide(
    val title: String? = null,
    val location: Location,
    val requirements: Map<String, Int> = emptyMap(),
)
