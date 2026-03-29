# CLAUDE_IOS_UI.md — iOS Native SwiftUI для LocoDriver

## Статус: 🔴 Не начат
Ветка: `feature/ios-ui`

---

## Цель
Реализовать нативный iOS интерфейс на SwiftUI для всех экранов LocoDriver.
ViewModels — повторно использовать KMP (Kotlin/Native → XCFramework).
Тот же функционал и логика расположения элементов, что на Android.

---

## Архитектура

```
iosApp/
├── iosApp.xcodeproj                  — Xcode-проект
├── iosApp/                           — Swift sources
│   ├── iOSApp.swift                  — Entry point (@main)
│   ├── Navigation/
│   │   └── AppCoordinator.swift      — NavigationStack / TabView
│   ├── ViewModels/                   — Swift-обёртки над KMP ViewModels
│   │   ├── HomeViewModelWrapper.swift
│   │   ├── FormViewModelWrapper.swift
│   │   └── ... (по одному на каждый экран)
│   └── Screens/
│       ├── Home/
│       │   └── HomeView.swift
│       ├── Form/
│       │   ├── FormView.swift
│       │   ├── FormLocoView.swift
│       │   ├── FormTrainView.swift
│       │   └── FormPassengerView.swift
│       ├── SalaryCalculation/
│       │   └── SalaryCalculationView.swift
│       ├── SalarySetting/
│       │   └── SalarySettingView.swift
│       ├── Settings/
│       │   └── SettingsView.swift
│       ├── Profile/
│       │   └── ProfileView.swift
│       ├── Search/
│       │   └── SearchView.swift
│       ├── Purchases/
│       │   └── PurchasesView.swift
│       ├── WorkSchedule/
│       │   └── WorkScheduleView.swift
│       ├── SelectReleaseDays/
│       │   └── SelectReleaseDaysView.swift
│       └── AllRoutes/
│           └── AllRoutesView.swift
```

---

## Связь SwiftUI ↔ KMP ViewModel

KMP ViewModels компилируются в XCFramework (ComposeApp.framework).
В Swift они доступны как обычные Kotlin-классы.

### Паттерн Swift-обёртки:
```swift
// HomeViewModelWrapper.swift
import ComposeApp
import Combine

@MainActor
class HomeViewModelWrapper: ObservableObject {
    private let viewModel: HomeIosViewModel

    @Published var routes: [Route] = []
    @Published var currentMonthOfYear: MonthOfYear? = nil
    @Published var workTime: Int64 = 0
    @Published var isLoading: Bool = false

    private var cancellables = Set<AnyCancellable>()

    init() {
        viewModel = HomeIosViewModel()
        observeState()
    }

    private func observeState() {
        // Наблюдение за StateFlow через Kotlin coroutines bridge
        // Используем FlowCollector helper из KMP модуля
        Task {
            for await state in viewModel.state {
                await MainActor.run {
                    self.routes = state.routes
                    self.currentMonthOfYear = state.currentMonthOfYear
                    self.isLoading = state.isLoading
                }
            }
        }
    }

    func loadRoutes() { viewModel.loadRoutes() }
    func deleteRoute(id: String) { viewModel.deleteRoute(id: id) }
    func copyRoute(id: String) { viewModel.copyRoute(id: id) }
}
```

> **ВАЖНО**: Для удобной работы с KMP StateFlow в Swift используется `KMPNativeCoroutines` или ручной FlowCollector. Рекомендуется добавить в KMP-модуль вспомогательные функции `watchState(callback:)` для каждого ViewModel, которые принимают Swift-callback вместо Flow.

### Вспомогательный паттерн watchState в KMP:
```kotlin
// HomeIosViewModel.kt (в iosApp commonMain)
fun watchState(callback: (HomeUiState) -> Unit) {
    viewModelScope.launch {
        state.collect { callback(it) }
    }
}
```

```swift
// В Swift-обёртке
viewModel.watchState { [weak self] state in
    DispatchQueue.main.async {
        self?.routes = state.routes
    }
}
```

---

