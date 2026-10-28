package com.z_company.loco_driver.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Замер холодного старта без профиля и с Baseline Profile — чтобы видеть эффект
 * профиля в миллисекундах, а не на глаз.
 *
 * Запуск (release-подобная сборка, устройство с входом в аккаунт):
 *   ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=com.z_company.loco_driver.baselineprofile.StartupBenchmark
 * Смотреть timeToInitialDisplayMs: startupCompilationNone vs startupBaselineProfile.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupCompilationNone() = startup(CompilationMode.None())

    @Test
    fun startupBaselineProfile() =
        startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = "com.z_company.loco_driver",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = 8,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
