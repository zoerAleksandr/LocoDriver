import Foundation

// Реестр экранов и шторок iOS — зеркало SCREEN_SPECS.md §3.2 (методы Router)
// и §24.5 (реестр overlays).
//
// ОБЩАЯ ЗОНА: волны только ДОБАВЛЯЮТ кейсы/ветки, существующие не переименовывают
// и не удаляют (см. docs/IOS_UI_GUIDE.md).
//
// Не заводятся (🚫 мёртвый код Android / Android-легаси, в iOS не реализуется):
// SignIn/LogIn (§20), FirstPresentationBlock (§20), DetailsRoute (§22),
// SelectReleaseDaysScreen (§16.2), UpdatePresentationBlock «Что нового» (§22),
// MigrationRecoveryScreen и диагностика (§25), ConfirmExitDialog (§3.3),
// CustomDatePickerDialog (§24.2), EnteredCoefficientDialog/EnteredRefuelDialog (§1.3).

/// Корневые вкладки (SCREEN_SPECS §3.1). «+» — не вкладка, а действие (§3.4).
enum AppTab: Hashable, CaseIterable {
    case home, salary, settings, profile

    var title: String {
        switch self {
        case .home: return "Главная"
        case .salary: return "Зарплата"
        case .settings: return "Настройки"
        case .profile: return "Профиль"
        }
    }

    var icon: DSIcon {
        switch self {
        case .home: return .document
        case .salary: return .ruble
        case .settings: return .sliders
        case .profile: return .profile
        }
    }
}

/// Под-разделы Настроек (§3.2: `showSettingsX` → `SettingsScreenRoute` с аргументом).
enum SettingsSection: Hashable {
    /// `ROUTE`
    case route
    /// `ROUTE_FORM`
    case routeForm
    /// `LOCOMOTIVE` / `LOCOMOTIVE_SERIES_<имя>`
    case locomotive(series: String?)
    /// `TRAIN`
    case train
    /// `REST`
    case rest
    /// `SERIES_LIST`
    case seriesList
    /// `SERIES_EDITOR_<id>`
    case seriesEditor(id: String)
    /// `SERIES_NEW_<имя>`
    case seriesNew(name: String)
    /// `STATION_LIST`
    case stationList
    /// `STATION_EDITOR_<id>`
    case stationEditor(id: String)

    var title: String {
        switch self {
        case .route: return "Маршрут"
        case .routeForm: return "Форма маршрута"
        case .locomotive: return "Локомотив"
        case .train: return "Поезд"
        case .rest: return "Отдых"
        case .seriesList: return "Серии локомотивов"
        case .seriesEditor, .seriesNew: return "Серия"
        case .stationList: return "Станции"
        case .stationEditor: return "Станция"
        }
    }

    var specSection: String {
        switch self {
        case .route, .routeForm, .locomotive, .train, .rest: return "§17.5"
        case .seriesList, .seriesEditor, .seriesNew, .stationList, .stationEditor: return "§17.4, §2.10"
        }
    }
}

/// Экраны, открываемые push'ем в стеке текущей вкладки.
enum AppRoute: Hashable {
    // §5 Форма маршрута
    case routeForm(basicId: String?)
    case routeFormCopy(basicId: String)
    /// Shared preview по ссылке `locodriver://share/{id}` (§3.1, §5.6)
    case sharedRoutePreview(routeId: String)
    // §1 Локомотив, §6 Поезд, §7 Пассажиром, §8 Прочая работа
    case locoForm(basicId: String, locoId: String?)
    case trainForm(basicId: String, trainId: String?)
    case passengerForm(basicId: String, passengerId: String?)
    case otherWorkForm(basicId: String, otherWorkId: String?)
    // §8.5 Напарники
    case partnersManage
    case partnerPicker(basicId: String)
    case partnerEditor(partnerId: String?)
    // §9 Все маршруты, §9.4 Корзина
    case allRoutes
    case trash
    // §10 Поиск
    case search
    // §11.8 Справочник кодов
    case payrollCodeSearch
    // §12 Настройки зарплаты
    case settingSalary
    // §13 Статистика
    case statistics
    // §14 Календарь, §15 Мастер, §16.4 Отвлечения
    case calendar
    case scheduleWizard
    case absence
    // §17 Под-разделы настроек
    case settingsSection(SettingsSection)
    // §18.R Пригласить друга
    case referral
    // §19 Покупки (только через гейт — AppRouter.showPurchases)
    case purchases

