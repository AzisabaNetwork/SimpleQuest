package net.azisaba.simplequest.domain.registry

/**
 * A generic registry that maps keys to [Keyed] entries, preserving insertion order.
 */
open class Registry<T : Keyed> {
    private val map = linkedMapOf<String, T>()

    fun register(entry: T): T {
        map[entry.key] = entry
        return entry
    }

    fun get(key: String): T? = map[key]

    fun unregister(key: String): T? = map.remove(key)

    fun clear() {
        map.clear()
    }

    val entries: Collection<T> get() = map.values
    val keys: Set<String> get() = map.keys
    val size: Int get() = map.size
}
