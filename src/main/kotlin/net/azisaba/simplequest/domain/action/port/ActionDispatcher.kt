package net.azisaba.simplequest.domain.action.port

import net.azisaba.simplequest.domain.action.Action

/**
 * Port for dispatching multiple actions to multiple players.
 */
interface ActionDispatcher {
    fun dispatch(
        action: Action,
        playerId: String,
    )

    fun dispatchAll(
        actions: List<Action>,
        playerIds: List<String>,
    )
}
