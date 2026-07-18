package net.azisaba.simplequest.application.quest

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.domain.action.port.ActionDispatcher
import net.azisaba.simplequest.domain.quest.model.EndReason
import net.azisaba.simplequest.domain.quest.model.QuestResult
import net.azisaba.simplequest.domain.quest.model.QuestState
import net.azisaba.simplequest.domain.quest.model.QuestType
import net.azisaba.simplequest.domain.quest.port.QuestNotifier
import net.azisaba.simplequest.domain.quest.port.QuestRepository
import net.azisaba.simplequest.domain.script.port.ScriptRunner
import java.time.Instant
import net.azisaba.simplequest.domain.party.model.Party as DomainParty
import net.azisaba.simplequest.domain.quest.model.Quest as DomainQuest
import net.azisaba.simplequest.domain.script.Script as DomainScript

/**
 * Application service for the Quest bounded context.
 * Orchestrates quest lifecycle: start, progress, end, and player tracking.
 */
@Singleton
class QuestService
    @Inject
    constructor(
        private val questRepository: QuestRepository,
        private val actionDispatcher: ActionDispatcher,
        private val scriptRunner: ScriptRunner,
        private val questNotifier: QuestNotifier,
    ) {
        // questKey -> DomainQuest
        private val activeQuests = mutableMapOf<String, DomainQuest>()

        // playerId -> questKey  (reverse index for lookup)
        private val playerQuests = mutableMapOf<String, String>()

        /**
         * Attempts to start a quest for the given party members.
         */
        fun startQuest(
            type: QuestType,
            party: DomainParty,
            playerIds: List<String>,
        ): QuestResult {
            if (!party.hasPermission(type)) {
                return QuestResult.Failure("Party does not meet requirements")
            }
            for (playerId in playerIds) {
                if (!hasPermission(playerId, type)) {
                    return QuestResult.Failure("Player $playerId has not unlocked this quest")
                }
            }

            val quest = createQuest(type, playerIds)
            activeQuests[type.key] = quest
            playerIds.forEach { playerQuests[it] = type.key }
            quest.start()

            playerIds.forEach { questNotifier.showQuestPanel(it, type.key) }
            return QuestResult.Success(quest)
        }

        /**
         * Ends a quest with the given reason.
         */
        fun endQuest(
            quest: DomainQuest,
            reason: EndReason,
        ) {
            if (quest.state != QuestState.ACTIVE) return
            quest.end(reason)
            activeQuests.remove(quest.type.key)
            // Remove player mappings
            playerQuests.entries.removeAll { it.value == quest.type.key }

            runQuestCompletion(quest, reason)
        }

        /**
         * Cancels all active quests.
         */
        fun cancelAll(reason: EndReason = EndReason.PLUGIN) {
            activeQuests.values.toList().forEach { endQuest(it, reason) }
        }

        /**
         * Updates quest progress for a specific requirement.
         */
        fun updateProgress(
            quest: DomainQuest,
            reqKey: String,
            delta: Int,
        ) {
            quest.updateProgress(reqKey, delta)
        }

        /**
         * Returns the active quest for a player, or null if none.
         */
        fun getQuestByPlayerId(playerId: String): DomainQuest? {
            val questKey = playerQuests[playerId] ?: return null
            return activeQuests[questKey]
        }

        /**
         * Checks whether a player has permission to start the given quest type.
         */
        fun hasPermission(
            playerId: String,
            type: QuestType,
        ): Boolean {
            val key = type.key
            if (!questRepository.isGranted(playerId, key)) return false

            val limits = type.playLimits
            if (limits.lifetime != null &&
                questRepository.getCompletionsSince(playerId, key, Instant.EPOCH) >= limits.lifetime
            ) {
                return false
            }
            if (limits.weekly != null &&
                questRepository.getWeeklyCompletions(playerId, key) >= limits.weekly
            ) {
                return false
            }
            if (limits.monthly != null &&
                questRepository.getMonthlyCompletions(playerId, key) >= limits.monthly
            ) {
                return false
            }
            if (limits.yearly != null &&
                questRepository.getYearlyCompletions(playerId, key) >= limits.yearly
            ) {
                return false
            }

            return true
        }

        val activeQuestCount: Int get() = activeQuests.size

        /** Grants a quest type to a player. */
        fun grantQuest(
            playerId: String,
            questKey: String,
        ) {
            questRepository.grant(playerId, questKey)
        }

        /** Revokes a quest type from a player. */
        fun revokeQuest(
            playerId: String,
            questKey: String,
        ) {
            questRepository.revoke(playerId, questKey)
        }

        // ---- private ----

        private fun createQuest(
            type: QuestType,
            playerIds: List<String>,
        ): DomainQuest = QuestInstance(type, playerIds)

        private fun runQuestCompletion(
            quest: DomainQuest,
            reason: EndReason,
        ) {
            val type = quest.type
            val playerIds = playerQuests.filter { it.value == type.key }.keys.toList()
            when (reason) {
                EndReason.COMPLETE -> {
                    type.scripts
                        .filter { it.trigger == DomainScript.Trigger.COMPLETE }
                        .forEach { scriptRunner.run(it, playerIds) }
                    val actions = type.actions?.onComplete ?: emptyList()
                    actionDispatcher.dispatchAll(actions, playerIds)
                }

                EndReason.CANCEL -> {
                    type.scripts
                        .filter { it.trigger == DomainScript.Trigger.CANCEL }
                        .forEach { scriptRunner.run(it, playerIds) }
                }

                else -> {}
            }
        }
    }

/**
 * Simple in-memory domain Quest implementation.
 */
private class QuestInstance(
    override val type: net.azisaba.simplequest.domain.quest.model.QuestType,
    playerIds: List<String>,
) : DomainQuest {
    private val _playerIds = playerIds.toMutableList()

    override val state: QuestState
        get() = _state
    override val progresses: net.azisaba.simplequest.domain.quest.model.Progresses =
        net.azisaba.simplequest.domain.quest.model
            .Progresses(type.requirements)

    private var _state: QuestState = QuestState.ACTIVE

    override fun start() {
        _state = QuestState.ACTIVE
    }

    override fun end(reason: EndReason) {
        if (_state != QuestState.ACTIVE) return
        _state = reason.toQuestState()
    }

    override fun updateProgress(
        reqKey: String,
        delta: Int,
    ) {
        progresses.addDelta(reqKey, delta)
    }
}
