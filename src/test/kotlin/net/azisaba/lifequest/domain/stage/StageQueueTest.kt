package net.azisaba.lifequest.domain.stage

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class StageQueueTest :
    FunSpec({

        context("StageTask") {
            test("full constructor") {
                val task = StageTask(key = "task1", description = "Kill 10 zombies", isComplete = false)
                task.key shouldBe "task1"
                task.description shouldBe "Kill 10 zombies"
                task.isComplete shouldBe false
            }

            test("default isComplete is false") {
                val task = StageTask(key = "task2", description = "Collect items")
                task.isComplete shouldBe false
            }

            test("completed task") {
                val task = StageTask(key = "task3", description = "Done", isComplete = true)
                task.isComplete shouldBe true
            }

            test("task equality") {
                val a = StageTask("k", "desc", false)
                val b = StageTask("k", "desc", false)
                a shouldBe b
            }

            test("task inequality") {
                val a = StageTask("k1", "desc", false)
                val b = StageTask("k2", "desc", false)
                (a == b) shouldBe false
            }

            test("empty key") {
                val task = StageTask("", "")
                task.key shouldBe ""
                task.description shouldBe ""
            }

            test("long description") {
                val longDesc = "A".repeat(500)
                val task = StageTask("key", longDesc)
                task.description.length shouldBe 500
            }
        }

        context("Queue") {
            test("empty queue has size 0") {
                val q = Queue<FakeStage>()
                q.size shouldBe 0
                q.isEmpty shouldBe true
            }

            test("enqueue adds item") {
                val q = Queue<FakeStage>()
                val stage = FakeStage("a")
                q.enqueue(stage)
                q.size shouldBe 1
                q.isEmpty shouldBe false
            }

            test("peek returns first without removing") {
                val q = Queue<FakeStage>()
                val stage = FakeStage("first")
                q.enqueue(stage)
                q.peek() shouldBe stage
                q.size shouldBe 1
            }

            test("dequeue removes and returns first") {
                val q = Queue<FakeStage>()
                val a = FakeStage("a")
                val b = FakeStage("b")
                q.enqueue(a)
                q.enqueue(b)

                q.dequeue() shouldBe a
                q.size shouldBe 1
                q.peek() shouldBe b
            }

            test("peek on empty returns null") {
                val q = Queue<FakeStage>()
                q.peek().shouldBeNull()
            }

            test("dequeue on empty returns null") {
                val q = Queue<FakeStage>()
                q.dequeue().shouldBeNull()
            }

            test("FIFO order preserved") {
                val q = Queue<FakeStage>()
                val stages = (1..5).map { FakeStage("s$it") }
                stages.forEach { q.enqueue(it) }

                val dequeued = mutableListOf<FakeStage>()
                while (!q.isEmpty) {
                    dequeued.add(q.dequeue()!!)
                }
                dequeued shouldBe stages
            }

            test("clear empties and calls unmount") {
                val q = Queue<FakeStage>()
                val a = FakeStage("a")
                val b = FakeStage("b")
                q.enqueue(a)
                q.enqueue(b)

                q.clear()
                q.isEmpty shouldBe true
                q.size shouldBe 0
                a.unmountCalled shouldBe true
                b.unmountCalled shouldBe true
            }

            test("clear on empty queue is no-op") {
                val q = Queue<FakeStage>()
                q.clear()
                q.isEmpty shouldBe true
            }

            test("queue with initial items") {
                val stages = mutableListOf(FakeStage("a"), FakeStage("b"))
                val q = Queue(stages)
                q.size shouldBe 2
                q.peek()?.key shouldBe "a"
            }

            test("enqueue many items") {
                val q = Queue<FakeStage>()
                repeat(100) { q.enqueue(FakeStage("s$it")) }
                q.size shouldBe 100
            }

            test("dequeue until empty then peek") {
                val q = Queue<FakeStage>()
                q.enqueue(FakeStage("only"))
                q.dequeue()
                q.isEmpty shouldBe true
                q.peek().shouldBeNull()
                q.dequeue().shouldBeNull()
            }
        }

        context("FakeStage") {
            test("mount and unmount tracking") {
                val s = FakeStage("test")
                s.mountCalled shouldBe false
                s.unmountCalled shouldBe false
                s.mount()
                s.mountCalled shouldBe true
                s.unmount()
                s.unmountCalled shouldBe true
            }

            test("key accessor") {
                val s = FakeStage("mykey")
                s.key shouldBe "mykey"
            }
        }
    })

private class FakeStage(
    override val key: String,
) : StageLike {
    var mountCalled: Boolean = false
        private set
    var unmountCalled: Boolean = false
        private set

    override fun mount() {
        mountCalled = true
    }

    override fun unmount() {
        unmountCalled = true
    }
}
