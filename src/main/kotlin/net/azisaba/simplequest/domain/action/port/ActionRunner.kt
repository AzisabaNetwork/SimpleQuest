package net.azisaba.simplequest.domain.action.port

import net.azisaba.simplequest.domain.action.Action

/**
 * Port for executing quest reward actions on a player.
 */
interface ActionRunner {
    fun run(
        action: Action,
        playerId: String,
    )
}
