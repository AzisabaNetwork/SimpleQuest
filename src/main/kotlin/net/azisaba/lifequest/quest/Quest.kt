package net.azisaba.lifequest.quest

import net.azisaba.lifequest.party.Party
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
