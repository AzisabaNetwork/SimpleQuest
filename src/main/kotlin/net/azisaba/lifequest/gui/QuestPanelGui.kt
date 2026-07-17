// Original: https://github.com/AzisabaNetwork/quem by tksimeji
// Adapted for LifeQuest

package net.azisaba.lifequest.gui

import com.tksimeji.kunectron.ScoreboardGui
import com.tksimeji.kunectron.event.GuiHandler
import com.tksimeji.kunectron.event.ScoreboardGuiEvents
import com.tksimeji.kunectron.hooks.ScoreboardGuiHooks
import net.azisaba.lifequest.LifeQuest
import net.azisaba.lifequest.quest.Quest
import net.azisaba.lifequest.stage.Stage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

@ScoreboardGui
class QuestPanelGui(
    private val quest: Quest,
) : ScoreboardGuiHooks {
    private val party = quest.party
    private val members = quest.players.toList()
    private var staged = false

    @ScoreboardGui.Title
    private val title = Component.text(LifeQuest.plugin.configData.panel.title)

    @ScoreboardGui.Line(index = 1)
    private val line2 =
        Component
            .text("Active: ")
            .color(NamedTextColor.GRAY)
            .append(quest.type.title.let { t -> Component.text(t).color(NamedTextColor.WHITE) })

    @ScoreboardGui.Line(index = 2)
    private val line3 =
        Component
            .text("Progress: ")
            .color(NamedTextColor.GRAY)
            .append(Component.text(quest.progresses.totalProgress.toString()).color(NamedTextColor.GREEN))
            .append(Component.text("/").color(NamedTextColor.DARK_GRAY))
            .append(Component.text(quest.progresses.totalRequired.toString()).color(NamedTextColor.WHITE))

    @ScoreboardGui.Line(index = 4)
    private val line5 =
        Component
            .text("Party (${quest.players.size}):")
            .color(NamedTextColor.GRAY)

    @GuiHandler
    fun onTick(event: ScoreboardGuiEvents.TickEvent) {
        var index = 5

        for (member in members) {
            val isPlayer = quest.players.contains(member)
            val component =
                Component
                    .space()
                    .append(
                        Component
                            .text(member.name)
                            .color(if (isPlayer) NamedTextColor.AQUA else NamedTextColor.RED)
                            .decoration(
                                TextDecoration.STRIKETHROUGH,
                                if (isPlayer) TextDecoration.State.NOT_SET else TextDecoration.State.TRUE,
                            ),
                    ).appendSpace()
                    .append(
                        when {
                            isPlayer -> {
                                val health = member.health.toInt()
                                val healthColor =
                                    when {
                                        health <= 4 -> NamedTextColor.RED
                                        health <= 8 -> NamedTextColor.GOLD
                                        else -> NamedTextColor.GREEN
                                    }
                                Component
                                    .text(health)
                                    .color(healthColor)
                                    .append(Component.text("\u2665").color(NamedTextColor.RED))
                            }

                            !party.members.contains(member) && party.leader != member -> {
                                Component.text("Left").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                            }

                            else -> {
                                Component.text("Dead").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                            }
                        },
                    )
            useLine(index++, component)
        }

        index++

        if (staged && party.stage == null) {
            useRemoveLines()
            useLine(1, line2)
            useLine(2, line3)
            useLine(4, line5)
        }

        if (party.stage != null) {
            val stage = party.stage!!

            if (stage is Stage) {
                useLine(
                    index,
                    Component
                        .text("Stage: ")
                        .color(NamedTextColor.GRAY)
                        .append(Component.text(stage.title).color(NamedTextColor.WHITE)),
                )
            }

            staged = true
            index += 2
        } else {
            staged = false
        }

        useLine(index, Component.text(LifeQuest.plugin.configData.panel.footer))
    }
}