## Навигация

Использовать `NavigationStack` + `TabView` (iOS 16+).

```swift
// AppCoordinator.swift
struct AppCoordinator: View {
    @State private var selectedTab: Tab = .home

    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack { HomeView() }
                .tabItem { Label("Поездки", systemImage: "list.bullet") }
                .tag(Tab.home)

            NavigationStack { SalaryCalculationView() }
                .tabItem { Label("Зарплата", systemImage: "rublesign.circle") }
                .tag(Tab.salary)

            // Tab Add — открывает новый маршрут
            NavigationStack { FormView(routeId: nil) }
                .tabItem { Label("Добавить", systemImage: "plus.circle.fill") }
                .tag(Tab.add)

            NavigationStack { SettingsView() }
                .tabItem { Label("Настройки", systemImage: "gear") }
                .tag(Tab.settings)

            NavigationStack { ProfileView() }
                .tabItem { Label("Профиль", systemImage: "person.circle") }
                .tag(Tab.profile)
        }
    }
}

enum Tab { case home, salary, add, settings, profile }
```

---

## Экраны — подробная спецификация

---

### 1. HomeView (Главный экран)

**ViewModel**: `HomeIosViewModel` → `HomeViewModelWrapper`

**Структура экрана:**
```
NavigationView
├── ToolbarItem(.navigationBarLeading)  — кнопка "←" месяц
├── ToolbarItem(.principal)             — "Месяц ГГГГ" (Picker)
├── ToolbarItem(.navigationBarTrailing) — иконка поиска + синхронизация
└── ScrollView
    ├── MainInfoSection                 — статистика месяца
    │   ├── HStack: "Всего: X ч Y мин" + прогресс-бар норма
    │   ├── HStack: "Ночных: X ч"
    │   ├── HStack: "Пассажирских: X ч" (если есть)
    │   ├── HStack: "Отработано на {дата}: X ч" (если учитывать будущие)
    │   └── LinearProgressView(value: %, total: 1.0)
    └── LazyVStack
        └── ForEach(routes) { RouteItemView(route:) }
```

**RouteItemView:**
```swift
struct RouteItemView: View {
    let route: Route
    let isLongTrain: Bool
    // Фоновый цвет: будущий маршрут → .secondary, прошедший → .surface
    // Длинный состав: значок "длинный поезд" в правом верхнем углу
    // Отображать: номер маршрута, время начала, время работы, локомотив, поезд
}
```

**Взаимодействия:**
- Нажатие на маршрут → `NavigationLink` к `FormView(routeId: route.id)`
- Долгое нажатие → `.contextMenu` с опциями: Скопировать, Удалить
- Picker месяца/года → обновление списка
- Кнопка "Синхронизация" → `viewModel.syncRoutes()`
- Pull-to-refresh → `viewModel.loadRoutes()`

---

### 2. FormView (Форма маршрута)

**ViewModel**: `FormIosViewModel` → `FormViewModelWrapper`

**Структура:**
```
NavigationView
├── ToolbarItem: "Сохранить" (disabled если нет изменений)
└── Form / ScrollView
    ├── Section "Основные данные"
    │   ├── TextField "Номер маршрута"
    │   ├── DatePicker "Начало работы"
    │   ├── DatePicker "Окончание работы"
    │   ├── Toggle "Одиночная тяга"
    │   └── Button "Перерыв" → раскрывает поля:
    │       ├── DatePicker "Начало перерыва"
    │       └── DatePicker "Конец перерыва"
    ├── Section "Локомотивы" (список + кнопка "+")
    │   └── ForEach(locos) { LocoRowView → NavigationLink FormLocoView }
    ├── Section "Поезда" (список + кнопка "+")
    │   └── ForEach(trains) { TrainRowView → NavigationLink FormTrainView }
    ├── Section "Пассажирские" (список + кнопка "+")
    │   └── ForEach(passengers) { PassengerRowView → NavigationLink FormPassengerView }
    ├── Section "Ночное время" (только отображение)
    │   └── Text "X ч Y мин"
    ├── Section "Зарплата" (только отображение)
    │   └── Text "~X ₽"
    └── Section
        └── TextField "Заметки" (многострочный)
```

