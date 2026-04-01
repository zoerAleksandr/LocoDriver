import SwiftUI
import ComposeApp

// MARK: - Root

struct SettingsView: View {
    @StateObject private var vm = SettingsViewModelWrapper()

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let s = vm.settings {
                SettingsHubView(settings: s, vm: vm)
            } else {
                Text("Настройки не загружены")
                    .foregroundColor(.secondary)
            }
        }
        .navigationTitle("Настройки")
        .navigationBarTitleDisplayMode(.large)
    }
}

// MARK: - Hub (main list)

private struct SettingsHubView: View {
    let settings: DomainUserSettings
    @ObservedObject var vm: SettingsViewModelWrapper

    var body: some View {
        List {
            NavigationLink(destination: SettingsNormaView(settings: settings, vm: vm)) {
                Text("Норма")
            }
            NavigationLink(destination: SettingsAccountingView(settings: settings, vm: vm)) {
                Text("Учёт")
            }
            NavigationLink(destination: SettingsRestView(settings: settings, vm: vm)) {
                Text("Отдых")
            }
            NavigationLink(destination: SettingsRouteView(settings: settings, vm: vm)) {
                Text("Маршрут")
            }
            NavigationLink(destination: SettingsLocoView(settings: settings, vm: vm)) {
                Text("Локомотив")
            }
        }
    }
}

// MARK: - Норма

private struct SettingsNormaView: View {
    let settings: DomainUserSettings
    @ObservedObject var vm: SettingsViewModelWrapper

    @State private var normaHours: Int

    init(settings: DomainUserSettings, vm: SettingsViewModelWrapper) {
        self.settings = settings
        self.vm = vm
        _normaHours = State(initialValue: Int(settings.defaultWorkTime / 3_600_000))
    }

