package net.azisaba.simplequest

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.azisaba.simplequest.registry.Keyed
import net.azisaba.simplequest.registry.Registry
import net.kyori.adventure.key.Key

class RegistryTest :
    FunSpec({

        data class TestEntry(
            override val key: Key,
            val name: String,
        ) : Keyed

        lateinit var registry: Registry<TestEntry>

        beforeTest {
            registry = Registry()
        }

        test("register and get entry") {
            val entry = TestEntry(Key.key("test", "entry1"), "Entry 1")
            registry.register(entry)

            registry.get(Key.key("test", "entry1")) shouldBe entry
        }

        test("register returns the entry") {
            val entry = TestEntry(Key.key("test", "ret"), "Return")
            registry.register(entry) shouldBe entry
        }

        test("get returns null for missing key") {
            registry.get(Key.key("test", "nonexistent")).shouldBeNull()
        }

        test("unregister removes entry") {
            val entry = TestEntry(Key.key("test", "remove"), "Remove")
            registry.register(entry)
            registry.size shouldBe 1

            registry.unregister(Key.key("test", "remove")) shouldBe entry
            registry.size shouldBe 0
            registry.get(Key.key("test", "remove")).shouldBeNull()
        }

        test("unregister non-existent returns null") {
            registry.unregister(Key.key("test", "nope")).shouldBeNull()
        }

        test("clear empties registry") {
            registry.register(TestEntry(Key.key("test", "a"), "A"))
            registry.register(TestEntry(Key.key("test", "b"), "B"))
            registry.register(TestEntry(Key.key("test", "c"), "C"))
            registry.size shouldBe 3

            registry.clear()
            registry.size shouldBe 0
        }

        test("contains checks key existence") {
            registry.register(TestEntry(Key.key("test", "exists"), "Exists"))
            registry.get(Key.key("test", "exists")) shouldNotBe null
            registry.get(Key.key("test", "no")) shouldBe null
        }

        test("entries collection") {
            val a = TestEntry(Key.key("test", "a"), "A")
            val b = TestEntry(Key.key("test", "b"), "B")
            registry.register(a)
            registry.register(b)

            registry.entries.toList() shouldBe listOf(a, b)
        }

        test("keys property") {
            registry.register(TestEntry(Key.key("test", "x"), "X"))
            registry.keys shouldBe setOf(Key.key("test", "x"))
        }

        test("operator get works") {
            val entry = TestEntry(Key.key("test", "op"), "Op")
            registry.register(entry)

            registry.get(Key.key("test", "op")) shouldBe entry
            registry.get(Key.key("test", "missing")).shouldBeNull()
        }

        test("preserves insertion order") {
            val entries =
                (1..10).map { i ->
                    TestEntry(Key.key("test", "e$i"), "E$i")
                }
            entries.forEach { registry.register(it) }

            val ordered = registry.entries.toList()
            ordered.size shouldBe 10
            ordered.forEachIndexed { index, entry ->
                entry.key shouldBe Key.key("test", "e${index + 1}")
            }
        }

        test("registering same key twice overwrites") {
            val first = TestEntry(Key.key("test", "dup"), "First")
            val second = TestEntry(Key.key("test", "dup"), "Second")

            registry.register(first)
            registry.register(second)

            registry.size shouldBe 1
            registry.get(Key.key("test", "dup")) shouldBe second
        }

        test("size initially zero") {
            registry.size shouldBe 0
        }
    })
