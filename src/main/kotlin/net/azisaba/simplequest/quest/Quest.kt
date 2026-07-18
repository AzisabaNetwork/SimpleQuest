package net.azisaba.simplequest.quest

import net.azisaba.simplequest.party.Party
import org.bukkit.entity.Player

interface Quest {
    val type: QuestType
    val party: Party
    val state: QuestState
    val players: List<Player>
    val progresses: Progresses

    fun start()

    fun end(reason: EndReason)

    fun addPlayer(player: Player)

    fun removePlayer(player: Player)

    fun updateProgress(
        reqKey: String,
        delta: Int,
    )
}
