package net.azisaba.lifequest.domain.registry

/**
 * Interface for objects that can be registered in a [Registry] by key.
 */
interface Keyed {
    val key: String
}
