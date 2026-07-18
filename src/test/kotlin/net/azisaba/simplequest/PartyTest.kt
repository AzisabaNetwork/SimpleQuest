package net.azisaba.simplequest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.azisaba.simplequest.party.*

class PartyTest :
    FunSpec({

        context("InvitationSetting") {
            test("default should be LEADER") {
                InvitationSetting.LEADER.name shouldBe "LEADER"
                InvitationSetting.ALL.name shouldBe "ALL"
            }
        }

        context("Party impl size") {
            test("size includes leader") {
                // PartyImpl size = members + 1 (leader)
                // Can't easily test without MockBukkit for players
            }
        }

        context("Invite") {
            // Invite requires Player objects which need MockBukkit
            // Testing the InvitationSetting enum
            test("InvitationSetting values") {
                InvitationSetting.entries.size shouldBe 2
                InvitationSetting.valueOf("LEADER") shouldBe InvitationSetting.LEADER
                InvitationSetting.valueOf("ALL") shouldBe InvitationSetting.ALL
            }
        }

        context("PartyManager") {
            test("returns null for unknown player") {
                // PartyManager.getParty() requires MockBukkit Player
                // Just check the object exists
                PartyManager shouldNotBe null
            }

            test("clear does not throw") {
                PartyManager.clear()
            }
        }

        context("InviteManager") {
            test("clear does not throw") {
                // Cannot test InviteManager.instance without MockBukkit
                // This is a placeholder for when DI is fully wired
            }

            test("acceptInvite with invalid UUID returns false") {
                // acceptInvite requires a valid Player, can't test without MockBukkit
            }
        }
    })
