package dijkstra

import kotlinx.atomicfu.atomic
import java.util.*
import java.util.concurrent.Phaser
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }
val activeNodes = atomic(1)

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    //    val q = PriorityQueue(workers, NODE_DISTANCE_COMPARATOR)
    val q = MultiQueue(workers, NODE_DISTANCE_COMPARATOR)
    q.add(start)
    activeNodes.incrementAndGet()
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (activeNodes.value > 0) {
                //System.out.println(Thread.currentThread().name)
                val cur: Node = q.poll() ?: break
                for (e in cur.outgoingEdges) {
                    while (e.to.distance > cur.distance + e.weight) {
                        if (!e.to.casDistance(e.to.distance, cur.distance + e.weight)) continue
                        q.add(e.to)
                        activeNodes.incrementAndGet()
                    }
                }
                activeNodes.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

class MultiQueue(private val initialCapacity: Int, comparator: Comparator<Node>) {
    private val queues: List<PriorityQueue<Node>> = Collections.nCopies(initialCapacity, PriorityQueue(comparator))
    private val random = Random(0)

    fun poll(): Node? {
        var cur: Node?

        val firstIndex = random.nextInt(initialCapacity)
        val secondIndex = random.nextInt(initialCapacity)
//        Ограничиваем доступ на две рандомные очереди
        synchronized(queues[firstIndex]) {
            synchronized(queues[secondIndex]) {
                cur = when {
//                    Сравнивая значения из очереди, берем меньшее
                    queues[firstIndex].isEmpty() && queues[secondIndex].isEmpty() -> null
                    queues[firstIndex].isEmpty() && queues[secondIndex] != null -> queues[secondIndex].poll()
                    queues[secondIndex].isEmpty() && queues[firstIndex] != null -> queues[firstIndex].poll()
                    queues[firstIndex].peek().distance < queues[secondIndex].peek().distance -> queues[firstIndex].poll()
                    else -> queues[secondIndex].poll()
                }
                return cur
            }
        }
    }

    fun add(node: Node) {
        val queue = queues[random.nextInt(initialCapacity)]
//        Ограничивая доступ к рандомной очреди добавляем к ней новый узел
        synchronized(queue) { queue.add(node) }
    }
}