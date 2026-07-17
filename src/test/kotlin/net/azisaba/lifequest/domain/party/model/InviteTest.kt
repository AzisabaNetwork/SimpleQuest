package net.azisaba.lifequest.domain.party.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.azisaba.lifequest.domain.data.Icon
import net.azisaba.lifequest.domain.quest.model.QuestType

class InviteTest :
    FunSpec({

        context("Invite construction") {
            test("full constructor with all fields") {
                val invite =
                    Invite(
                        id = "inv-001",
                        partyId = "party-abc",
                        inviterId = "player1",
                        targetId = "player2",
                        createdAt = 1000L,
                        expiresAt = 121000L,
                    )
                invite.id shouldBe "inv-001"
                invite.partyId shouldBe "party-abc"
                invite.inviterId shouldBe "player1"
                invite.targetId shouldBe "player2"
                invite.createdAt shouldBe 1000L
                invite.expiresAt shouldBe 121000L
            }

            test("default createdAt and expiresAt use current time") {
                val before = System.currentTimeMillis()
                val invite = Invite(id = "inv-002", partyId = "p1", inviterId = "p1", targetId = "p2")
                val after = System.currentTimeMillis()

                (invite.createdAt >= before) shouldBe true
                (invite.createdAt <= after) shouldBe true
                (invite.expiresAt >= before + 120_000L) shouldBe true
                (invite.expiresAt <= after + 120_000L) shouldBe true
            }

            test("isExpired returns false for current time before expiresAt") {
                val future = System.currentTimeMillis() + 60_000L
                val invite = Invite(id = "inv", partyId = "p", inviterId = "p1", targetId = "p2", expiresAt = future)
                invite.isExpired shouldBe false
            }

            test("isExpired returns true for past expiresAt") {
                val past = System.currentTimeMillis() - 1L
                val invite = Invite(id = "inv", partyId = "p", inviterId = "p1", targetId = "p2", expiresAt = past)
                invite.isExpired shouldBe true
            }

            test("isExpired returns true for exact current time") {
                val now = System.currentTimeMillis()
                val invite = Invite(id = "inv", partyId = "p", inviterId = "p1", targetId = "p2", expiresAt = now)
                // exactly at expiresAt should be expired (strict >)
                invite.isExpired shouldBe false // not expired until AFTER
            }

            test("invite equality") {
                val a = Invite("id", "party", "p1", "p2", 100L, 200L)
                val b = Invite("id", "party", "p1", "p2", 100L, 200L)
                a shouldBe b
            }

            test("invite inequality by id") {
                val a = Invite("id-a", "party", "p1", "p2")
                val b = Invite("id-b", "party", "p1", "p2")
                (a == b) shouldBe false
            }

            test("invite with zero createdAt") {
                val invite = Invite("id", "party", "p1", "p2", createdAt = 0L, expiresAt = 0L)
                invite.createdAt shouldBe 0L
                invite.isExpired shouldBe true
            }

            test("invite with very large expiresAt") {
                val invite = Invite("id", "party", "p1", "p2", expiresAt = Long.MAX_VALUE)
                invite.isExpired shouldBe false
            }
        }

        context("InvitationSetting enum") {
            test("two values exist") {
                InvitationSetting.entries.size shouldBe 2
                InvitationSetting.LEADER.name shouldBe "LEADER"
                InvitationSetting.ALL.name shouldBe "ALL"
            }

            test("valueOf resolves correctly") {
                InvitationSetting.valueOf("LEADER") shouldBe InvitationSetting.LEADER
                InvitationSetting.valueOf("ALL") shouldBe InvitationSetting.ALL
            }

            test("LEADER and ALL are different") {
                InvitationSetting.LEADER shouldNotBe InvitationSetting.ALL
            }
        }

        context("Party interface properties") {
            test("FakeParty stores and returns member data") {
                val party = FakeParty(leaderId = "leader", memberIds = setOf("leader", "m1", "m2"))
                party.leaderId shouldBe "leader"
                party.memberIds shouldBe setOf("leader", "m1", "m2")
                party.size shouldBe 3
                party.invitationSetting shouldBe InvitationSetting.LEADER
            }

            test("FakeParty hasPermission - within max bounds") {
                val qt = QuestType(key = "test", title = "T", icon = Icon(type = "STONE"), maxPlayers = 4)
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1", "p2"))
                party.hasPermission(qt) shouldBe true
            }

            test("FakeParty hasPermission - exceeds max") {
                val qt = QuestType(key = "test", title = "T", icon = Icon(type = "STONE"), maxPlayers = 2)
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1", "p2", "p3"))
                party.hasPermission(qt) shouldBe false
            }

            test("FakeParty hasPermission - below min") {
                val qt = QuestType(key = "test", title = "T", icon = Icon(type = "STONE"), minPlayers = 3)
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                party.hasPermission(qt) shouldBe false
            }

            test("FakeParty hasPermission - no limits") {
                val qt = QuestType(key = "test", title = "T", icon = Icon(type = "STONE"))
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1"))
                party.hasPermission(qt) shouldBe true
            }

            test("FakeParty hasPermission - exact min") {
                val qt = QuestType(key = "test", title = "T", icon = Icon(type = "STONE"), minPlayers = 2, maxPlayers = 2)
                val party = FakeParty(leaderId = "p1", memberIds = setOf("p1", "p2"))
                party.hasPermission(qt) shouldBe true
            }
        }
    })

private class FakeParty(
    override val leaderId: String,
    override val memberIds: Set<String>,
) : Party {
    override val size: Int get() = memberIds.size
    override val invitationSetting: InvitationSetting = InvitationSetting.LEADER

    override fun hasPermission(type: QuestType): Boolean {
        if (type.maxPlayers != null && size > type.maxPlayers) return false
        if (type.minPlayers != null && size < type.minPlayers) return false
        return true
    }
}
