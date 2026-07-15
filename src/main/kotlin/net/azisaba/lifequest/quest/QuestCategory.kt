package net.azisaba.lifequest.quest

import net.azisaba.lifequest.domain.data.Icon
import net.azisaba.lifequest.registry.Keyed
import net.kyori.adventure.key.Key

class QuestCategory(
    override val key: Key,
    val title: String,
    val icon: Icon? = null,
) : Keyed
