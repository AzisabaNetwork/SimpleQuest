package net.azisaba.simplequest.domain.registry

/**
 * Interface for objects that can be registered in a [Registry] by key.
 */
interface Keyed {
    val key: String
}
