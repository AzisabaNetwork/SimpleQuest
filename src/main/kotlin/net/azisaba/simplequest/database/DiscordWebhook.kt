package net.azisaba.simplequest.database

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Sends error/event notifications to a Discord webhook.
 * Injectable via Dagger; also provides backward-compat static helpers.
 */
@Singleton
class DiscordWebhook
    @Inject
    constructor(
        private val httpClient: HttpClient,
        private val webhookUrl: String,
        private val logger: java.util.logging.Logger,
    ) {
        private var running = true

        fun sendMessage(message: String) {
            if (!running || webhookUrl.isBlank()) return
            runBlocking {
                try {
                    val payload = WebhookPayload(content = message)
                    val json = Json.encodeToString(WebhookPayload.serializer(), payload)
                    httpClient.post(webhookUrl) {
                        contentType(ContentType.Application.Json)
                        setBody(json)
                    }
                } catch (e: Exception) {
                    logger.warning("Discord sendMessage failed: ${e.message}")
                }
            }
        }

        fun sendError(
            title: String,
            description: String,
            color: Int = 0xFF0000,
        ) {
            if (!running || webhookUrl.isBlank()) return
            runBlocking {
                try {
                    val embed = Embed(title = title, description = description, color = color)
                    val payload = EmbedPayload(embeds = listOf(embed))
                    val json = Json.encodeToString(EmbedPayload.serializer(), payload)
                    httpClient.post(webhookUrl) {
                        contentType(ContentType.Application.Json)
                        setBody(json)
                    }
                } catch (e: Exception) {
                    logger.warning("Discord sendError failed: ${e.message}")
                }
            }
        }

        fun shutdown() {
            running = false
        }

        @Serializable
        private data class WebhookPayload(
            val content: String,
            val username: String = "SimpleQuest",
        )

        @Serializable
        private data class Embed(
            val title: String? = null,
            val description: String? = null,
            val color: Int? = null,
        )

        @Serializable
        private data class EmbedPayload(
            val embeds: List<Embed>,
            val username: String = "SimpleQuest",
        )

        companion object {
            /** Legacy fallback — no-op when DI is not wired. */
            fun sendError(
                title: String,
                description: String,
                color: Int,
            ) {
                // Replaced by DI-managed instance
            }

            fun shutdown() {
                // Replaced by DI-managed instance
            }
        }
    }
