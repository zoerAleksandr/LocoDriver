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
            if let settings = vm.settings {
                Section("Маршруты") {
                    LabeledContent("Часовой пояс (мин)", value: "\(settings.timeZone)")
                    LabeledContent("Учитывать будущие маршруты") {
                        Image(systemName: settings.isFuturePlanningEnabled ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(settings.isFuturePlanningEnabled ? .accentColor : .secondary)
                    }
                }

                Section("Норма") {
                    LabeledContent("Часовая норма", value: "\(settings.normaHours) ч")
                }

                Section("Локомотив") {
                    LabeledContent("Показывать отопление") {
                        Image(systemName: settings.isShowLocoHeating ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(settings.isShowLocoHeating ? .accentColor : .secondary)
                    }
                    LabeledContent("Показывать вспомогательное") {
                        Image(systemName: settings.isShowLocoAuxiliary ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(settings.isShowLocoAuxiliary ? .accentColor : .secondary)
                    }
                    LabeledContent("Показывать статистику") {
                        Image(systemName: settings.isShowLocoStatistics ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(settings.isShowLocoStatistics ? .accentColor : .secondary)
                    }
                    LabeledContent("Показывать норму") {
                        Image(systemName: settings.isShowLocoNorma ? "checkmark.circle.fill" : "circle")
                            .foregroundColor(settings.isShowLocoNorma ? .accentColor : .secondary)
                    }
                }
            } else {
                Section {
                    Text("Настройки не загружены")
                        .foregroundColor(.secondary)
                }
            }
        }
    }
}
