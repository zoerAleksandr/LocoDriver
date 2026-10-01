# IOS_UI_GUIDE.md — как делать экраны iOS

Этот документ — правила для сессий («волн»), которые реализуют экраны iOS-версии
LocoDriver поверх фундамента (этап 0): дизайн-кит, общие компоненты, навигация.
Читать вместе с `CLAUDE.md` / `AGENTS.md` и `SCREEN_SPECS.md`.

---

## 1. Источники правды

| Что | Откуда | Кто прав при конфликте |
|---|---|---|
| **Содержание и поведение** (что на экране, тексты, расчёты, хранение, валидации, переходы) | `SCREEN_SPECS.md` | **спека** |
| **Внешний вид** (цвета, шрифты, отступы, радиусы, тени, форма компонентов) | дизайн-кит `iosApp/iosApp/Shared/Design` + `Shared/Components` и iOS-макет в `design/src/*.jsx` | кит/макет |
| **Эталон реализации** (как ведёт себя в проде) | Android: `features/route/src/main/java/com/z_company/route/ui/*Screen.kt` + `viewmodel/*` | спека (Android-баги — см. п. 2.5) |

## 2. Правила

1. **Содержание и поведение — строго по `SCREEN_SPECS.md`.** Внешний вид — по
   дизайн-киту и iOS-макету из `design/`.
2. **Элемент есть в спеке, но нет в макете** — собирать из компонентов кита по логике
   спеки, в стиле соседних экранов (те же карточки, заголовки групп, строки, шторки).
   Новые «одноразовые» стили не изобретать.
3. **Макет противоречит спеке по содержанию** (другой текст, лишнее/отсутствующее поле,
   другое поведение) — **прав текст спеки**. Пример: в макете таб «Главный»/«Поездки», в
   спеке §3.1 — «Главная» → в коде «Главная». В макетах есть демонстрационные данные
   и элементы, которых нет в Android (например, кнопка «Сохранить» в навбаре маршрута при
   автосохранении, §3.3) — их не переносить.
4. **Несколько вариантов экрана в макете** (`IOSPaywallA/B/C`, `IOSStatsA/B/C`) — выбрать
   вариант, **ближайший к текущему Android-экрану** (состав блоков и порядок), и указать
   выбор с обоснованием в описании PR.
5. **Баги Android** (приложение спеки «Баги Android на проверку», `BUG-NN`, в тексте —
   «⚠️ Поведение Android (возможный баг, BUG-NN)») — **переносить как есть** (паритет с
   Android — решение владельца). В коде рядом оставлять комментарий `// BUG-NN (паритет с Android)`.
6. Любое изменение поведения экрана из спеки — правка `SCREEN_SPECS.md` в том же коммите
   (правило 0 из `CLAUDE.md`). Изменения JSON-контракта — только после вопроса владельцу.
7. Не писать новый код в `iosApp/src/commonMain` и `iosApp/src/iosMain`, кроме
   `IosUseCaseModule.kt` и `IosViewModelHelper.kt` (регистрация iOS-VM).
8. `project.pbxproj` не редактировать: `iosApp/iosApp/` — синхронизируемая папка,
   новые `.swift`-файлы попадают в таргет автоматически.
9. Deployment target — **iOS 16.0**, Swift 5. Не использовать API iOS 17+
   (`@Observable`, `onChange(of:initial:_:)` с двумя параметрами, `ContentUnavailableView`,
   `scrollTargetBehavior`, `presentationCornerRadius`/`presentationBackground` (16.4) и т.п.)
   без `if #available`.

## 3. Дизайн-кит (`iosApp/iosApp/Shared/Design`)

| Файл | Что | Источник |
|---|---|---|
| `DSColor.swift` | `DSColor.*` — все цвета `M.light`/`M.dark`, динамические (light/dark автоматически), `DSColor.scrim` | `design/tokens.js` §1 |
| `DSTypography.swift` | `DSFont.sans/mono(size, weight)`, `DSTextStyle` (= `M.t.*`), модификатор `.dsTextStyle(_:color:)` | `tokens.js` §2, §7; «Правила шрифтов.md» |
| `DSLayout.swift` | `DSRadius`, `DSSpacing`, `DSMetrics` (= `M.semantic`), `DSShadow` + `.dsShadow(_:)` | `tokens.js` §3–6 |
| `DSIcon.swift` | `DSIcon` — SF Symbols вместо `Ic*` из `design/src/icons.jsx` | `icons.jsx` |
| `DSPreviewSupport.swift` | `DSPreviewCanvas` для `#Preview` | — |

