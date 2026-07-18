package net.azisaba.simplequest.domain.party.model

import net.azisaba.simplequest.domain.quest.model.QuestType

/**
 * Aggregate root representing a party of players working together on quests.
 */
interface Party {
    val leaderId: String
    val memberIds: Set<String>
    val size: Int
    val invitationSetting: InvitationSetting

    fun hasPermission(type: QuestType): Boolean
}
