import SwiftUI
import ComposeApp

// SCREEN_SPECS §17.1–§17.5: под-экраны Настроек. Макет: design/src/settings-subscreens.jsx,
// IOSScreenSettingsGeneral/Accounting/Locomotive/Norms/Shoulders (design/src/ios-screens.jsx).

/// Точка входа для `AppRoute.settingsSection` (в т.ч. deep link из форм).
struct SettingsSectionDestination: View {
    let section: SettingsSection

    var body: some View {
        switch section {
        case .route: SettingsGeneralView()
        case .routeForm: SettingsRouteFormView()
        case .locomotive: SettingsLocoView()
        case .train: SettingsTrainView()
        case .rest: SettingsRestView()
        case .norma: SettingsNormaView()
        case .normaMonth: NormMonthView()
        case .accounting: SettingsAccountingView()
        case .shoulders: SettingsShouldersView()
        // Справочники серий/станций — отдельная задача (§17.4, сессия «стандартные редакторы»).
        case .seriesList, .seriesEditor, .seriesNew, .stationList, .stationEditor:
            StubView(title: section.title, specSection: section.specSection)
        }
    }
}

// MARK: - Основные (§17.5)

private struct SettingsGeneralView: View {
    @StateObject private var vm = SettingsScreenViewModelWrapper()
    @State private var showWorkTime = false

    var body: some View {
        SettingsScaffold(title: "Основные", onDisappear: { vm.flush() }) {
            if let st = vm.state, let s = st.settings {
                DSList(header: "ТЕМА ОФОРМЛЕНИЯ", headerTopSpacing: DSMetrics.groupHeadFirst) {
                    ForEach([("MODE_SYSTEM", "Как в системе"), ("MODE_LIGHT", "Светлая"), ("MODE_DARK", "Тёмная")], id: \.0) { item in
                        SettingsRadioRow(title: item.1, isSelected: st.themeMode == item.0,
                                         showsDivider: item.0 != "MODE_DARK") { vm.setThemeMode(item.0) }
                    }
                }
                DSList(header: "ВИД ТЯГИ") {
                    SettingsRadioRow(title: "Электротяга", isSelected: s.defaultLocoType == .electric) {
                        vm.setDefaultLocoType(electric: true)
                    }
                    SettingsRadioRow(title: "Теплотяга", isSelected: s.defaultLocoType == .diesel, showsDivider: false) {
                        vm.setDefaultLocoType(electric: false)
                    }
                }
                DSList(header: "ФОРМАТ ВРЕМЕНИ") {
                    SettingsRadioRow(title: "Часы и минуты", subtitle: "Например, 12:30", isSelected: !s.isDecimalTime) {
                        vm.setDecimalTime(false)
                    }
                    SettingsRadioRow(title: "Десятичный", subtitle: "Например, 12,5", isSelected: s.isDecimalTime,
                                     showsDivider: false) { vm.setDecimalTime(true) }
                }
                DSList(header: "ВРЕМЯ РАБОТЫ") {
                    SettingsToggleRow(title: "Стандартное время работы", isOn: s.usingDefaultWorkTime,
                                      showsDivider: s.usingDefaultWorkTime) { vm.setUsingDefaultWorkTime($0) }
                    if s.usingDefaultWorkTime {
                        SettingsValueRow(title: "Время работы", value: SettingsFormat.hhmm(s.defaultWorkTime),
                                         mono: true, showsDivider: false) { showWorkTime = true }
                    }
                }
                SettingsHint("Подставляется по умолчанию при создании нового маршрута.")
                DSList(header: "СТИЛЬ ВЫБОРА ВРЕМЕНИ") {
                    SettingsRadioRow(title: "Системный", subtitle: "Стандартный диалог iOS",
                                     isSelected: s.useStandardTimePicker) { vm.setUseStandardTimePicker(true) }
                    SettingsRadioRow(title: "Кастомный", subtitle: "Быстрый ввод цифрами",
                                     isSelected: !s.useStandardTimePicker, showsDivider: false) {
                        vm.setUseStandardTimePicker(false)
                    }
                }
                DSList(header: "СВЫШЕ 12 ЧАСОВ - ПАССАЖИРОМ") {
                    ForEach([("ALWAYS_ASK", "Всегда спрашивать"), ("AUTO", "Автоматически"), ("NEVER_ASK", "Никогда не спрашивать")], id: \.0) { item in
                        SettingsRadioRow(title: item.1, isSelected: st.passenger12h == item.0,
                                         showsDivider: item.0 != "NEVER_ASK") { vm.setPassenger12h(item.0) }
                    }
                }
                SettingsHint("Если поездка длится более 12 часов, часть времени можно оформить как следование пассажиром.")
            }
        }
        .dsSheet(isPresented: $showWorkTime, detents: [.height(320)]) {
            SettingsDurationSheet(title: "Время работы по умолчанию", maxHours: 23,
                                  initialMillis: vm.settings?.defaultWorkTime ?? 43_200_000,
                                  onSave: { vm.setDefaultWorkTime(millis: $0) },
                                  onClose: { showWorkTime = false })
        }
    }
}