Правила:
- Цвета — только `DSColor.*`. Никаких `#hex`, `.blue`, `.accentColor`,
  `Color(UIColor.secondarySystemBackground)` в экранах.
- Шрифты — только `DSFont`/`DSTextStyle`. **Mono** (JetBrains Mono) — время, деньги, нормы,
  счётчики, номера маршрута/поезда/вагона, серия+номер локомотива, UPPERCASE-заголовки групп.
  **Sans** (Inter) — всё, что читается как язык: заголовки, лейблы, кнопки, названия станций,
  заметки. Выбор — по типу контента, не по автору значения.
- Цвет текста со стилем меняется параметром: `.dsTextStyle(.value, color: DSColor.textFaint)`.
  Внешний `.foregroundColor` поверх `.dsTextStyle` не сработает.
- Файлы шрифтов Inter/JetBrains Mono в бандл ещё не добавлены — сейчас работает системный
  фолбэк (SF Pro / SF Mono) с теми же кеглями. Добавление шрифтов — отдельная задача
  (.ttf + `UIAppFonts` в Info.plist); код кита менять не придётся.
- Отступ контента экрана от краёв — `DSMetrics.screenPadX` (16); строки в карточке —
  `rowPadX` 20 / `rowPadY` 14; зазор между карточками группы — `cardGap` 12.

## 4. Компоненты (`iosApp/iosApp/Shared/Components`)

| Компонент | Назначение | Макет |
|---|---|---|
| `DSNavBar` (+ `DSNavPillButton`, `.dsHideSystemNavBar()`) | навбар: pill-стиль (формы) и text-стиль (справочники) | `ios-screens.jsx` IOSScreenRoute; `shared-ui.jsx` NavBarIOS/NavPillBtn |
| `DSTabBar` (+ `DSBottomScrim`) | плавающий таб-бар 4 вкладки + «+» | `ios-screens.jsx` IOSScreenTrips/TabBtn; `settings-screens.jsx` FauxTabBarIOS |
| `DSCard`, `DSDivider`, `DSIconAvatar` | карточка секции, разделитель, иконка-аватар | `shared-ui.jsx` Card/Sep/IconAvatar |
| `DSGroupHeader`, `DSSectionHeader` | заголовок группы (mono UPPERCASE) и секции (20/700) | `shared-ui.jsx` GroupHead/SectionH2 |
| `DSList`, `DSListRow`, `DSFieldRow`, `DSAddRow` | список-карточка и строки | `ios-frame.jsx` IOSList/IOSListRow; `settings-screens.jsx` SettingsRow; `shared-ui.jsx` FieldRow/ActionRow |
| `DSTimeRow`, `DSSheetTimeRow` | строка времени формы и шторки времени | `ios-screens.jsx` TimeRow; `time-sheet.jsx` SheetTimeRow |
| `DSChip`, `DSIconChip`, `DSPill`, `DSTag` | чипы (assist/choice), бейджи, теги | `all-routes.jsx` arChip/arIconChip; `shared-ui.jsx` Pill |
| `DSCTAButton`, `DSTonalButton`, `DSDangerButton`, `DSTextAction`, `DSIconButton` | кнопки | `all-routes.jsx` IOSFilterSheet; `stats-screens.jsx` IOSStatsEmpty; `shared-ui.jsx` AddBtn/DangerBtn/TextAction/IconBtn |
| `DSWarningBlock` | оранжевый блок предупреждения (§2.7 WarnItem, «Вторая ночь подряд») | `night-warn.jsx` RouteNightWarn; `time-sheet.jsx` NormWarn |
| `DSGlassPill`, `DSGlassIconButton` | стеклянная пилюля (iOS 16: `.ultraThinMaterial`) | `ios-frame.jsx` IOSGlassPill |
| `DSBottomSheet` (+ `.dsSheet`) | каркас шторки: грэббер, шапка (✕ или Отмена/Готово), прокрутка | `ios-screens.jsx` RouteSheetShell; `time-sheet.jsx` TimeSheetIOS |
| `DSEmptyState` | пустое состояние с CTA | `stats-screens.jsx` IOSStatsEmpty; `all-routes.jsx` ArEmpty |
| `DSSkeletonBlock`, `DSSkeletonRouteRow`, `DSSkeletonList` | скелетон первой загрузки | `route-quick-view.jsx` QVGhostList; спека §4.1 |

Каждый компонент имеет `#Preview` в light и dark. Не хватает компонента — сначала искать в
`design/src/shared-ui.jsx`, затем добавлять в `Shared/Components` новым файлом (с превью).

