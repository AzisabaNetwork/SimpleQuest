package net.azisaba.lifequest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import net.azisaba.lifequest.registry.Keyed
import net.azisaba.lifequest.registry.Registry
import net.kyori.adventure.key.Key

class RegistryExtTest :
    FunSpec({

        data class TestEntry(
            override val key: Key,
            val name: String,
        ) : Keyed

        lateinit var registry: Registry<TestEntry>

        beforeTest {
            registry = Registry()
        }

        // ---- constructor and size ----

        test("empty registry has size 0") {
            registry.size shouldBe 0
            registry.entries.toList() shouldBe emptyList()
            registry.keys shouldBe emptySet()
        }

        test("register returns the entry") {
            val e = TestEntry(Key.key("test", "ret"), "Return")
            registry.register(e) shouldBe e
        }

        // ---- overwrite behavior ----

        test("register same key twice updates and size stays 1") {
            val first = TestEntry(Key.key("test", "dup"), "First")
            val second = TestEntry(Key.key("test", "dup"), "Second")
            registry.register(first)
            registry.register(second)
            registry.size shouldBe 1
            registry.get(Key.key("test", "dup")) shouldBe second
        }

        test("register preserves insertion order after overwrite") {
            val a = TestEntry(Key.key("test", "a"), "A")
            val b = TestEntry(Key.key("test", "b"), "B")
            val a2 = TestEntry(Key.key("test", "a"), "A2")
            registry.register(a)
            registry.register(b)
            registry.register(a2)
            registry.entries.toList() shouldBe listOf(a2, b)
        }

        // ---- unregister ----

        test("unregister removes entry") {
            registry.register(TestEntry(Key.key("test", "to-remove"), "R"))
            registry.size shouldBe 1
            registry.unregister(Key.key("test", "to-remove"))
            registry.size shouldBe 0
            registry.get(Key.key("test", "to-remove")).shouldBeNull()
        }

        test("unregister missing key returns null") {
            registry.unregister(Key.key("test", "nope")).shouldBeNull()
        }

        // ---- clear ----

        test("clear after unregister is no-op") {
            registry.register(TestEntry(Key.key("test", "x"), "X"))
            registry.unregister(Key.key("test", "x"))
            registry.clear()
            registry.size shouldBe 0
        }

        // ---- get ----

        test("get returns null for missing key") {
            registry.get(Key.key("test", "nonexistent")).shouldBeNull()
        }

        // ---- keys and entries ----

        test("keys matches registered entries") {
            registry.register(TestEntry(Key.key("test", "a"), "A"))
            registry.register(TestEntry(Key.key("test", "b"), "B"))
            registry.keys shouldBe setOf(Key.key("test", "a"), Key.key("test", "b"))
        }

        // ---- large number of entries ----

        test("large number of entries") {
            val entries = (1..100).map { TestEntry(Key.key("test", "key-$it"), "Name-$it") }
            entries.forEach { registry.register(it) }
            registry.size shouldBe 100
            registry.keys.size shouldBe 100
            registry.entries.first().key shouldBe Key.key("test", "key-1")
            registry.entries.last().key shouldBe Key.key("test", "key-100")
        }

        // ---- interleaved register/unregister ----

        test("register and unregister interleaved") {
            registry.register(TestEntry(Key.key("test", "a"), "A"))
            registry.register(TestEntry(Key.key("test", "b"), "B"))
            registry.unregister(Key.key("test", "a"))
            registry.register(TestEntry(Key.key("test", "c"), "C"))
            registry.keys shouldBe setOf(Key.key("test", "b"), Key.key("test", "c"))
            registry.entries.toList().map { it.key } shouldBe listOf(Key.key("test", "b"), Key.key("test", "c"))
        }
    })
