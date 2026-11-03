package com.z_company.repository.remote_rest.tracks

/**
 * Ключи приложения для конверта участков (см. [SectionSeal]), по идентификатору ключа (kid). Хранятся тремя долями,
 * которые склеиваются XOR-ом при использовании: поиск строки по бинарнику ничего не даёт. Это затруднение, а не секрет:
 * ключ лежит в приложении. Настоящая защита - вход, квоты и водяной знак на сервере. Сервер ротирует kid без релиза
 * клиента, пока старый kid ещё есть здесь; новый kid добавляется доля-в-долю в следующем релизе.
 */
internal object SectionSealKeys {
    private val A1 = "\u0058\u0070\u007c\u00b3\u00cc\u00c9\u0068\u00a1\u00e6\u006f\u009a\u00ab\u0050\u001d\u00c2\u0092\u00eb\u0018\u00fb\u00a4\u00c8\u0053\u00b5\u00cd\u0099\u00f8\u00e8\u00ab\u0091\u0090\u00f0\u0019"
    private val B1 = "\u00b1\u00c3\u001c\u00d6\u00cd\u0054\u00b1\u00d4\u00dd\u007b\u0063\u0054\u00a0\u0076\u0092\u001e\u0074\u007e\u0086\u0083\u0005\u0082\u0007\u0075\u00eb\u003b\u00e6\u000e\u0013\u00ab\u00d6\u0059"
    private val C1 = "\u008c\u00fb\u003d\u003a\u00db\u000d\u00cf\u00c1\u0081\u00b1\u00fe\u009f\u00c6\u00b3\u00f7\u001c\u00eb\u008d\u000c\u00d6\u00a6\u0034\u00b0\u000f\u007e\u00a7\u002f\u0049\u004f\u0084\u00d7\u00c7"

    private fun share(s: String): ByteArray = ByteArray(s.length) { s[it].code.toByte() }

    fun forKid(kid: Int): ByteArray? = when (kid) {
        1 -> {
            val a = share(A1); val b = share(B1); val c = share(C1)
            ByteArray(32) { (a[it].toInt() xor b[it].toInt() xor c[it].toInt()).toByte() }
        }
        else -> null
    }
}