## 5. Навигация

- `AppRouter.shared` — iOS-аналог `Router` (§3.2): `show(_:)`, `back()`, `popToRoot()`,
  `selectTab(_:)`, `present(_ sheet:)`, методы `showRouteForm`, `showLocoForm`, …,
  `showPurchases(isAuthorized:)` (гейт покупок), `startNewRoute()` («+», §3.4).
- `AppRoute` — все push-экраны; `AppSheet` — шторки/оверлеи; `AppTab` — 4 вкладки.
- `AppDestinationView` / `AppSheetView` — сопоставление маршрута и View. Нереализованные —
  `StubView` / `StubSheetView` (название + раздел спеки).
- **Как волна подключает экран**: создать `Screens/<Раздел>/<Экран>View.swift`, заменить в
  `AppDestinationView` ветку `StubView` своего кейса на экран. Переходы — через
  `AppRouter.shared.show(...)`, не через `NavigationLink(destination:)`.
- **Шторки**: открываемые из разных мест и не возвращающие результат — через
  `AppRouter.shared.present(...)`. Шторки, которые редактируют состояние экрана (время,
  коэффициенты, пикеры) — экран показывает локально через `.dsSheet(item:)` с
  `DSBottomSheet`; кейс `AppSheet` остаётся реестром. Подтверждения («Удалить …?»,
  «Создать копию маршрута?») — локально, по контракту §24.3.
- Нижнее меню видно только на корневых экранах вкладок (прикреплено через `safeAreaInset`
  в `AppCoordinator`) — на вложенных экранах его нет автоматически.

### Маршруты (этап 0)

| Кейс | Экран | Сейчас |
|---|---|---|
| вкладки `home` / `salary` / `settings` / `profile` | Главная §4 / Зарплата §11 / Настройки §17 / Профиль §18 | существующие `HomeView` / `SalaryCalculationView` / `SettingsView` / `ProfileView` |
| `routeForm(basicId:)` | Маршрут §5 | `FormView` |
| `routeFormCopy`, `sharedRoutePreview` | копия §24.3, shared preview §5.6 | Stub |
| `locoForm` / `trainForm` / `passengerForm` | §1 / §6 / §7 | `FormLocoView` / `FormTrainView` / `FormPassengerView` |
| `otherWorkForm` | §8 | Stub |
| `partnersManage` / `partnerPicker` / `partnerEditor` | §8.5 | Stub |
| `allRoutes` | §9 | `AllRoutesView` |
| `trash` | §9.4 | Stub |
| `search` | §10 | `SearchView` |
| `payrollCodeSearch` | §11.8 | Stub |
| `settingSalary` | §12 | Stub |
| `statistics` | §13 | Stub |
| `calendar` | §14 | `WorkScheduleView` (временная частичная) |
| `scheduleWizard` / `absence` | §15 / §16.4 | Stub |
| `settingsSection(...)` | §17.4–17.5 | Stub |
| `referral` | §18.R | Stub |
| `purchases` | §19 (через гейт) | `PurchasesView` |

Шторки `AppSheet` (все — Stub): `timeSheet` §2, `seriesPicker`/`stationPicker` §2.10,
`coeffSheet` §1.3, `monthPicker` §4.4/§9.0.1/§11.7, `metricInfo` §4.4, `routeUnits` §4.4,
`routeQuickView` §9.1, `routeLegend` §9.3, `calcSheet` §11.9, `restSheet` §5.7,
`passenger12h` §5.5, `stationEdit` §6.4, `segmentEdit` §6.5, `shoulders`/`shoulderEdit` §6.6,
`trainParams`/`wagonCounter` §6.2, `trainHistory` §6.7, `otherWorkType`/`locoPicker` §8.3,
`routesFilter`/`routesSort` §9.0.1, `searchSettings` §10.3, `tariffChanged` §12.3,
`statisticsCompare` §13.3, `statisticsFreightInfo` §13.4, `addEvent` §14.3,
`continueSchedule` §15.1, `settingsPicker` §17.2, `nightRange` §17.3, `appInput`/`emailPassword`
§18.6, `pdfContent`/`pdfActions` §21, `dateTimePicker` §24.2, `announcement` §23 (полный экран).

**Не заводятся** (🚫 мёртвый код / Android-легаси): SignIn/LogIn, FirstPresentationBlock (§20),
DetailsRoute (§22), SelectReleaseDaysScreen (§16.2), «Что нового» UpdatePresentationBlock (§22),
MigrationRecoveryScreen и диагностика (§25), ConfirmExitDialog (§3.3), CustomDatePickerDialog
(§24.2), EnteredCoefficientDialog/EnteredRefuelDialog (§1.3).

