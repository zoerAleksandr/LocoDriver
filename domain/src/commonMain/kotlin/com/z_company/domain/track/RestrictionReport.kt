package com.z_company.domain.track

/** Правила отчёта о проследовании ограничений (PDF формируется в приложении, SCREEN_SPECS §21 / «Журнал проследования»). */
object RestrictionReport {
    /** Отчёт можно сформировать, когда у поезда есть записи и ВСЕ они проследованы. */
    fun isReady(entries: List<RestrictionPass>): Boolean {
        val live = entries.filter { !it.isDeleted }
        return live.isNotEmpty() && live.all { it.status == RestrictionStatus.DONE }
    }

    /** Сколько записей ещё не проследовано (для подсказки на неактивной кнопке). */
    fun remaining(entries: List<RestrictionPass>): Int = entries.count { !it.isDeleted && it.status != RestrictionStatus.DONE }

    /** Сколько ограничений скорости превышено. */
    fun exceededCount(entries: List<RestrictionPass>): Int = entries.count { !it.isDeleted && it.exceeded }
}