**Поля перерыва:**
- Показывать если `timeStartBreak != nil && timeStartBreak != 0` ИЛИ пользователь нажал кнопку "Перерыв"
- Фон поля "Начало перерыва": `.secondary` если заполнено, `.surface` если пустое

**DatePicker:**
- Использовать нативный iOS `DatePicker` с `displayedComponents: [.date, .hourAndMinute]`
- Или кастомный барабан (UIPickerView) если нужна более точная стилистика, аналогичная Android

---

### 3. FormLocoView (Форма локомотива)

**ViewModel**: `LocoFormIosViewModel` → новый KMP ViewModel (создать аналог Android `LocoFormViewModel`)

**Структура:**
```
Form
├── Section "Локомотив"
│   ├── TextField "Номер"
│   ├── Picker "Серия" (с возможностью добавить новую)
│   └── Picker "Тип" (Тепловоз / Электровоз / Паровоз...)
├── Section "Время"
│   ├── DatePicker "Принятие"
│   └── DatePicker "Сдача"
├── Section "Топливо" (только для тепловозов)
│   ├── TextField "Принято, л"
│   └── TextField "Сдано, л"
├── Section "Секции" (для каждой секции — ExpandableRow)
│   ├── TextField "Топливо принято секция N"
│   ├── TextField "Топливо сдано секция N"
│   └── TextField "Коэффициент"
└── Button "Добавить секцию"
```

---

### 4. FormTrainView (Форма поезда)

**ViewModel**: `TrainFormIosViewModel` (создать)

**Структура:**
```
Form
├── Section
│   ├── TextField "Номер поезда"
│   ├── TextField "Расстояние, км" (числовое)
│   ├── TextField "Вес, т" (числовое)
│   ├── TextField "Количество осей" (числовое)
│   └── TextField "Длина, м" (числовое, условно видимое)
├── Section "Станция назначения"
│   └── Picker / SearchableList для выбора станции
└── Section "Фаза обслуживания"
    └── MultiSelect список фаз
```

---

### 5. FormPassengerView (Пассажирский поезд)

**ViewModel**: `PassengerFormIosViewModel` (создать)

**Структура:**
```
Form
├── Section
│   └── TextField "Номер поезда"
├── Section "Маршрут"
│   ├── SearchableStationPicker "Станция отправления"
│   └── SearchableStationPicker "Станция прибытия"
├── Section "Время"
│   ├── DatePicker "Отправление"
│   └── DatePicker "Прибытие"
├── Section (только отображение)
│   └── Text "В пути: X ч Y мин"
└── Section
    └── TextField "Заметки"
```

---

### 6. SalaryCalculationView

**ViewModel**: `SalaryCalculationIosViewModel` → `SalaryCalculationViewModelWrapper`

**Структура:**
```
NavigationView
├── ToolbarItem: "Настройки зарплаты" (шестерёнка)
└── ScrollView
    ├── InfoBlockView (сворачиваемый — ставка предупреждение)
    └── ScrollView(axis: .horizontal)
        └── Grid / LazyHStack — таблица зарплаты
            ├── Заголовки столбцов: Месяц, Маршруты, Базовая, Коэффициент...
            └── ForEach(months) { row }
```

**Горизонтальный скролл таблицы:**
- `ScrollView(.horizontal)` + `LazyHStack` или `Grid`
- Первый столбец (месяц) — фиксированный, остальные скроллятся

---

### 7. SalarySettingView

**ViewModel**: `SettingSalaryIosViewModel` (создать)

