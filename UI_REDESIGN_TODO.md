# UI Redesign — Задачи по новому функционалу

Источник: дизайн-пакет из Claude Design (`design_handoff_android/`).
Ветка: `feature/ui-redesign`

---

## Дизайн-система (завершено)

- [x] Новые цвета Light/Dark по токенам `tokens.js`
- [x] Шрифты Inter + JetBrains Mono
- [x] Типографика (displayLarge, displayMedium, headlineLarge, headlineMedium, titleMedium, bodyMedium, labelMedium и т.д.)
- [x] Расширенная палитра `MashinistColors` (bg, bgElevated, bgSubtle, surface, surfaceAlt, accent, accentSoft, cta, chipBg, success, warning, danger)
- [x] Spacing/Radius/Semantic токены
- [x] Тема `LocoDriverTheme` с CompositionLocal для `MashinistTheme.colors`

## Общие компоненты (завершено)

- [x] `MCard` — карточка с surface, borderRadius 16, тень sm
- [x] `GroupHeader` — UPPERCASE mono заголовок группы
- [x] `FieldRow` + `CardDivider` — строка поля в карточке
- [x] `MashinistNavigationBar` — 5-пунктная нижняя навигация (Главный/Зарплата/+/Настройки/Профиль)
- [x] `MashinistTopBar` / `MashinistLargeTopBar` — топ-бары
- [x] `StubScreen` — заглушка для неготовых экранов

## Заглушки новых экранов (созданы)

- [x] `StatisticsScreen` — Месяц/Год/История (заглушка)
- [x] `CalendarScreen` — расширенный календарь (заглушка)
- [x] `ScheduleWizardScreen` — мастер «Заполнить месяц» (заглушка)
- [x] `NormsScreen` — справочники норм серий и станций (заглушка)
- [x] `WidgetsInfoScreen` — виджеты на рабочий стол (заглушка)

---

## Цветовая миграция (завершена)

Все экраны автоматически получают новые цвета через обновлённую тему:
- [x] Фон экрана: #F2F3F5 (light) / #0F1011 (dark)
- [x] Карточки: #FFFFFF (light) / #2A2B2D (dark)
- [x] Текст: #0A0E14 (light) / #F5F5F5 (dark)
- [x] Акцент: #00A0F5 (light) / #33BFFF (dark)
- [x] HomeScreen — gradient→surface, text secondary→onSurfaceVariant, progress tertiary
- [x] WorkedTimeHeader — time primary, chip accentSoft+tertiary
- [x] DetailWorkTimeCard/DetailTrainCard — text onSurfaceVariant
- [x] Shapes — small 10, medium 16, large 28

## Layout обновления (в процессе)

### TopAppBar и заголовки (завершено)
- [x] **HomeScreen** — TopAppBar с «М Машинист» + иконки, крупный «Месяц Год»
- [x] **HomeScreen** — hero-метрика displayMedium (mono 32/700), breakdown bodySmall
- [x] **FormScreen** — TopAppBar «Новый маршрут» / «Маршрут · №N» + кнопка назад
- [x] **AllRouteScreen** — переключатель «Май 2026» titleMedium
- [x] **SalaryScreen** — TopAppBar «Зарплата» + hero «К ВЫДАЧЕ» mono + displayMedium
- [x] **SettingsScreen** — группы РАСЧЁТ/ВНЕШНИЙ ВИД/О ПРИЛОЖЕНИИ mono UPPERCASE
- [x] **ProfileScreen** — группы АККАУНТ/СИНХРОНИЗАЦИЯ mono UPPERCASE

### Детальная переверстка layout (в процессе)

- [x] **HomeScreen** — плитка «НА РАБОТЕ» с displayMedium + прогресс-бар, UPPERCASE заголовки секций
- [x] **FormScreen** — UPPERCASE mono заголовки групп в ItemAddingScreen
- [x] **SettingsScreen** — SettingsNavItem с subtitle/value, мягкая тень, подписи к пунктам
- [x] **SalaryScreen** — итоговый блок «К ВЫДАЧЕ» с акцентным стилем
- [x] **PurchasesScreen** — заголовок «Машинист Pro»
- [x] **FormScreen** — группы ОСНОВНЫЕ ДАННЫЕ / ВРЕМЯ РАБОТЫ
- [x] **ProfileScreen** — заголовок «Профиль» headlineLarge, группа ПОДПИСКА
- [x] **LoginScreen** — логотип «М» + заголовок «Вход в Машинист»
- [x] **FormLocoScreen** — TopAppBar «Локомотив» + кнопка назад
- [x] **FormTrainScreen** — TopAppBar «Поезд» + кнопка назад
- [x] **FormPassengerScreen** — TopAppBar «Пассажиром» + кнопка назад

### Подэкраны настроек (завершено)
- [x] SettingsRouteContent: ДАННЫЕ ПО УМОЛЧАНИЮ / ПЕРЕРЫВ / СТИЛЬ ВЫБОРА ВРЕМЕНИ / СВЫШЕ 12 ЧАСОВ
- [x] SettingsAccountingContent: НОЧНЫЕ ЧАСЫ / БУДУЩИЕ МАРШРУТЫ
- [x] SettingsLocoContent: ПОКАЗАТЕЛИ
- [x] SettingSalaryScreen: Начисления/Удержания headlineMedium
- [x] SalaryCalculationScreen: НАЧИСЛЕНИЯ / УДЕРЖАНИЯ mono UPPERCASE

