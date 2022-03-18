import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls

class FAAQueue<T> {
    private val head: AtomicRef<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicRef<Segment> // Tail pointer, similarly to the Michael-Scott queue

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(x: T) {
        while (true) {
            val curTail = tail.value
            val enqIdx = curTail.enqIdx.getAndIncrement()
            if (enqIdx < SEGMENT_SIZE) {
//                Если есть место то записываем значение
                if (curTail.elements[enqIdx].compareAndSet(null, x)) return
                continue
            }
//            Создаем сегмент
            val newTail = Segment(x)
            if (curTail.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(curTail, newTail)
                return
            }
//            Двигаем tail вправо
            tail.compareAndSet(curTail, curTail.next.value!!)
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    @Suppress("UNCHECKED_CAST")
    fun dequeue(): T? {
        while (true) {
            val curHead = head.value
            val deqIdx = curHead.deqIdx.getAndIncrement()
            if (deqIdx >= SEGMENT_SIZE) {
//                Двигаем head
                val nextHead = curHead.next.value ?: return null
                head.compareAndSet(curHead, nextHead)
                continue
            }
//            Ломаем ячейку
            return (curHead.elements[deqIdx].getAndSet(DONE) ?: continue) as T?
        }
    }

    /**
     * Returns `true` if this queue is empty;
     * `false` otherwise.
     */
    val isEmpty: Boolean get() {
        while (true) {
            val curHead = head.value
//            Если очередь переполнена
            if (curHead.isEmpty) {
                val nextHead = curHead.next.value ?: return true
//                Двигаем head
                head.compareAndSet(curHead, nextHead)
                continue
            } else {
                return false
            }
        }
    }
}

private class Segment {
    val next: AtomicRef<Segment?> = atomic(null)
    val enqIdx = atomic(0) // index for the next enqueue operation (T)
    val deqIdx = atomic(0) // index for the next dequeue operation (H)
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)

    constructor() // for the first segment creation

    constructor(x: Any?) { // each next new segment should be constructed with an element
        enqIdx.value = 1
        elements[0].value = x
    }

    val isEmpty: Boolean get() = deqIdx.value >= enqIdx.value || deqIdx.value >= SEGMENT_SIZE
}

private val DONE = Any() // Marker for the "DONE" slot state; to avoid memory leaks
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS