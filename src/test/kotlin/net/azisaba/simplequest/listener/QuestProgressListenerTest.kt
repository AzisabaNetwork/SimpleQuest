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
import org.bukkit.block.Block
import org.bukkit.event.block.BlockBreakEvent
import java.time.Instant
import net.azisaba.simplequest.domain.party.model.InvitationSetting as DomainInvitation
import net.azisaba.simplequest.domain.party.model.Party as DomainParty

class QuestProgressListenerTest :
    FunSpec({

        lateinit var server: ServerMock
        lateinit var service: QuestService
        lateinit var listener: QuestProgressListener
        // Keep a reference accessible from inline helper
        var testRepo: FakeRepository? = null

        beforeSpec {
            server = MockBukkit.mock()
        }

        afterSpec {
            MockBukkit.unmock()
        }

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

        /**
         * Grants and starts a quest for the given player.
         */
        fun grantAndStart(
            player: PlayerMock,
            type: QuestType,
        ) {
            val repo = testRepo!!
            repo.granted.add("${player.uniqueId}:${type.key}")
            val party = FakeParty(player.uniqueId.toString())
            service.startQuest(type, party, listOf(player.uniqueId.toString()))
        }

        fun fakeStoneBlock(player: PlayerMock): Block {
            val world = player.server.addSimpleWorld("world")
            val loc = org.bukkit.Location(world, 0.0, 64.0, 0.0)
            val block = loc.block
            block.type = org.bukkit.Material.STONE
            return block
        }

        fun questType(
            key: String,
            reqs: Map<String, Int> = emptyMap(),
        ): QuestType =
            QuestType(
                key = key,
                title = "Test Quest",
                icon = Icon(type = "STONE"),
                requirements = reqs.mapValues { (k, v) -> QuestRequirement(k, v) },
            )

        context("BlockBreakEvent") {

            test("should match BreakStone objective") {
                val player = server.addPlayer("TestPlayer")
                grantAndStart(player, questType("test:break", mapOf("BreakStone" to 5)))

                listener.onBlockBreak(BlockBreakEvent(fakeStoneBlock(player), player))

                val quest = service.getQuestByPlayerId(player.uniqueId.toString())!!
                quest.progresses["BreakStone"] shouldBe 1
            }

            test("should not match when player has no active quest") {
                val player = server.addPlayer("NoQuestPlayer")
                // Must not throw
                listener.onBlockBreak(BlockBreakEvent(fakeStoneBlock(player), player))
            }

            test("should increment multiple times") {
                val player = server.addPlayer("MultiBreak")
                grantAndStart(player, questType("test:break2", mapOf("BreakStone" to 10)))

                repeat(3) {
                    listener.onBlockBreak(BlockBreakEvent(fakeStoneBlock(player), player))
                }

                val quest = service.getQuestByPlayerId(player.uniqueId.toString())!!
                quest.progresses["BreakStone"] shouldBe 3
            }

            test("should auto-complete when all requirements met") {
                val player = server.addPlayer("CompleteBreak")
                grantAndStart(player, questType("test:break3", mapOf("BreakStone" to 1)))

                listener.onBlockBreak(BlockBreakEvent(fakeStoneBlock(player), player))

                // Quest ended after requirements met — no longer active
                service.getQuestByPlayerId(player.uniqueId.toString()) shouldBe null
            }
        }

        context("objective key matching") {

            test("should ignore unknown prefix objectives") {
                val player = server.addPlayer("Unknown")
                grantAndStart(player, questType("test:unknown", mapOf("UnknownPrefix" to 1)))

                listener.onBlockBreak(BlockBreakEvent(fakeStoneBlock(player), player))

                val quest = service.getQuestByPlayerId(player.uniqueId.toString())!!
                quest.progresses["UnknownPrefix"] shouldBe 0
            }
        }

        context("PlayerItemConsumeEvent / Consume") {

            test("handler compiles and increments progress") {
                val player = server.addPlayer("HungryPlayer")
                grantAndStart(player, questType("test:consume", mapOf("ConsumeApple" to 3)))

                val item = org.bukkit.inventory.ItemStack(org.bukkit.Material.APPLE)
                val event =
                    org.bukkit.event.player
                        .PlayerItemConsumeEvent(player, item)
                listener.onItemConsume(event)

                val quest = service.getQuestByPlayerId(player.uniqueId.toString())!!
                quest.progresses["ConsumeApple"] shouldBe 1
            }
        }
    })

// ---- Fake implementations ----

private class FakeRepository : QuestRepository {
    val granted = mutableSetOf<String>()

    override fun isGranted(
        playerId: String,
        questKey: String,
    ): Boolean = "$playerId:$questKey" in granted

    override fun getCompletionsSince(
        playerId: String,
        questKey: String,
        since: Instant,
    ): Int = 0

    override fun grant(
        playerId: String,
        questKey: String,
    ) {
        granted.add("$playerId:$questKey")
    }

    override fun revoke(
        playerId: String,
        questKey: String,
    ) {
        granted.remove("$playerId:$questKey")
    }

    override fun getPlays(
        playerId: String,
        questKey: String,
    ): Int = 0

    override fun getWeeklyCompletions(
        playerId: String,
        questKey: String,
    ): Int = 0

    override fun getMonthlyCompletions(
        playerId: String,
        questKey: String,
    ): Int = 0

    override fun getYearlyCompletions(
        playerId: String,
        questKey: String,
    ): Int = 0

    override fun isFirstCompletion(
        playerId: String,
        questKey: String,
    ): Boolean = true
}

private class FakeDispatcher : ActionDispatcher {
    override fun dispatch(
        action: Action,
        playerId: String,
    ) {}

    override fun dispatchAll(
        actions: List<Action>,
        playerIds: List<String>,
    ) {}
}

private class FakeScriptRunner : ScriptRunner {
    override fun run(
        script: Script,
        playerIds: List<String>,
    ) {}
}

private class FakeNotifier : QuestNotifier {
    override fun showQuestPanel(
        playerId: String,
        questKey: String,
    ) {}

    override fun hideQuestPanel(playerId: String) {}

    override fun sendMessage(
        playerId: String,
        message: String,
    ) {}
}

private class FakeParty(
    override val leaderId: String,
) : DomainParty {
    override val memberIds: Set<String> = setOf(leaderId)
    override val size: Int = 1
    override val invitationSetting: DomainInvitation = DomainInvitation.LEADER

    override fun hasPermission(type: QuestType): Boolean = true
}
