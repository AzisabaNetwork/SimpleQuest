package net.azisaba.simplequest.party

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.data.SimpleQuestConfig
import org.bukkit.Server
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages party invitations with expiration.
 * Injectable via Dagger.
 */
@Singleton
class InviteManager
    @Inject
    constructor(
        private val server: Server,
        private val configData: SimpleQuestConfig,
    ) {
        private val invites = ConcurrentHashMap<UUID, Invite>()

        fun createInvite(
            party: Party,
            sender: Player,
            target: Player,
        ): Invite {
            val expireTick = server.currentTick.toLong() + configData.partyInviteLimit
            val invite = Invite(party, sender, target, expireTick)
            invites[invite.id] = invite
            return invite
        }

        fun acceptInvite(
            player: Player,
            inviteIdStr: String,
        ): Boolean {
            val uuid =
                try {
                    UUID.fromString(inviteIdStr)
                } catch (_: Exception) {
                    return false
                }
            val invite = invites[uuid] ?: return false
            if (invite.target.uniqueId != player.uniqueId) return false
            if (invite.isExpired(server.currentTick.toLong())) {
                invites.remove(uuid)
                return false
            }
            val success = invite.accept(server.currentTick.toLong())
            invites.remove(uuid)
            return success
        }

        fun cleanup() {
            val tick = server.currentTick.toLong()
            invites.entries.removeAll { it.value.isExpired(tick) }
        }

        fun clear() {
            invites.clear()
        }

        companion object {
            val instance: InviteManager by lazy {
                InviteManager(
                    net.azisaba.simplequest.SimpleQuest.plugin.server,
                    net.azisaba.simplequest.SimpleQuest.plugin.configData,
                )
            }
        }
    }
