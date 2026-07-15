package net.azisaba.lifequest.domain.data

/**
 * Represents a visual icon for quest display in GUIs.
 */
data class Icon(
    val type: String,
    val customModelData: Int? = null,
    val aura: Boolean = false,
    val model: String? = null,
)
