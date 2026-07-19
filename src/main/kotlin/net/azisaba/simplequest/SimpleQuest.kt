package net.azisaba.simplequest

import net.azisaba.simplequest.application.quest.QuestService
import net.azisaba.simplequest.command.Formula
import net.azisaba.simplequest.data.SimpleQuestConfig
import net.azisaba.simplequest.database.BackupService
import net.azisaba.simplequest.database.DatabaseManager
import net.azisaba.simplequest.database.DiscordWebhook
import net.azisaba.simplequest.database.MigrationRunner
import net.azisaba.simplequest.database.RedisManager
import net.azisaba.simplequest.database.SyncService
import net.azisaba.simplequest.di.BukkitModule
import net.azisaba.simplequest.di.DaggerSimpleQuestComponent
import net.azisaba.simplequest.di.SimpleQuestComponent
import net.azisaba.simplequest.gui.PartyMenuGui
import net.azisaba.simplequest.listener.PlayerListener
import net.azisaba.simplequest.listener.QuestProgressListener
import net.azisaba.simplequest.party.InviteManager
import net.azisaba.simplequest.party.PartyImpl
import net.azisaba.simplequest.party.PartyManager
import net.azisaba.simplequest.quest.QuestManager
import net.azisaba.simplequest.registry.DomainQuestTypes
import net.azisaba.simplequest.registry.QuestCategories
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.LegacyPaperCommandManager
import org.incendo.cloud.parser.standard.StringParser
import net.azisaba.simplequest.gui.QuestGui as QuestGuiObj

class SimpleQuest : JavaPlugin() {
    lateinit var diComponent: SimpleQuestComponent
        private set
    lateinit var configData: SimpleQuestConfig
        private set
    lateinit var databaseManager: DatabaseManager
        private set
    lateinit var redisManager: RedisManager
        private set
    lateinit var questManager: QuestManager
        private set
    lateinit var questService: QuestService
        private set
    lateinit var syncService: SyncService
        private set
    lateinit var migrationRunner: MigrationRunner
        private set
    lateinit var backupService: BackupService
        private set
    lateinit var discordWebhook: DiscordWebhook
        private set
    lateinit var simpleQuestLoader: SimpleQuestLoader
        private set
    lateinit var questProgressListener: QuestProgressListener
        private set

    override fun onEnable() {
        plugin = this
        saveDefaultConfig()
        diComponent = DaggerSimpleQuestComponent.builder().bukkitModule(BukkitModule(this)).build()
        configData = diComponent.configData()
        databaseManager = diComponent.databaseManager()
        redisManager = diComponent.redisManager()
        questManager = diComponent.questManager()
        questService = diComponent.questService()
        syncService = diComponent.syncService()
        migrationRunner = diComponent.migrationRunner()
        backupService = diComponent.backupService()
        discordWebhook = diComponent.discordWebhook()
        simpleQuestLoader = diComponent.simpleQuestLoader()
        questProgressListener = diComponent.questProgressListener()
        runDatabaseDependentSetup()
        registerBuiltInCategories()
        loadQuestDefinitions()
        registerCommands()
        registerListeners()
        startBackupIfConnected()
        if (redisManager.isConnected) logger.info("Redis connected.") else logger.info("Redis disabled.")
        logger.info("SimpleQuest enabled.")
    }

