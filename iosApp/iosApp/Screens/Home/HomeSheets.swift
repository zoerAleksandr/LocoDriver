import SwiftUI
import ComposeApp

private let homeMonthNames = [
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
]

// MARK: - Шторка месяца (§4.4 п.2)

/// «Выберите месяц и год»: чипы месяцев и лет из доступных, «Применить».
/// Выбор чипа сам по себе ничего не применяет.
struct HomeMonthSheet: View {
    let months: [Int]
    let years: [Int]
    let onApply: (_ year: Int, _ month: Int) -> Void
    let onClose: () -> Void

    @State private var month: Int
    @State private var year: Int

    init(
        months: [Int],
        years: [Int],
        selectedMonth: Int,
        selectedYear: Int,
        onApply: @escaping (_ year: Int, _ month: Int) -> Void,
        onClose: @escaping () -> Void
    ) {
        self.months = months
        self.years = years
        self.onApply = onApply
        self.onClose = onClose
        _month = State(initialValue: selectedMonth)
        _year = State(initialValue: selectedYear)
    }

    private let columns = [GridItem(.adaptive(minimum: 104), spacing: DSSpacing.sm)]

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Выберите месяц и год", subtitle: nil), onClose: onClose) {
            DSGroupHeader("Месяц", topSpacing: DSMetrics.groupHeadFirst)
            LazyVGrid(columns: columns, alignment: .leading, spacing: DSSpacing.sm) {
                ForEach(months, id: \.self) { m in
                    DSChip(
                        title: homeMonthNames.indices.contains(m) ? homeMonthNames[m] : "\(m + 1)",
                        style: .choice(isSelected: m == month)
                    ) { month = m }
                }
            }
            DSGroupHeader("Год")
            LazyVGrid(columns: columns, alignment: .leading, spacing: DSSpacing.sm) {
                ForEach(years, id: \.self) { y in
                    DSChip(title: String(y), style: .choice(isSelected: y == year)) { year = y }
                }
            }
            DSCTAButton(title: "Применить") { onApply(year, month) }
                .padding(.top, DSSpacing.xxl)
        }
    }
}

// MARK: - «Перейти на сайт?» (§4.4 п.1)

struct HomeSiteSheet: View {
    let onGo: () -> Void
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Перейти на сайт?", subtitle: nil), onClose: onClose) {
            Text("Будет выполнен переход на официальный сайт приложения locodriver.ru")
                .font(DSFont.sans(15))
                .foregroundColor(DSColor.textMuted)
                .fixedSize(horizontal: false, vertical: true)
            DSCTAButton(title: "Перейти", action: onGo)
                .padding(.top, DSSpacing.xl)
        }
    }
}

// MARK: - Удаление маршрута (§4.5)

struct HomeDeleteRouteSheet: View {
    let subtitle: String
    let onConfirm: () -> Void
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(
            header: .titleClose(title: "Удалить маршрут?", subtitle: subtitle.isEmpty ? nil : subtitle),
            onClose: onClose
        ) {
            DSDangerButton(title: "Да, удалить", action: onConfirm)
                .padding(.top, DSSpacing.lg)
        }
    }
}

// MARK: - Список единиц текущего маршрута (§4.3)

/// «Локомотивы · N» / «Поезда · N» / «Пассажиром · N» и пунктирная «+ Добавить …».
/// Макет: `IOSUnitsSheet`.
struct HomeUnitsSheet: View {
    let tile: HomeIosUnitTile
    let onSelect: (String) -> Void
    let onAdd: () -> Void
    let onClose: () -> Void

    private var title: String {
        switch tile.type {
        case "loco": return "Локомотивы · \(tile.count)"
        case "train": return "Поезда · \(tile.count)"
        default: return "Пассажиром · \(tile.count)"
        }
    }

    private var addTitle: String {
        switch tile.type {
        case "loco": return "Добавить локомотив"
        case "train": return "Добавить поезд"
        default: return "Добавить пассажиром"
        }
    }

    private var icon: DSIcon {
        switch tile.type {
        case "loco": return .locomotive
        case "train": return .train
        default: return .passenger
        }
    }

