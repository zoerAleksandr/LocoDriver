package com.z_company.loco_driver.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Генерирует Baseline Profile для критических путей приложения.
 *
 * Запуск: ANDROID_SERIAL=R58R625VJBP ./gradlew :app:generateReleaseBaselineProfile
 *
 * Профиль сохраняется в: app/src/main/baselineProfiles/baseline-prof.txt
 * При следующей сборке release APK профиль автоматически встраивается,
 * и ART использует его для AOT-компиляции при установке.
 *
 * Покрываемые сценарии:
 *   1. Cold start
 *   2. Скролл HomeScreen
 *   3. Переход HomeScreen → AllRouteScreen → скролл → возврат
 *   4. Переход HomeScreen → WorkScheduleScreen → скролл → возврат
 *   5. Переход HomeScreen → SelectReleaseDaysScreen (отвлечения) → скролл → возврат
 *
 * Эффект: cold start −30-40%, исчезают JIT-spike (которые мы видели в трейсе на 154ms).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    private val pkg = "com.z_company.loco_driver"

    @Test
    fun generate() = rule.collect(
        packageName = pkg,
        includeInStartupProfile = true,
        maxIterations = 5,
    ) {
        // ===== STARTUP =====
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.res(pkg, "home_lazy_column")), 20_000)
        Thread.sleep(3000)

        // ===== HOME SCROLL =====
        scrollList("home_lazy_column", times = 2)

        // ===== HOME → ALL ROUTES → BACK =====
        navigateAndScroll(
            buttonText = "Все",
            destinationTag = "all_route_lazy_column",
            scrollTimes = 3
        )

        // ===== HOME → WORK SCHEDULE → BACK =====
        // Карточка «График» на HomeScreen открывает WorkScheduleScreen (календарь графика работы)
        navigateAndScroll(
            buttonText = "График",
            destinationTag = "work_schedule_lazy_column",
            scrollTimes = 2
        )

        // ===== HOME → RELEASE DAYS (отвлечения) → BACK =====
        // Карточка «Отвлечения» открывает SelectReleaseDaysScreen
        navigateAndScroll(
            buttonText = "Отвлечения",
            destinationTag = "release_days_lazy_column",
            scrollTimes = 2
        )
    }

    /**
     * Тапает кнопку с текстом, ждёт открытия экрана с testTag, скроллит,
     * возвращается на HomeScreen.
     */
    private fun androidx.benchmark.macro.MacrobenchmarkScope.navigateAndScroll(
        buttonText: String,
        destinationTag: String,
        scrollTimes: Int
    ) {
        val btn = device.findObject(By.text(buttonText))
        if (btn == null) return
        btn.click()

        val opened = device.wait(Until.hasObject(By.res(pkg, destinationTag)), 10_000)
        if (opened) {
            Thread.sleep(1000)
            scrollList(destinationTag, times = scrollTimes)
        }

        // Возврат на HomeScreen
        device.pressBack()
        device.wait(Until.hasObject(By.res(pkg, "home_lazy_column")), 5000)
        Thread.sleep(500)
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.scrollList(
        tag: String,
        times: Int
    ) {
        val list = device.findObject(By.res(pkg, tag)) ?: return
        repeat(times) {
            list.scroll(Direction.DOWN, 0.8f)
            Thread.sleep(250)
            list.scroll(Direction.UP, 0.8f)
            Thread.sleep(250)
        }
    }
}
