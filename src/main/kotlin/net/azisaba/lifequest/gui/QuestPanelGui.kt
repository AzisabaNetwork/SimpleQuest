package net.azisaba.lifequest.gui

import com.tksimeji.kunectron.builder.GuiBuilder
import com.tksimeji.kunectron.hooks.ScoreboardGuiHooks
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.lifequest.data.PanelConfig
import net.azisaba.lifequest.quest.Quest
import net.azisaba.lifequest.quest.QuestState
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Manages quest scoreboard panels displayed to players.
 * Injectable via Dagger; legacy static methods provided in companion.
 */
@Singleton
class QuestPanelGui
    @Inject
    constructor(
        private val plugin: Plugin,
        private val panelConfig: PanelConfig,
    ) {
        private val activePanels = mutableMapOf<Player, ScoreboardGuiHooks>()

        fun show(
            player: Player,
            quest: Quest,
        ) {
            val hooks =
                GuiBuilder
                    .scoreboard()
                    .title(Component.text(panelConfig.title))
                    .line(Component.text(""))
                    .line(Component.text("§6Progress: §e${quest.type.title}"))
                    .line(Component.text("  §7${quest.progresses.totalProgress} / ${quest.progresses.totalRequired}"))
                    .line(Component.text(""))
                    .line(Component.text("§eParty (${quest.players.size}):"))
                    .build()

            quest.players.forEach { member ->
                val hp = member.health.toInt()
                val line =
                    Component
                        .text()
                        .append(Component.text("  ", NamedTextColor.GRAY))
                        .append(Component.text("${member.name} ", NamedTextColor.AQUA))
                        .append(Component.text("♥$hp", NamedTextColor.RED))
                        .build()
                hooks.useAddLine(line)
            }

            hooks.useAddLine(Component.text(""))
            hooks.useAddLine(Component.text("§7${quest.type.description.firstOrNull() ?: ""}"))
            hooks.useAddLine(Component.text(""))
            hooks.useAddLine(Component.text(panelConfig.footer))

            hooks.useAddPlayer(player)
            activePanels[player] = hooks

            val taskId =
                Bukkit
                    .getScheduler()
                    .runTaskTimer(
                        plugin,
                        Runnable {
                            if (quest.state != QuestState.ACTIVE) {
                                hooks.useClose()
                                activePanels.remove(player)
                                return@Runnable
                            }
                            hooks.useLine(2, Component.text("  §7${quest.progresses.totalProgress} / ${quest.progresses.totalRequired}"))
                        },
                        5L,
                        5L,
                    ).taskId

            Bukkit.getScheduler().runTaskLater(
                plugin,
                Runnable {
                    Bukkit.getScheduler().cancelTask(taskId)
                },
                20L * 60L * 5L,
            )
        }

        fun hide(player: Player) {
            activePanels[player]?.useClose()
            activePanels.remove(player)
        }
    }
