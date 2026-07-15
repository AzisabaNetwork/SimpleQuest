package net.azisaba.lifequest.domain.stage

/**
 * Represents a stage in a staged quest progression.
 */
interface Stage : StageLike {
    val active: Boolean
    val tasks: List<StageTask>
}

/**
 * A single task within a stage.
 */
data class StageTask(
    val key: String,
    val description: String,
    val isComplete: Boolean = false,
)