    var title: String {
        switch self {
        case .routeForm(let basicId): return basicId == nil ? "Новый маршрут" : "Маршрут"
        case .routeFormCopy: return "Копия маршрута"
        case .sharedRoutePreview: return "Маршрут"
        case .locoForm: return "Локомотив"
        case .trainForm: return "Поезд"
        case .passengerForm: return "Пассажиром"
        case .otherWorkForm: return "Прочая работа"
        case .partnersManage: return "Напарники"
        case .partnerPicker: return "Выбор напарников"
        case .partnerEditor(let id): return id == nil ? "Новый напарник" : "Напарник"
        case .allRoutes: return "Маршруты"
        case .trash: return "Корзина"
        case .search: return "Поиск"
        case .payrollCodeSearch: return "Коды начислений"
        case .settingSalary: return "Настройки зарплаты"
        case .statistics: return "Статистика"
        case .calendar: return "Календарь"
        case .scheduleWizard: return "Заполнить месяц"
        case .absence: return "Новое отвлечение"
        case .settingsSection(let section): return section.title
        case .referral: return "Пригласить друга"
        case .purchases: return "Машинист Pro"
        }
    }

    var specSection: String {
        switch self {
        case .routeForm: return "§5"
        case .routeFormCopy: return "§5.6, §24.3"
        case .sharedRoutePreview: return "§5.6"
        case .locoForm: return "§1"
        case .trainForm: return "§6"
        case .passengerForm: return "§7"
        case .otherWorkForm: return "§8"
        case .partnersManage, .partnerPicker: return "§8.5.1"
        case .partnerEditor: return "§8.5.2"
        case .allRoutes: return "§9"
        case .trash: return "§9.4"
        case .search: return "§10"
        case .payrollCodeSearch: return "§11.8"
        case .settingSalary: return "§12"
        case .statistics: return "§13"
        case .calendar: return "§14"
        case .scheduleWizard: return "§15"
        case .absence: return "§16.4"
        case .settingsSection(let section): return section.specSection
        case .referral: return "§18.R"
        case .purchases: return "§19"
        }
    }
}

/// Шторки и модальные экраны уровня приложения (`AppRouter.present`).
///
/// Шторки, которые редактируют состояние конкретного экрана и возвращают ему
/// результат (время, коэффициенты, пикеры), экран может показывать локально через
/// `.dsSheet` — кейс здесь остаётся реестром/заглушкой. Короткие подтверждения
/// («Удалить …?», «Создать копию маршрута?») — локальные, в реестр не входят.
enum AppSheet: Identifiable, Hashable {
    enum TimeKind: String, Hashable { case arrival, departure }
    enum MonthPickerContext: String, Hashable { case home, allRoutes, salary, statistics }

    // §1–2 Локомотив и шторка времени
    case timeSheet(locoId: String, kind: TimeKind)
    case seriesPicker
    case stationPicker
    case coeffSheet(locoId: String)
    // §4 Главная
    case monthPicker(MonthPickerContext)
    case metricInfo
    case routeUnits(basicId: String)
    // §9.1, §9.3 Быстрый просмотр и легенда
    case routeQuickView(basicId: String)
    case routeLegend
    // §5 Маршрут
    case calcSheet(basicId: String)
    case restSheet(basicId: String)
    case passenger12h(basicId: String)
    // §6 Поезд
    case stationEdit(trainId: String, stationId: String?)
    case segmentEdit(trainId: String)
    case shoulders(trainId: String)
    case shoulderEdit(shoulderId: String?)
    case trainParams(trainId: String)
    case wagonCounter(trainId: String)
    case trainHistory(trainId: String)
    // §8 Прочая работа
    case otherWorkType
    case locoPicker(basicId: String)
    // §9.0.1 Все маршруты
    case routesFilter
    case routesSort
    // §10.3 Поиск
    case searchSettings
    // §12.3 Настройки зарплаты
    case tariffChanged
    // §13 Статистика
    case statisticsCompare
    case statisticsFreightInfo
    // §14.3 Календарь
    case addEvent(dayMillis: Int64)
    // §15.1 Мастер графика
    case continueSchedule
    // §17 Настройки
    case settingsPicker(key: String)
    case nightRange
    // §18.6 Профиль: шторки ввода
    case appInput(key: String)
    case emailPassword
    // §21 PDF
    case pdfContent
    case pdfActions
    // §24.2 Дата и время
    case dateTimePicker(key: String)
    // §23 Новость при запуске (полноэкранный оверлей)
    case announcement

