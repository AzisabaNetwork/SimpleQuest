package net.azisaba.lifequest.domain.action.port

import net.azisaba.lifequest.domain.action.Action

/**
 * Port for executing quest reward actions on a player.
 */
interface ActionRunner {
    fun run(
        action: Action,
        playerId: String,
    )
}
