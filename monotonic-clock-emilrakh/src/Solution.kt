/**
 * В теле класса решения разрешено использовать только переменные делегированные в класс RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author : Rakhmangulov Emil
 */
class Solution : MonotonicClock {
        private var c1 by RegularInt(0)
        private var c2 by RegularInt(0)
        private var c3 by RegularInt(0)
        private var c4 by RegularInt(0)
        private var c5 by RegularInt(0)
        private var c6 by RegularInt(0)

        override fun write(time: Time) {
                c1 = time.d1
                c2 = time.d2
                c3 = time.d3

                c6 = time.d3
                c5 = time.d2
                c4 = time.d1
        }

        override fun read(): Time {
                val h1 = c4
                val m2 = c5
                val s3 = c5

                val s6 = c3
                val m5 = c2
                val h4 = c1

                val time: Time = when {
                        h4 != h1 -> Time(h4, 0, 0)
                        m5 != m2 -> Time(h1, m5, 0)
                        s6 != s3 -> Time(h1, m2, s6)
                        else -> Time(h1, m2, s3)
                }
                return time
        }
}