## 6. Карта экранов

Android-эталон: `features/route/src/main/java/com/z_company/route/` (далее `…/`).

| Экран | Спека | iOS-макет (файл → компонент) | Android-эталон |
|---|---|---|---|
| Сплэш | §3.1 | `splash-screens.jsx` → `IOSSplash` | системный сплэш, `app/.../MainActivity.kt` |
| Нижнее меню | §3.1 | `ios-screens.jsx` → таб-бар `IOSScreenTrips`; `settings-screens.jsx` → `FauxTabBarIOS` | `…/component/BottomNavigationBar.kt` |
| Главная | §4 | `ios-screens.jsx` → `IOSScreenTrips` (`HeroCard`, `UpcomingRoute`, `RestAtTurnaround`, `TripRow`), `IOSUnitsSheet`, `IOSLegendSheet` | `…/ui/HomeScreen.kt`, `HomeStateBlocks.kt`, `viewmodel/home_view_model/HomeViewModel.kt` |
| Маршрут (форма) | §5 | `ios-screens.jsx` → `IOSScreenRoute`, `IOSCalcSheet`, `IOSRestSheet`, `RouteDeleteAlert`; `night-warn.jsx` → `RouteNightWarn` | `…/ui/FormScreen.kt`, `viewmodel/FormViewModel.kt` |
| Локомотив | §1 | `locomotive-screens-v2.jsx` → `IOSScreenLocomotiveV2` | `…/ui/FormLocoScreen.kt`, `viewmodel/LocoFormViewModel.kt` |
| Шторка времени | §2 | `time-sheet.jsx` → `TimeSheetIOS`, `RefPickerSheet`, `SheetWrap(platform="ios")` | `…/ui/TimeBottomSheet.kt`, `StationPickerSheet.kt`, `SeriesPickerSheet.kt` |
| Поезд | §6 | `ios-add-train.jsx` → `IOSScreenAddTrain`; `station-edit-sheet.jsx` → `StationEditSheetWrap(platform="ios")` | `…/ui/FormTrainScreen.kt`, `viewmodel/TrainFormViewModel.kt` |
| Пассажиром | §7 | `pass-screen.jsx` → `IOSScreenPass` | `…/ui/FormPassengerScreen.kt` |
| Прочая работа | §8 | нет макета — собирать из кита (стиль «Пассажиром») | `…/ui/FormOtherWorkScreen.kt` |
| Напарники | §8.5 | нет макета — стиль справочников `norms-screens.jsx` | `…/ui/PartnersListScreen.kt`, `PartnerEditScreen.kt` |
| Все маршруты | §9 | `all-routes.jsx` → `IOSScreenAllRoutes`, `IOSRouteCard`, `IOSFilterSheet`, `IOSSortSheet` | `…/ui/AllRouteScreen.kt`, `viewmodel/all_route_view_model/AllRouteViewModel.kt` |
| Быстрый просмотр | §9.1 | `route-quick-view.jsx` → `IOSRouteQuickView` | `…/component/RouteQuickViewSheet.kt` |
| Легенда значков | §9.3 | `ios-screens.jsx` → `IOSLegendSheet` / `IOSScreenLegend` | `…/component/RouteLegendSheet.kt` |
| Корзина | §9.4 | нет макета — список-карточки кита | `…/ui/TrashScreen.kt` |
| Поиск | §10 | нет макета | `…/ui/SearchScreen.kt` |
| Расчёт зарплаты | §11 | `salary-screen.jsx` → `IOSScreenSalary`; шторка — `IOSCalcSheet` | `…/ui/SalaryCalculationScreen.kt` + `domain/salary/SalaryCalculator.kt` |
| Настройки зарплаты | §12 | `salary-settings.jsx` → `IOSScreenSalarySettings` | `…/ui/SettingSalaryScreen.kt` |
| Статистика | §13 | `stats-screens.jsx` → `IOSStatsA` / `IOSStatsB` / `IOSStatsC` (**выбор по п. 2.4**), `IOSStatsCompare`, `IOSStatsYear`, `IOSStatsEmpty`, `IOSStatsDetail`, `IOSStatsDetailYear`, `IOSStatsLegend`, `IOSStatsHistory` | `…/ui/StatisticsScreen.kt` |
| Календарь | §14 | `calendar-variants-de.jsx` → `CalendarVariantE(platform="ios")`, `AddEventSheetE` | `…/ui/CalendarScreen.kt` |
| Мастер «Заполнить месяц» | §15 | `schedule-wizard.jsx` → `ScheduleWizardScreen(platform="ios")` | `…/ui/ScheduleWizardScreen.kt` |
| Отвлечения | §16.4 | `calendar-variants-de.jsx` → `AbsenceFlowScreen(platform="ios")` | `…/ui/AbsenceScreen.kt` |
| Настройки (хаб) | §17.1 | `settings-screens.jsx` → `IOSScreenSettings`, `IOSScreenSettingsGeneral` | `…/ui/SettingsScreen.kt` |
| Настройки: под-разделы | §17.2–17.5 | `settings-subscreens.jsx` → `IOSScreenSettingsNorms`, `IOSScreenSettingsAccounting`, `IOSScreenSettingsShoulders`, `IOSScreenSettingsLocomotive` | `…/ui/SettingsScreen.kt`, `ui/settings/*` |
| Справочники норм | §17.4, §2.10 | `norms-screens.jsx` → `IOSSeriesList`, `IOSSeriesEditor`, `IOSStationsList`, `IOSStationEditor` | `…/ui/settings/SettingsSeriesEditorContent.kt`, `SettingsStationEditorContent.kt` |
| Профиль | §18.1 | `profile-screens.jsx` → `IOSProfile` | `…/ui/ProfileScreen.kt` |
| Вход / регистрация | §18.0 | `login-screens.jsx` → `IOSAuth` | `…/ui/ProfileScreen.kt` (форма входа) |
| Пригласить друга | §18.R | нет макета | `…/ui/ReferralScreen.kt` |
| Покупки | §19 | `purchase-screens.jsx` → `IOSPaywallA` / `IOSPaywallB` / `IOSPaywallC` (**выбор по п. 2.4**), `IOSPaywallCompare`, `IOSPaywallUnlock`, `IOSProManage`, `IOSRenewSheet`, `IOSPaywallExpired`, `IOSExpiringSoon` | `…/ui/PurchasesScreen.kt`, `SubscriptionLimitDialog.kt` |
| PDF | §21 | нет макета | `…/component/PdfContentDialog.kt`, `PdfActionSheet.kt` |
| Новость при запуске | §23 | нет макета | `app/src/main/java/com/z_company/loco_driver/ui/AnnouncementScreen.kt` |

## 7. Общая зона — меняется только ДОБАВЛЕНИЕМ

Эти файлы правят несколько волн параллельно. Разрешено только добавлять (новый кейс,
новую ветку `switch`, новый метод, новый компонент/токен). Нельзя переименовывать,
удалять, менять сигнатуры и значения существующего. Нужна правка существующего —
отдельный PR «фундамента», не в рамках волны.

- `iosApp/iosApp/Shared/Design/*` — токены кита (`DSColor`, `DSTypography`, `DSLayout`, `DSIcon`, `DSPreviewSupport`).
- `iosApp/iosApp/Shared/Components/*` — общие компоненты.
- `iosApp/iosApp/Navigation/AppRoute.swift` — `AppTab`, `AppRoute`, `SettingsSection`, `AppSheet`.
- `iosApp/iosApp/Navigation/AppRouter.swift` — роутер.
- `iosApp/iosApp/Navigation/AppDestinations.swift` — волна заменяет только ветку своего экрана/шторки.
- `iosApp/iosApp/Navigation/AppCoordinator.swift`, `StubView.swift`.
- `iosApp/src/commonMain/kotlin/com/z_company/iosapp/di/IosViewModelHelper.kt` — регистрация iOS-VM.
- `iosApp/src/commonMain/kotlin/com/z_company/iosapp/di/IosUseCaseModule.kt` — DI iOS-VM.

Конфликты при слиянии в этих файлах разрешаются объединением (обе стороны добавляли).

## 8. Чек-лист PR волны

- [ ] Поведение сверено с разделом `SCREEN_SPECS.md`; BUG-NN перенесены как есть.
- [ ] Внешний вид — компоненты кита; нет `#hex`/системных цветов/`Font.system` в экране
      (кроме иконок SF Symbols).
- [ ] Выбран вариант макета (если их несколько) — указан в PR.
- [ ] Экран подключён в `AppDestinationView` вместо `StubView`; общая зона — только добавления.
- [ ] `#Preview` light + dark для новых компонентов.
- [ ] Нет API iOS 17+ без `if #available`.
- [ ] Если в среде есть `swiftc` — `swiftc -parse` по изменённым файлам; иначе в PR
      перечислено, что не проверено компиляцией.
