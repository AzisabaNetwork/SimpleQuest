package net.azisaba.simplequest.domain.action

/**
 * Types of actions that can be executed as quest rewards.
 */
enum class ActionType {
    COMMAND,
    ITEM_GIVE,
    MYTHIC_ITEM_GIVE,
    PVELEVEL_EXP,

    /** Picks one of several candidate items at random */
    RANDOM_ITEM,
}
