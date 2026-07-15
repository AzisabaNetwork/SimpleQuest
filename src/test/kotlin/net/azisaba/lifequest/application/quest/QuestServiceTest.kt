package net.azisaba.lifequest.application.quest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.azisaba.lifequest.domain.action.Action
import net.azisaba.lifequest.domain.action.port.ActionDispatcher
import net.azisaba.lifequest.domain.data.Icon
import net.azisaba.lifequest.domain.party.model.InvitationSetting
import net.azisaba.lifequest.domain.quest.model.EndReason
import net.azisaba.lifequest.domain.quest.model.PlayLimits
import net.azisaba.lifequest.domain.quest.model.QuestRequirement
import net.azisaba.lifequest.domain.quest.model.QuestResult
import net.azisaba.lifequest.domain.quest.model.QuestState
import net.azisaba.lifequest.domain.quest.model.QuestType
import net.azisaba.lifequest.domain.quest.port.QuestNotifier
import net.azisaba.lifequest.domain.quest.port.QuestRepository
import net.azisaba.lifequest.domain.script.Script
import net.azisaba.lifequest.domain.script.port.ScriptRunner
import java.time.Instant
import net.azisaba.lifequest.domain.party.model.Party as DomainParty

class QuestServiceTest :
    FunSpec({

        lateinit var service: QuestService
        lateinit var fakeRepo: FakeQuestRepository
        lateinit var fakeDispatcher: FakeActionDispatcher
        lateinit var fakeScriptRunner: FakeScriptRunner
        lateinit var fakeNotifier: FakeQuestNotifier

        beforeTest {
            fakeRepo = FakeQuestRepository()
            fakeDispatcher = FakeActionDispatcher()
            fakeScriptRunner = FakeScriptRunner()
            fakeNotifier = FakeQuestNotifier()
            service =
                QuestService(
                    questRepository = fakeRepo,
                    actionDispatcher = fakeDispatcher,
                    scriptRunner = fakeScriptRunner,
                    questNotifier = fakeNotifier,
                )
        }

        context("startQuest") {
            test("returns success when party has permission") {
                val type = createQuestType("test:simple")
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                fakeRepo.grantQuest("p1", type.key)

                val result = service.startQuest(type, party, listOf("p1"))

                val quest = (result as QuestResult.Success).quest
                quest.type.key shouldBe "test:simple"
                quest.state shouldBe QuestState.ACTIVE
            }

            test("fails when party lacks permission") {
                val type = createQuestType("test:no-perm", maxPlayers = 1)
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1", "p2"))
                fakeRepo.grantQuest("p1", type.key)

                val result = service.startQuest(type, party, listOf("p1", "p2"))

                val failure = result as QuestResult.Failure
                failure.reason shouldNotBe ""
            }

            test("fails when player does not have quest granted") {
                val type = createQuestType("test:locked")
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))

                val result = service.startQuest(type, party, listOf("p1"))

                val failure = result as QuestResult.Failure
                failure.reason shouldNotBe ""
            }

            test("fails when player exceeds lifetime limit") {
                val type = createQuestType("test:limited", playLimits = PlayLimits(lifetime = 1))
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                fakeRepo.grantQuest("p1", type.key)
                fakeRepo.completionCount = 1

                val result = service.startQuest(type, party, listOf("p1"))

                val failure = result as QuestResult.Failure
                failure.reason shouldNotBe ""
            }

            test("notifies players on start") {
                val type = createQuestType("test:notify")
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1", "p2"))
                fakeRepo.grantQuest("p1", type.key)
                fakeRepo.grantQuest("p2", type.key)

                service.startQuest(type, party, listOf("p1", "p2"))

                fakeNotifier.shownPlayers shouldBe listOf("p1", "p2")
            }
        }

        context("endQuest") {
            test("ending active quest transitions state") {
                val type = createQuestType("test:end")
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                fakeRepo.grantQuest("p1", type.key)

                val result = service.startQuest(type, party, listOf("p1"))
                val quest = (result as QuestResult.Success).quest

                service.endQuest(quest, EndReason.COMPLETE)
                quest.state shouldBe QuestState.COMPLETED
            }

            test("ending already-ended quest is no-op") {
                val type = createQuestType("test:double-end")
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                fakeRepo.grantQuest("p1", type.key)

                val result = service.startQuest(type, party, listOf("p1"))
                val quest = (result as QuestResult.Success).quest

                service.endQuest(quest, EndReason.COMPLETE)
                service.endQuest(quest, EndReason.CANCEL)
                quest.state shouldBe QuestState.COMPLETED
            }
        }

        context("updateProgress") {
            test("increments progress") {
                val type = createQuestType("test:prog", requirements = mapOf("kill" to 10))
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                fakeRepo.grantQuest("p1", type.key)

                val result = service.startQuest(type, party, listOf("p1"))
                val quest = (result as QuestResult.Success).quest

                service.updateProgress(quest, "kill", 3)
                quest.progresses["kill"] shouldBe 3
            }
        }

        context("cancelAll") {
            test("cancels all active quests") {
                val type1 = createQuestType("test:ca1")
                val type2 = createQuestType("test:ca2")
                val party1 = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                val party2 = FakeParty(leaderId = "p2", memberIds = setOf("p2"))
                fakeRepo.grantQuest("p1", type1.key)
                fakeRepo.grantQuest("p2", type2.key)

                service.startQuest(type1, party1, listOf("p1"))
                service.startQuest(type2, party2, listOf("p2"))

                service.cancelAll()
                service.activeQuestCount shouldBe 0
            }
        }
    })

