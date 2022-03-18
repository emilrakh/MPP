import kotlinx.atomicfu.*

/**
 * Atomic block.
 */
fun <T> atomic(block: TxScope.() -> T): T {
    while (true) {
        val transaction = Transaction()
        try {
            val result = block(transaction)
            if (transaction.commit()) return result
            transaction.abort()
        } catch (e: AbortException) {
            transaction.abort()
        }
    }
}

/**
 * Transactional operations are performed in this scope.
 */
abstract class TxScope {
    abstract fun <T> TxVar<T>.read(): T
    abstract fun <T> TxVar<T>.write(x: T): T
}

/**
 * Transactional variable.
 */
class TxVar<T>(initial: T)  {
    private val loc = atomic(Loc(initial, initial, rootTx))

    /**
     * Opens this transactional variable in the specified transaction [tx] and applies
     * updating function [update] to it. Returns the updated value.
     */
    fun openIn(tx: Transaction, update: (T) -> T): T {
        // todo: FIXME: this implementation does not actually implement transactional update
//        Забираем транзакцию себе
        while (true) {
            val curLoc = loc.value // читаем владельца

            val curValue = curLoc.valueIn(tx) { // забираем текущее значение
                owner -> contention(tx, owner) // если владелец не мы то отменяем другую транзакцию
            }
            if (curValue === TxStatus.ACTIVE) continue // пытаемся орткрыть операцию
            val updValue = update(curValue as T) // обновляет новое значение
            val updLoc = Loc(curValue, updValue, tx)
            if (loc.compareAndSet(curLoc, updLoc)) { // обновляем указатель
                if (tx.status == TxStatus.ABORTED) throw AbortException // проверяем не стали ли мы зомби
                return updValue
            }
        }
    }

//    Отмена другую транзакцию
    fun contention(tx: Transaction, owner: Transaction) {
        owner.abort() // obstraction-free
    }
}

/**
 * State of transactional value
 */
private class Loc<T>(
    val oldValue: T,
    val newValue: T,
    val owner: Transaction // корневая транзакция
) {
    fun valueIn(tx: Transaction, onActive: (Transaction) -> Unit): Any? =
        if (owner === tx) newValue else // читаем значени
            when (owner.status) { // если мы не владелец
                TxStatus.ABORTED -> oldValue
                TxStatus.COMMITTED -> newValue
                TxStatus.ACTIVE -> { // хитрое поведение
                    onActive(owner)
                    TxStatus.ACTIVE
                }
            }
}

private val rootTx = Transaction().apply { commit() }

/**
 * Transaction status.
 */
enum class TxStatus { ACTIVE, COMMITTED, ABORTED }

/**
 * Transaction implementation.
 */
//Атомарный статус операции
class Transaction : TxScope() {
    private val _status = atomic(TxStatus.ACTIVE)
    val status: TxStatus get() = _status.value

//    Меняет статус, считается, что выполнено логически, но не физически
    fun commit(): Boolean =
        _status.compareAndSet(TxStatus.ACTIVE, TxStatus.COMMITTED)

    fun abort() {
        _status.compareAndSet(TxStatus.ACTIVE, TxStatus.ABORTED)
    }

    override fun <T> TxVar<T>.read(): T = openIn(this@Transaction) { it }
    override fun <T> TxVar<T>.write(x: T) = openIn(this@Transaction) { x }
}

/**
 * This exception is thrown when transaction is aborted.
 */
private object AbortException : Exception() {
    override fun fillInStackTrace(): Throwable = this
}