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
 * Запуск: ./gradlew :app:generateReleaseBaselineProfile
 *
 * Профиль сохраняется в: app/src/main/baselineProfiles/baseline-prof.txt
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
        maxIterations = 5,
    ) {
        // ===== STARTUP =====
        // Прогревает: класс-загрузка, Koin DI, splash screen, MainActivity, первый Compose-tree
        pressHome()
        startActivityAndWait()

        // Ждём появления списка с реальными данными (на Samsung — есть данные)
        // или просто надолго ждём чтобы прогрелась инициализация Koin/Room/Ktor
        device.wait(
            Until.hasObject(By.res("com.z_company.loco_driver", "home_lazy_column")),
            20_000
        )
        Thread.sleep(3000)  // дополнительная пауза для асинхронной загрузки данных

        // ===== HOME SCROLL =====
        // Прогревает: ItemHomeScreen, LazyColumn, Modifier-цепочки, кешированные ресурсы
        val homeList = device.findObject(By.res("com.z_company.loco_driver", "home_lazy_column"))
        if (homeList != null) {
            repeat(2) {
                homeList.scroll(Direction.DOWN, 0.8f)
                Thread.sleep(300)
                homeList.scroll(Direction.UP, 0.8f)
                Thread.sleep(300)
            }
        }

        // ===== NAVIGATION HOME → ALL ROUTES =====
        // Прогревает: NavController, переход экранов, AllRouteViewModel, AllRouteScreen
        val btnVse = device.findObject(By.text("Все"))
        if (btnVse != null) {
            btnVse.click()
            val hasAllRouteList = device.wait(
                Until.hasObject(By.res("com.z_company.loco_driver", "all_route_lazy_column")),
                10_000
            )
            if (hasAllRouteList) {
                Thread.sleep(1500)
                val allRouteList = device.findObject(
                    By.res("com.z_company.loco_driver", "all_route_lazy_column")
                )

                // ===== ALL ROUTES SCROLL =====
                // Прогревает: ItemHomeScreen в контексте списка, scroll-механика
                repeat(3) {
                    allRouteList?.scroll(Direction.DOWN, 0.8f)
                    Thread.sleep(200)
                    allRouteList?.scroll(Direction.UP, 0.8f)
                    Thread.sleep(200)
                }
            }

            // ===== BACK NAVIGATION =====
            // Прогревает: возврат на HomeScreen — самая больная точка
            device.pressBack()
            device.wait(
                Until.hasObject(By.res("com.z_company.loco_driver", "home_lazy_column")),
                5000
            )
        }
    }
}