// MARK: - Маршрут (§17.5)

private struct SettingsRouteFormView: View {
    @StateObject private var vm = SettingsScreenViewModelWrapper()

    var body: some View {
        SettingsScaffold(title: "Маршрут", onDisappear: { vm.flush() }) {
            if let s = vm.settings {
                DSList(header: "ЭЛЕМЕНТЫ ФОРМЫ", headerTopSpacing: DSMetrics.groupHeadFirst) {
                    SettingsToggleRow(title: "Показывать перерыв", subtitle: "Поля перерыва в форме маршрута",
                                      isOn: s.isShowBreak) { vm.setShowBreak($0) }
                    SettingsToggleRow(title: "Переключатель «Одно лицо»", subtitle: "Отметка работы в одно лицо в маршруте",
                                      isOn: s.isShowOnePersonSwitch) { vm.setShowOnePerson($0) }
                    SettingsToggleRow(title: "Добавить напарника", subtitle: "Раздел «Напарники» в форме маршрута",
                                      isOn: s.isShowPartner, showsDivider: false) { vm.setShowPartner($0) }
                }
                DSList(header: "РАЗДЕЛЫ МАРШРУТА") {
                    SettingsToggleRow(title: "Показывать локомотив", isOn: s.isShowLocomotive) { vm.setShowLocomotive($0) }
                    SettingsToggleRow(title: "Показывать поездную работу", isOn: s.isShowTrain) { vm.setShowTrain($0) }
                    SettingsToggleRow(title: "Показывать пассажиром", isOn: s.isShowPassenger) { vm.setShowPassenger($0) }
                    SettingsToggleRow(title: "Показывать прочую работу", subtitle: "Маневровая, вывозная, при депо и др.",
                                      isOn: s.isShowOtherWork, showsDivider: false) { vm.setShowOtherWork($0) }
                }
                SettingsHint("Управляет отображением разделов и элементов в форме маршрута.")
            }
        }
    }
}

// MARK: - Локомотив (§17.5)

private struct SettingsLocoView: View {
    @StateObject private var vm = SettingsScreenViewModelWrapper()

    var body: some View {
        SettingsScaffold(title: "Локомотив", onDisappear: { vm.flush() }) {
            if let st = vm.state, let s = st.settings {
                DSList(header: "ПОЛЯ ВВОДА ПОКАЗАНИЙ", headerTopSpacing: DSMetrics.groupHeadFirst) {
                    SettingsToggleRow(title: "Отопление", isOn: st.showLocoHeating) { vm.setShowLocoHeating($0) }
                    if st.showLocoHeating {
                        SettingsToggleRow(title: "Учитывать в общем расходе", subtitle: "Отопление",
                                          isOn: s.isConsiderLocoHeatingInTotal) { vm.setConsiderHeatingInTotal($0) }
                    }
                    SettingsToggleRow(title: "Собственные нужды", isOn: st.showLocoAuxiliary,
                                      showsDivider: st.showLocoAuxiliary) { vm.setShowLocoAuxiliary($0) }
                    if st.showLocoAuxiliary {
                        SettingsToggleRow(title: "Учитывать в общем расходе", subtitle: "Собственные нужды",
                                          isOn: s.isConsiderLocoAuxiliaryInTotal, showsDivider: false) {
                            vm.setConsiderAuxiliaryInTotal($0)
                        }
                    }
                }
                SettingsHint("Показывать эти поля в форме локомотива для ввода показаний.")
                DSList(header: "РОД ТОКА") {
                    SettingsToggleRow(title: "Смена рода тока", subtitle: "Для переменно-постоянных электровозов.",
                                      isOn: st.showOtherCurrent, showsDivider: false) { vm.setShowOtherCurrent($0) }
                }
                DSList(header: "ТИП ТЯГИ ПО УМОЛЧАНИЮ") {
                    SettingsRadioRow(title: "Электротяга", isSelected: s.defaultLocoType == .electric) {
                        vm.setDefaultLocoType(electric: true)
                    }
                    SettingsRadioRow(title: "Теплотяга", isSelected: s.defaultLocoType == .diesel, showsDivider: false) {
                        vm.setDefaultLocoType(electric: false)
                    }
                }
                SettingsHint("Будет выбран при добавлении локомотива.")
            }
        }
    }
}

