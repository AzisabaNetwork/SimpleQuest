package net.azisaba.simplequest.quest

import net.kyori.adventure.key.Key

data class AcceptConditions(
    val pveLevel: Int? = null,
    val requiredQuests: List<Key>? = null,
    val permissions: List<String>? = null,
    val partyMode: Boolean = false,
)
