# Architecture Driving Advisor — LocoDriver

> Руководство для агентов, генерирующих/обновляющих экраны.
> Источники истины: CLAUDE.md (жёсткие правила), CODEBASE.md (детали).

---

## 1. Платформы и UI-стек

| Платформа | UI-фреймворк | Где лежит код |
|-----------|-------------|---------------|
| Android | Jetpack Compose | `features/<module>/src/main/java/com/z_company/<module>/ui/` |
| iOS | SwiftUI | `iosApp/iosApp/Screens/<ScreenName>/` |
| Общая логика | Kotlin (KMP) | `domain/`, `data_local/`, `data_remote/`, `core/` |

**Compose Multiplatform (CMP) — запрещён.** Никакого `commonMain` UI-кода.

---

## 2. Расположение файлов по экранам

### Android (Jetpack Compose)
```
features/route/src/main/java/com/z_company/route/
  ui/
    FormScreen.kt
    FormPassengerScreen.kt       ← PassengerScreen
    FormTrainScreen.kt           ← TrainScreen
    FormLocoScreen.kt
    AllRouteScreen.kt
    SalaryCalculationScreen.kt   ← SalaryScreen
    ProfileScreen.kt
  viewmodel/
    FormViewModel.kt
    PassengerFormViewModel.kt
    TrainFormViewModel.kt
    SalaryCalculationViewModel.kt
    ProfileViewModel.kt
    SettingSalaryViewModel.kt

features/settings/src/main/java/com/z_company/settings/
  ui/
    SettingsScreen.kt            ← SettingScreen
```

### iOS (SwiftUI)
```
iosApp/iosApp/
  Screens/
    Form/
      FormView.swift
      FormPassengerView.swift    ← PassengerScreen
      FormTrainView.swift        ← TrainScreen
      FormLocoView.swift
    AllRoutes/
      AllRoutesView.swift
    SalaryCalculation/
      SalaryCalculationView.swift
    Profile/
      ProfileView.swift
    Settings/
      SettingsView.swift
  ViewModels/
    FormViewModelWrapper.swift
    PassengerFormViewModelWrapper.swift
    TrainFormViewModelWrapper.swift
    LocoFormViewModelWrapper.swift
    HomeViewModelWrapper.swift      ← используется AllRoutesView
    SalaryCalculationViewModelWrapper.swift
    ProfileViewModelWrapper.swift
    SettingsViewModelWrapper.swift
```

---

## 3. Паттерн iOS: SwiftUI + ViewModelWrapper

### Получение ViewModel
```swift
// Всегда через IosViewModelHelper.shared
private let viewModel = IosViewModelHelper.shared.getFormViewModel()
```

### Шаблон ViewModelWrapper
```swift
@MainActor
final class XxxViewModelWrapper: ObservableObject {
    private let viewModel = IosViewModelHelper.shared.getXxxViewModel()

    @Published var data: SomeType? = nil
    @Published var isLoading: Bool = true
    @Published var errorMessage: String? = nil

    init() {
        viewModel.watchData { [weak self] value in
            DispatchQueue.main.async { self?.data = value }
        }
        viewModel.watchIsLoading { [weak self] loading in
            DispatchQueue.main.async { self?.isLoading = loading.boolValue }
        }
    }

    func doAction() { viewModel.doAction() }
}
```

### Шаблон SwiftUI View
```swift
import SwiftUI
import ComposeApp   // ← всегда нужен для KMP-типов

struct XxxView: View {
    @StateObject private var vm = XxxViewModelWrapper()

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                mainContent
            }
        }
        .navigationTitle("Заголовок")
        .navigationBarTitleDisplayMode(.inline)  // или .large
    }
}
```

### Правила Swift
- `@MainActor` на всех Wrapper'ах
- `@Published` для каждого observable-свойства
- `[weak self]` в каждом callback из Kotlin
- `DispatchQueue.main.async { ... }` оборачивает любое изменение `@Published`
- Минимум force-unwrap (`!`), предпочитать `?` и `guard let`
- Deployment target: iOS 16+ (`.onChange(of:)` без `oldValue` требует iOS 17 — проверяй)

---

## 4. Паттерн Android: Jetpack Compose + ViewModel

