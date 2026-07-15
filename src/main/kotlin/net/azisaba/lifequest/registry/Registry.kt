package net.azisaba.lifequest.registry

import net.kyori.adventure.key.Key

open class Registry<T : Keyed> {
    private val map = linkedMapOf<Key, T>()

    fun register(entry: T): T {
        map[entry.key] = entry
        return entry
    }

    fun get(key: Key): T? = map[key]

    fun unregister(key: Key): T? = map.remove(key)

    fun clear() {
        map.clear()
    }

    val entries: Collection<T> get() = map.values

    val keys: Set<Key> get() = map.keys

    val size: Int get() = map.size
}
