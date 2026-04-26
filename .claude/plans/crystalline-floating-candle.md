# Улучшения FormLocoScreen: иконки, настройки видимости секций, fix итого, onboarding

## Контекст
После редизайна FormLocoScreen (сворачиваемые секции) нужно:
1. Улучшить визуал заголовков секций — иконки + центрирование
2. Переименовать заголовки времени
3. Добавить в настройки возможность скрывать секции Отопление/Собственные нужды/Статистика
4. Исправить расчёт итого электроэнергии (считать только при указанной норме)
5. Информировать пользователей о новых полях при первом входе

---

## Изменения

### 1. CollapsibleSection — иконки + переименования
**Файл:** `features/route/src/main/java/com/z_company/route/component/CollapsibleSection.kt`

Добавить параметр `icon: Int? = null` (drawable resource ID).

В Row заголовка: слева иконка + текст, справа summary + шеврон. Всё `verticalAlignment = Alignment.CenterVertically` (уже есть).

Убедиться что padding `vertical = 12.dp` достаточен для центрирования между верхним CustomDivider и нижним краем заголовка.

**Файл:** `features/route/src/main/java/com/z_company/route/ui/FormLocoScreen.kt`

Переименования и иконки:
- `"Время приёмки/сдачи"` → `"Время"`, icon = `R.drawable.schedule_24px`
- `"Отопление"` → без изменений, icon = `R.drawable.nest_farsight_heat_24px`
- `"Собственные нужды"` → без изменений, icon = `R.drawable.electric_bolt_24px`
- `"Статистика"` → без изменений, icon = `R.drawable.finance_24px`

Внутри секции "Время":
- `"Время приёмки"` → `"Приёмка"`
- `"Время сдачи"` → `"Сдача"`

### 2. Настройки видимости секций
**Цепочка изменений (снизу вверх):**

#### 2a. UserSettings (domain entity)
**Файл:** `domain/src/commonMain/kotlin/com/z_company/domain/entities/setting/UserSettings.kt`
Добавить 3 поля:
```kotlin
val isShowLocoHeating: Boolean = true,
val isShowLocoAuxiliary: Boolean = true,
val isShowLocoStatistics: Boolean = true,
```

#### 2b. SQLDelight schema
**Файл:** `data_local/src/commonMain/sqldelight/SettingsDatabase/com/z_company/data_local/setting/db/UserSettings.sq`
- Добавить 3 колонки: `isShowLocoHeating INTEGER NOT NULL DEFAULT 1`, `isShowLocoAuxiliary INTEGER NOT NULL DEFAULT 1`, `isShowLocoStatistics INTEGER NOT NULL DEFAULT 1`
- Обновить `insertOrReplace` query

#### 2c. SettingsMapper
**Файл:** `data_local/src/commonMain/kotlin/com/z_company/data_local/setting/mapping/SettingsMapper.kt`
- `toData()`: добавить маппинг `isShowLocoHeating = row.isShowLocoHeating != 0L`, аналогично для остальных

#### 2d. SqlDelightSettingRepository
**Файл:** `data_local/src/commonMain/kotlin/com/z_company/data_local/setting/SqlDelightSettingRepository.kt`
- `insertUserSettings()`: добавить 3 параметра

#### 2e. SettingsViewModel
**Файл:** `features/route/src/main/java/com/z_company/route/viewmodel/SettingsViewModel.kt`
Добавить 3 метода:
```kotlin
fun changeShowLocoHeating(value: Boolean) { currentSettings = currentSettings?.copy(isShowLocoHeating = value) }
fun changeShowLocoAuxiliary(value: Boolean) { ... }
fun changeShowLocoStatistics(value: Boolean) { ... }
```

#### 2f. SettingsScreen — раздел "Локомотив"
**Файл:** `features/route/src/main/java/com/z_company/route/ui/SettingsScreen.kt`

Добавить callbacks в параметры `SettingsScreen`:
```kotlin
changeShowLocoHeating: (Boolean) -> Unit,
changeShowLocoAuxiliary: (Boolean) -> Unit,
changeShowLocoStatistics: (Boolean) -> Unit,
```

Новая секция "Локомотив" (перед "О приложении"):
```
Заголовок: "Локомотив"
Card:
  Switch "Отопление"         — подсказка: "Показывать счётчики отопления в форме локомотива"
  Switch "Собственные нужды"  — подсказка: "Показывать счётчики собственных нужд"
  Switch "Статистика"         — подсказка: "Показывать раздел статистики расхода"
```

Паттерн рендеринга — как существующий Switch "Показывать перерыв" (строки 1161–1204).

#### 2g. SettingDestination
**Файл:** `features/route/src/main/java/com/z_company/route/navigation/SettingDestination.kt`
Прокинуть 3 новых callback-а из SettingsViewModel в SettingsScreen.

#### 2h. FormLocoScreen — условная видимость
**Файл:** `features/route/src/main/java/com/z_company/route/ui/FormLocoScreen.kt`

