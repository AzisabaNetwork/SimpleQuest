package net.azisaba.lifequest.domain.data

/**
 * Represents a world coordinate with optional rotation.
 */
data class Location(
    val world: String = "world",
    val x: Double = 0.0,
    val y: Double = 64.0,
    val z: Double = 0.0,
    val yaw: Float = 0.0f,
    val pitch: Float = 0.0f,
)
