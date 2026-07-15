package net.azisaba.lifequest.domain.quest.model

import net.azisaba.lifequest.domain.data.Location

/**
 * A waypoint/guide entry for a quest, showing a location and condition to players.
 */
data class GameGuide(
    val title: String? = null,
    val location: Location,
    val requirements: Map<String, Int> = emptyMap(),
)
