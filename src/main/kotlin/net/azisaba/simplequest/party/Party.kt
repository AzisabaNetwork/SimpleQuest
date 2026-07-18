package net.azisaba.simplequest.party

import net.azisaba.simplequest.SimpleQuest
import net.azisaba.simplequest.quest.Quest
import net.azisaba.simplequest.quest.QuestType
import net.azisaba.simplequest.stage.StageLike
import org.bukkit.entity.Player

interface Party : Iterable<Player> {
    var quest: Quest?
    var stage: StageLike?
    var leader: Player
    val members: Set<Player>
    val size: Int
    var invitationSetting: InvitationSetting

    fun addMember(player: Player)

    fun removeMember(player: Player)

    fun hasPermission(type: QuestType): Boolean

    companion object {
        val MAX_SIZE: Int get() = SimpleQuest.plugin.configData.maxPartySize

        fun create(leader: Player): Party = PartyImpl(leader)

        fun solo(player: Player): Party = SoloPartyImpl(player)

        val Player.party: Party? get() = PartyManager.getParty(this)
    }
}
