package net.azisaba.simplequest.domain.script.port

import net.azisaba.simplequest.domain.script.Script

/**
 * Port for executing script commands with player context.
 */
interface ScriptRunner {
    fun run(
        script: Script,
        playerIds: List<String>,
    )
}