**Структура:**
```
Form
├── Section "Тарифная ставка"
│   ├── TextField "Текущая ставка (руб/ч)"
│   └── TextField "Старая ставка (руб/ч)"
├── Section "Коэффициенты"
│   ├── TextField "Районный коэффициент"
│   ├── TextField "Северная надбавка"
│   └── TextField "Зональная надбавка"
├── Section "Классность"
│   └── TextField "Надбавка за класс, %"
├── Section "Одиночная тяга"
│   ├── TextField "% грузовые"
│   └── TextField "% пассажирские"
├── Section "Вредность"
│   └── TextField "%"
├── Section "Тяжеловесные поезда" (список + кнопка "+")
│   └── ForEach { HStack: TextField вес + TextField процент + Button удалить }
├── Section "Длинносоставные" (список + кнопка "+")
│   └── аналогично
├── Section "Расширенная фаза" (список + кнопка "+")
│   └── аналогично
├── Section "Удержания"
│   ├── TextField "НДФЛ, %"
│   ├── TextField "Профсоюз, %"
│   └── TextField "Прочие, %"
└── Button "Сохранить"
```

**Диалог при сохранении:**
- Alert: "Применить тарифную ставку: с текущего месяца или с текущего и следующего?"
- Кнопки: "Только текущий", "Текущий и следующий", "Отмена"

---

### 8. SettingsView

**ViewModel**: `SettingsIosViewModel` → `SettingsViewModelWrapper`

**Структура:**
```
NavigationView
└── List / Form
    ├── Section "Маршруты"
    │   ├── DatePicker "Время работы по умолчанию" (только время)
    │   ├── Toggle "Использовать по умолчанию"
    │   └── Toggle "Учитывать будущие маршруты"
    ├── Section "Отдых"
    │   ├── DatePicker "Минимальный отдых ПО"
    │   ├── DatePicker "Минимальный домашний отдых"
    │   ├── DatePicker "Начало ночи"
    │   └── DatePicker "Конец ночи"
    ├── Section "Норма"
    │   ├── TextField "Часовая норма"
    │   └── Toggle "Персональная норма"
    ├── Section "Учёт"
    │   ├── Picker "Часовой пояс" (список)
    │   ├── Button "Выгрузить данные"
    │   └── Button "Загрузить данные"
    ├── Section "Локомотив"
    │   ├── Toggle "Показывать отопление"
    │   ├── Toggle "Показывать вспомогательное"
    │   ├── Toggle "Показывать статистику"
    │   └── Toggle "Показывать норму"
    ├── Section "Фазы обслуживания" (список + кнопка "+")
    │   └── ForEach { Text название → NavigationLink редактирование }
    └── Section
        └── Button "Выйти из аккаунта" (красный)
```

---

### 9. ProfileView

**ViewModel**: `ProfileIosViewModel` (создать или расширить существующий)

**Структура:**
```
NavigationView
└── Form
    ├── Section "Аккаунт"
    │   ├── Text "Email: ..."
    │   ├── SecureField "Пароль" + кнопка видимости
    │   └── Button "Войти через ВКонтакте" (VK ID)
    ├── Section
    │   └── Button "Подписка" → PurchasesView
    ├── Section
    │   └── Button "Обновить профиль" (pullToRefresh или кнопка)
    └── Section
        └── Button "Удалить аккаунт" (красный)
```

---

### 10. SearchView

**ViewModel**: `SearchIosViewModel` (создать)

**Структура:**
```
NavigationView
├── .searchable(text: $query, placement: .navigationBarDrawer)
└── VStack
    ├── FilterChipsRow (ScrollView horizontal)
    │   └── ForEach(filters) { FilterChip }
    ├── PeriodFilterRow
    ├── If query.isEmpty:
    │   └── List "История поиска"
    │       └── ForEach(history) { HistoryRow + swipe-to-delete }
    └── If !query.isEmpty:
        └── List результатов
            └── ForEach(results) { RouteRowView → NavigationLink FormView }
```

---

### 11. PurchasesView

**ViewModel**: `PurchasesViewModel` → `PurchasesViewModelWrapper`

**Структура:**
```
NavigationView "Подписка"
└── VStack
    ├── ForEach(products) {
    │   Card / RoundedRectangle
    │   ├── Text название (1 месяц / 3 месяца / 1 год)
    │   ├── Text описание
    │   ├── Text цена
    │   └── Button "Купить" → initPayment()
    ├── Button "Восстановить покупки"
    └── Text "Текущая подписка до: {дата}" (если активна)
    }
```

