package net.azisaba.lifequest.domain.script

/**
 * A script command to be executed at a specific trigger point during quest lifecycle.
 */
data class Script(
    val trigger: Trigger,
    val delay: Long = 0L,
    val commands: List<String> = emptyList(),
) {
    enum class Trigger {
        START,
        END,
        COMPLETE,
        CANCEL,
    }
}
