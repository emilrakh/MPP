package linked_list_set;

import kotlinx.atomicfu.AtomicRef;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class SetImpl implements Set {
    private static class Node {
        AtomicMarkableReference<Node> next;
        int key;

        Node(int key, Node next) {
//            Создаем объект с флагом и сслыкой на next
            this.next = new AtomicMarkableReference<>(next, false);
            this.key = key;
        }
    }

    private static class Window {
        Node cur, next;
    }
//    Данная модель гарантирует что при чтении next нам видны все предыдущие записи

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        retry:
        while(true) {
            Window w = new Window();
            w.cur = head;
            w.next = w.cur.next.getReference();
//            Создаем массив для флага
            boolean[] removed = new boolean[1];
            Node node;
            while (w.next.key < x) {
//                Атомарно читаем вершину и флаг
                node = w.next.next.get(removed);
//                Если флаг выставлен то передвигаем через логичесмки удаленные узлы
                if (removed[0]) {
//                    Удаляем физически
                    if (!w.cur.next.compareAndSet(w.next, node, false, false)) continue retry; // аналогично break
                    w.next = node;
                } else {
                    w.cur = w.next;
                    w.next = w.cur.next.getReference();
                }
            }
//            Также проверяем что next не удален логическм
            node = w.next.next.get(removed);
            if (!removed[0]) return w;
            else {
//                Удаляем физически
                w.cur.next.compareAndSet(w.next, node, false, false);
                w.next = node;
            }
        }
    }

    @Override
    public boolean add(int x) {
        while(true) {
            Window w = findWindow(x);
            if (w.next.key == x) return false;
//            В найденном окне cur может быть удален(вставляем элемент между узлами)
//            Если удалось поменять с next на новый узел и флаг false то все хорошо
            if (w.cur.next.compareAndSet(w.next, new Node(x, w.next), false, false)) return true;
        }
    }

    @Override
    public boolean remove(int x) {
        while(true) {
            Window w = findWindow(x);
            if (w.next.key != x) return false;
            Node node = w.next.next.getReference();
//            Если у нас не поменялось окно меняем флаг(Удаляем логически)
            if (w.next.next.compareAndSet(node, node, false, true)) {
//                Удаляем физически
                w.cur.next.compareAndSet(w.next, node, false, false);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
//        Гарантированно увидем какое-то состояние
        Window w = findWindow(x);
        return w.next.key == x;
    }
}