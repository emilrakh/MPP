import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class FCPriorityQueue<E : Comparable<E>> {
    private val q = PriorityQueue<E>()
    private val SIZE = Thread.activeCount()
    private val combineArray = atomicArrayOfNulls<Operation<E>>(SIZE)
    private val index = ThreadLocalRandom.current().nextInt(SIZE)
    private val lock = atomic(false)

    private class Operation<E>(val operation: Type, var value: E? = null)

    private enum class Type {
        POLL, PEEK, ADD
    }

    private fun tryLock(): Boolean {
        return lock.compareAndSet(false, true)
    }

    private fun unlock() {
        lock.compareAndSet(true, false)
    }

    private fun combine(operation: Operation<E>): E? {
        while (true) {
            if (!combineArray[index].compareAndSet(null, operation)) continue
            if (tryLock()) { // критическая секция
                for (i in 0 until SIZE) {
                    val op = combineArray[i].value ?: continue
                    when (op.operation) {
                        Type.POLL -> op.value = q.poll()
                        Type.PEEK -> op.value = q.peek()
                        Type.ADD -> q.add(op.value)
                    }
                }
                unlock()
            }
            combineArray[index].value = null
            return operation.value
        }
    }

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        val operation = Operation<E>(Type.POLL)
        return combine(operation)
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        val operation = Operation<E>(Type.PEEK)
        return combine(operation)
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        val operation = Operation(Type.ADD, element)
        combine(operation)
    }
}