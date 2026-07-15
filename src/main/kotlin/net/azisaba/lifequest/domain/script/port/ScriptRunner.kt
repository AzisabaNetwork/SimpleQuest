package net.azisaba.lifequest.domain.script.port

import net.azisaba.lifequest.domain.script.Script

/**
 * Port for executing script commands with player context.
 */
interface ScriptRunner {
    fun run(
        script: Script,
        playerIds: List<String>,
    )
}