    var body: some View {
        Form {
            Section(
                header: Text(""),
                footer: Text("Установите плановую норму рабочих часов в месяц.")
            ) {
                Stepper(value: $normaHours, in: 1...300, step: 1) {
                    HStack {
                        Text("Норма часов в месяц")
                        Spacer()
                        Text("\(normaHours) ч")
                            .foregroundColor(.secondary)
                    }
                }
                .onChange(of: normaHours) { newValue in
                    vm.setNormaHours(Int64(newValue))
                }
            }
        }
        .navigationTitle("Норма")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Учёт (Accounting)

private struct SettingsAccountingView: View {
    let settings: DomainUserSettings
    @ObservedObject var vm: SettingsViewModelWrapper

    @State private var nightStart: Date
    @State private var nightEnd: Date
    @State private var considerFuture: Bool

    init(settings: DomainUserSettings, vm: SettingsViewModelWrapper) {
        self.settings = settings
        self.vm = vm
        _nightStart = State(initialValue: Self.timeDate(
            hour: Int(settings.nightTime.startNightHour),
            minute: Int(settings.nightTime.startNightMinute)
        ))
        _nightEnd = State(initialValue: Self.timeDate(
            hour: Int(settings.nightTime.endNightHour),
            minute: Int(settings.nightTime.endNightMinute)
        ))
        _considerFuture = State(initialValue: settings.isConsiderFutureRoute)
    }

    var body: some View {
        Form {
            Section(
                header: Text("Ночные часы"),
                footer: Text("Ночные часы используются при расчёте ночных надбавок.")
            ) {
                DatePicker("Начало ночи", selection: $nightStart, displayedComponents: .hourAndMinute)
                    .onChange(of: nightStart) { newValue in
                        let cal = Calendar.current
                        vm.setNightStartTime(
                            hour: Int32(cal.component(.hour, from: newValue)),
                            minute: Int32(cal.component(.minute, from: newValue))
                        )
                    }

                DatePicker("Конец ночи", selection: $nightEnd, displayedComponents: .hourAndMinute)
                    .onChange(of: nightEnd) { newValue in
                        let cal = Calendar.current
                        vm.setNightEndTime(
                            hour: Int32(cal.component(.hour, from: newValue)),
                            minute: Int32(cal.component(.minute, from: newValue))
                        )
                    }
            }

            Section(
                header: Text(""),
                footer: Text("Маршруты, время явки которых ещё не наступило, будут учитываться при подсчёте отработанного времени.")
            ) {
                Toggle("Учитывать будущие маршруты", isOn: $considerFuture)
                    .onChange(of: considerFuture) { vm.setConsiderFutureRoute($0) }
            }
        }
        .navigationTitle("Учёт")
        .navigationBarTitleDisplayMode(.inline)
    }

    private static func timeDate(hour: Int, minute: Int) -> Date {
        var comps = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        comps.hour = hour
        comps.minute = minute
        comps.second = 0
        return Calendar.current.date(from: comps) ?? Date()
    }
}

// MARK: - Отдых (Rest)

private struct SettingsRestView: View {
    let settings: DomainUserSettings
    @ObservedObject var vm: SettingsViewModelWrapper

    @State private var restMinutes: Int
    @State private var homeRestMinutes: Int

    init(settings: DomainUserSettings, vm: SettingsViewModelWrapper) {
        self.settings = settings
        self.vm = vm
        _restMinutes     = State(initialValue: Int(settings.minTimeRestPointOfTurnover / 60_000))
        _homeRestMinutes = State(initialValue: Int(settings.minTimeHomeRest / 60_000))
    }

    var body: some View {
        Form {
            Section(
                header: Text(""),
                footer: Text("Минимальное время отдыха используется при расчёте отдыха после поездки.")
            ) {
                Stepper(value: $restMinutes, in: 0...1440, step: 15) {
                    HStack {
                        Text("Отдых в ПО")
                        Spacer()
                        Text(Self.formatDuration(minutes: restMinutes))
                            .foregroundColor(.secondary)
                    }
                }
                .onChange(of: restMinutes) { vm.setMinTimeRest(Int64($0) * 60_000) }

                Stepper(value: $homeRestMinutes, in: 0...2880, step: 15) {
                    HStack {
                        Text("Домашний отдых")
                        Spacer()
                        Text(Self.formatDuration(minutes: homeRestMinutes))
                            .foregroundColor(.secondary)
                    }
                }
                .onChange(of: homeRestMinutes) { vm.setMinTimeHomeRest(Int64($0) * 60_000) }
            }
        }
        .navigationTitle("Отдых")
        .navigationBarTitleDisplayMode(.inline)
    }

    private static func formatDuration(minutes: Int) -> String {
        let h = minutes / 60
        let m = minutes % 60
        if h == 0 { return "\(m) мин" }
        if m == 0 { return "\(h) ч" }
        return "\(h) ч \(m) мин"
    }
}

// MARK: - Маршрут (Route)

private struct SettingsRouteView: View {
    let settings: DomainUserSettings
    @ObservedObject var vm: SettingsViewModelWrapper

    @State private var usingDefault: Bool
    @State private var showBreak: Bool
    @State private var defaultWorkMinutes: Int

    init(settings: DomainUserSettings, vm: SettingsViewModelWrapper) {
        self.settings = settings
        self.vm = vm
        _usingDefault       = State(initialValue: settings.usingDefaultWorkTime)
        _showBreak          = State(initialValue: settings.isShowBreak)
        _defaultWorkMinutes = State(initialValue: Int(settings.defaultWorkTime / 60_000))
    }

    var body: some View {
        Form {
            Section(
                header: Text("Данные по умолчанию"),
                footer: Text("Эти значения будут установлены при создании нового маршрута.")
            ) {
                Toggle("Использовать стандартное время работы", isOn: $usingDefault)
                    .onChange(of: usingDefault) { vm.setUsingDefaultWorkTime($0) }

                if usingDefault {
                    Stepper(value: $defaultWorkMinutes, in: 0...1440, step: 15) {
                        HStack {
                            Text("Время работы")
                            Spacer()
                            Text(Self.formatDuration(minutes: defaultWorkMinutes))
                                .foregroundColor(.secondary)
                        }
                    }
                    .onChange(of: defaultWorkMinutes) {
                        vm.setDefaultWorkTime(Int64($0) * 60_000)
                    }
                }
            }

            Section(
                header: Text(""),
                footer: Text("Показывать поля перерыва в форме маршрута.")
            ) {
                Toggle("Показывать перерыв", isOn: $showBreak)
                    .onChange(of: showBreak) { vm.setShowBreak($0) }
            }
        }
        .navigationTitle("Маршрут")
        .navigationBarTitleDisplayMode(.inline)
    }

    private static func formatDuration(minutes: Int) -> String {
        let h = minutes / 60
        let m = minutes % 60
        if h == 0 { return "\(m) мин" }
        if m == 0 { return "\(h) ч" }
        return "\(h) ч \(m) мин"
    }
}

// MARK: - Локомотив (Loco)

private struct SettingsLocoView: View {
    let settings: DomainUserSettings
    @ObservedObject var vm: SettingsViewModelWrapper

    @State private var showHeating: Bool
    @State private var showAuxiliary: Bool
    @State private var showStatistics: Bool
    @State private var showNorma: Bool
    @State private var showOtherCurrent: Bool

    init(settings: DomainUserSettings, vm: SettingsViewModelWrapper) {
        self.settings = settings
        self.vm = vm
        _showHeating      = State(initialValue: settings.isShowLocoHeating)
        _showAuxiliary    = State(initialValue: settings.isShowLocoAuxiliary)
        _showStatistics   = State(initialValue: settings.isShowLocoStatistics)
        _showNorma        = State(initialValue: settings.isShowLocoNorma)
        _showOtherCurrent = State(initialValue: settings.isShowOtherCurrent)
    }

    var body: some View {
        Form {
            Section(
                header: Text("Показывать поля"),
                footer: Text("Эти поля будут отображаться при вводе показаний локомотива.")
            ) {
                Toggle("Отопление", isOn: $showHeating)
                    .onChange(of: showHeating)      { vm.setShowLocoHeating($0) }
                Toggle("Собственные нужды", isOn: $showAuxiliary)
                    .onChange(of: showAuxiliary)    { vm.setShowLocoAuxiliary($0) }
                Toggle("Статистика", isOn: $showStatistics)
                    .onChange(of: showStatistics)   { vm.setShowLocoStatistics($0) }
                Toggle("Норма", isOn: $showNorma)
                    .onChange(of: showNorma)        { vm.setShowLocoNorma($0) }
            }

            Section(
                header: Text(""),
                footer: Text("Для переменно-постоянных электровозов.")
            ) {
                Toggle("Смена рода тока", isOn: $showOtherCurrent)
                    .onChange(of: showOtherCurrent) { vm.setShowOtherCurrent($0) }
            }
        }
        .navigationTitle("Локомотив")
        .navigationBarTitleDisplayMode(.inline)
    }
}
