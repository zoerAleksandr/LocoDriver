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

## Экраны для полной переверстки layout по дизайну

### Высокий приоритет

- [ ] **HomeScreen** — large top app bar с логотипом «М Машинист», hero-метрика месяца mono 48/800, чип «+30:05 сверх», свайп-карточка (3 страницы: нормы/виды работы/доплатные признаки) с точечным индикатором, горизонтальные плитки «Текущий маршрут» (150x150: На работе, Локомотив, Поезд, Пассажиром), «Последние маршруты» с иконками-признаками, «Инструменты» (График/Отвлечения/Поиск/PDF). Состояния: current/upcoming/rest/empty. Шторки legend и units.
- [ ] **FormScreen (Маршрут)** — карточки MCard, группы (ОСНОВНЫЕ ДАННЫЕ, ВРЕМЯ РАБОТЫ, ЛОКОМОТИВЫ, ПОЕЗДА, ПАССАЖИРОМ), плитки «Расчёт» и «Отдых», bottom bar с действиями (настройки/избранное/поделиться/копировать/удалить). Автосохранение (нет кнопки «Сохранить»). Шторки calc, rest/rest-home, confirm delete, nightWarn.
- [ ] **AllRouteScreen** — переключатель месяца «< Май >», assist-chips фильтр/сортировка, карточки маршрутов compact/expanded, шторки filter/sort, пустой месяц.
- [ ] **SalaryCalculationScreen** — hero «К выдаче» mono, таблицы начислений/удержаний с фиксированными колонками, filled-итоги с акцентным цветом.

### Средний приоритет

- [ ] **SettingsScreen** — хаб настроек с группами (СПРАВОЧНИКИ НОРМ, РАСЧЁТ, ВНЕШНИЙ ВИД, ПОДСВЕТКА), строки с иконками и значениями. Экспорт данных.
- [ ] **SettingsGeneralScreen** — основные настройки (переключатели, selectors).
- [ ] **SettingSalaryScreen** — filled-инпуты для тарифов, bottom sheet даты тарифа.
- [ ] **ProfileScreen** — large top app bar, VK ID фото/имя, email с кнопкой редактирования, подписка «Машинист Pro» + бейдж АКТИВНА, синхронизация (сохранить/загрузить из облака), кнопка «Выйти из аккаунта».
- [ ] **LoginScreen** — primary tabs (Вход/Регистрация), filled text fields, VK ID кнопка, восстановление пароля, экран «письмо отправлено».

### Низкий приоритет

- [ ] **FormLocoScreen** — traction diesel/electric, экипировка свёрнута/развёрнута, шторки коэффициентов секции/экипировки.
- [ ] **FormTrainScreen** — форма поезда, шторки плеч и настроек.
- [ ] **FormPassengerScreen** — следование пассажиром с флагом workStart.
- [ ] **SplashScreen** — логотип «М» по центру, слоган «Для тех, у кого всё под контролем.»
- [ ] **PurchasesScreen (Paywall)** — витрина тарифов (Месяц 199₽/3мес 499₽/Год 1490₽), outlined-тарифы с radio, filled-кнопка. Состояния: active/expired/renew sheet.

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
