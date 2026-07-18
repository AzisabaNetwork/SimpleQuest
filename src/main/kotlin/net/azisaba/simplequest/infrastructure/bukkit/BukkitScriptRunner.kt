package net.azisaba.simplequest.infrastructure.bukkit

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.domain.script.Script
import net.azisaba.simplequest.domain.script.port.ScriptRunner
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Bukkit implementation of [ScriptRunner] that uses Paper's scheduler for delayed commands.
 */
@Singleton
class BukkitScriptRunner
    @Inject
    constructor(
        private val plugin: Plugin,
    ) : ScriptRunner {
        override fun run(
            script: Script,
            playerIds: List<String>,
        ) {
            val runnable = Runnable { execute(script, playerIds) }
            if (script.delay > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, runnable, script.delay)
            } else {
                runnable.run()
            }
        }

        private fun execute(
            script: Script,
            playerIds: List<String>,
        ) {
            for (command in script.commands) {
                if (command.startsWith(":")) {
                    val cmd = command.substring(1)
                    for (playerId in playerIds) {
                        val player = Bukkit.getPlayer(UUID.fromString(playerId))
                        if (player != null) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%", player.name))
                        }
                    }
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                }
            }
        }
    }
