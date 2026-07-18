package net.azisaba.simplequest.domain.quest.model

import net.azisaba.simplequest.domain.action.ActionSet
import net.azisaba.simplequest.domain.data.Icon
import net.azisaba.simplequest.domain.data.Location
import net.azisaba.simplequest.domain.script.Script

/**
 * Static definition/blueprint for a quest type.
 * This is the template from which quest instances are created.
 */
data class QuestType(
    val key: String,
    val title: String,
    val icon: Icon,
    val description: List<String> = emptyList(),
    val category: String = "lq:general",
    val location: Location? = null,
    val giver: String? = null,
    val playLimits: PlayLimits = PlayLimits(),
    val acceptConditions: AcceptConditions = AcceptConditions(),
    val maxPlayers: Int? = null,
    val minPlayers: Int? = null,
    val deathLimit: Int? = null,
    val guides: List<GameGuide> = emptyList(),
    val requirements: Map<String, QuestRequirement> = emptyMap(),
    val actions: ActionSet? = null,
    val scripts: List<Script> = emptyList(),
)