// MARK: - Поезд (§17.5)

private struct SettingsTrainView: View {
    @StateObject private var vm = SettingsScreenViewModelWrapper()
    @State private var editing = false
    @State private var text = ""

    private var meters: Double { vm.settings?.passengerWagonLengthMeters ?? 24.5 }

    private func format(_ v: Double) -> String {
        v == v.rounded() ? String(Int(v)) : String(v).replacingOccurrences(of: ".", with: ",")
    }

    private var parsed: Double? {
        let v = Double(text.replacingOccurrences(of: ",", with: "."))
        return (v ?? 0) > 0 ? v : nil
    }

    var body: some View {
        SettingsScaffold(title: "Поезд", onDisappear: { vm.flush() }) {
            DSList(header: "РАСЧЁТ ДЛИНЫ", headerTopSpacing: DSMetrics.groupHeadFirst) {
                SettingsValueRow(title: "Пассажирский вагон", value: "\(format(meters)) м", mono: true,
                                 showsDivider: false) {
                    text = format(meters)
                    editing = true
                }
            }
            SettingsHint("Для пассажирского поезда значение У.Д. считается количеством вагонов и умножается на эту длину. Пассажирский поезд определяется по номеру. Для грузового поезда сохраняется расчёт 1 У.Д. = 14 м.")
        }
        .sheet(isPresented: $editing) {
            NavigationStack {
                Form {
                    Section(footer: Text("По умолчанию 24,5 м")) {
                        HStack {
                            Text("Метры")
                            TextField("24,5", text: $text)
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                                .onChange(of: text) { new in
                                    let f = new.filter { "0123456789,.".contains($0) }
                                    if f != new { text = f }
                                }
                        }
                        if parsed == nil {
                            Text("Введите число больше нуля").font(.footnote).foregroundColor(DSColor.danger)
                        }
                    }
                }
                .navigationTitle("Длина пассажирского вагона")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("Отмена") { editing = false } }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Сохранить") {
                            if let v = parsed { vm.setPassengerWagonLength(v) }
                            editing = false
                        }
                        .disabled(parsed == nil)
                    }
                }
            }
            .presentationDetents([.medium])
        }
    }
}

// MARK: - Отдых (§17.5)

private struct SettingsRestView: View {
    @StateObject private var vm = SettingsScreenViewModelWrapper()

    private enum Field: Int, Identifiable {
        case turnover, second, home
        var id: Int { rawValue }
    }
    @State private var field: Field? = nil

