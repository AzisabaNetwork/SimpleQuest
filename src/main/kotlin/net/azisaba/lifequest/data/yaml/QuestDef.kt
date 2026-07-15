package net.azisaba.lifequest.data.yaml

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestDef(
    @SerialName("Title") val title: String,
    @SerialName("Description") val description: List<String> = emptyList(),
    @SerialName("Icon") val icon: String = "STONE",
    @SerialName("Aura") val aura: Boolean = false,
    @SerialName("Giver") val giver: String? = null,
    @SerialName("Category") val category: String = "lq:general",
    @SerialName("Location") val location: String? = null,
    @SerialName("Options") val options: QuestOptionsDef? = null,
    @SerialName("Requirements") val requirements: RequirementDef? = null,
    @SerialName("Objectives") val objectives: Map<String, String> = emptyMap(),
    @SerialName("Actions") val actions: ActionsDef? = null,
    @SerialName("Guides") val guides: List<GuideDef>? = null,
    @SerialName("Scripts") val scripts: Map<String, List<String>>? = null,
    @SerialName("Unlock") val unlock: List<UnlockDef>? = null,
)

@Serializable
data class QuestOptionsDef(
    @SerialName("MaxParty") val maxParty: String? = null,
    @SerialName("Limits") val limits: PlayLimitDef? = null,
    @SerialName("DeathLimit") val deathLimit: Int? = null,
)

@Serializable
data class PlayLimitDef(
    @SerialName("Daily") val daily: Int? = null,
    @SerialName("Weekly") val weekly: Int? = null,
    @SerialName("Monthly") val monthly: Int? = null,
    @SerialName("Yearly") val yearly: Int? = null,
    @SerialName("Lifetime") val lifetime: Int? = null,
)

@Serializable
data class RequirementDef(
    @SerialName("PvELevel") val pveLevel: Int? = null,
    @SerialName("Money") val money: Double? = null,
    @SerialName("PartyMode") val partyMode: Boolean = false,
)

@Serializable
data class ActionsDef(
    @SerialName("OnFirstComplete") val onFirstComplete: List<ActionEntry> = emptyList(),
    @SerialName("OnComplete") val onComplete: List<ActionEntry> = emptyList(),
)

@Serializable
data class ActionEntry(
    @SerialName("Type") val type: String,
    @SerialName("Params") val params: String,
    @SerialName("DisplayName") val displayName: String? = null,
)

@Serializable
data class GuideDef(
    @SerialName("Title") val title: String? = null,
    @SerialName("Location") val location: String,
    @SerialName("Condition") val condition: String? = null,
)

@Serializable
data class UnlockDef(
    @SerialName("EnterArea") val enterArea: String? = null,
)
