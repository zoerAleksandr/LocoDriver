package com.z_company.core.ui.perf

import android.os.Build
import android.os.SystemClock
import android.os.Trace
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

/**
 * Замеры производительности без PII: только статические имена экранов и длительности.
 *
 * Как смотреть:
 *  - Perfetto / Android Studio Profiler: секции `screen_open:<имя>` (async) и
 *    `perf:<имя>` (sync) на вкладке приложения;
 *  - logcat: `adb shell setprop log.tag.ScreenPerf DEBUG`, затем
 *    `adb logcat -s ScreenPerf` — «<имя>: N ms» от первой композиции до второго кадра.
 */
object ScreenPerf {
    private const val TAG = "ScreenPerf"
    @PublishedApi
    internal const val MAX_NAME = 100

    /** Синхронная секция (только для кода, не пересекающего suspend-точки). */
    inline fun <T> section(name: String, block: () -> T): T {
        Trace.beginSection("perf:" + name.take(MAX_NAME))
        try {
            return block()
        } finally {
            Trace.endSection()
        }
    }

    internal fun begin(name: String): Long {
        val cookie = name.hashCode()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.beginAsyncSection("screen_open:" + name.take(MAX_NAME), cookie)
        }
        return SystemClock.elapsedRealtime()
    }

    internal fun end(name: String, startedAt: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.endAsyncSection("screen_open:" + name.take(MAX_NAME), name.hashCode())
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "$name: ${SystemClock.elapsedRealtime() - startedAt} ms")
        }
    }
}

/**
 * Замер «открытия экрана»: от первой композиции [name] до второго отрисованного кадра.
 * Ставится первой строкой в Destination-composable экрана.
 */
@Composable
fun ScreenOpenTrace(name: String) {
    val startedAt = remember(name) { ScreenPerf.begin(name) }
    LaunchedEffect(name) {
        try {
            withFrameNanos { }
            withFrameNanos { }
        } finally {
            ScreenPerf.end(name, startedAt)
        }
    }
}
