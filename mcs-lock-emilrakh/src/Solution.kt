import java.util.concurrent.atomic.*

class Solution(val env: Environment) : Lock<Solution.Node> {
    val v = AtomicReference<Node>(null)

    override fun lock(): Node {
        val my = Node() // сделали узел
        my.locked.value = true
        val value = v.getAndSet(my)

        if (value != null) {
            value.next.value = my
//            Усыпляем поток пока мы не понадобимся
            while (my.locked.value) env.park()
        }
        return my // вернули узел
    }

    override fun unlock(node: Node) {
//        Ожидаем пока нам проставят next
        if (v.compareAndSet(node, null)) return // срабатывает если узел один
        else {
            while (node.next.get() == null) {} // ждем пока кто-то проставит next(придут другие потоки)
        }
//        Даем разрешение на пробуждение
        node.next.value.locked.value = false
        env.unpark(node.next.value.thread)
    }

    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val locked = AtomicReference<Boolean>(false) // объявление переменной в классе
        val next = AtomicReference<Node>(null)
    }
}