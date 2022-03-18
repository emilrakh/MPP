import kotlinx.atomicfu.*

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<E>(INITIAL_CAPACITY))
    private val arraySize = atomic(0)

    override fun get(index: Int): E {
        require(index < size) { "Invalid index: $index" }
        while (true) {
            if (core.value.capacity <= index) continue
            return core.value.array[index].value ?: continue
        }
    }

    override fun put(index: Int, element: E) {
        require(index < size) { "Invalid index: $index" }
        while (true) {
            val curCore = core.value
            if (curCore.capacity <= index) continue
            val value = curCore.array[index].getAndSet(null)
            if (value != null) {
                curCore.array[index].compareAndSet(null, element)
                break
            }
        }
    }

    override fun pushBack(element: E) {
        val curSize = arraySize.getAndIncrement()
        while (true) {
            val curCore = core.value
            if (curSize < curCore.capacity) {
                if (curCore.array[curSize].compareAndSet(null, element)) return
            } else {
                // 1. создаем новую таблицу
                val newCore = Core<E>(curCore.capacity * 2)
                // 2. записываем ссылку на нее в next текущей
                if (!curCore.next.compareAndSet(null, newCore)) continue // критическая секция
                // 3. перемещаем все элементы
                for (i in 0 until curCore.capacity) {
                    while (true) {
                        val value = curCore.array[i].getAndSet(null)
                        if (value != null) {
                            newCore.array[i].compareAndSet(null, value)
                            break
                        }
                    }
                }
                // 4. обновляем cur_table со старого на новый
                core.value = newCore
            }
        }
    }

    override val size: Int get() {
        return arraySize.value
    }
}

private class Core<E>(val capacity: Int) {
    val array = atomicArrayOfNulls<E>(capacity)
    val next: AtomicRef<Core<E>?> = atomic(null)
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME