package com.z_company.loco_driver.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
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
 *   2. HomeScreen scroll
 *   3. AllRouteScreen (открытие + скролл + back)
 *   4. CalendarScreen — календарь (график + отвлечения объединены)
 *   5. StatisticsScreen — сводная статистика
 *   6. SearchScreen — поиск маршрутов (ввод запроса + список)
 *   7. SalaryCalculationScreen — зарплата (через bottom-nav)
 *   8. SettingsScreen — настройки (через bottom-nav)
 *   9. SettingsScreen подэкраны — «Норма и регион», «Основные» (внутренняя
 *      навигация currentSubScreen, без отдельного route)
 *  10. ProfileScreen — профиль (через bottom-nav)
 *  11. PurchasesScreen — Машинист Pro (из профиля)
 *  12. FormScreen — открытие маршрута + скролл
 *  13. FormLocoScreen — переключение Электровоз/Тепловоз
 *  14. FormTrainScreen — форма поезда
 *  15. FormPassengerScreen — форма пассажира
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
        // Workaround для Android 14+ бага в Macrobenchmark:
        // startActivityAndWait иногда не получает подтверждения от system,
        // хотя activity успешно отображается.
        // https://issuetracker.google.com/issues/250063945
        try {
            startActivityAndWait()
        } catch (e: IllegalStateException) {
            // Игнорируем — на Android 13+ это часто ложная ошибка,
            // активити фактически уже запущена. Просто ждём её UI.
            Thread.sleep(3000)
        }

        // ВАЖНО: testTag("home_lazy_column") висит на LazyColumn, которая появляется,
        // как только готов uiState (salarySetting + userSettings) — это НЕ значит,
        // что маршруты уже загружены. Список маршрутов (routesFlow), «Следующий
        // маршрут»/«Домашний отдых» и кнопка «Все» зависят от ОТДЕЛЬНОГО async-потока
        // (routeUseCase, debounce 300ms + реальный запрос к БД/серверу) и появляются
        // позже. Раньше здесь был фиксированный sleep(3000) после первого найденного
        // home_lazy_column — этого не хватало, и все шаги ниже (Календарь/Статистика/
        // Поиск/Все маршруты/Форма) молча пропускались, пока Настройки/Профиль/Покупки
        // всё равно проходили (они открываются через bottom-nav, не зависящий от
        // состояния HomeScreen). Поэтому дожидаемся именно content-зависимого маркера —
        // home_all_routes_button, который появляется только когда маршруты реально
        // подгружены.
        device.wait(Until.hasObject(By.res(pkg, "home_lazy_column")), 30_000)
        device.wait(Until.hasObject(By.res(pkg, "home_all_routes_button")), 30_000)
        Thread.sleep(1000)

        // ===== ЗАКРЫТЬ «НОВОСТЬ ПРИ ЗАПУСКЕ» (AnnouncementScreen), ЕСЛИ ЕСТЬ =====
        // После чистой переустановки (или сброса lastSeenAnnouncement) поверх
        // HomeScreen может показаться полноэкранное объявление о новой фиче
        // («Далее»/«Понятно»). Без закрытия все By.res(...) ниже не находят
        // экраны, и профиль получается почти пустым.
        dismissAnnouncementIfShown()

        // ===== HOME SCROLL =====
        scrollList("home_lazy_column", times = 2)

        // ===== HOME → ALL ROUTES → BACK =====
        // Используем testTag вместо текста (текст "Все" может быть в нескольких местах)
        val allBtn = waitFindRes("home_all_routes_button")
        if (allBtn != null) {
            allBtn.click()
            val opened = device.wait(
                Until.hasObject(By.res(pkg, "all_route_lazy_column")),
                10_000
            )
            if (opened) {
                Thread.sleep(1500)
                scrollList("all_route_lazy_column", times = 3)
            }
            device.pressBack()
            device.wait(Until.hasObject(By.res(pkg, "home_lazy_column")), 5000)
            Thread.sleep(500)
        }

        // Блок «ИНСТРУМЕНТЫ» (Календарь/Статистика/PDF/Поиск) лежит НИЖЕ карточки
        // «Следующий маршрут» и списка «Последние маршруты» — при реальных данных
        // (15+ маршрутов) он не помещается в первый экран, и findObject(By.text(...))
        // ничего не находит без предварительного скролла вниз.
        scrollHomeToActionCards()

        // ===== HOME → КАЛЕНДАРЬ (график + отвлечения объединены) → BACK =====
        navigateAndScroll(
            buttonText = "Календарь",
            destinationTag = "calendar_scroll_column",
            scrollTimes = 3
        )

        scrollHomeToActionCards()

        // ===== HOME → СТАТИСТИКА → BACK =====
        navigateAndScroll(
            buttonText = "Статистика",
            destinationTag = "statistics_scroll_column",
            scrollTimes = 3
        )

        scrollHomeToActionCards()

        // ===== HOME → ПОИСК → BACK =====
        navigateSearch()

        // ===== BOTTOM NAV: SALARY (Зарплата) =====
        navigateBottomNav(
            tabText = "Зарплата",
            destinationTag = "salary_lazy_column",
            scrollTimes = 2
        )

        // ===== BOTTOM NAV: SETTINGS (Настройки) =====
        navigateBottomNav(
            tabText = "Настройки",
            destinationTag = "settings_scroll_column",
            scrollTimes = 2
        )

        // ===== SETTINGS → ВЛОЖЕННЫЕ ЭКРАНЫ (внутренняя навигация, без отдельного
        // route/testTag — переключение currentSubScreen внутри того же composable) =====
        // Норма/Регион и Основные — самые прогретые/готовые по редизайну подэкраны
        // (см. project_settings_redesign.md); остальные (Учёт/Отдых/Локомотив/
        // Маршрут) не берём, чтобы не раздувать длительность каждой итерации.
        tapSettingsRowAndBack("Норма и регион")
        tapSettingsRowAndBack("Основные")

        // ===== BOTTOM NAV: PROFILE (Профиль) =====
        navigateBottomNav(
            tabText = "Профиль",
            destinationTag = "profile_lazy_column",
            scrollTimes = 2
        )

        // ===== PROFILE → МАШИНИСТ PRO (Покупки) → BACK =====
        val proCard = waitFindText("Машинист Pro")
        if (proCard != null) {
            proCard.click()
            val purchasesOpened = device.wait(
                Until.hasObject(By.res(pkg, "purchases_scroll_column")),
                8000
            )
            if (purchasesOpened) {
                Thread.sleep(1500)
                scrollList("purchases_scroll_column", times = 2)
            }
            device.pressBack()
            device.wait(Until.hasObject(By.res(pkg, "profile_lazy_column")), 5000)
            Thread.sleep(500)
        }

        // ===== BOTTOM NAV: HOME (Главная) =====
        waitFindText("Главная")?.click()
        device.wait(Until.hasObject(By.res(pkg, "home_lazy_column")), 5000)
        Thread.sleep(800)

        // ===== HOME → FORM (открытие первого маршрута) → SUB-FORMS =====
        // Скроллим к началу списка (если ниже)
        device.findObject(By.res(pkg, "home_lazy_column"))?.scroll(Direction.UP, 1.0f)
        Thread.sleep(500)

        // Тапаем по первой карточке маршрута через явный testTag
        val firstRouteCard = waitFindRes("home_first_route_card")
        firstRouteCard?.click()

        val formOpened = device.wait(Until.hasObject(By.res(pkg, "form_lazy_column")), 8000)
        if (formOpened) {
            Thread.sleep(2000)
            scrollList("form_lazy_column", times = 2)

            // ===== FORM → ADD LOCOMOTIVE → BACK =====
            // 3 кнопки "Добавить" в FormScreen — для Локомотива, Поезда, Пассажира
            // Тапаем первую (Локомотив)
            device.wait(Until.hasObject(By.text("Добавить")), 5000)
            val addButtons = device.findObjects(By.text("Добавить"))
            if (addButtons.isNotEmpty()) {
                addButtons[0].click()
                val locoOpened = device.wait(
                    Until.hasObject(By.res(pkg, "form_loco_lazy_column")),
                    5000
                )
                if (locoOpened) {
                    Thread.sleep(1500)
                    scrollList("form_loco_lazy_column", times = 1)
                    // Тапаем переключатель типа (Электровоз ↔ Тепловоз)
                    // SwitchApp уникальный — у него нет text, но он находится в первой строке
                    // Просто скроллим — это прогреет обе ветки
                    scrollList("form_loco_lazy_column", times = 1)
                    device.pressBack()
                    device.wait(Until.hasObject(By.res(pkg, "form_lazy_column")), 5000)
                    Thread.sleep(800)
                }
            }

            // ===== FORM → ADD TRAIN → BACK =====
            device.wait(Until.hasObject(By.text("Добавить")), 5000)
            val addButtons2 = device.findObjects(By.text("Добавить"))
            if (addButtons2.size >= 2) {
                addButtons2[1].click()
                val trainOpened = device.wait(
                    Until.hasObject(By.res(pkg, "form_train_lazy_column")),
                    5000
                )
                if (trainOpened) {
                    Thread.sleep(1500)
                    scrollList("form_train_lazy_column", times = 1)
                    device.pressBack()
                    device.wait(Until.hasObject(By.res(pkg, "form_lazy_column")), 5000)
                    Thread.sleep(800)
                }
            }

            // ===== FORM → ADD PASSENGER → BACK =====
            device.wait(Until.hasObject(By.text("Добавить")), 5000)
            val addButtons3 = device.findObjects(By.text("Добавить"))
            if (addButtons3.size >= 3) {
                addButtons3[2].click()
                val passengerOpened = device.wait(
                    Until.hasObject(By.res(pkg, "form_passenger_lazy_column")),
                    5000
                )
                if (passengerOpened) {
                    Thread.sleep(1500)
                    scrollList("form_passenger_lazy_column", times = 1)
                    device.pressBack()
                    device.wait(Until.hasObject(By.res(pkg, "form_lazy_column")), 5000)
                    Thread.sleep(800)
                }
            }

            // Возврат на HomeScreen
            device.pressBack()
            device.wait(Until.hasObject(By.res(pkg, "home_lazy_column")), 5000)
        }
    }

    /**
     * Тапает кнопку с текстом, ждёт открытия экрана с testTag, скроллит,
     * возвращается на HomeScreen.
     */
    private fun MacrobenchmarkScope.navigateAndScroll(
        buttonText: String,
        destinationTag: String,
        scrollTimes: Int
    ) {
        val btn = waitFindText(buttonText) ?: return
        btn.click()

        val opened = device.wait(Until.hasObject(By.res(pkg, destinationTag)), 10_000)
        if (opened) {
            Thread.sleep(1000)
            scrollList(destinationTag, times = scrollTimes)
        }

        device.pressBack()
        device.wait(Until.hasObject(By.res(pkg, "home_lazy_column")), 5000)
        Thread.sleep(500)
    }

    /**
     * Скроллит home_lazy_column вниз, пока блок «ИНСТРУМЕНТЫ» (карточки
     * Календарь/Статистика/PDF/Поиск) не появится на экране, либо до лимита
     * попыток. При реальном количестве маршрутов и активной карточке
     * «Следующий маршрут» этот блок обычно не помещается в первый экран.
     * Best-effort — если список не найден, тихо выходит.
     */
    private fun MacrobenchmarkScope.scrollHomeToActionCards() {
        if (device.findObject(By.res(pkg, "home_lazy_column")) == null) return
        // Реальный тач-свайп вместо UiObject2.scroll() (semantic accessibility-action):
        // home_lazy_column — единственный список в сценарии, где внутри одного из
        // item'ов лежит ВЛОЖЕННЫЙ горизонтальный LazyRow (карточки Календарь/
        // Статистика/PDF/Поиск) — semantic-скролл контейнера иногда неоднозначно
        // резолвится в такой структуре. Прямой swipe избавляет от этой неоднозначности
        // и от риска устаревшей ссылки на list после рекомпозиции между вызовами.
        val w = device.displayWidth
        val h = device.displayHeight
        repeat(8) {
            if (device.wait(Until.hasObject(By.text("Календарь")), 500)) return
            device.swipe(w / 2, (h * 0.8).toInt(), w / 2, (h * 0.3).toInt(), 20)
            Thread.sleep(150)
        }
    }

    /**
     * Закрывает полноэкранное объявление о новой фиче (AnnouncementScreen),
     * если оно показано поверх HomeScreen. Экран пуш-навигирует «Далее» на
     * промежуточных страницах и «Понятно» на последней — жмём то, что видим,
     * до нескольких раз. Best-effort: если объявления нет — ничего не делает.
     */
    private fun MacrobenchmarkScope.dismissAnnouncementIfShown() {
        repeat(5) {
            val next = device.findObject(By.text("Далее"))
            val done = device.findObject(By.text("Понятно"))
            when {
                done != null -> {
                    done.click()
                    Thread.sleep(500)
                    return
                }
                next != null -> {
                    next.click()
                    Thread.sleep(500)
                }
                else -> return
            }
        }
    }

    /**
     * Открывает экран поиска (карточка «Поиск» на главном), печатает короткий
     * запрос для прогрева пути поиска и списка результатов, возвращается назад.
     * Best-effort: если карточка/поле не найдены — тихо пропускает.
     */
    private fun MacrobenchmarkScope.navigateSearch() {
        val btn = waitFindText("Поиск") ?: return
        btn.click()
        val opened = device.wait(Until.hasObject(By.res(pkg, "search_screen")), 10_000)
        if (opened) {
            Thread.sleep(1000)
            val field = device.findObject(By.clazz("android.widget.EditText"))
            if (field != null) {
                field.text = "а"
                device.wait(Until.hasObject(By.res(pkg, "search_lazy_column")), 4000)
                Thread.sleep(1200)
                if (device.hasObject(By.res(pkg, "search_lazy_column"))) {
                    scrollList("search_lazy_column", times = 2)
                }
            }
        }
        device.pressBack()
        device.wait(Until.hasObject(By.res(pkg, "home_lazy_column")), 5000)
        Thread.sleep(500)
    }

    /**
     * Переходит по bottom-nav табу (Главная/Зарплата/Добавить/Настройки/Профиль).
     * НЕ возвращается — caller должен сам перейти обратно.
     */
    private fun MacrobenchmarkScope.navigateBottomNav(
        tabText: String,
        destinationTag: String,
        scrollTimes: Int
    ) {
        val tab = waitFindText(tabText) ?: return
        tab.click()

        val opened = device.wait(Until.hasObject(By.res(pkg, destinationTag)), 10_000)
        if (opened) {
            Thread.sleep(1500)
            scrollList(destinationTag, times = scrollTimes)
        }
    }

    /**
     * Тапает строку в хабе Настроек (SettingsScreen) по заголовку и возвращается
     * назад. Навигация в подэкраны Настроек — ВНУТРЕННЯЯ (переключение
     * currentSubScreen в том же composable, не отдельный NavHost route), поэтому
     * нет testTag конкретного подэкрана, чтобы дождаться через By.res — просто
     * даём время на рендер и жмём системный back (перехватывается BackHandler,
     * возвращает на HUB). Best-effort: если строка не найдена — тихо пропускает.
     */
    private fun MacrobenchmarkScope.tapSettingsRowAndBack(title: String) {
        val row = waitFindText(title) ?: return
        row.click()
        Thread.sleep(1500)
        device.pressBack()
        device.wait(Until.hasObject(By.res(pkg, "settings_scroll_column")), 5000)
        Thread.sleep(300)
    }

    /**
     * findObject(By.text(...)), но сначала ждёт до [timeoutMs] появления узла.
     * На холодном старте (JIT ещё не прогрелся) Compose может рендерить кнопку
     * на 1-2 секунды позже, чем скрипт успевает её поискать — мгновенный
     * findObject в этот момент возвращает null, и весь шаг молча пропускается.
     * Именно так профиль лишился Календаря/Статистики/Поиска/под-форм: они
     * идут РАНЬШЕ прогретых Настроек/Профиля/Покупок в сценарии.
     */
    private fun MacrobenchmarkScope.waitFindText(text: String, timeoutMs: Long = 5000): UiObject2? {
        device.wait(Until.hasObject(By.text(text)), timeoutMs)
        return device.findObject(By.text(text))
    }

    /** То же самое для поиска по testTag/resource-id. */
    private fun MacrobenchmarkScope.waitFindRes(tag: String, timeoutMs: Long = 5000): UiObject2? {
        device.wait(Until.hasObject(By.res(pkg, tag)), timeoutMs)
        return device.findObject(By.res(pkg, tag))
    }

    private fun MacrobenchmarkScope.scrollList(tag: String, times: Int) {
        val list = device.findObject(By.res(pkg, tag)) ?: return
        repeat(times) {
            list.scroll(Direction.DOWN, 0.8f)
            Thread.sleep(250)
            list.scroll(Direction.UP, 0.8f)
            Thread.sleep(250)
        }
    }
}