**Платёжная интеграция:**
- Robokassa SDK для iOS (WebView или нативный лаунчер)
- Или App Store In-App Purchases (StoreKit 2) — уточнить у пользователя

---

### 12. WorkScheduleView

**ViewModel**: `WorkScheduleIosViewModel` (создать)

**Структура:**
```
NavigationView
├── ToolbarItem: "Выходные" → SelectReleaseDaysView
└── ScrollView
    └── LazyVGrid(columns: 7) (7 дней в неделю)
        └── ForEach(days) { DayCellView(day:) }

DayCellView:
- Фон: рабочий день → .blue, выходной → .green, будущий → .secondary
- Text: число месяца
- Иконка/индикатор если маршрут
```

---

### 13. SelectReleaseDaysView

**ViewModel**: `SelectReleaseDaysIosViewModel` (создать)

**Структура:**
```
NavigationView "Выходные"
└── Form
    ├── Section "Период"
    │   ├── Picker "Месяц"
    │   └── Picker "Год"
    ├── Section "Периоды выходных"
    │   └── ForEach(periods) {
    │       HStack: DatePicker начало + DatePicker конец + Picker тип
    │       + swipe-to-delete
    │   }
    ├── Button "+ Добавить период"
    └── Button "Сохранить"
```

---

### 14. AllRoutesView

**ViewModel**: `AllRouteIosViewModel` (создать)

**Структура:**
```
NavigationView "Все маршруты"
├── ToolbarItem: "Сортировка" (Sheet с опциями)
├── ToolbarItem: "Фильтр" (chips)
└── List
    └── ForEach(routes) { RouteItemView → NavigationLink FormView }
        + contextMenu: Скопировать, Поделиться, Удалить
```

---

## KMP ViewModels — что создать нового

Для экранов, у которых ещё нет KMP ViewModel:

| iOS экран | Нужно создать в iosApp/commonMain |
|-----------|----------------------------------|
| FormLocoView | `LocoFormIosViewModel` |
| FormTrainView | `TrainFormIosViewModel` |
| FormPassengerView | `PassengerFormIosViewModel` |
| SalarySettingView | `SettingSalaryIosViewModel` |
| SearchView | `SearchIosViewModel` |
| WorkScheduleView | `WorkScheduleIosViewModel` |
| SelectReleaseDaysView | `SelectReleaseDaysIosViewModel` |
| AllRoutesView | `AllRouteIosViewModel` |
| ProfileView | расширить существующий `ProfileIosViewModel` |

Каждый ViewModel:
1. Наследует `androidx.lifecycle.ViewModel` (KMP)
2. Использует `viewModelScope` + `Dispatchers.Default`
3. Имеет `val state: StateFlow<UiState>` + метод `watchState(callback: (UiState) -> Unit)`
4. Внедряет зависимости через Koin (аналогично Android)
5. Регистрируется в `IosUseCaseModule.kt`

---

## Стиль и дизайн

### Цветовая схема (iOS System Colors):
| Android MaterialTheme | iOS эквивалент |
|----------------------|----------------|
| `colorScheme.primary` | `.accentColor` / `Color.blue` |
| `colorScheme.surface` | `Color(UIColor.systemBackground)` |
| `colorScheme.secondary` | `Color(UIColor.secondarySystemBackground)` |
| `colorScheme.onSurface` | `Color(UIColor.label)` |
| `colorScheme.error` | `Color.red` |

### Типографика:
- `MaterialTheme.typography.titleLarge` → `.font(.title2)` / `.font(.headline)`
- `MaterialTheme.typography.bodyMedium` → `.font(.body)`
- `MaterialTheme.typography.labelMedium` → `.font(.caption)`
- `MaterialTheme.typography.displayLarge` → `.font(.system(size: 48, weight: .bold))`

