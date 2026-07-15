package net.azisaba.lifequest.infrastructure.persistence

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.lifequest.database.repository.PlayerQuestTypeRepository
import net.azisaba.lifequest.domain.quest.port.QuestRepository
import java.time.Instant
import java.util.UUID

/**
 * Infrastructure implementation of [QuestRepository] using the existing
 * [PlayerQuestTypeRepository] for database access.
 */
@Singleton
class QuestRepositoryImpl
    @Inject
    constructor(
        private val playerQuestTypeRepository: PlayerQuestTypeRepository,
    ) : QuestRepository {
        override fun getCompletionsSince(
            playerId: String,
            questKey: String,
            since: Instant,
        ): Int =
            playerQuestTypeRepository.getCompletionsSince(
                UUID.fromString(playerId),
                questKey,
                since,
            )

        override fun isGranted(
            playerId: String,
            questKey: String,
        ): Boolean = playerQuestTypeRepository.isGranted(UUID.fromString(playerId), questKey)

        override fun grant(
            playerId: String,
            questKey: String,
        ) = playerQuestTypeRepository.grant(UUID.fromString(playerId), questKey)

        override fun revoke(
            playerId: String,
            questKey: String,
        ) = playerQuestTypeRepository.revoke(UUID.fromString(playerId), questKey)

        override fun getPlays(
            playerId: String,
            questKey: String,
        ): Int = playerQuestTypeRepository.getPlays(UUID.fromString(playerId), questKey)

        override fun getWeeklyCompletions(
            playerId: String,
            questKey: String,
        ): Int = playerQuestTypeRepository.getWeeklyCompletions(UUID.fromString(playerId), questKey)

        override fun getMonthlyCompletions(
            playerId: String,
            questKey: String,
        ): Int = playerQuestTypeRepository.getMonthlyCompletions(UUID.fromString(playerId), questKey)

        override fun getYearlyCompletions(
            playerId: String,
            questKey: String,
        ): Int = playerQuestTypeRepository.getYearlyCompletions(UUID.fromString(playerId), questKey)

        override fun isFirstCompletion(
            playerId: String,
            questKey: String,
        ): Boolean = playerQuestTypeRepository.isFirstCompletion(UUID.fromString(playerId), questKey)
    }
