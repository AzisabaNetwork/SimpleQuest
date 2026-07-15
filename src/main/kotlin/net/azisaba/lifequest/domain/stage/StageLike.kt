package net.azisaba.lifequest.domain.stage

/**
 * Interface for objects that can be mounted as a stage.
 */
interface StageLike {
    val key: String

    fun mount()

    fun unmount()
}
