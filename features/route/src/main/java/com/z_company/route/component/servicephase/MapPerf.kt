package com.z_company.route.component.servicephase

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.os.Trace
import android.util.Log

/**
 * Замеры горячих мест карты поезда. Секции видны в системной трассировке (Perfetto / Android Studio Profiler) под именем
 * `map:<имя>` всегда (Trace почти бесплатен), а в лог (тег `MapPerf`) попадают только в отладочной сборке и только если
 * заняли дольше [LOG_THRESHOLD_MS]. В логе - только имя секции и время: ни координат, ни данных пользователя.
 */
internal object MapPerf {
    private const val TAG = "MapPerf"
    private const val LOG_THRESHOLD_MS = 8L

    @Volatile
    private var debuggable: Boolean? = null

    private fun isDebug(context: Context?): Boolean {
        debuggable?.let { return it }
        val v = context != null && (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (context != null) debuggable = v
        return v
    }

    /** Замер блока [block]; [context] нужен только для определения отладочной сборки. */
    inline fun <T> measure(context: Context?, name: String, block: () -> T): T {
        val t0 = begin(name)
        try {
            return block()
        } finally {
            end(context, name, t0)
        }
    }

    fun begin(name: String): Long {
        Trace.beginSection("map:$name")
        return SystemClock.elapsedRealtime()
    }

    fun end(context: Context?, name: String, startedAt: Long) {
        Trace.endSection()
        val dt = SystemClock.elapsedRealtime() - startedAt
        if (dt >= LOG_THRESHOLD_MS && isDebug(context)) Log.d(TAG, "$name ${dt}ms")
    }
}
