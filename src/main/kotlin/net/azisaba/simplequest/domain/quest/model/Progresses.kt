package net.azisaba.simplequest.domain.quest.model

/**
 * Tracks progress toward quest objective requirements.
 *
 * Pure domain value object — no side effects, no Bukkit dependencies.
 */
class Progresses(
    requirements: Map<String, QuestRequirement>,
) {
    private val data: MutableMap<String, Int> =
        requirements
            .mapValues { (_, req) ->
                if (req.amount == 0) req.amount else 0
            }.toMutableMap()

    private val required: Map<String, Int> =
        requirements.mapValues { (_, req) -> req.amount }

    /** Returns the current progress for a requirement key. */
    operator fun get(key: String): Int = data[key] ?: 0

    /** Sets the progress for a requirement key (clamped to >= 0). */
    operator fun set(
        key: String,
        value: Int,
    ) {
        if (data.containsKey(key)) {
            data[key] = value.coerceAtLeast(0)
        }
    }

    /** Adds a delta to the progress for a requirement key. */
    fun addDelta(
        key: String,
        delta: Int,
    ) {
        if (data.containsKey(key)) {
            data[key] = (data[key]!! + delta).coerceAtLeast(0)
        }
    }

    /** Returns true if all requirements are met. */
    val isComplete: Boolean
        get() = required.all { (key, amount) -> (data[key] ?: 0) >= amount }

    /** The sum of all current progress values. */
    val totalProgress: Int
        get() = data.values.sum()

    /** The sum of all required amounts. */
    val totalRequired: Int
        get() = required.values.sum()

    /** Loads progress from a saved map. Only loads keys that exist in requirements. */
    fun loadFrom(saved: Map<String, Int>) {
        saved.forEach { (key, value) ->
            if (data.containsKey(key)) {
                data[key] = value.coerceAtLeast(0)
            }
        }
    }

    /** Returns an immutable snapshot of current progress. */
    fun snapshot(): Map<String, Int> = data.toMap()

    /** Resets all progress to zero. */
    fun reset() {
        data.keys.forEach { data[it] = 0 }
    }
}
