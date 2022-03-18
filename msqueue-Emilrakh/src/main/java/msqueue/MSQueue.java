package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private final AtomicRef<Node> head;
    private final AtomicRef<Node> tail;

    public MSQueue() {
//        В начале оба указателя указывают на dummy
        Node dummy = new Node(0);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
//        Важное условие если очередь пуста голова и хвост указываеют на один элемент
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x);
        while(true) {
            Node curTail = tail.getValue();
//            Если следующего элемента нет то теперь мы хвост
            if (curTail.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(curTail, newTail);
                return;
            }
//            Если есть следующий элемент то двигаемся вправо
            tail.compareAndSet(curTail, curTail.next.getValue());
        }
    }

    @Override
    public int dequeue() {
        while(true) {
//            В данном случае нет необходимости помогать двигать хвост
            Node curHead = head.getValue();
            Node nextHead = curHead.next.getValue();
            if (nextHead == null) return Integer.MIN_VALUE;
//            Проверяем изменился ли head
            if (head.compareAndSet(curHead, nextHead)) return nextHead.x;
        }
    }

    @Override
    public int peek() {
//        В данном случае нет необходимости помогать двигать хвост
        Node curHead = head.getValue();
        Node nextHead = curHead.next.getValue();
        if (nextHead == null) return Integer.MIN_VALUE;
        return nextHead.x;
    }

    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x) {
            this.x = x;
            this.next = new AtomicRef<>(null);
        }
    }
}