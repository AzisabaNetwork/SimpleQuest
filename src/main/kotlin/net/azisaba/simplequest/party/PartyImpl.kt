package net.azisaba.simplequest.party

import net.azisaba.simplequest.SimpleQuest
import net.azisaba.simplequest.quest.EndReason
import net.azisaba.simplequest.quest.Quest
import net.azisaba.simplequest.quest.QuestManager
import net.azisaba.simplequest.quest.QuestType
import net.azisaba.simplequest.stage.StageLike
import org.bukkit.entity.Player

open class PartyImpl(
    override var leader: Player,
    private val questManager: QuestManager? = null,
) : Party {
    private val _members = mutableSetOf<Player>()
    override val members: Set<Player> get() = _members.toSet()
    override val size: Int get() = _members.size + 1

    override var quest: Quest? = null
    override var stage: StageLike? = null
    override var invitationSetting: InvitationSetting = InvitationSetting.LEADER

    override fun addMember(player: Player) {
        require(size < Party.MAX_SIZE) { "Party is full" }
        _members.add(player)
        PartyManager.setParty(player, this)
    }

    override fun removeMember(player: Player) {
        _members.remove(player)
        PartyManager.removePlayer(player)
        if (player == leader || _members.isEmpty()) disband()
    }

    private fun disband() {
        val qm = questManager ?: SimpleQuest.plugin.questManager
        quest?.let { qm.endQuest(it, EndReason.OTHER) }
        forEach { PartyManager.removePlayer(it) }
        _members.clear()
    }

    override fun hasPermission(type: QuestType): Boolean {
        if (type.maxPlayers != null && size > type.maxPlayers) return false
        if (type.minPlayers != null && size < type.minPlayers) return false
        return true
    }

    override fun iterator(): Iterator<Player> = (listOf(leader) + _members).iterator()
}
