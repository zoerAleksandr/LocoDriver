import SwiftUI
import ComposeApp

struct SettingsView: View {
    @StateObject private var vm = SettingsViewModelWrapper()

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                settingsList
            }
        }
        .navigationTitle("Настройки")
    }

    private var settingsList: some View {
        List {
            guard let s = vm.settings else {
                return AnyView(Text("Настройки не загружены").foregroundColor(.secondary))
            }
            return AnyView(Group {
                Section("Маршруты") {
                    Toggle("Использовать время по умолчанию", isOn: Binding(
                        get: { s.usingDefaultWorkTime },
                        set: { vm.setUsingDefaultWorkTime($0) }
                    ))
                    Toggle("Учитывать будущие маршруты", isOn: Binding(
                        get: { s.isConsiderFutureRoute },
                        set: { vm.setConsiderFutureRoute($0) }
                    ))
                }

                Section("Норма") {
                    LabeledContent("Часов в месяц") {
                        Text("\(s.defaultWorkTime / 3_600_000) ч")
                            .foregroundColor(.secondary)
                    }
                }

                Section("Ночное время") {
                    LabeledContent("Начало ночи") {
                        Text(String(format: "%02d:%02d", s.nightTime.startNightHour, s.nightTime.startNightMinute))
                            .foregroundColor(.secondary)
                    }
                    LabeledContent("Конец ночи") {
                        Text(String(format: "%02d:%02d", s.nightTime.endNightHour, s.nightTime.endNightMinute))
                            .foregroundColor(.secondary)
                    }
                }

                Section("Локомотив") {
                    Toggle("Отопление", isOn: Binding(
                        get: { s.isShowLocoHeating },
                        set: { vm.setShowLocoHeating($0) }
                    ))
                    Toggle("Собственные нужды", isOn: Binding(
                        get: { s.isShowLocoAuxiliary },
                        set: { vm.setShowLocoAuxiliary($0) }
                    ))
                    Toggle("Статистика", isOn: Binding(
                        get: { s.isShowLocoStatistics },
                        set: { vm.setShowLocoStatistics($0) }
                    ))
                    Toggle("Норма", isOn: Binding(
                        get: { s.isShowLocoNorma },
                        set: { vm.setShowLocoNorma($0) }
                    ))
                }

                if !s.servicePhases.isEmpty {
                    Section("Фазы обслуживания") {
                        ForEach(s.servicePhases, id: \.id) { phase in
                            HStack {
                                Text(phase.departureStation)
                                Image(systemName: "arrow.right")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text(phase.arrivalStation)
                                Spacer()
                                Text("\(phase.distance) км")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
            })
        }
    }
}