    var body: some View {
        SettingsScaffold(title: "Отдых", onDisappear: { vm.flush() }) {
            if let s = vm.settings {
                DSList(header: "МИНИМАЛЬНЫЙ ОТДЫХ", headerTopSpacing: DSMetrics.groupHeadFirst) {
                    SettingsValueRow(title: "Отдых в ПО", value: SettingsFormat.hhmm(s.minTimeRestPointOfTurnover),
                                     mono: true) { field = .turnover }
                    SettingsValueRow(title: "Отдых в ПО (второй отдых)",
                                     value: SettingsFormat.hhmm(s.minTimeRestPointOfTurnoverSecond),
                                     mono: true) { field = .second }
                    SettingsValueRow(title: "Домашний отдых", value: SettingsFormat.hhmm(s.minTimeHomeRest),
                                     mono: true, showsDivider: false) { field = .home }
                }
                SettingsHint("Второй минимум применяется, когда два последовательных маршрута заканчиваются отдыхом в ПО.")
            }
        }
        .dsSheet(item: $field, detents: [.height(320)]) { f in
            switch f {
            case .turnover:
                SettingsDurationSheet(title: "Минимальный отдых в ПО", maxHours: 99,
                                      initialMillis: vm.settings?.minTimeRestPointOfTurnover ?? 0,
                                      onSave: { vm.setMinTimeRest(millis: $0) }, onClose: { field = nil })
            case .second:
                SettingsDurationSheet(title: "Отдых в ПО (второй отдых)", maxHours: 99,
                                      initialMillis: vm.settings?.minTimeRestPointOfTurnoverSecond ?? 0,
                                      onSave: { vm.setMinTimeRestSecond(millis: $0) }, onClose: { field = nil })
            case .home:
                SettingsDurationSheet(title: "Минимальный домашний отдых", maxHours: 99,
                                      initialMillis: vm.settings?.minTimeHomeRest ?? 0,
                                      onSave: { vm.setMinTimeHomeRest(millis: $0) }, onClose: { field = nil })
            }
        }
    }
}

// MARK: - Учёт (§17.3)

private struct SettingsAccountingView: View {
    @StateObject private var vm = SettingsScreenViewModelWrapper()
    @State private var showNight = false

    private func nightText(_ s: DomainUserSettings) -> String {
        let n = s.nightTime
        return String(format: "%02d:%02d - %02d:%02d", Int(n.startNightHour), Int(n.startNightMinute),
                      Int(n.endNightHour), Int(n.endNightMinute))
    }

    var body: some View {
        SettingsScaffold(title: "Учёт", onDisappear: { vm.flush() }) {
            if let s = vm.settings {
                DSList(header: "НОЧНЫЕ ЧАСЫ", headerTopSpacing: DSMetrics.groupHeadFirst) {
                    SettingsValueRow(title: "Ночь", value: nightText(s), mono: true, showsDivider: false) {
                        showNight = true
                    }
                }
                SettingsHint("Интервал ночных часов для расчёта доплаты за работу ночью. Время указывается по МСК.")
                DSList(header: "БУДУЩИЕ МАРШРУТЫ") {
                    SettingsToggleRow(title: "Учитывать будущие маршруты", subtitle: "С ещё не наступившей явкой",
                                      isOn: s.isConsiderFutureRoute, showsDivider: false) { vm.setConsiderFutureRoute($0) }
                }
                SettingsHint("Маршруты, время явки которых не наступило, будут учитываться при подсчёте отработанного времени.")
            }
        }
        .dsSheet(isPresented: $showNight, detents: [.height(460)]) {
            if let s = vm.settings {
                NightRangeSheet(
                    startHour: Int(s.nightTime.startNightHour), startMinute: Int(s.nightTime.startNightMinute),
                    endHour: Int(s.nightTime.endNightHour), endMinute: Int(s.nightTime.endNightMinute),
                    onSave: { sh, sm, eh, em in
                        vm.setNightTime(startHour: sh, startMinute: sm, endHour: eh, endMinute: em)
                    },
                    onClose: { showNight = false })
            }
        }
    }
}

/// `NightRangeSheet` (§17.3): две плитки «Начало» → «Конец» правят черновик; «Готово» и «Сохранить» сохраняют обе границы.
private struct NightRangeSheet: View {
    let onSave: (Int, Int, Int, Int) -> Void
    let onClose: () -> Void

    @State private var startHour: Int
    @State private var startMinute: Int
    @State private var endHour: Int
    @State private var endMinute: Int
    @State private var editingStart = true

    init(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int,
         onSave: @escaping (Int, Int, Int, Int) -> Void, onClose: @escaping () -> Void) {
        _startHour = State(initialValue: startHour)
        _startMinute = State(initialValue: startMinute)
        _endHour = State(initialValue: endHour)
        _endMinute = State(initialValue: endMinute)
        self.onSave = onSave
        self.onClose = onClose
    }

    private func save() {
        onSave(startHour, startMinute, endHour, endMinute)
        onClose()
    }

