package net.azisaba.lifequest.domain.action.port

import net.azisaba.lifequest.domain.action.Action

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