    var id: String {
        switch self {
        case let .timeSheet(locoId, kind): return "timeSheet-\(locoId)-\(kind.rawValue)"
        case .seriesPicker: return "seriesPicker"
        case .stationPicker: return "stationPicker"
        case let .coeffSheet(locoId): return "coeffSheet-\(locoId)"
        case let .monthPicker(context): return "monthPicker-\(context.rawValue)"
        case .metricInfo: return "metricInfo"
        case let .routeUnits(basicId): return "routeUnits-\(basicId)"
        case let .routeQuickView(basicId): return "routeQuickView-\(basicId)"
        case .routeLegend: return "routeLegend"
        case let .calcSheet(basicId): return "calcSheet-\(basicId)"
        case let .restSheet(basicId): return "restSheet-\(basicId)"
        case let .passenger12h(basicId): return "passenger12h-\(basicId)"
        case let .stationEdit(trainId, stationId): return "stationEdit-\(trainId)-\(stationId ?? "new")"
        case let .segmentEdit(trainId): return "segmentEdit-\(trainId)"
        case let .shoulders(trainId): return "shoulders-\(trainId)"
        case let .shoulderEdit(shoulderId): return "shoulderEdit-\(shoulderId ?? "new")"
        case let .trainParams(trainId): return "trainParams-\(trainId)"
        case let .wagonCounter(trainId): return "wagonCounter-\(trainId)"
        case let .trainHistory(trainId): return "trainHistory-\(trainId)"
        case .otherWorkType: return "otherWorkType"
        case let .locoPicker(basicId): return "locoPicker-\(basicId)"
        case .routesFilter: return "routesFilter"
        case .routesSort: return "routesSort"
        case .searchSettings: return "searchSettings"
        case .tariffChanged: return "tariffChanged"
        case .statisticsCompare: return "statisticsCompare"
        case .statisticsFreightInfo: return "statisticsFreightInfo"
        case let .addEvent(dayMillis): return "addEvent-\(dayMillis)"
        case .continueSchedule: return "continueSchedule"
        case let .settingsPicker(key): return "settingsPicker-\(key)"
        case .nightRange: return "nightRange"
        case let .appInput(key): return "appInput-\(key)"
        case .emailPassword: return "emailPassword"
        case .pdfContent: return "pdfContent"
        case .pdfActions: return "pdfActions"
        case let .dateTimePicker(key): return "dateTimePicker-\(key)"
        case .announcement: return "announcement"
        }
    }

    var title: String {
        switch self {
        case let .timeSheet(_, kind): return kind == .arrival ? "Приёмка" : "Сдача"
        case .seriesPicker: return "Серия"
        case .stationPicker: return "Станция"
        case .coeffSheet: return "Коэффициент"
        case .monthPicker: return "Выберите месяц и год"
        case .metricInfo: return "Пояснение"
        case .routeUnits: return "Единицы маршрута"
        case .routeQuickView: return "Маршрут"
        case .routeLegend: return "Обозначения"
        case .calcSheet: return "Расчёт за смену"
        case .restSheet: return "Отдых"
        case .passenger12h: return "Смена > 12 ч"
        case .stationEdit: return "Станция"
        case .segmentEdit: return "Перегон"
        case .shoulders: return "Плечи"
        case .shoulderEdit: return "Плечо"
        case .trainParams: return "Параметры поезда"
        case .wagonCounter: return "Вагонник"
        case .trainHistory: return "История данных поезда"
        case .otherWorkType: return "Тип работы"
        case .locoPicker: return "Локомотив"
        case .routesFilter: return "Фильтры"
        case .routesSort: return "Сортировка"
        case .searchSettings: return "Параметры поиска"
        case .tariffChanged: return "Изменилась тарифная ставка"
        case .statisticsCompare: return "Сравнение"
        case .statisticsFreightInfo: return "Грузооборот"
        case .addEvent: return "Что добавить?"
        case .continueSchedule: return "Продолжить график прошлого месяца?"
        case .settingsPicker: return "Выбор"
        case .nightRange: return "Ночные часы"
        case .appInput: return "Ввод"
        case .emailPassword: return "Email и пароль"
        case .pdfContent: return "Содержание PDF"
        case .pdfActions: return "PDF"
        case .dateTimePicker: return "Дата и время"
        case .announcement: return "Новость"
        }
    }

    var specSection: String {
        switch self {
        case .timeSheet: return "§2"
        case .seriesPicker, .stationPicker: return "§2.10"
        case .coeffSheet: return "§1.3"
        case .monthPicker: return "§4.4, §9.0.1, §11.7"
        case .metricInfo: return "§4.4"
        case .routeUnits: return "§4.4"
        case .routeQuickView: return "§9.1"
        case .routeLegend: return "§9.3"
        case .calcSheet: return "§11.9"
        case .restSheet: return "§5.7"
        case .passenger12h: return "§5.5"
        case .stationEdit: return "§6.4"
        case .segmentEdit: return "§6.5"
        case .shoulders, .shoulderEdit: return "§6.6"
        case .trainParams, .wagonCounter: return "§6.2"
        case .trainHistory: return "§6.7"
        case .otherWorkType, .locoPicker: return "§8.3"
        case .routesFilter, .routesSort: return "§9.0.1"
        case .searchSettings: return "§10.3"
        case .tariffChanged: return "§12.3"
        case .statisticsCompare: return "§13.3"
        case .statisticsFreightInfo: return "§13.4"
        case .addEvent: return "§14.3"
        case .continueSchedule: return "§15.1"
        case .settingsPicker: return "§17.2"
        case .nightRange: return "§17.3"
        case .appInput, .emailPassword: return "§18.6"
        case .pdfContent, .pdfActions: return "§21"
        case .dateTimePicker: return "§24.2"
        case .announcement: return "§23"
        }
    }

    /// Показывать на весь экран (`fullScreenCover`), а не шторкой.
    var isFullScreen: Bool {
        self == .announcement
    }
}
