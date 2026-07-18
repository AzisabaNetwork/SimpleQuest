package net.azisaba.simplequest.quest

import net.azisaba.simplequest.domain.data.Icon
import net.azisaba.simplequest.registry.Keyed
import net.kyori.adventure.key.Key

class QuestCategory(
    override val key: Key,
    val title: String,
    val icon: Icon? = null,
) : Keyed