### Компоненты:
- `OutlinedTextField` → нативный `TextField` с `RoundedBorderTextFieldStyle`
- `AlertDialog` → `Alert` или `.confirmationDialog`
- `BottomSheet` → `.sheet(isPresented:)`
- `Chip` / `FilterChip` → `Capsule` c текстом или `Toggle` стиль `.button`
- `LazyColumn` → `List` или `LazyVStack` внутри `ScrollView`
- `SwipeToDismiss` → `.swipeActions(edge: .trailing)`
- `DropdownMenu` → `Picker` с `.menu` style или `Menu` view

---

## Нативные iOS паттерны (отличия от Android)

| Android | iOS |
|---------|-----|
| `BackHandler` | `.navigationBarBackButtonHidden` + кастомная кнопка |
| `DateTimePicker` (барабан) | `DatePicker` с `.wheels` стилем |
| `SnackBar` | `.toast` через `overlay` или `UINotificationFeedbackGenerator` |
| `BottomNavigation` | `TabView` |
| `Scaffold` | `NavigationView` / `NavigationStack` |
| `TopAppBar` | `.navigationTitle` + `toolbar` |
| `LinearProgressIndicator` | `ProgressView(value:)` |
| `CircularProgressIndicator` | `ProgressView()` (без параметров) |
| `IconButton` | `Button { Image(systemName:) }` |

---

## Порядок реализации (пошагово)

### Приоритет 1 (основные экраны):
1. Настроить Swift-проект: обновить `iOSApp.swift` для нативного SwiftUI (убрать Compose MP `MainViewController`)
2. `AppCoordinator.swift` — TabView навигация
3. Создать паттерн `ViewModelWrapper` (базовый класс + `watchState`)
4. `HomeView` + `HomeViewModelWrapper`
5. `FormView` + `FormViewModelWrapper`
6. `FormLocoView` + `LocoFormIosViewModel` (KMP)
7. `FormTrainView` + `TrainFormIosViewModel` (KMP)
8. `FormPassengerView` + `PassengerFormIosViewModel` (KMP)

### Приоритет 2 (зарплата и настройки):
9. `SalaryCalculationView` + `SalaryCalculationViewModelWrapper`
10. `SalarySettingView` + `SettingSalaryIosViewModel` (KMP)
11. `SettingsView` + `SettingsViewModelWrapper`

### Приоритет 3 (остальные):
12. `ProfileView`
13. `SearchView` + `SearchIosViewModel` (KMP)
14. `WorkScheduleView` + `WorkScheduleIosViewModel` (KMP)
15. `SelectReleaseDaysView`
16. `AllRoutesView`
17. `PurchasesView` (интеграция платежей)

---

## Изменения в KMP-модулях

### iosApp/src/commonMain/... — добавить ViewModels:
Для каждого нового ViewModel:
```kotlin
// Пример: LocoFormIosViewModel.kt
class LocoFormIosViewModel(
    private val routeUseCase: RouteUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(LocoFormUiState())
    val state: StateFlow<LocoFormUiState> = _state

    fun watchState(callback: (LocoFormUiState) -> Unit) {
        viewModelScope.launch {
            state.collect { callback(it) }
        }
    }
    // ... методы
}
```

### IosUseCaseModule.kt — зарегистрировать новые ViewModels:
```kotlin
single { LocoFormIosViewModel(get()) }
single { TrainFormIosViewModel(get()) }
// и т.д.
```

---

## Важные ограничения

- ✅ ТОЛЬКО SwiftUI — никакого UIKit кроме специфичных компонентов (VK ID SDK, WebView)
- ✅ iOS 16+ minimum deployment target
- ✅ Все данные только через KMP ViewModels — никакой прямой работы с БД/сетью в Swift
- ✅ Локализация: все строки на русском (как на Android)
- ✅ DatePicker использовать `.wheels` style для барабанов (аналог Android)
- ✅ Поддержка Light/Dark mode через `.colorScheme`
- ❌ НЕ дублировать бизнес-логику в Swift — только UI
- ❌ НЕ использовать Combine напрямую для бизнес-логики — только для bridging KMP → SwiftUI
