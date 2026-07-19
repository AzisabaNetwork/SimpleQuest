package net.azisaba.simplequest

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.azisaba.simplequest.command.Formula
import net.azisaba.simplequest.party.InviteManager
import net.azisaba.simplequest.party.PartyImpl
import net.azisaba.simplequest.party.PartyManager
import net.azisaba.simplequest.registry.DomainQuestTypes
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class SimpleQuestBootstrap : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        val manager = context.lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val registrar = event.registrar()
            registrar.register("simplequest", "SimpleQuest management command", simpleQuestCommand)
            registrar.register("party", "Party management command", partyCommand)
        }
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin = SimpleQuest()
}

private val simpleQuestCommand =
    BasicCommand { source, args ->
        val sender = source.sender
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("§6SimpleQuest §7v${SimpleQuest.plugin.description.version}"))
            return@BasicCommand
        }
        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("simplequest.reload")) {
                    sender.sendMessage(Component.text("§cYou don't have permission."))
                    return@BasicCommand
                }
                val useLocal = args.any { it == "--use-local" }
                val useMySql = args.any { it == "--use-mysql" }
                SimpleQuest.plugin.reloadPlugin(useLocal, useMySql)
                if (SimpleQuest.plugin.syncService.hasConflicts) {
                    sender.sendMessage(Component.text("§cConflicts remain! Use --use-local or --use-mysql"))
                } else {
                    sender.sendMessage(Component.text("§aSimpleQuest reloaded."))
                }
            }

            "quest", "gui" -> {
                if (sender is Player) {
                    net.azisaba.simplequest.gui.QuestGui
                        .open(sender)
                } else {
                    sender.sendMessage(Component.text("§cPlayer only command"))
                }
            }

            "party" -> {
                if (sender is org.bukkit.entity.Player) {
                    net.azisaba.simplequest.gui.PartyMenuGui
                        .open(sender)
                } else {
                    sender.sendMessage(Component.text("§cPlayer only command"))
                }
            }

            "grant" -> {
                if (!sender.hasPermission("simplequest.grant")) {
                    sender.sendMessage(Component.text("§cYou don't have permission."))
                    return@BasicCommand
                }
                if (args.size < 3) {
                    sender.sendMessage(Component.text("§cUsage: /simplequest grant <player> <questType>"))
                    return@BasicCommand
                }
                val questKey = args[2]
                val type = DomainQuestTypes.get(questKey)
                if (type == null) {
                    sender.sendMessage(Component.text("§cQuest type not found: $questKey"))
                    return@BasicCommand
                }
                val playerId = resolvePlayerId(args[1])
                SimpleQuest.plugin.questService.grantQuest(playerId, questKey)
                sender.sendMessage(Component.text("§aGranted §e$questKey §ato §e${args[1]}"))
            }

            "revoke" -> {
                if (!sender.hasPermission("simplequest.revoke")) {
                    sender.sendMessage(Component.text("§cYou don't have permission."))
                    return@BasicCommand
                }
                if (args.size < 3) {
                    sender.sendMessage(Component.text("§cUsage: /simplequest revoke <player> <questType>"))
                    return@BasicCommand
                }
                val questKey = args[2]
                val playerId = resolvePlayerId(args[1])
                SimpleQuest.plugin.questService.revokeQuest(playerId, questKey)
                sender.sendMessage(Component.text("§aRevoked §e$questKey §afrom §e${args[1]}"))
            }

            "progress" -> {
                if (!sender.hasPermission("simplequest.progress")) {
                    sender.sendMessage(Component.text("§cYou don't have permission."))
                    return@BasicCommand
                }
                if (args.size < 4) {
                    sender.sendMessage(Component.text("§cUsage: /simplequest progress <player> <reqKey> <formula>"))
                    return@BasicCommand
                }
                val player = Bukkit.getPlayer(args[1])
                if (player == null) {
                    sender.sendMessage(Component.text("§cPlayer not online: ${args[1]}"))
                    return@BasicCommand
                }
                val quest = SimpleQuest.plugin.questService.getQuestByPlayerId(player.uniqueId.toString())
                if (quest == null) {
                    sender.sendMessage(Component.text("§cPlayer has no active quest."))
                    return@BasicCommand
                }
                val reqKey = args[2]
                val formula = Formula.parse(args[3])
                val current = quest.progresses[reqKey]
                val newValue = formula.apply(current)
                val delta = newValue - current
                SimpleQuest.plugin.questService.updateProgress(quest, reqKey, delta)
                sender.sendMessage(Component.text("§aProgress [$reqKey]: $current → $newValue"))
            }

            else -> {
                sender.sendMessage(Component.text("§cUnknown subcommand."))
            }
        }
    }

