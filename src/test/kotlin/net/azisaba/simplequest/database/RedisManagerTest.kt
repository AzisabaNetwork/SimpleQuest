package net.azisaba.simplequest.database

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.azisaba.simplequest.data.RedisConfig

class RedisManagerTest :
    FunSpec({

        context("buildUri") {
            test("maps host/port from config (default user, no password)") {
                val manager = RedisManager(RedisConfig(host = "redis.local", port = 6380))
                val uri = manager.buildUri()
                uri.host shouldBe "redis.local"
                uri.port shouldBe 6380
                // Empty user defaults to the `default` Redis user (username is null on the URI).
                uri.username shouldBe null
                uri.password shouldBe null
            }

            test("maps ACL username when user is set") {
                val manager = RedisManager(RedisConfig(user = "cache", host = "redis.local", port = 6379))
                val uri = manager.buildUri()
                uri.username shouldBe "cache"
            }

            test("maps password when set") {
                val manager = RedisManager(RedisConfig(host = "redis.local", password = "secret"))
                val uri = manager.buildUri()
                uri.password?.concatToString() shouldBe "secret"
            }

            test("omits username and password when blank") {
                val manager = RedisManager(RedisConfig(user = "", password = ""))
                val uri = manager.buildUri()
                uri.username shouldBe null
                uri.password shouldBe null
            }
        }
    })
