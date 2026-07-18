package net.azisaba.simplequest.listener

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import be.seeseemelk.mockbukkit.entity.PlayerMock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.azisaba.simplequest.application.quest.QuestService
import net.azisaba.simplequest.domain.action.Action
import net.azisaba.simplequest.domain.action.port.ActionDispatcher
import net.azisaba.simplequest.domain.data.Icon
import net.azisaba.simplequest.domain.quest.model.QuestRequirement
import net.azisaba.simplequest.domain.quest.model.QuestType
import net.azisaba.simplequest.domain.quest.port.QuestNotifier
import net.azisaba.simplequest.domain.quest.port.QuestRepository
import net.azisaba.simplequest.domain.script.Script
import net.azisaba.simplequest.domain.script.port.ScriptRunner
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import java.time.Instant
import net.azisaba.simplequest.domain.party.model.InvitationSetting as DomainInvitation
import net.azisaba.simplequest.domain.party.model.Party as DomainParty

class QuestProgressListenerTest :
    FunSpec({

        lateinit var server: ServerMock
        lateinit var service: QuestService
        lateinit var listener: QuestProgressListener
        var testRepo: FakeRepository? = null

        beforeSpec { server = MockBukkit.mock() }
        afterSpec { MockBukkit.unmock() }

        beforeTest {
            val repo = FakeRepository()
            testRepo = repo
            service =
                QuestService(
                    questRepository = repo,
                    actionDispatcher = FakeDispatcher(),
                    scriptRunner = FakeScriptRunner(),
                    questNotifier = FakeNotifier(),
                )
            listener = QuestProgressListener(service)
        }

        // ---- helpers ----

        fun grantAndStart(
            player: PlayerMock,
            type: QuestType,
        ) {
            val repo = testRepo!!
            repo.granted.add("${player.uniqueId}:${type.key}")
            service.startQuest(type, FakeParty(player.uniqueId.toString()), listOf(player.uniqueId.toString()))
        }

        fun stoneBlock(player: PlayerMock): Block {
            val w = player.server.addSimpleWorld("world")
            val b = org.bukkit.Location(w, 0.0, 64.0, 0.0).block
            b.type = Material.STONE
            return b
        }

        fun questType(
            key: String,
            reqs: Map<String, Int> = emptyMap(),
        ): QuestType =
            QuestType(
                key = key,
                title = "T",
                icon = Icon(type = "STONE"),
                requirements = reqs.mapValues { (k, v) -> QuestRequirement(k, v) },
            )

        // ---- BlockBreakEvent ----

        context("BlockBreakEvent") {
            test("matches BreakStone") {
                val p = server.addPlayer("P1")
                grantAndStart(p, questType("t:1", mapOf("BreakStone" to 5)))
                listener.onBlockBreak(BlockBreakEvent(stoneBlock(p), p))
                service.getQuestByPlayerId(p.uniqueId.toString())!!.progresses["BreakStone"] shouldBe 1
            }
            test("no-op when no quest") {
                listener.onBlockBreak(BlockBreakEvent(stoneBlock(server.addPlayer("P2")), server.getPlayer("P2")!!))
                // must not throw
            }
            test("auto-completes when requirements met") {
                val p = server.addPlayer("P3")
                grantAndStart(p, questType("t:ac", mapOf("BreakStone" to 1)))
                listener.onBlockBreak(BlockBreakEvent(stoneBlock(p), p))
                service.getQuestByPlayerId(p.uniqueId.toString()) shouldBe null
            }
        }

        // ---- BlockPlaceEvent ----

        context("BlockPlaceEvent") {
            test("matches PlaceDirt") {
                val p = server.addPlayer("P4")
                grantAndStart(p, questType("t:place", mapOf("PlaceDirt" to 3)))
                val b = stoneBlock(p)
                b.type = Material.DIRT
                // BlockPlaceEvent constructor varies; use the one MockBukkit provides
                // Minimal: fire the event and verify progress
                val event = BlockPlaceEvent(b, b.state, b, ItemStack(Material.DIRT), p, true, org.bukkit.inventory.EquipmentSlot.HAND)
                listener.onBlockPlace(event)
                service.getQuestByPlayerId(p.uniqueId.toString())!!.progresses["PlaceDirt"] shouldBe 1
            }
        }

        // ---- PlayerItemConsumeEvent ----

        context("PlayerItemConsumeEvent") {
            test("matches ConsumeApple") {
                val p = server.addPlayer("P5")
                grantAndStart(p, questType("t:eat", mapOf("ConsumeApple" to 3)))
                listener.onItemConsume(PlayerItemConsumeEvent(p, ItemStack(Material.APPLE)))
                service.getQuestByPlayerId(p.uniqueId.toString())!!.progresses["ConsumeApple"] shouldBe 1
            }
        }

        // ---- prefix matching ----

        context("objective key matching") {
            test("ignores unknown prefix") {
                val p = server.addPlayer("P6")
                grantAndStart(p, questType("t:unknown", mapOf("UnknownPrefix" to 1)))
                listener.onBlockBreak(BlockBreakEvent(stoneBlock(p), p))
                service.getQuestByPlayerId(p.uniqueId.toString())!!.progresses["UnknownPrefix"] shouldBe 0
            }
            test("ignores correct prefix but wrong material") {
                val p = server.addPlayer("P7")
                grantAndStart(p, questType("t:wrong", mapOf("BreakDiamond" to 1)))
                listener.onBlockBreak(BlockBreakEvent(stoneBlock(p), p)) // stone ≠ diamond
                service.getQuestByPlayerId(p.uniqueId.toString())!!.progresses["BreakDiamond"] shouldBe 0
            }
            test("case-insensitive prefix matching") {
                val p = server.addPlayer("P8")
                grantAndStart(p, questType("t:case", mapOf("breakstone" to 5)))
                listener.onBlockBreak(BlockBreakEvent(stoneBlock(p), p))
                service.getQuestByPlayerId(p.uniqueId.toString())!!.progresses["breakstone"] shouldBe 1
            }
        }
    })