/** Resolves a player name (or UUID string) to a player UUID for grant/revoke. */
private fun resolvePlayerId(nameOrUuid: String): String {
    // If it already looks like a UUID, use it directly
    if (nameOrUuid.length == 36 && nameOrUuid.count { it == '-' } == 4) {
        return nameOrUuid
    }
    // Try online player first, then offline
    val online = Bukkit.getPlayer(nameOrUuid)
    if (online != null) return online.uniqueId.toString()
    @Suppress("DEPRECATION")
    val offline = Bukkit.getOfflinePlayer(nameOrUuid)
    return offline.uniqueId.toString()
}

private val partyCommand =
    BasicCommand { source, args ->
        val player =
            source.sender as? Player ?: run {
                source.sender.sendMessage(Component.text("§cPlayer only command"))
                return@BasicCommand
            }
        if (args.isEmpty()) {
            player.sendMessage(Component.text("§6/party invite <player> | accept <id> | kick <player>"))
            return@BasicCommand
        }
        when (args[0].lowercase()) {
            "invite" -> {
                if (args.size < 2) {
                    player.sendMessage(Component.text("§cUsage: /party invite <player>"))
                    return@BasicCommand
                }
                val target = player.server.getPlayer(args[1])
                if (target == null) {
                    player.sendMessage(Component.text("§cPlayer not found."))
                    return@BasicCommand
                }
                if (target == player) {
                    player.sendMessage(Component.text("§cYou cannot invite yourself."))
                    return@BasicCommand
                }
                val party = PartyManager.getParty(player) ?: PartyImpl(player)
                PartyManager.setParty(player, party)
                val invite = InviteManager.instance.createInvite(party, player, target)
                target.sendMessage(Component.text("§e${player.name} §ainvited you to a party!"))
                target.sendMessage(Component.text("§7/party accept ${invite.id} §eto accept"))
                player.sendMessage(Component.text("§aInvited §e${target.name}"))
            }

            "accept" -> {
                if (args.size < 2) {
                    player.sendMessage(Component.text("§cUsage: /party accept <id>"))
                    return@BasicCommand
                }
                if (InviteManager.instance.acceptInvite(player, args[1])) {
                    player.sendMessage(Component.text("§aJoined party!"))
                } else {
                    player.sendMessage(Component.text("§cInvalid or expired invite."))
                }
            }

            "kick" -> {
                val party = PartyManager.getParty(player) as? PartyImpl
                if (party == null || party.leader != player) {
                    player.sendMessage(Component.text("§cOnly the party leader can kick members."))
                    return@BasicCommand
                }
                if (args.size < 2) {
                    player.sendMessage(Component.text("§cUsage: /party kick <player>"))
                    return@BasicCommand
                }
                val target = player.server.getPlayer(args[1])
                if (target == null || target !in party) {
                    player.sendMessage(Component.text("§cPlayer not found in party."))
                    return@BasicCommand
                }
                party.removeMember(target)
                target.sendMessage(Component.text("§cYou were kicked from the party."))
                player.sendMessage(Component.text("§aKicked §e${target.name}§a."))
            }

            else -> {
                player.sendMessage(Component.text("§cUnknown subcommand."))
            }
        }
    }
