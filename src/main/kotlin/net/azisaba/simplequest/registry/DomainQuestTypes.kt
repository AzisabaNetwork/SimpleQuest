package net.azisaba.simplequest.registry

import net.azisaba.simplequest.domain.quest.model.QuestType

/**
 * In-memory registry of domain [QuestType] definitions.
 * Replaces the old [QuestTypes] registry that used Adventure [Key] objects.
 */
object DomainQuestTypes {
    private val map = linkedMapOf<String, QuestType>()

    fun register(type: QuestType) {
        map[type.key] = type
    }

    fun get(key: String): QuestType? = map[key]

    fun unregister(key: String): QuestType? = map.remove(key)

    fun clear() {
        map.clear()
    }

    val entries: Collection<QuestType> get() = map.values
    val keys: Set<String> get() = map.keys
    val size: Int get() = map.size
}
