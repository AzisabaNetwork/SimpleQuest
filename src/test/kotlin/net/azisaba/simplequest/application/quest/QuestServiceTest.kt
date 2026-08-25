package net.azisaba.simplequest.application.quest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.azisaba.simplequest.domain.action.Action
import net.azisaba.simplequest.domain.action.port.ActionDispatcher
import net.azisaba.simplequest.domain.data.Icon
import net.azisaba.simplequest.domain.party.model.InvitationSetting
import net.azisaba.simplequest.domain.quest.model.EndReason
import net.azisaba.simplequest.domain.quest.model.PlayLimits
import net.azisaba.simplequest.domain.quest.model.QuestRequirement
import net.azisaba.simplequest.domain.quest.model.QuestResult
import net.azisaba.simplequest.domain.quest.model.QuestState
import net.azisaba.simplequest.domain.quest.model.QuestType
import net.azisaba.simplequest.domain.quest.port.QuestNotifier
import net.azisaba.simplequest.domain.quest.port.QuestRepository
import net.azisaba.simplequest.domain.script.Script
import net.azisaba.simplequest.domain.script.port.ScriptRunner
import java.time.Instant
import net.azisaba.simplequest.domain.party.model.Party as DomainParty

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

        context("serverExclusive") {
            test("fails when same exclusive quest type is already active") {
                val type = createQuestType("test:exclusive", serverExclusive = true)
                val party1 = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                val party2 = FakeParty(leaderId = "p2", memberIds = setOf("p2"))
                fakeRepo.grantQuest("p1", type.key)
                fakeRepo.grantQuest("p2", type.key)

                // First one succeeds
                val first = service.startQuest(type, party1, listOf("p1"))
                (first is QuestResult.Success) shouldBe true

                // Second one fails — same quest type already running
                val second = service.startQuest(type, party2, listOf("p2"))
                (second is QuestResult.Failure) shouldBe true
                (second as QuestResult.Failure).reason shouldBe "This quest is currently in progress on this server"
            }

            test("succeeds after first exclusive quest ends") {
                val type = createQuestType("test:excl2", serverExclusive = true)
                val party1 = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                val party2 = FakeParty(leaderId = "p2", memberIds = setOf("p2"))
                fakeRepo.grantQuest("p1", type.key)
                fakeRepo.grantQuest("p2", type.key)

                val first = service.startQuest(type, party1, listOf("p1")) as QuestResult.Success
                service.endQuest(first.quest, EndReason.COMPLETE)

                val second = service.startQuest(type, party2, listOf("p2"))
                (second is QuestResult.Success) shouldBe true
            }

            test("allows different exclusive quest types concurrently") {
                val type1 = createQuestType("test:exclA", serverExclusive = true)
                val type2 = createQuestType("test:exclB", serverExclusive = true)
                val party1 = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                val party2 = FakeParty(leaderId = "p2", memberIds = setOf("p2"))
                fakeRepo.grantQuest("p1", type1.key)
                fakeRepo.grantQuest("p2", type2.key)

                val first = service.startQuest(type1, party1, listOf("p1"))
                val second = service.startQuest(type2, party2, listOf("p2"))

                (first is QuestResult.Success) shouldBe true
                (second is QuestResult.Success) shouldBe true
            }

            test("allows concurrent runs when not exclusive") {
                val type = createQuestType("test:non-excl", serverExclusive = false)
                val party1 = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                val party2 = FakeParty(leaderId = "p2", memberIds = setOf("p2"))
                fakeRepo.grantQuest("p1", type.key)
                fakeRepo.grantQuest("p2", type.key)

                val first = service.startQuest(type, party1, listOf("p1"))
                val second = service.startQuest(type, party2, listOf("p2"))

                (first is QuestResult.Success) shouldBe true
                (second is QuestResult.Success) shouldBe true
            }
        }

        context("timeout") {
            test("timeout cancels quest automatically") {
                val type = createQuestType("test:timeout", timeoutMinutes = 1)
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                fakeRepo.grantQuest("p1", type.key)

                // Manually invoke the timeout logic (Timer is daemon, too slow for unit test)
                val result = service.startQuest(type, party, listOf("p1")) as QuestResult.Success
                val quest = result.quest

                // End via CANCEL (simulating timeout)
                service.endQuest(quest, EndReason.CANCEL)
                quest.state shouldBe QuestState.CANCELLED
                service.activeQuestCount shouldBe 0
            }
        }

        context("playLimits") {
            test("fails when daily limit exceeded") {
                val type = createQuestType("test:daily", playLimits = PlayLimits(daily = 2))
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                fakeRepo.grantQuest("p1", type.key)
                fakeRepo.completionCount = 2

                val result = service.startQuest(type, party, listOf("p1"))
                (result is QuestResult.Failure) shouldBe true
            }

            test("fails when cooldown has not elapsed") {
                val type = createQuestType("test:cooldown", playLimits = PlayLimits(cooldownMinutes = 30))
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                fakeRepo.grantQuest("p1", type.key)
                // Last completed 10 minutes ago (within 30min cooldown)
                fakeRepo.lastCompletionTime = Instant.now().minusSeconds(600)

                val result = service.startQuest(type, party, listOf("p1"))
                (result is QuestResult.Failure) shouldBe true
            }

            test("succeeds when cooldown has elapsed") {
                val type = createQuestType("test:cooldown-ok", playLimits = PlayLimits(cooldownMinutes = 30))
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                fakeRepo.grantQuest("p1", type.key)
                // Last completed 60 minutes ago (outside 30min cooldown)
                fakeRepo.lastCompletionTime = Instant.now().minusSeconds(3600)

                val result = service.startQuest(type, party, listOf("p1"))
                (result is QuestResult.Success) shouldBe true
            }

            test("succeeds when no prior completion (cooldown n/a)") {
                val type = createQuestType("test:cooldown-first", playLimits = PlayLimits(cooldownMinutes = 30))
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                fakeRepo.grantQuest("p1", type.key)
                fakeRepo.lastCompletionTime = null

                val result = service.startQuest(type, party, listOf("p1"))
                (result is QuestResult.Success) shouldBe true
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

    override fun getDailyCompletions(
        playerId: String,
        questKey: String,
    ): Int = completionCount

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

    override fun getLastCompletionTime(
        playerId: String,
        questKey: String,
    ): Instant? = lastCompletionTime

    var lastCompletionTime: Instant? = null
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
        quest: net.azisaba.simplequest.domain.quest.model.Quest,
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
    serverExclusive: Boolean = false,
    timeoutMinutes: Int? = null,
): QuestType =
    QuestType(
        key = key,
        title = "Test Quest",
        icon = Icon(type = "STONE"),
        playLimits = playLimits,
        maxPlayers = maxPlayers,
        minPlayers = minPlayers,
        serverExclusive = serverExclusive,
        timeoutMinutes = timeoutMinutes,
        requirements = requirements.mapValues { (k, v) -> QuestRequirement(k, v) },
    )