LocoFormViewModel уже загружает `settingsState` с `UserSettings`. Использовать `settings.isShowLocoHeating`, `isShowLocoAuxiliary`, `isShowLocoStatistics` чтобы условно рендерить секции:
```kotlin
if (settings.isShowLocoHeating) {
    CollapsibleSection(title = "Отопление", ...) { ... }
}
```

### 3. Исправить расчёт итого электроэнергии
**Файл:** `features/route/src/main/java/com/z_company/route/component/StatisticsSection.kt`

Текущий баг: итог считается всегда, даже когда норма `null`. Нужно показывать итог только при указанной норме (как уже сделано для дизеля на строке 166 `if (!locomotive.normaDiesel.isNullOrBlank())`).

**Ток 1 (строки 75–85):**
```kotlin
// Было: всегда считает итог
// Стало: обернуть в if
if (locomotive.normaElectricCurrent1 != null) {
    val result = locomotive.normaElectricCurrent1!! - (overResult?.toLong()?.toInt() ?: 0)
    ...
    StatRow(label = "Итог", value = resultText, valueColor = resultColor)
}
```

**Ток 2 (строки 109–118):** аналогично — обернуть в `if (locomotive.normaElectricCurrent2 != null)`.

### 4. Onboarding-диалог для новых полей
**Подход:** Использовать существующий механизм `SharedPreferencesRepositories` с версионным флагом.

#### 4a. SharedPreferencesRepositories (domain interface)
**Файл:** `domain/src/commonMain/kotlin/com/z_company/domain/repositories/SharedPreferencesRepositories.kt`
Добавить:
```kotlin
fun isShowLocoFormUpdateHint(): Boolean
fun setLocoFormUpdateHintShown()
```

#### 4b. SharedPreferenceStorage (Android impl)
**Файл:** `data_local/src/androidMain/kotlin/com/z_company/data_local/SharedPreferenceStorage.kt`
Новый ключ `TOKEN_SHOW_LOCO_FORM_UPDATE_V2_1_7` (default: `true`).

#### 4c. LocoFormViewModel
**Файл:** `features/route/src/main/java/com/z_company/route/viewmodel/LocoFormViewModel.kt`
- Inject `SharedPreferencesRepositories`
- При init: проверить `isShowLocoFormUpdateHint()`, если true → выставить `isShowUpdateHint = true` в UI state

#### 4d. LocoFormUiState
**Файл:** `features/route/src/main/java/com/z_company/route/viewmodel/LocoFormUiState.kt`
Добавить: `val isShowUpdateHint: Boolean = false`

#### 4e. Диалог в FormLocoScreen
**Файл:** `features/route/src/main/java/com/z_company/route/ui/FormLocoScreen.kt`

Показать `AlertDialog` при `formUiState.isShowUpdateHint`:
```
Заголовок: "Обновление"
Текст: "В форму локомотива добавлены новые поля: счётчики отопления и собственных нужд.
Вы можете отключить их отображение в Настройках → Локомотив."
Кнопка: "Понятно" → dismiss + setLocoFormUpdateHintShown()
```

---

## Файлы (итого)

| Файл | Действие |
|------|----------|
| `features/.../component/CollapsibleSection.kt` | EDIT: +icon параметр |
| `features/.../component/StatisticsSection.kt` | EDIT: fix итого электро |
| `features/.../ui/FormLocoScreen.kt` | EDIT: иконки, переименования, условная видимость, диалог |
| `features/.../viewmodel/LocoFormUiState.kt` | EDIT: +isShowUpdateHint |
| `features/.../viewmodel/LocoFormViewModel.kt` | EDIT: hint logic |
| `domain/.../entities/setting/UserSettings.kt` | EDIT: +3 поля |
| `data_local/.../db/UserSettings.sq` | EDIT: +3 колонки |
| `data_local/.../mapping/SettingsMapper.kt` | EDIT: маппинг 3 полей |
| `data_local/.../SqlDelightSettingRepository.kt` | EDIT: insert 3 полей |
| `domain/.../repositories/SharedPreferencesRepositories.kt` | EDIT: +2 метода |
| `data_local/.../SharedPreferenceStorage.kt` | EDIT: impl 2 методов |
| `features/.../viewmodel/SettingsViewModel.kt` | EDIT: +3 change-метода |
| `features/.../ui/SettingsScreen.kt` | EDIT: секция "Локомотив" с 3 Switch |
| `features/.../navigation/SettingDestination.kt` | EDIT: прокинуть callbacks |

## Проверка
1. `./gradlew :features:route:compileDebugKotlin` — компиляция
2. Открыть FormLocoScreen — секции имеют иконки слева, текст центрирован
3. Секция "Время" — заголовок "Время" с иконкой часов, внутри "Приёмка" и "Сдача"
4. Настройки → "Локомотив" → отключить Отопление → вернуться → секция скрыта
5. Не указывать норму → итог не показывается; указать → итог появляется
6. Первый вход → диалог с информацией о новых полях → "Понятно" → больше не показывается