// ---- Fake implementations ----

private class FakeQuestRepository : QuestRepository {
    private val granted = mutableSetOf<String>()
    var completionCount: Int = 0

    fun grantQuest(
        playerId: String,
        key: String,
    ) {
        granted.add("$playerId:$key")
    }

    override fun isGranted(
        playerId: String,
        questKey: String,
    ): Boolean = "$playerId:$questKey" in granted

    override fun getCompletionsSince(
        playerId: String,
        questKey: String,
        since: Instant,
    ): Int = completionCount

    override fun grant(
        playerId: String,
        questKey: String,
    ) = grantQuest(playerId, questKey)

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
    ): Int = completionCount

    override fun getMonthlyCompletions(
        playerId: String,
        questKey: String,
    ): Int = completionCount

    override fun getYearlyCompletions(
        playerId: String,
        questKey: String,
    ): Int = completionCount

    override fun isFirstCompletion(
        playerId: String,
        questKey: String,
    ): Boolean = completionCount == 0
}

private class FakeActionDispatcher : ActionDispatcher {
    val dispatched = mutableListOf<Pair<Action, String>>()

    override fun dispatch(
        action: Action,
        playerId: String,
    ) {
        dispatched.add(action to playerId)
    }

    override fun dispatchAll(
        actions: List<Action>,
        playerIds: List<String>,
    ) {
        playerIds.forEach { pid -> actions.forEach { a -> dispatch(a, pid) } }
    }
}

private class FakeScriptRunner : ScriptRunner {
    val ran = mutableListOf<Pair<Script, List<String>>>()

    override fun run(
        script: Script,
        playerIds: List<String>,
    ) {
        ran.add(script to playerIds)
    }
}

private class FakeQuestNotifier : QuestNotifier {
    val shownPlayers = mutableListOf<String>()
    val hiddenPlayers = mutableListOf<String>()

    override fun showQuestPanel(
        playerId: String,
        questKey: String,
    ) {
        shownPlayers.add(playerId)
    }

    override fun hideQuestPanel(playerId: String) {
        hiddenPlayers.add(playerId)
    }

    override fun sendMessage(
        playerId: String,
        message: String,
    ) {}
}

private class FakeParty(
    override val leaderId: String,
    override val memberIds: Set<String>,
) : DomainParty {
    override val size: Int get() = memberIds.size
    override val invitationSetting: InvitationSetting = InvitationSetting.LEADER

    override fun hasPermission(type: QuestType): Boolean {
        if (type.maxPlayers != null && size > type.maxPlayers) return false
        if (type.minPlayers != null && size < type.minPlayers) return false
        return true
    }
}

// ---- Test helpers ----

private fun createQuestType(
    key: String,
    maxPlayers: Int? = null,
    minPlayers: Int? = null,
    playLimits: PlayLimits = PlayLimits(),
    requirements: Map<String, Int> = emptyMap(),
): QuestType =
    QuestType(
        key = key,
        title = "Test Quest",
        icon = Icon(type = "STONE"),
        playLimits = playLimits,
        maxPlayers = maxPlayers,
        minPlayers = minPlayers,
        requirements = requirements.mapValues { (k, v) -> QuestRequirement(k, v) },
    )
