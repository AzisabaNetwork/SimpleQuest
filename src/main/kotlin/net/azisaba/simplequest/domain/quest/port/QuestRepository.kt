package net.azisaba.simplequest.domain.quest.port

import java.time.Instant

/**
 * Repository for querying quest completion and player progress data.
 * Implemented in the infrastructure layer (database).
 */
interface QuestRepository {
    /** Returns the timestamp of the last completion, or null if never completed. */
    fun getLastCompletionTime(
        playerId: String,
        questKey: String,
    ): Instant?

    /** Returns the number of times [playerId] has completed [questKey] since [since]. */
    fun getCompletionsSince(
        playerId: String,
        questKey: String,
        since: Instant,
    ): Int

    /** Returns true if [playerId] has the quest type [questKey] granted. */
    fun isGranted(
        playerId: String,
        questKey: String,
    ): Boolean

    /** Grants [questKey] to [playerId]. */
    fun grant(
        playerId: String,
        questKey: String,
    )

    /** Revokes [questKey] from [playerId]. */
    fun revoke(
        playerId: String,
        questKey: String,
    )

    /** Returns the number of plays of [questKey] by [playerId]. */
    fun getPlays(
        playerId: String,
        questKey: String,
    ): Int

    fun getDailyCompletions(
        playerId: String,
        questKey: String,
    ): Int

    fun getWeeklyCompletions(
        playerId: String,
        questKey: String,
    ): Int

    fun getMonthlyCompletions(
        playerId: String,
        questKey: String,
    ): Int

    fun getYearlyCompletions(
        playerId: String,
        questKey: String,
    ): Int
}