### Дочерние формы (завершено)
- [x] FormLocoScreen: ОСНОВНЫЕ ДАННЫЕ group header
- [x] FormTrainScreen: МАРШРУТ group header
- [x] BottomNavigationBar: bodySmall Inter, accent selected, alwaysShowLabel

### Hi-fi заглушки (завершено)
- [x] StatisticsScreen: табы, hero, сетка метрик 2x4, топ направлений
- [x] CalendarScreen: hero, табы, сетка дней, кнопки, детали дня
- [x] ScheduleWizardScreen: stepper, паттерны, время смены
- [x] NormsScreen: группы серий, строки с нормами, FAB
- [x] WidgetsInfoScreen: превью Mini + Expanded виджетов

### Дополнительные TopAppBar (завершено)
- [x] AllRouteScreen: «Маршруты»
- [x] WorkScheduleScreen: «График»
- [x] SettingsScreen: headlineLarge на хабе

### Карточки и чипы (завершено)
- [x] ItemHomeScreen: compact mode с поездом №+станции и номером маршрута #N
- [x] AllRouteScreen: assist-chips Фильтр/Дата, переключатель месяца ‹ Май 2026 ›
- [x] Навигация: stub-экраны подключены в NavGraph
- [x] SettingsScreen: группы СПРАВОЧНИКИ НОРМ / ПРИЛОЖЕНИЕ / ПОДСВЕТКА

### Полная переверстка (завершено)
- [x] PurchasesScreen: hero «Машинист Pro», преимущества, radio-тарифы, pill-кнопка «Оформить»
- [x] SettingsScreen: «Резервные копии» с badge «Вкл»
- [x] AllRouteScreen: TopAppBar с навигацией назад и PDF action
- [x] WorkScheduleScreen: TopAppBar с навигацией назад
- [x] Мягкие тени 1dp по всему проекту (40+ файлов)
- [x] ItemHomeScreen: убран BorderStroke

### Низкий приоритет (оставшееся)

- [ ] **FormLocoScreen** — экипировка свёрнута/развёрнута, шторки коэффициентов.
- [ ] **FormTrainScreen** — шторки плеч и настроек.
- [ ] **FormPassengerScreen** — флаг workStart.

---

## Новый функционал (требует бэкенд/логику)

### Статистика
- [ ] Экран «Месяц» — детализация по видам работ, графики
- [ ] Экран «Год» — сводка по месяцам
- [ ] Экран «История» — полная история за все время
- [ ] Пустое состояние

### Расширенный календарь
- [ ] Режим «месяц» (expanded) — сетка дней с цветовой кодировкой
- [ ] Режим «неделя» (collapsed) — компактная неделя
- [ ] Шторка «Что добавить?» — маршрут, отвлечение, выходной
- [ ] День-выходной с x2 тарифом
- [ ] Добавление маршрута из календаря (`RouteAddScreen`)
- [ ] Отвлечение-диапазон (`AbsenceFlowScreen` — отпуск, больничный и т.п.)

### Мастер «Заполнить месяц»
- [ ] Шаг 1 — выбор паттерна смен (2/2, свой цикл), редактирование, пикер типа дня, подтверждение удаления
- [ ] Шаг 2 — выбор даты старта + предпросмотр заполненного месяца

### Справочники норм
- [ ] Список серий локомотивов с нормами
- [ ] Редактор серии
- [ ] Список станций с нормами
- [ ] Редактор станции

### Виджеты Android
- [ ] Mini (2x2) — норма за месяц, чёрный чип «М», mono-метрика
- [ ] Expanded (4x4) — норма + текущий маршрут + быстрая запись
- [ ] GlanceAppWidget implementation

### Предупреждение о ночных
- [ ] NightWarnSheet — предупреждение «вторая ночь подряд»

### Альтернативные палитры
- [ ] Реализовать `M_PALETTES` (пресеты типа «Кремовая бумага»)
- [ ] Выбор палитры в настройках (раздел «Тема»)

---

## Навигация

- [ ] Интегрировать `MashinistNavigationBar` в MainScreen / NavHost
- [ ] Привязать вкладки: HOME → HomeScreen, SALARY → SalaryCalculationScreen, ADD → создание маршрута, SETTINGS → SettingsScreen, PROFILE → ProfileScreen
- [ ] Добавить навигацию к stub-экранам (Statistics, Calendar, ScheduleWizard, Norms, Widgets)

---

## Заметки

- Все экраны должны поддерживать Light и Dark тему
- Шрифты: Inter (UI текст) + JetBrains Mono (числа, время, суммы, UPPERCASE-заголовки)
- Карточки: borderRadius 16, тень sm, overflow hidden
- Bottom sheets: borderRadius 28 сверху, ручка 32x4, scrim rgba(20,18,14,0.42)
- AlertDialog: maxWidth 312, borderRadius 28, padding 24
- Touch targets: минимум 48x48 для навбар кнопок, 44 для инпутов