// ---- Fake implementations ----

private class FakeRepository : QuestRepository {
    val granted = mutableSetOf<String>()

    override fun isGranted(
        pid: String,
        qk: String,
    ) = "$pid:$qk" in granted

    override fun getCompletionsSince(
        pid: String,
        qk: String,
        since: Instant,
    ) = 0

    override fun grant(
        pid: String,
        qk: String,
    ) {
        granted.add("$pid:$qk")
    }

    override fun revoke(
        pid: String,
        qk: String,
    ) {
        granted.remove("$pid:$qk")
    }

    override fun getPlays(
        pid: String,
        qk: String,
    ) = 0

    override fun getWeeklyCompletions(
        pid: String,
        qk: String,
    ) = 0

    override fun getMonthlyCompletions(
        pid: String,
        qk: String,
    ) = 0

    override fun getYearlyCompletions(
        pid: String,
        qk: String,
    ) = 0

    override fun isFirstCompletion(
        pid: String,
        qk: String,
    ) = true
}

private class FakeDispatcher : ActionDispatcher {
    override fun dispatch(
        a: Action,
        pid: String,
    ) {}

    override fun dispatchAll(
        as_: List<Action>,
        pids: List<String>,
    ) {}
}

private class FakeScriptRunner : ScriptRunner {
    override fun run(
        s: Script,
        pids: List<String>,
    ) {}
}

private class FakeNotifier : QuestNotifier {
    override fun showQuestPanel(
        pid: String,
        qk: String,
    ) {}

    override fun hideQuestPanel(pid: String) {}

    override fun sendMessage(
        pid: String,
        msg: String,
    ) {}
}

private class FakeParty(
    override val leaderId: String,
) : DomainParty {
    override val memberIds = setOf(leaderId)
    override val size = 1
    override val invitationSetting = DomainInvitation.LEADER

    override fun hasPermission(t: QuestType) = true
}
