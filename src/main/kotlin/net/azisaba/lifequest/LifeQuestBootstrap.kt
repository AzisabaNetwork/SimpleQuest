package net.azisaba.lifequest

import com.tksimeji.kunectron.Kunectron
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.bootstrap.PluginProviderContext
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.azisaba.lifequest.party.InviteManager
import net.azisaba.lifequest.party.Party
import net.azisaba.lifequest.party.PartyImpl
import net.azisaba.lifequest.party.PartyManager
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class LifeQuestBootstrap : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        val manager = context.lifecycleManager
        manager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val registrar = event.registrar()
            registrar.register("lifequest", "LifeQuest management command", lifeQuestCommand)
            registrar.register("party", "Party management command", partyCommand)
        }
    }

    override fun createPlugin(context: PluginProviderContext): JavaPlugin = LifeQuest()
}

private val lifeQuestCommand =
    BasicCommand { source, args ->
        val sender = source.sender
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("§6LifeQuest §7v${LifeQuest.plugin.description.version}"))
            return@BasicCommand
        }
        when (args[0].lowercase()) {
            "reload" -> {
                if (!sender.hasPermission("lifequest.reload")) {
                    sender.sendMessage(Component.text("§cYou don't have permission."))
                    return@BasicCommand
                }
                val useLocal = args.any { it == "--use-local" }
                val useMySql = args.any { it == "--use-mysql" }
                LifeQuest.plugin.reloadPlugin(useLocal, useMySql)
                if (LifeQuest.plugin.syncService.hasConflicts) {
                    sender.sendMessage(Component.text("§cConflicts remain! Use --use-local or --use-mysql"))
                } else {
                    sender.sendMessage(Component.text("§aLifeQuest reloaded."))
                }
            }

            "quest", "gui" -> {
                if (sender is Player) {
                    Kunectron
                        .create(
                            net.azisaba.lifequest.gui
                                .QuestGui(sender),
                        )
                } else {
                    sender.sendMessage(Component.text("§cPlayer only command"))
                }
            }

            "party" -> {
                if (sender is org.bukkit.entity.Player) {
                    val party = PartyManager.getParty(sender)
                    if (party != null) {
                        Kunectron.create(
                            net.azisaba.lifequest.gui
                                .PartyMenuGui(sender, party),
                        )
                    } else {
                        Kunectron.create(
                            net.azisaba.lifequest.gui
                                .PartyCreateGui(sender),
                        )
                    }
                } else {
                    sender.sendMessage(Component.text("§cPlayer only command"))
                }
            }

            else -> {
                sender.sendMessage(Component.text("§cUnknown subcommand."))
            }
        }
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
