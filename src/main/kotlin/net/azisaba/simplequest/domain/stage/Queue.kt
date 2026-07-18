package net.azisaba.simplequest.domain.stage

/**
 * A queue of stage-like elements to be processed sequentially.
 */
data class Queue<T : StageLike>(
    val items: MutableList<T> = mutableListOf(),
) {
    val size: Int get() = items.size
    val isEmpty: Boolean get() = items.isEmpty()

    fun enqueue(item: T) {
        items.add(item)
    }

    fun dequeue(): T? = if (items.isNotEmpty()) items.removeFirst() else null

    fun peek(): T? = items.firstOrNull()

    fun clear() {
        items.forEach { it.unmount() }
        items.clear()
    }
}