    var body: some View {
        DSBottomSheet(header: .titleClose(title: title, subtitle: nil), onClose: onClose) {
            DSCard {
                ForEach(Array(tile.items.enumerated()), id: \.element.id) { index, item in
                    Button { onSelect(item.id) } label: {
                        HStack(spacing: 14) {
                            icon.image
                                .font(.system(size: 20, weight: .regular))
                                .foregroundColor(DSColor.accent)
                                .frame(width: 44, height: 44)
                                .background(DSColor.bgSubtle)
                                .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
                            Text(item.name)
                                .font(DSFont.mono(16, .bold))
                                .foregroundColor(DSColor.text)
                                .lineLimit(2)
                            Spacer(minLength: DSSpacing.sm)
                            DSIcon.chevronRight.image
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(DSColor.textFaint)
                        }
                        .padding(.horizontal, 18)
                        .padding(.vertical, 14)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(DSRowPressStyle())
                    if index < tile.items.count - 1 { DSDivider(inset: 74) }
                }
            }
            Button(action: onAdd) {
                HStack(spacing: DSSpacing.sm) {
                    DSIcon.plus.image.font(.system(size: 18, weight: .semibold))
                    Text(addTitle).font(DSFont.sans(15, .semibold))
                }
                .foregroundColor(DSColor.accent)
                .frame(maxWidth: .infinity)
                .padding(14)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .strokeBorder(DSColor.borderStrong, style: StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
                )
                .contentShape(Rectangle())
            }
            .buttonStyle(DSPressableStyle())
            .padding(.top, DSSpacing.xl)
        }
    }
}

// MARK: - Выгрузка данных (§4.6, `SyncProgressDialog` в режиме процентов)

struct HomeSyncProgressSheet: View {
    let dialog: HomeIosSyncDialog
    let onSendReport: () -> Void
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(header: .none, showsGrabber: true, onClose: onClose) {
            if dialog.isNetworkError {
                stateView(
                    systemImage: "wifi.slash",
                    title: "Нет интернета",
                    message: nil
                )
            } else if dialog.isSessionExpired {
                stateView(
                    systemImage: "person.crop.circle.badge.exclamationmark",
                    title: "Сессия истекла",
                    message: "Войдите в аккаунт заново в разделе «Профиль». Данные на устройстве сохранены и будут синхронизированы после входа."
                )
            } else {
                progressView
            }
        }
    }

    private var progressView: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Выгрузка данных")
                .font(DSFont.sans(20, .bold))
                .foregroundColor(DSColor.text)
                .frame(maxWidth: .infinity)
                .padding(.top, DSSpacing.md)
            Text(dialog.stepTitle)
                .font(DSFont.sans(15))
                .foregroundColor(DSColor.textMuted)
                .padding(.top, DSSpacing.lg)
            HomeProgressBar(progress: Double(dialog.progress), height: 4, fill: DSColor.accent, track: DSColor.bgSubtle)
                .padding(.top, 10)
            Text(dialog.percentText)
                .font(DSFont.mono(14, .semibold))
                .foregroundColor(DSColor.text)
                .padding(.top, DSSpacing.sm)
            if let error = dialog.errorText {
                Text(error)
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.danger)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.top, DSSpacing.sm)
            }
            if dialog.isComplete {
                if dialog.canSendReport {
                    DSTonalButton(title: "Отправить отчет об ошибке", tone: .accent, action: onSendReport)
                        .padding(.top, DSSpacing.xl)
                }
                DSCTAButton(title: "Понятно", action: onClose)
                    .padding(.top, dialog.canSendReport ? DSSpacing.md : DSSpacing.xl)
            }
        }
    }

    private func stateView(systemImage: String, title: String, message: String?) -> some View {
        VStack(spacing: DSSpacing.md) {
            Image(systemName: systemImage)
                .font(.system(size: 40, weight: .regular))
                .foregroundColor(DSColor.textMuted)
                .padding(.top, DSSpacing.lg)
            Text(title)
                .font(DSFont.sans(19, .bold))
                .foregroundColor(DSColor.text)
            if let message = message {
                Text(message)
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.textMuted)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            DSCTAButton(title: "Понятно", action: onClose)
                .padding(.top, DSSpacing.md)
        }
        .frame(maxWidth: .infinity)
    }
}
