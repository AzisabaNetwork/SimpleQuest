package net.azisaba.lifequest.data

import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Serializable
data class QuestVersion(
    val version: Int = 1,
    val checksum: String,
    val updatedAt: String,
    val updatedBy: String,
) {
    companion object {
        fun compute(yamlText: String): QuestVersion {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(yamlText.toByteArray()).joinToString("") { "%02x".format(it) }
            return QuestVersion(
                checksum = hash,
                updatedAt = Instant.now().toString(),
                updatedBy = System.getProperty("lifequest.server-id", UUID.randomUUID().toString()),
            )
        }
    }
}
