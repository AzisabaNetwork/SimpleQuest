package net.azisaba.simplequest.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SimpleQuestConfig(
    val database: DatabaseConfig = DatabaseConfig(),
    val redis: RedisConfig = RedisConfig(),
    val maxPartySize: Int = 8,
    val partyInviteLimit: Long = 1200,
    val panel: PanelConfig = PanelConfig(),
    @SerialName("multi-server")
    val multiServer: MultiServerConfig = MultiServerConfig(),
    val discord: DiscordConfig = DiscordConfig(),
)

@Serializable
data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val name: String = "simplequest",
    val user: String = "root",
    val password: String = "",
)

@Serializable
data class RedisConfig(
    val user: String = "",
    val host: String = "localhost",
    val port: Int = 6379,
    val password: String = "",
)

@Serializable
data class PanelConfig(
    val title: String = "&dSimpleQuest",
    val footer: String = "&7azisaba.net",
)

@Serializable
data class MultiServerConfig(
    @SerialName("write-to-mysql") val writeToMysql: Boolean = false,
    @SerialName("write-to-yaml") val writeToYaml: Boolean = false,
    @SerialName("conflict-mode") val conflictMode: String = "LOCAL",
    val backup: BackupConfig = BackupConfig(),
)

@Serializable
data class BackupConfig(
    val enabled: Boolean = false,
    @SerialName("interval-hours") val intervalHours: Int = 24,
    @SerialName("retention-days") val retentionDays: Int = 30,
    val directory: String = "plugins/SimpleQuest/backups/",
)

@Serializable
data class DiscordConfig(
    @SerialName("webhook-url") val webhookUrl: String = "",
)
