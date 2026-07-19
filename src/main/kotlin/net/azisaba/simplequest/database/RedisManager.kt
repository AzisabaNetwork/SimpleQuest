package net.azisaba.simplequest.database

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.azisaba.simplequest.data.RedisConfig

/**
 * Manages the Lettuce-based Redis connection used for caching and PubSub messaging.
 *
 * The plugin can still load without Redis: when the connection fails, the manager
 * disables itself and downstream Redis-dependent features are skipped, mirroring the
 * graceful-degradation behaviour of [DatabaseManager].
 */
@Singleton
class RedisManager
    @Inject
    constructor(
        private val config: RedisConfig,
    ) {
        private var client: RedisClient? = null
        private var connection: StatefulRedisConnection<String, String>? = null
        private var failed: Boolean = false

        init {
            try {
                connect()
            } catch (e: Exception) {
                // Plugin can still load without Redis.
                // Redis-dependent features (cache, PubSub) will be skipped.
                System.err.println("[SimpleQuest] Failed to connect to Redis: ${e.message}")
                System.err.println("[SimpleQuest] Redis-dependent features are disabled.")
                disconnect()
            }
        }

        /**
         * Builds the [RedisURI] from the current [RedisConfig], mapping the `user`
         * field to the Redis 6+ ACL username (empty means the default user).
         */
        internal fun buildUri(): RedisURI {
            val uri =
                RedisURI
                    .Builder
                    .redis(config.host, config.port)
                    .apply {
                        if (config.password.isNotBlank()) withPassword(config.password)
                    }.build()
            // Redis 6+ ACL: empty user means the default user.
            // withUsername is not available on the Builder, so set it on the built URI.
            if (config.user.isNotBlank()) uri.username = config.user
            return uri
        }

        fun connect() {
            if (failed) return
            val uri = buildUri()
            client = RedisClient.create(uri)
            connection = client!!.connect()
            // Verify the connection eagerly so misconfiguration fails fast.
            connection!!.sync().ping()
            failed = false
        }

        fun disconnect() {
            try {
                connection?.close()
            } catch (_: Exception) {
            }
            try {
                client?.shutdown()
            } catch (_: Exception) {
            }
            connection = null
            client = null
        }

        val isConnected: Boolean
            get() = !failed && connection != null && connection!!.isOpen

        /**
         * Returns a synchronous command API, or null when Redis is unavailable.
         */
        fun syncCommands(): io.lettuce.core.api.sync.RedisCommands<String, String>? = connection?.sync()

        /**
         * Returns an asynchronous command API, or null when Redis is unavailable.
         */
        fun asyncCommands(): io.lettuce.core.api.async.RedisAsyncCommands<String, String>? = connection?.async()
    }
