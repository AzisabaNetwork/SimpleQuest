package net.azisaba.simplequest.stage

import net.azisaba.simplequest.domain.data.Location
import net.azisaba.simplequest.party.Party
import net.azisaba.simplequest.registry.Keyed
import net.kyori.adventure.key.Key

class Stage(
    override val key: Key,
    val title: String,
    val location: Location,
    val unmountLocation: Location? = null,
    val maxParties: Int = 1,
) : StageLike,
    Keyed {
    override val stage: Stage get() = this
    val queue = Queue(this)

    private val _parties = mutableListOf<Party>()
    val parties: List<Party> get() = _parties.toList()

    fun mount(party: Party) {
        require(party !in _parties) { "Party already mounted" }
        require(_parties.size < maxParties) { "No available slots" }
        _parties.add(party)
        party.stage = this
        party.forEach { player ->
            player.teleport(
                org.bukkit.Location(
                    org.bukkit.Bukkit.getWorld(location.world),
                    location.x,
                    location.y,
                    location.z,
                    location.yaw,
                    location.pitch,
                ),
            )
        }
    }

    fun unmount(party: Party) {
        require(party in _parties) { "Party not mounted" }
        _parties.remove(party)
        party.stage = null
        party.forEach { player ->
            val dest = unmountLocation
            if (dest != null) {
                player.teleport(
                    org.bukkit.Location(
                        org.bukkit.Bukkit.getWorld(dest.world),
                        dest.x,
                        dest.y,
                        dest.z,
                        dest.yaw,
                        dest.pitch,
                    ),
                )
            }
        }
        queue.first?.let { mount(it) }
    }
}
