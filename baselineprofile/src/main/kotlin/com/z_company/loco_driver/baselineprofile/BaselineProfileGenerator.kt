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
 * Запуск: ./gradlew :baselineprofile:generateBaselineProfile
 *
 * Профиль сохраняется в: app/src/release/generated/baselineProfiles/baseline-prof.txt
 * При следующей сборке release APK профиль автоматически встраивается,
 * и ART использует его для AOT-компиляции при установке.
 *
 * Эффект: cold start −30-40%, first-frame значительно быстрее,
 * исчезают JIT-spike (которые мы видели в трейсе на 154ms).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.z_company.loco_driver",
        includeInStartupProfile = true,
    ) {
        // Cold start
        pressHome()
        startActivityAndWait()

        // Ждём появления HomeScreen с реальными данными
        device.wait(Until.hasObject(By.res("com.z_company.loco_driver", "home_lazy_column")), 15_000)
        Thread.sleep(2000)

        // Скролл главного экрана — прогревает скроллинг + ItemHomeScreen
        val homeList = device.findObject(By.res("com.z_company.loco_driver", "home_lazy_column"))
        if (homeList != null) {
            repeat(2) {
                homeList.scroll(Direction.DOWN, 0.8f)
                Thread.sleep(300)
                homeList.scroll(Direction.UP, 0.8f)
                Thread.sleep(300)
            }
        }

        // Переход на AllRouteScreen — прогревает навигацию + AllRouteScreen
        val btnVse = device.findObject(By.text("Все"))
        if (btnVse != null) {
            btnVse.click()
            val hasAllRouteList = device.wait(
                Until.hasObject(By.res("com.z_company.loco_driver", "all_route_lazy_column")),
                10_000
            )
            if (hasAllRouteList) {
                Thread.sleep(1000)
                val allRouteList = device.findObject(
                    By.res("com.z_company.loco_driver", "all_route_lazy_column")
                )
                // Скролл AllRouteScreen
                repeat(3) {
                    allRouteList?.scroll(Direction.DOWN, 0.8f)
                    Thread.sleep(200)
                    allRouteList?.scroll(Direction.UP, 0.8f)
                    Thread.sleep(200)
                }
            }
            // Возврат — прогревает обратный переход (тот самый «лаг при возврате»)
            device.pressBack()
            device.wait(Until.hasObject(By.res("com.z_company.loco_driver", "home_lazy_column")), 5000)
        }
    }
}
