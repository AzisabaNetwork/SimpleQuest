package net.azisaba.lifequest.quest

class Progresses(
    private val requirements: Map<String, QuestRequirement>,
    private val onComplete: () -> Unit,
) {
    private val progress: MutableMap<String, Int> = mutableMapOf()

    operator fun get(key: String): Int = progress[key] ?: 0

    operator fun set(
        key: String,
        value: Int,
    ) {
        progress[key] = value.coerceAtLeast(0)
        checkCompletion()
    }

    fun loadFrom(saved: Map<String, Int>) {
        progress.putAll(saved)
        checkCompletion()
    }

    private fun checkCompletion() {
        if (isComplete) onComplete()
    }

    val totalProgress: Int get() = progress.values.sum()
    val totalRequired: Int get() = requirements.values.sumOf { it.amount }
    val isComplete: Boolean
        get() =
            requirements.all { (key, req) ->
                (progress[key] ?: 0) >= req.amount
            }
}
