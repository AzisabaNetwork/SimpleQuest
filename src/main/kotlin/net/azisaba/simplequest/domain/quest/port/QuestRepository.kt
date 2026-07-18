package net.azisaba.simplequest.domain.quest.port

/**
 * Repository for querying quest completion and player progress data.
 * Implemented in the infrastructure layer (database).
 */
interface QuestRepository {
    /** Returns the number of times [playerId] has completed [questKey] since [since]. */
    fun getCompletionsSince(
        playerId: String,
        questKey: String,
        since: java.time.Instant,
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

    fun isFirstCompletion(
        playerId: String,
        questKey: String,
    ): Boolean
}