### Шаблон composable-экрана
```kotlin
@Composable
fun XxxScreen(
    viewModel: XxxViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

### Правила Android
- Только `androidx.compose.*` импорты (никаких `java.*`)
- Цвета: `MaterialTheme.colorScheme.xxx`, типографика: `MaterialTheme.typography.xxx`
- Никаких хардкод-hex в Compose-коде
- `Dispatchers.Default` (не `IO`, он JVM-only)

---

## 5. Маппинг design-токенов → платформа

Токены из `design/src/tokens.js` (`M.light.*`, `M.dark.*`, `M.r.*`, `M.s.*`):

### Цвета
| Токен дизайна | iOS SwiftUI | Android Compose |
|--------------|-------------|-----------------|
| `M.light.accent` / `M.dark.accent` | `.accentColor` / `Color.accentColor` | `MaterialTheme.colorScheme.primary` |
| `M.light.text` / `M.dark.text` | `.primary` | `MaterialTheme.colorScheme.onBackground` |
| `M.light.textMuted` | `.secondary` | `MaterialTheme.colorScheme.onSurfaceVariant` |
| `M.light.surface` | `Color(.systemBackground)` | `MaterialTheme.colorScheme.surface` |
| `M.light.bg` | `Color(.systemGroupedBackground)` | `MaterialTheme.colorScheme.background` |
| `M.light.danger` | `.red` | `MaterialTheme.colorScheme.error` |
| `M.light.success` | `.green` | `Color(0xFF00B341)` через theme extension |
| `M.light.warning` | `.orange` | `Color(0xFFFF8A00)` через theme extension |

**Никаких хардкод-hex-значений в платформенном коде.** Используй семантические имена выше.

### Отступы (M.s.* → dp)
| Токен | Значение | iOS | Android |
|-------|----------|-----|---------|
| `M.s.xs` | 4px | `4` | `4.dp` |
| `M.s.sm` | 8px | `8` | `8.dp` |
| `M.s.md` | 12px | `12` | `12.dp` |
| `M.s.lg` | 16px | `16` | `16.dp` |
| `M.s.xl` | 20px | `20` | `20.dp` |
| `M.s.xxl` | 24px | `24` | `24.dp` |

### Радиусы (M.r.*)
| Токен | iOS | Android |
|-------|-----|---------|
| `M.r.cardIOS` (18) | `.cornerRadius(18)` | — |
| `M.r.cardAndroid` (16) | — | `RoundedCornerShape(16.dp)` |
| `M.r.md` / `M.r.input` (12) | `.cornerRadius(12)` | `RoundedCornerShape(12.dp)` |
| `M.r.pill` (999) | `.clipShape(Capsule())` | `RoundedCornerShape(50%)` |

### Типографика
| Токен | iOS | Android |
|-------|-----|---------|
| `M.t.display` | `.font(.largeTitle.bold())` | `MaterialTheme.typography.headlineLarge` |
| `M.t.title` | `.font(.title2.bold())` | `MaterialTheme.typography.titleLarge` |
| `M.t.body` | `.font(.body)` | `MaterialTheme.typography.bodyLarge` |
| `M.t.label` | `.font(.subheadline)` | `MaterialTheme.typography.labelLarge` |
| `M.t.caption` | `.font(.caption)` | `MaterialTheme.typography.bodySmall` |
| `M.t.mono` | `.font(.system(.body, design: .monospaced))` | `MaterialTheme.typography.bodyMedium + FontFamily.Monospace` |

---

## 6. Доменные модели (commonMain)

```
domain/src/commonMain/kotlin/com/z_company/domain/entities/
  route/
    Route.kt         → BasicData + locomotives: List<Locomotive> + trains: List<Train> + passengers: List<Passenger>
    BasicData.kt     → id, number, timeStartWork: Long?, timeEndWork: Long?, notes, isDeleted, ...
    Locomotive.kt    → id, series, number, locoType: LocoType, sections: List<Section>, ...
    Train.kt         → id, number, weight: String, axle: String, distance: String,
                       conditionalLength: String, isHeavyLongDistance: Boolean
    Passenger.kt     → id, trainNumber, timeDeparture: Long?, timeArrival: Long?, notes
  setting/
    UserSettings.kt  → defaultWorkTime: Long, норма-поля, флаги учёта
    SalarySetting.kt → коэффициенты, надбавки, типы расчёта
  User.kt            → email, vkId, token
```

**Время** = `Long` миллисекунды UTC. Конвертация:
- iOS: `TimeFormatter.msToDate(_ ms: Int64) -> Date` (утилита в проекте)
- Android: `Instant.fromEpochMilliseconds(ms)`

**Месяц в API** = 0-based (январь = 0). Конвертировать только в DTO-маппере, не в UI.

---

## 7. Классификация типов экранов и подход к генерации

| Тип | Экраны | iOS-паттерн | Android-паттерн |
|-----|--------|-------------|-----------------|
| **Форма ввода** | FormScreen, PassengerScreen, TrainScreen | `Form { Section { ... } }` с `TextField`, `DatePicker`, `Toggle` | `Column + OutlinedTextField + DatePickerDialog` |
| **Список-таблица** | AllRouteScreen | `List { ForEach(...) }` с `NavigationLink` | `LazyColumn + Card` |
| **Настройки** | SettingScreen | `List` + вложенные `NavigationLink` в подэкраны | `LazyColumn + ListItem` |
| **Расчёт/сводка** | SalaryScreen | `List` со `Section` header/footer + числа | `LazyColumn + SummaryCard` |
| **Профиль** | ProfileScreen | Условный рендер: loginForm vs loggedIn | `Column` с условием |

---

## 8. Что проверять перед записью файла

1. Нет `import java.*`, `import android.*`, `import UIKit` в commonMain
2. Нет хардкод-hex в UI-коде (`#FF0000`, `Color(0xFF...)` без theme extension)
3. Все `@Published` изменяются только на `DispatchQueue.main.async`
4. Поля моделей взяты as-is из domain (не придумывать новые)
5. `IosViewModelHelper.shared.getXxxViewModel()` — а не прямой `XxxViewModel()`
6. Android: `collectAsStateWithLifecycle()`, не `collectAsState()`

---

## 9. Что НЕ трогать

- Не менять JSON-контракт с сервером (поля, типы, наличие)
- Не изменять схему БД без подтверждения пользователя
- Не писать новый код в `iosApp/src/commonMain/` или `iosApp/src/iosMain/` (мёртвый CMP-груз)
- Не добавлять `Co-Authored-By: Claude ...` в git-коммиты
- Не удалять поле `photos` из SyncData (есть соглашение с сервером)
- Android-экраны в production — изменять осторожно, только UI (не логику VM)
