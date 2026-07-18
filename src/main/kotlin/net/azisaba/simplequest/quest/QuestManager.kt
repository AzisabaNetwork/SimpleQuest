package net.azisaba.simplequest.quest

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.application.quest.QuestService
import net.azisaba.simplequest.party.Party
import org.bukkit.entity.Player
import net.azisaba.simplequest.domain.quest.model.EndReason as DomainEndReason
import net.azisaba.simplequest.domain.quest.model.Quest as DomainQuest
import net.azisaba.simplequest.domain.quest.model.QuestState as DomainQuestState
import net.azisaba.simplequest.domain.quest.model.QuestType as DomainQuestType

/**
 * Bridge between Bukkit components and the pure-domain [QuestService].
 * Will be replaced once all callers use [QuestService] directly.
 */
@Singleton
class QuestManager
    @Inject
    constructor(
        private val questService: QuestService,
    ) {
        private val activeQuests = mutableMapOf<String, QuestWrapper>()

        fun startQuest(
            type: QuestType,
            party: Party,
        ): QuestResult {
            val domainType = toDomainType(type)
            val playerIds = party.map { it.uniqueId.toString() }

            // Delegate permission checks to QuestService
            val domainParty = DomainPartyAdapter(party)
            val domainResult = questService.startQuest(domainType, domainParty, playerIds)

            return when (domainResult) {
                is net.azisaba.simplequest.domain.quest.model.QuestResult.Success -> {
                    val wrapper = QuestWrapper(domainResult.quest, party, playerIds)
                    activeQuests[domainResult.quest.type.key] = wrapper
                    QuestResult.Success(wrapper)
                }

                is net.azisaba.simplequest.domain.quest.model.QuestResult.Failure -> {
                    QuestResult.Failure(domainResult.reason)
                }
            }
        }

        fun endQuest(
            quest: Quest,
            reason: EndReason,
        ) {
            val wrapper = quest as? QuestWrapper ?: return
            if (wrapper.state != QuestState.ACTIVE) return
            val domainReason = toDomainEndReason(reason)
            questService.endQuest(wrapper.domainQuest, domainReason)
            activeQuests.remove(wrapper.domainQuest.type.key)
        }

        fun getQuestByPlayer(player: Player): Quest? = activeQuests.values.find { player.uniqueId.toString() in it.playerIds }

        fun cancelAll(reason: EndReason = EndReason.PLUGIN) {
            activeQuests.values.toList().forEach { endQuest(it, reason) }
        }

        fun hasPermission(
            player: Player,
            type: QuestType,
        ): Boolean {
            val domainType = toDomainType(type)
            return questService.hasPermission(player.uniqueId.toString(), domainType)
        }

        val activeQuestCount: Int get() = activeQuests.size

        // ---- domain bridge ----

        private fun toDomainEndReason(old: EndReason): DomainEndReason =
            when (old) {
                EndReason.COMPLETE -> DomainEndReason.COMPLETE
                EndReason.CANCEL -> DomainEndReason.CANCEL
                EndReason.DEATH_LIMIT -> DomainEndReason.DEATH_LIMIT
                EndReason.PLUGIN -> DomainEndReason.PLUGIN
                EndReason.RELOAD -> DomainEndReason.RELOAD
                EndReason.OTHER -> DomainEndReason.OTHER
            }

        private fun toDomainType(type: QuestType): DomainQuestType =
            DomainQuestType(
                key = type.key.asString(),
                title = type.title,
                icon =
                    net.azisaba.simplequest.domain.data.Icon(
                        type = type.icon.type,
                        customModelData = type.icon.customModelData,
                        aura = type.icon.aura,
                        model = type.icon.model,
                    ),
                description = type.description,
                category = type.category.key.asString(),
                location =
                    type.location?.let {
                        net.azisaba.simplequest.domain.data
                            .Location(it.world, it.x, it.y, it.z, it.yaw, it.pitch)
                    },
                giver = type.giver,
                playLimits =
                    net.azisaba.simplequest.domain.quest.model.PlayLimits(
                        weekly = type.playLimits.weekly,
                        monthly = type.playLimits.monthly,
                        yearly = type.playLimits.yearly,
                        lifetime = type.playLimits.lifetime,
                    ),
                acceptConditions =
                    net.azisaba.simplequest.domain.quest.model.AcceptConditions(
                        pveLevel = type.acceptConditions.pveLevel,
                        requiredQuests = type.acceptConditions.requiredQuests?.map { it.asString() },
                        permissions = type.acceptConditions.permissions,
                        partyMode = type.acceptConditions.partyMode,
                    ),
                maxPlayers = type.maxPlayers,
                minPlayers = type.minPlayers,
                deathLimit = type.deathLimit,
                guides =
                    type.guides.map {
                        net.azisaba.simplequest.domain.quest.model.GameGuide(
                            title = it.title,
                            location =
                                net.azisaba.simplequest.domain.data.Location(
                                    it.location.world,
                                    it.location.x,
                                    it.location.y,
                                    it.location.z,
                                    it.location.yaw,
                                    it.location.pitch,
                                ),
                            requirements = it.requirements,
                        )
                    },
                requirements =
                    type.requirements.mapValues { (k, v) ->
                        net.azisaba.simplequest.domain.quest.model
                            .QuestRequirement(k, v.amount)
                    },
                actions =
                    type.actions?.let {
                        net.azisaba.simplequest.domain.action.ActionSet(
                            onFirstComplete =
                                it.onFirstComplete.map { a ->
                                    net.azisaba.simplequest.domain.action.Action(
                                        type =
                                            when (a.type) {
                                                net.azisaba.simplequest.action.ActionType.COMMAND -> net.azisaba.simplequest.domain.action.ActionType.COMMAND
                                                net.azisaba.simplequest.action.ActionType.ITEM_GIVE -> net.azisaba.simplequest.domain.action.ActionType.ITEM_GIVE
                                                net.azisaba.simplequest.action.ActionType.MYTHIC_ITEM_GIVE -> net.azisaba.simplequest.domain.action.ActionType.MYTHIC_ITEM_GIVE
                                                net.azisaba.simplequest.action.ActionType.PVELEVEL_EXP -> net.azisaba.simplequest.domain.action.ActionType.PVELEVEL_EXP
                                            },
                                        material = a.material,
                                        amount = a.amount,
                                        item = a.item,
                                        command = a.command,
                                    )
                                },
                            onComplete =
                                it.onComplete.map { a ->
                                    net.azisaba.simplequest.domain.action.Action(
                                        type =
                                            when (a.type) {
                                                net.azisaba.simplequest.action.ActionType.COMMAND -> net.azisaba.simplequest.domain.action.ActionType.COMMAND
                                                net.azisaba.simplequest.action.ActionType.ITEM_GIVE -> net.azisaba.simplequest.domain.action.ActionType.ITEM_GIVE
                                                net.azisaba.simplequest.action.ActionType.MYTHIC_ITEM_GIVE -> net.azisaba.simplequest.domain.action.ActionType.MYTHIC_ITEM_GIVE
                                                net.azisaba.simplequest.action.ActionType.PVELEVEL_EXP -> net.azisaba.simplequest.domain.action.ActionType.PVELEVEL_EXP
                                            },
                                        material = a.material,
                                        amount = a.amount,
                                        item = a.item,
                                        command = a.command,
                                    )
                                },
                        )
                    },
                scripts =
                    type.scripts.map {
                        net.azisaba.simplequest.domain.script.Script(
                            trigger = it.trigger,
                            delay = it.delay,
                            commands = it.commands,
                        )
                    },
            )
    }