    override fun onDisable() {
        if (::backupService.isInitialized) backupService.stop()
        if (::discordWebhook.isInitialized) discordWebhook.shutdown()
        if (::redisManager.isInitialized) redisManager.disconnect()
        if (::databaseManager.isInitialized) databaseManager.disconnect()
        logger.info("SimpleQuest disabled.")
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerCommands() {
        val mgr = LegacyPaperCommandManager.createNative(this, ExecutionCoordinator.simpleCoordinator())
        try {
            mgr.registerBrigadier()
        } catch (_: Exception) {
        }
        val str = StringParser.stringParser<CommandSender>()

        // Helper: creates a builder chain [root, child1, child2, ...]
        fun cmd(vararg parts: String): org.incendo.cloud.Command.Builder<CommandSender> {
            var b: org.incendo.cloud.Command.Builder<CommandSender> = mgr.commandBuilder(parts[0])
            for (i in 1 until parts.size) {
                b = b.literal(parts[i]) ?: error("Failed to create literal '${parts[i]}'")
            }
            return b
        }

        mgr.command(
            cmd("simplequest").handler { ctx -> ctx.sender().sendMessage(Component.text("§6SimpleQuest §7v${description.version}")) },
        )

        mgr.command(cmd("simplequest", "help").handler { ctx -> showSimpleQuestHelp(ctx.sender()) })
        mgr.command(
            cmd("simplequest", "reload")
                .permission("simplequest.reload")
                .handler { ctx ->
                    val raw = ctx.rawInput().input()
                    reloadPlugin(raw.contains("--use-local"), raw.contains("--use-mysql"))
                    ctx.sender().sendMessage(
                        if (syncService.hasConflicts) Component.text("§cConflicts remain!") else Component.text("§aSimpleQuest reloaded."),
                    )
                },
        )
        mgr.command(cmd("simplequest", "quest").handler { ctx -> playerOnly(ctx.sender()) { QuestGuiObj.open(it) } })
        mgr.command(cmd("simplequest", "gui").handler { ctx -> playerOnly(ctx.sender()) { QuestGuiObj.open(it) } })
        mgr.command(cmd("simplequest", "party").handler { ctx -> playerOnly(ctx.sender()) { PartyMenuGui.open(it) } })
        mgr.command(
            cmd("simplequest", "grant")
                .permission("simplequest.grant")
                .required("player", str)
                .required("questType", str)
                .handler { ctx ->
                    val qk = ctx.get<String>("questType")
                    if (DomainQuestTypes.get(qk) == null) {
                        ctx.sender().sendMessage(Component.text("§cQuest not found: $qk"))
                        return@handler
                    }
                    questService.grantQuest(resolvePlayerId(ctx.get("player")), qk)
                    ctx.sender().sendMessage(Component.text("§aGranted §e$qk §ato §e${ctx.get<String>("player")}"))
                },
        )
        mgr.command(
            cmd("simplequest", "revoke")
                .permission("simplequest.revoke")
                .required("player", str)
                .required("questType", str)
                .handler { ctx ->
                    questService.revokeQuest(resolvePlayerId(ctx.get("player")), ctx.get<String>("questType"))
                    ctx.sender().sendMessage(
                        Component.text("§aRevoked §e${ctx.get<String>("questType")} §afrom §e${ctx.get<String>("player")}"),
                    )
                },
        )
        mgr.command(
            cmd("simplequest", "progress")
                .permission("simplequest.progress")
                .required("player", str)
                .required("reqKey", str)
                .required("formula", str)
                .handler { ctx ->
                    val p =
                        Bukkit.getPlayer(ctx.get<String>("player"))
                            ?: run {
                                ctx.sender().sendMessage(Component.text("§cPlayer not online."))
                                return@handler
                            }
                    val q =
                        questService.getQuestByPlayerId(p.uniqueId.toString())
                            ?: run {
                                ctx.sender().sendMessage(Component.text("§cNo active quest."))
                                return@handler
                            }
                    val f = Formula.parse(ctx.get<String>("formula"))
                    val cur = q.progresses[ctx.get<String>("reqKey")]
                    val nv = f.apply(cur)
                    questService.updateProgress(q, ctx.get<String>("reqKey"), nv - cur)
                    ctx.sender().sendMessage(Component.text("§aProgress [${ctx.get<String>("reqKey")}]: $cur → $nv"))
                },
        )

        mgr.command(
            cmd(
                "party",
            ).handler { ctx -> ctx.sender().sendMessage(Component.text("§6/party invite <player> | accept <id> | kick <player>")) },
        )
        mgr.command(
            cmd("party", "invite")
                .required("target", str)
                .handler { ctx ->
                    val s = ctx.sender() as? Player ?: return@handler
                    val t =
                        Bukkit.getPlayer(ctx.get<String>("target"))
                            ?: run {
                                s.sendMessage(Component.text("§cPlayer not found."))
                                return@handler
                            }
                    if (t == s) {
                        s.sendMessage(Component.text("§cCannot invite yourself."))
                        return@handler
                    }
                    val party = PartyManager.getParty(s) ?: PartyImpl(s)
                    PartyManager.setParty(s, party)
                    val inv = InviteManager.instance.createInvite(party, s, t)
                    t.sendMessage(Component.text("§e${s.name} §ainvited you!"))
                    t.sendMessage(Component.text("§7/party accept ${inv.id}"))
                    s.sendMessage(Component.text("§aInvited §e${t.name}"))
                },
        )
        mgr.command(
            cmd("party", "accept").required("id", str).handler { ctx ->
                val s = ctx.sender() as? Player ?: return@handler
                if (InviteManager.instance.acceptInvite(s, ctx.get("id"))) {
                    s.sendMessage(Component.text("§aJoined party!"))
                } else {
                    s.sendMessage(Component.text("§cInvalid or expired invite."))
                }
            },
        )
        mgr.command(
            cmd("party", "kick").required("target", str).handler { ctx ->
                val s = ctx.sender() as? Player ?: return@handler
                val party =
                    PartyManager.getParty(s) as? PartyImpl ?: run {
                        s.sendMessage(Component.text("§cNot in party."))
                        return@handler
                    }
                if (party.leader != s) {
                    s.sendMessage(Component.text("§cLeader only."))
                    return@handler
                }
                val t =
                    Bukkit.getPlayer(ctx.get<String>("target"))
                        ?: run {
                            s.sendMessage(Component.text("§cPlayer not found."))
                            return@handler
                        }
                if (t !in party) {
                    s.sendMessage(Component.text("§cNot in party."))
                    return@handler
                }
                party.removeMember(t)
                t.sendMessage(Component.text("§cKicked."))
                s.sendMessage(Component.text("§aKicked §e${t.name}"))
            },
        )
    }

    private fun playerOnly(
        sender: CommandSender,
        action: (Player) -> Unit,
    ) {
        (sender as? Player)?.let(action) ?: sender.sendMessage(Component.text("§cPlayer only"))
    }

    private fun showSimpleQuestHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("§6===== SimpleQuest Help ====="))
        sender.sendMessage(Component.text("§e/simplequest §7- Show version"))
        sender.sendMessage(Component.text("§e/simplequest help §7- Show this help"))
        sender.sendMessage(Component.text("§e/simplequest reload §7- Reload config & quests"))
        sender.sendMessage(Component.text("§e/simplequest quest §7- Open quest GUI"))
        sender.sendMessage(Component.text("§e/simplequest party §7- Open party GUI"))
        sender.sendMessage(Component.text("§e/simplequest grant <player> <quest> §7- Grant quest"))
        sender.sendMessage(Component.text("§e/simplequest revoke <player> <quest> §7- Revoke quest"))
        sender.sendMessage(Component.text("§e/simplequest progress <player> <key> <formula> §7- Update progress"))
        sender.sendMessage(Component.text("§e/party invite <player> §7- Invite to party"))
        sender.sendMessage(Component.text("§e/party accept <id> §7- Accept invite"))
        sender.sendMessage(Component.text("§e/party kick <player> §7- Kick member"))
    }

    private fun resolvePlayerId(nameOrUuid: String): String {
        if (nameOrUuid.length == 36 && nameOrUuid.count { it == '-' } == 4) return nameOrUuid
        Bukkit.getPlayer(nameOrUuid)?.let { return it.uniqueId.toString() }
        @Suppress("DEPRECATION")
        return Bukkit.getOfflinePlayer(nameOrUuid).uniqueId.toString()
    }

    private fun registerBuiltInCategories() {
        QuestCategories.entries.size
    }

    fun reloadPlugin(
        useLocal: Boolean = false,
        useMySql: Boolean = false,
    ) {
        DomainQuestTypes.clear()
        if (useLocal) syncService.resolveUseLocal()
        if (useMySql) syncService.resolveUseMySql(dataFolder)
        loadQuestDefinitions()
        logger.info(if (syncService.hasConflicts) "Conflicts remain." else "SimpleQuest reloaded.")
    }

    private fun loadQuestDefinitions() {
        syncService.sync(dataFolder)
        if (syncService.hasConflicts) {
            logger.warning("Conflicts: ${syncService.conflictedQuests()}")
            return
        }
        simpleQuestLoader.loadAll(dataFolder)
    }

    private fun runDatabaseDependentSetup() {
        try {
            migrationRunner.run()
        } catch (e: Exception) {
            logger.warning("DB migration skipped: ${e.message}")
        }
    }

    private fun startBackupIfConnected() {
        if (databaseManager.isConnected) backupService.start() else logger.info("Backup skipped.")
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(PlayerListener(questManager, syncService), this)
        server.pluginManager.registerEvents(questProgressListener, this)
    }

    companion object {
        lateinit var plugin: SimpleQuest
            private set
    }
}