    private func tile(_ label: String, _ h: Int, _ m: Int, active: Bool, tap: @escaping () -> Void) -> some View {
        Button(action: tap) {
            VStack(spacing: 4) {
                Text(label).dsTextStyle(.captionMuted)
                Text(String(format: "%02d:%02d", h, m)).font(DSFont.mono(28, .semibold)).foregroundColor(DSColor.text)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(active ? DSColor.accentSoft : DSColor.surface)
            .overlay(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous)
                .stroke(active ? DSColor.accent : DSColor.border, lineWidth: 1.5))
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private var hourBinding: Binding<Int> {
        Binding(get: { editingStart ? startHour : endHour },
                set: { if editingStart { startHour = $0 } else { endHour = $0 } })
    }

    private var minuteBinding: Binding<Int> {
        Binding(get: { editingStart ? startMinute : endMinute },
                set: { if editingStart { startMinute = $0 } else { endMinute = $0 } })
    }

    var body: some View {
        DSBottomSheet(
            header: .cancelDone(title: "Ночные часы", cancel: "Отмена", done: "Готово", onDone: { save() }),
            showsGrabber: false, onClose: onClose
        ) {
            Text("Интервал, который считается ночным временем (по МСК)").dsTextStyle(.captionMuted)
                .padding(.bottom, 10)
            HStack(spacing: 12) {
                tile("Начало", startHour, startMinute, active: editingStart) { editingStart = true }
                Image(systemName: "arrow.right").foregroundColor(DSColor.textFaint)
                tile("Конец", endHour, endMinute, active: !editingStart) { editingStart = false }
            }
            HStack(spacing: 0) {
                Picker("Часы", selection: hourBinding) {
                    ForEach(0..<24, id: \.self) { Text(String(format: "%02d", $0)).tag($0) }
                }
                .pickerStyle(.wheel)
                Picker("Минуты", selection: minuteBinding) {
                    ForEach(0..<60, id: \.self) { Text(String(format: "%02d", $0)).tag($0) }
                }
                .pickerStyle(.wheel)
            }
            .frame(height: 130)
            DSCTAButton(title: "Сохранить") { save() }.padding(.top, 8)
        }
        .interactiveDismissDisabled()
    }
}

// MARK: - Норма и регион (§17.2)

private struct SettingsNormaView: View {
    @StateObject private var vm = SettingsScreenViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared

    private enum PickerKind: Int, Identifiable {
        case country, zone, cross
        var id: Int { rawValue }
    }
    @State private var picker: PickerKind? = nil

    private static let zones: [(Int64, String)] = [
        (-1, "Калининград (MSK–1, UTC+2)"), (0, "Москва (UTC+3)"), (1, "Самара (MSK+1, UTC+4)"),
        (2, "Екатеринбург (MSK+2, UTC+5)"), (3, "Омск (MSK+3, UTC+6)"), (4, "Красноярск (MSK+4, UTC+7)"),
        (5, "Иркутск (MSK+5, UTC+8)"), (6, "Якутск (MSK+6, UTC+9)"), (7, "Владивосток (MSK+7, UTC+10)"),
        (8, "Магадан (MSK+8, UTC+11)"), (9, "Камчатка (MSK+9, UTC+12)"), (10, "Анадырь (MSK+10, UTC+13)"),
    ]
    private static let hour: Int64 = 3_600_000

    private func countryTitle(_ c: String) -> String {
        switch c {
        case "KZ": return "🇰🇿 Казахстан"
        case "BY": return "🇧🇾 Беларусь"
        default: return "🇷🇺 Россия"
        }
    }

    private func zoneTitle(_ millis: Int64) -> String {
        Self.zones.first { $0.0 * Self.hour == millis }?.1 ?? "Москва (UTC+3)"
    }

    var body: some View {
        SettingsScaffold(title: "Норма и регион", onDisappear: { vm.flush() }) {
            if let st = vm.state, let s = st.settings {
                let monthIndex = max(0, min(11, Int(s.selectMonthOfYear.month)))
                let month = "\(SettingsFormat.months[monthIndex]) \(s.selectMonthOfYear.year)"
                DSList(header: "НОРМА ЧАСОВ", headerTopSpacing: DSMetrics.groupHeadFirst) {
                    SettingsValueRow(title: month,
                                     value: st.normaMinutes.map { SettingsFormat.hhmm(Int64($0.intValue) * 60_000) } ?? "—",
                                     mono: true, showsChevron: false)
                    // «Норма по месяцам» (§16.3.1).
                    DSListRow(title: "Изменить норму", showsChevron: false,
                              showsDivider: false, titleColor: DSColor.accent) {
                        router.show(.settingsSection(.normaMonth))
                    }
                }
                SettingsHint("Месячная норма рабочего времени. По ней считается переработка и недоработка.")

                DSList(header: "СТРАНА") {
                    SettingsValueRow(title: countryTitle(s.country), value: "", showsDivider: false) { picker = .country }
                }
                // Блок «РЕГИОН» не показан: список регионов и загрузка праздников — сквозная задача С1.

                DSList(header: "ДОМАШНИЙ ЧАСОВОЙ ПОЯС") {
                    if s.country == "RU" {
                        SettingsValueRow(title: zoneTitle(s.timeZone), value: "", showsDivider: false) { picker = .zone }
                    } else {
                        SettingsValueRow(title: s.country == "KZ" ? "UTC+5 (Kazakhstan Time, KZT)" : "UTC+3 (Минск)",
                                         value: "", showsChevron: false, showsDivider: false)
                    }
                }
                SettingsHint("Установите местный часовой пояс. Учитывается при расчёте ночных, праздничных часов и переходных поездках.")

                if s.country == "RU" && s.timeZone != 0 {
                    DSList(header: "ПЕРЕХОДНЫЕ МАРШРУТЫ") {
                        SettingsValueRow(title: s.crossMonthTimezone == .local ? "По местному времени" : "По московскому времени",
                                         value: "", showsDivider: false) { picker = .cross }
                    }
                    SettingsHint("Определяет, по какому времени переходной маршрут относится к месяцу.")
                }
            }
        }
        .dsSheet(item: $picker, detents: [.large]) { p in
            if let s = vm.settings {
                switch p {
                case .country:
                    SettingsPickerSheet(title: "Страна", options: [
                        SettingsPickerOption(id: "RU", title: "Россия", flag: "🇷🇺"),
                        SettingsPickerOption(id: "KZ", title: "Казахстан", flag: "🇰🇿"),
                        SettingsPickerOption(id: "BY", title: "Беларусь", flag: "🇧🇾"),
                    ], selectedId: s.country, onSelect: { vm.setCountry($0) }, onClose: { picker = nil })
                case .zone:
                    SettingsPickerSheet(title: "Часовой пояс",
                        options: Self.zones.map { SettingsPickerOption(id: String($0.0), title: $0.1) },
                        selectedId: String(Self.zones.first { $0.0 * Self.hour == s.timeZone }?.0 ?? 0),
                        onSelect: { if let h = Int64($0) { vm.setTimeZone(millis: h * Self.hour) } },
                        onClose: { picker = nil })
                case .cross:
                    SettingsPickerSheet(title: "Переходные маршруты",
                        hint: "По какому времени переходной маршрут относится к месяцу",
                        options: [SettingsPickerOption(id: "LOCAL", title: "По местному времени"),
                                  SettingsPickerOption(id: "MOSCOW", title: "По московскому времени")],
                        selectedId: s.crossMonthTimezone == .local ? "LOCAL" : "MOSCOW",
                        onSelect: { vm.setCrossMonthLocal($0 == "LOCAL") }, onClose: { picker = nil })
                }
            }
        }
    }
}

// MARK: - Плечи (§17.4)

private struct PhaseEditorTarget: Identifiable {
    let id = UUID()
    let phase: DomainServicePhase?
}

private struct SettingsShouldersView: View {
    @StateObject private var vm = SettingsScreenViewModelWrapper()
    @State private var editor: PhaseEditorTarget? = nil

    var body: some View {
        SettingsScaffold(title: "Плечи", showsDone: false, onDisappear: { vm.flush() }) {
            let phases = vm.phases
            SettingsHint("Расстояние участка обслуживания. Подставляется автоматически при выборе плеча в маршруте.")
            if phases.isEmpty {
                VStack(spacing: 6) {
                    Text("Нет сохранённых плеч").font(DSFont.sans(16, .semibold)).foregroundColor(DSColor.text)
                    Text("Добавьте участки обслуживания с расстоянием").dsTextStyle(.captionMuted)
                }
                .frame(maxWidth: .infinity).padding(.top, 48)
            } else {
                DSList(header: "ПЛЕЧИ · \(phases.count)") {
                    ForEach(Array(phases.enumerated()), id: \.element.id) { index, p in
                        DSListRow(title: "\(p.departureStation) — \(p.arrivalStation)",
                                  value: "\(p.distance) км · \(p.linearMileageRate) ₽/км", valueIsMono: true,
                                  showsDivider: index < phases.count - 1) { editor = PhaseEditorTarget(phase: p) }
                    }
                }
            }
            Color.clear.frame(height: 80)
        }
        // Кнопка закреплена внизу экрана (§17.4).
        .safeAreaInset(edge: .bottom) {
            DSCTAButton(title: "Добавить плечо", icon: .plus) { editor = PhaseEditorTarget(phase: nil) }
                .padding(.horizontal, DSSpacing.lg).padding(.vertical, 8)
                .background(DSColor.bg)
        }
        .sheet(item: $editor) { target in
            PhaseEditorSheet(phase: target.phase, onSave: { from, to, dist, rate, reverse in
                vm.savePhase(target.phase, from: from, to: to, distance: dist, rate: rate, alsoReverse: reverse)
            }, onDelete: {
                if let p = target.phase { vm.deletePhase(id: p.id) }
            })
        }
    }
}

/// Упрощённый редактор плеча (§6.6 целиком — волна «Поезд»): станции, расстояние, ставка, «создать обратное», удаление.
private struct PhaseEditorSheet: View {
    let phase: DomainServicePhase?
    let onSave: (String, String, Int, Double, Bool) -> Void
    let onDelete: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var from: String
    @State private var to: String
    @State private var distance: String
    @State private var rate: String
    @State private var reverse = false
    @State private var deleteRequest: DSDeleteRequest? = nil

    init(phase: DomainServicePhase?, onSave: @escaping (String, String, Int, Double, Bool) -> Void,
         onDelete: @escaping () -> Void) {
        self.phase = phase
        self.onSave = onSave
        self.onDelete = onDelete
        _from = State(initialValue: phase?.departureStation ?? "")
        _to = State(initialValue: phase?.arrivalStation ?? "")
        _distance = State(initialValue: phase.map { String($0.distance) } ?? "")
        _rate = State(initialValue: phase.map { String($0.linearMileageRate) } ?? "")
    }

    private var distanceValue: Int? {
        guard let v = Int(distance), v >= 0 else { return nil }
        return v
    }

    private var rateValue: Double? {
        if rate.isEmpty { return 0 }
        guard let v = Double(rate.replacingOccurrences(of: ",", with: ".")), v >= 0 else { return nil }
        return v
    }

    private var isValid: Bool {
        !from.trimmingCharacters(in: .whitespaces).isEmpty
            && !to.trimmingCharacters(in: .whitespaces).isEmpty
            && distanceValue != nil && rateValue != nil
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Станция отправления", text: $from)
                    TextField("Станция прибытия", text: $to)
                    TextField("Расстояние, км", text: $distance).keyboardType(.numberPad)
                    TextField("Ставка, ₽/км", text: $rate).keyboardType(.decimalPad)
                }
                if phase == nil {
                    Section { Toggle("Создать обратное плечо", isOn: $reverse) }
                } else {
                    Section {
                        Button("Удалить плечо", role: .destructive) {
                            deleteRequest = DSDeleteRequest(title: "Удалить плечо?") {
                                onDelete()
                                dismiss()
                            }
                        }
                    }
                }
            }
            .navigationTitle(phase == nil ? "Новое плечо" : "Плечо")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Отмена") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Сохранить") {
                        onSave(from.trimmingCharacters(in: .whitespaces), to.trimmingCharacters(in: .whitespaces),
                               distanceValue ?? 0, rateValue ?? 0, reverse)
                        dismiss()
                    }
                    .disabled(!isValid)
                }
            }
            .dsDeleteAlert($deleteRequest)
        }
    }
}
