/**
 * @author :TODO: Rakhmangulov Emil
 */
class Solution : AtomicCounter {
    // объявите здесь нужные вам поля
    private val root = Node(0)
    private val last: ThreadLocal<Node> = ThreadLocal.withInitial { root }

    override fun getAndAdd(x: Int): Int {
        // напишите здесь код
        while (true) {
            val old = last.get()
            val node = Node(old.x + x )
//            Возвращает последнее значение узла
            val lastNode = old.next.decide(node) // все потоки должны вернуть одно и то же значение из метода decide
            last.set(lastNode)
            if (lastNode == node) return old.x
        }
    }

    // вам наверняка потребуется дополнительный класс
    private class Node(val x: Int) {
        val next: Consensus<Node> = Consensus()
    }
}