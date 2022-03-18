package stack;

import kotlinx.atomicfu.AtomicRef;
import kotlinx.atomicfu.AtomicArray;
import java.util.Random;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private final AtomicRef<Node> head = new AtomicRef<>(null);
    private final AtomicArray<Integer> eliminationArray = new AtomicArray<>(50);
    private static final Random random = new Random(0);
    private static final int ELIMINATION_SIZE = 30, NEIGHBOURS_SIZE = 5, CHECK_TIME = 100;

    public boolean elimination(int x) {
        int index = random.nextInt(ELIMINATION_SIZE - NEIGHBOURS_SIZE);
        for (int i = index; i < index + NEIGHBOURS_SIZE; i++) {
            Integer saveX = x;
//            Ищем свободное место
            if (eliminationArray.get(i).compareAndSet(null, saveX)) {
//                Ожидаем что кто-то сможет забрать наше значение
                for (int j = 1; j < CHECK_TIME; j++) {
                    Integer curVal = eliminationArray.get(i).getValue();
//                    Если значение изменилось то произошел удачный обмен операций
                    if (curVal == null || !curVal.equals(saveX)) return false;
                }
//                Если значение не изменилось то операция выполняется в базовой версии стека
                return eliminationArray.get(i).compareAndSet(saveX, null);
            }
        }
        return false;
    }

    @Override
    public void push(int x) {
        if (elimination(x))
//        В случае неудачи идём в базовую версию стека
        while(true) {
            Node curHead = head.getValue();
            if (head.compareAndSet(curHead, new Node(x, curHead))) return;
        }
    }

    @Override
    public int pop() {
        int index = random.nextInt(ELIMINATION_SIZE - NEIGHBOURS_SIZE);
//        Сначала пытаемся найти себе пару в массиве для elimination-а
        for (int i = index; i < index + NEIGHBOURS_SIZE; i++) {
            Integer value = eliminationArray.get(i).getValue();
//            Если находим то произошел удачный обмен операций
            if (value != null && eliminationArray.get(i).compareAndSet(value, null)) return value;
        }
//        В случае неудачи идём в бозовую версию стека
        while(true) {
            Node curHead = head.getValue();
            if (curHead == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue())) return curHead.x;
        }
    }
}