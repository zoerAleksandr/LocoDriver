import SwiftUI
import ComposeApp

// SCREEN_SPECS §17 / §17.1: hub настроек. Макет: IOSScreenSettings (design/src/ios-screens.jsx).

struct SettingsView: View {
    @StateObject private var vm = SettingsScreenViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared
    @State private var confirmExport = false
    @State private var shareItems: ShareItems? = nil

    private struct ShareItems: Identifiable {
        let id = UUID()
        let items: [Any]
    }

    private var versionText: String {
        let v = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—"
        return "версия \(v)"
    }

    private var countryName: String {
        switch vm.settings?.country {
        case "KZ": return "Казахстан"
        case "BY": return "Беларусь"
        default: return "Россия"
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if let s = vm.state {
                    DSList(header: "СПРАВОЧНИКИ", headerTopSpacing: DSMetrics.groupHeadFirst) {
                        DSListRow(title: "Серии локомотивов",
                                  subtitle: SettingsFormat.plural(Int(s.seriesCount), "серия", "серии", "серий"),
                                  icon: .locomotive) { router.show(.settingsSection(.seriesList)) }
                        DSListRow(title: "Станции",
                                  subtitle: SettingsFormat.plural(Int(s.stationCount), "станция", "станции", "станций"),
                                  icon: .mapPin) { router.show(.settingsSection(.stationList)) }
                        DSListRow(title: "Плечи",
                                  subtitle: SettingsFormat.plural(Int(s.phasesCount), "плечо", "плечи", "плеч"),
                                  icon: .train) { router.show(.settingsSection(.shoulders)) }
                        DSListRow(title: "Напарники",
                                  subtitle: SettingsFormat.plural(Int(s.partnersCount), "напарник", "напарника", "напарников"),
                                  icon: .passenger, showsDivider: false) { router.show(.partnersManage) }
                    }
                    DSList(header: "РАСЧЁТ") {
                        DSListRow(title: "Зарплата", subtitle: "Тарифная ставка и доплаты", icon: .ruble) {
                            router.showSettingSalary()
                        }
                        DSListRow(title: "Норма и регион", value: countryName, icon: .calendar) {
                            router.show(.settingsSection(.norma))
                        }
                        DSListRow(title: "Учёт", subtitle: "Ночные часы и будущие маршруты", icon: .moon) {
                            router.show(.settingsSection(.accounting))
                        }
                        DSListRow(title: "Отдых", subtitle: "Нормы домашнего отдыха и в ПО", icon: .bed,
                                  showsDivider: false) { router.show(.settingsSection(.rest)) }
                    }
                    DSList(header: "ПОЕЗДКИ") {
                        DSListRow(title: "Основные", subtitle: "Значения по умолчанию для новых маршрутов", icon: .gear) {
                            router.show(.settingsSection(.route))
                        }
                        DSListRow(title: "Маршрут", subtitle: "Поля формы маршрута", icon: .document) {
                            router.show(.settingsSection(.routeForm))
                        }
                        DSListRow(title: "Локомотив", subtitle: "Поля ввода показаний и вид тяги", icon: .locomotive) {
                            router.show(.settingsSection(.locomotive(series: nil)))
                        }
                        DSListRow(title: "Поезд", subtitle: "Расчёт длины состава", icon: .train,
                                  showsDivider: false) { router.show(.settingsSection(.train)) }
                    }
                    DSList(header: "О ПРИЛОЖЕНИИ") {
                        DSListRow(title: "Машинист", subtitle: nil, value: versionText, valueIsMono: true,
                                  icon: .info, showsChevron: false)
                        DSListRow(title: "Поддержка", value: "locodriver.app@yandex.ru", icon: .share) {
                            if let url = URL(string: "mailto:locodriver.app@yandex.ru") { UIApplication.shared.open(url) }
                        }
                        DSListRow(title: "Экспорт диагностики", subtitle: "Для поддержки", icon: .pdf,
                                  showsDivider: false) { confirmExport = true }
                    }
                } else {
                    ProgressView().frame(maxWidth: .infinity).padding(.top, 80)
                }
            }
            .padding(.horizontal, DSSpacing.lg)
            .padding(.bottom, DSSpacing.xxxl)
        }
        .background(DSColor.bg.ignoresSafeArea())
        .navigationTitle("Настройки")
        .navigationBarTitleDisplayMode(.large)
        .alert("Экспорт диагностики", isPresented: $confirmExport) {
            Button("Отправить") { shareItems = ShareItems(items: [makeDiagnosticFile()]) }
            Button("Отмена", role: .cancel) {}
        } message: {
            Text("В поддержку будут отправлены обезличенные диагностические данные без личной информации.")
        }
        .sheet(item: $shareItems) { item in SettingsShareSheet(items: item.items) }
    }

    /// Обезличенный файл: версия приложения и системы. Маршруты, заметки, email и токены не включаются (§17.6).
    private func makeDiagnosticFile() -> URL {
        let info = Bundle.main.infoDictionary
        let text = """
        LocoDriver iOS diagnostics
        version: \(info?["CFBundleShortVersionString"] as? String ?? "?")
        build: \(info?["CFBundleVersion"] as? String ?? "?")
        system: iOS \(UIDevice.current.systemVersion)
        device: \(UIDevice.current.model)
        """
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("locodriver-diagnostics.txt")
        try? text.write(to: url, atomically: true, encoding: .utf8)
        return url
    }
}
