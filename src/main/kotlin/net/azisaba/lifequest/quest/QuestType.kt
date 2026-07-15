package net.azisaba.lifequest.quest

import net.azisaba.lifequest.action.ActionSet
import net.azisaba.lifequest.domain.data.Icon
import net.azisaba.lifequest.domain.data.Location
import net.azisaba.lifequest.domain.script.Script
import net.azisaba.lifequest.registry.Keyed
import net.kyori.adventure.key.Key

class QuestType(
    override val key: Key,
    val title: String,
    val icon: Icon,
    val description: List<String>,
    val category: QuestCategory,
    val location: Location?,
    val giver: String?,
    val playLimits: PlayLimits,
    val acceptConditions: AcceptConditions,
    val maxPlayers: Int?,
    val minPlayers: Int?,
    val deathLimit: Int?,
    val guides: List<GameGuide>,
    val requirements: Map<String, QuestRequirement>,
    val actions: ActionSet?,
    val scripts: List<Script> = emptyList(),
) : Keyed