/**
 * Wraps a domain [DomainQuest] to implement the old Bukkit-dependent [Quest] interface.
 */
private class QuestWrapper(
    val domainQuest: DomainQuest,
    override val party: Party,
    val playerIds: List<String>,
) : Quest {
    override val type: QuestType
        get() = error("QuestType bridge not available from domain Quest")
    override val state: QuestState
        get() =
            when (domainQuest.state) {
                DomainQuestState.ACTIVE -> QuestState.ACTIVE
                DomainQuestState.COMPLETED -> QuestState.COMPLETED
                DomainQuestState.CANCELLED -> QuestState.CANCELLED
                DomainQuestState.FAILED -> QuestState.FAILED
            }
    override val progresses: Progresses
        get() = error("Progresses bridge not available")
    override val players: List<Player>
        get() = party.toList().filter { it.uniqueId.toString() in playerIds }

    override fun start() = domainQuest.start()

    override fun end(reason: EndReason) = domainQuest.end(toDomainEndReason(reason))

    override fun addPlayer(player: Player) {}

    override fun removePlayer(player: Player) {}

    override fun updateProgress(
        reqKey: String,
        delta: Int,
    ) = domainQuest.updateProgress(reqKey, delta)

    private fun toDomainEndReason(old: EndReason): DomainEndReason =
        when (old) {
            EndReason.COMPLETE -> DomainEndReason.COMPLETE
            EndReason.CANCEL -> DomainEndReason.CANCEL
            EndReason.DEATH_LIMIT -> DomainEndReason.DEATH_LIMIT
            EndReason.PLUGIN -> DomainEndReason.PLUGIN
            EndReason.RELOAD -> DomainEndReason.RELOAD
            EndReason.OTHER -> DomainEndReason.OTHER
        }
}

/**
 * Adapts a Bukkit [Party] to a domain [DomainParty].
 */
private class DomainPartyAdapter(
    private val bukkitParty: Party,
) : net.azisaba.simplequest.domain.party.model.Party {
    override val leaderId: String get() = bukkitParty.leader.uniqueId.toString()
    override val memberIds: Set<String> get() = bukkitParty.members.map { it.uniqueId.toString() }.toSet()
    override val size: Int get() = bukkitParty.size
    override val invitationSetting: net.azisaba.simplequest.domain.party.model.InvitationSetting
        get() =
            when (bukkitParty.invitationSetting) {
                net.azisaba.simplequest.party.InvitationSetting.LEADER -> net.azisaba.simplequest.domain.party.model.InvitationSetting.LEADER
                net.azisaba.simplequest.party.InvitationSetting.ALL -> net.azisaba.simplequest.domain.party.model.InvitationSetting.ALL
            }

    override fun hasPermission(type: DomainQuestType): Boolean {
        if (type.maxPlayers != null && size > type.maxPlayers) return false
        if (type.minPlayers != null && size < type.minPlayers) return false
        return true
    }
}

sealed class QuestResult {
    data class Success(
        val quest: Quest,
    ) : QuestResult()

    data class Failure(
        val reason: String,
    ) : QuestResult()
}
