package net.azisaba.lifequest.stage

import net.azisaba.lifequest.party.Party

class Queue(
    override val stage: Stage,
) : StageLike {
    private val _queue = mutableListOf<Party>()

    val first: Party? get() = _queue.firstOrNull()
    val size: Int get() = _queue.size

    fun add(party: Party): Boolean =
        if (stage.parties.size < stage.maxParties) {
            stage.mount(party)
            true
        } else {
            _queue.add(party)
            party.stage = this
            false
        }

    fun remove(party: Party) {
        _queue.remove(party)
        if (party.stage == this) party.stage = null
    }

    fun indexOf(party: Party): Int = _queue.indexOf(party)
}
