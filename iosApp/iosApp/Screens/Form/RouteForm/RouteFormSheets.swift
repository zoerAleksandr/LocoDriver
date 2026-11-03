import SwiftUI
import ComposeApp

// Шторки формы маршрута. Макет: design/src/ios-screens.jsx → IOSCalcSheet, IOSRestSheet.
// Поведение: SCREEN_SPECS §5.4/§11.9 (расчёт), §5.7 (отдых), §5.5 (Passenger12h), §5.6 (shared preview).

// MARK: - Расчёт за смену

struct RouteCalcSheet: View {
    let salary: SalaryUi?
    let onSalarySettings: () -> Void
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Расчёт за смену", subtitle: salary?.subtitle), onClose: onClose) {
            if let salary = salary, salary.isCalculated {
                calculated(salary)
            } else {
                DSCard {
                    Text("Укажите начало и окончание рабочего времени для расчёта заработной платы за поездку")
                        .font(DSFont.sans(14))
                        .foregroundColor(DSColor.textMuted)
                        .padding(.horizontal, DSMetrics.rowPadX)
                        .padding(.vertical, 18)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) { pinnedSettings }
    }

    @ViewBuilder
    private func calculated(_ salary: SalaryUi) -> some View {
        // Итоговая сумма
        HStack(alignment: .bottom) {
            Text("Заработано")
                .font(DSFont.sans(13, .semibold))
                .foregroundColor(DSColor.accent)
            Spacer(minLength: 8)
            Text(salary.total)
                .font(DSFont.mono(30, .heavy))
                .tracking(-1)
                .foregroundColor(DSColor.accent)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, 18)
        .background(DSColor.accentSoft)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        .padding(.bottom, 12)

        // Командировка без среднего часа (§5.4)
        if salary.averageHourMissing {
            Button(action: onSalarySettings) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Маршрут в командировке")
                        .font(DSFont.sans(15, .semibold))
                        .foregroundColor(DSColor.businessTrip)
                    Text("Оплачивается только по среднему часу, без надбавок. Средний час не указан — поэтому сумма 0. Нажмите, чтобы задать его в настройках зарплаты.")
                        .font(DSFont.sans(14))
                        .foregroundColor(DSColor.text)
                        .multilineTextAlignment(.leading)
                }
                .padding(.horizontal, DSMetrics.rowPadX)
                .padding(.vertical, 16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(DSColor.businessTrip.opacity(0.14))
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
            }
            .buttonStyle(.plain)
            .padding(.bottom, 12)
        }

        let rows = (salary.rows as? [SalaryRowUi]) ?? []
        if !rows.isEmpty {
            DSCard {
                ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                    if index > 0 { DSDivider() }
                    HStack(spacing: 12) {
                        VStack(alignment: .leading, spacing: 1) {
                            Text(row.label).font(DSFont.sans(15, .semibold)).foregroundColor(DSColor.text)
                            Text(row.hint).font(DSFont.sans(12)).foregroundColor(DSColor.textMuted)
                        }
                        Spacer(minLength: 8)
                        Text(row.amount)
                            .font(DSFont.mono(16, .semibold))
                            .foregroundColor(DSColor.text)
                            .lineLimit(1)
                    }
                    .padding(.horizontal, DSMetrics.rowPadX)
                    .padding(.vertical, DSMetrics.rowPadY)
                }
            }
        }

        if salary.tariffMissing {
            Button(action: onSalarySettings) {
                Text("Установите значение тарифной ставки в настройках.")
                    .font(DSFont.sans(14).italic())
                    .underline()
                    .foregroundColor(DSColor.accent)
                    .multilineTextAlignment(.leading)
            }
            .buttonStyle(.plain)
            .padding(.horizontal, 4)
            .padding(.vertical, 10)
        }

        HStack(spacing: 8) {
            DSIcon.info.image.font(.system(size: 14))
            Text("Суммы рассчитаны по вашим тарифам и нормам.").font(DSFont.sans(13))
        }
        .foregroundColor(DSColor.textMuted)
        .padding(.horizontal, 6)
        .padding(.top, 14)
        .padding(.bottom, 6)
    }

    /// Кнопка настроек закреплена в самом низу шторки.
    private var pinnedSettings: some View {
        RouteSheetSettingsRow(title: "Настройки зарплаты", action: onSalarySettings)
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.top, 8)
            .padding(.bottom, 12)
            .background(DSColor.bg)
    }
}

/// Строка-кнопка «⚙ … ›» внизу шторок (Расчёт, Отдых).
struct RouteSheetSettingsRow: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                DSIcon.gear.image.font(.system(size: 18)).foregroundColor(DSColor.textMuted)
                Text(title).font(DSFont.sans(15, .medium)).foregroundColor(DSColor.text)
                Spacer(minLength: 0)
                DSIcon.chevronRight.image.font(.system(size: 12, weight: .semibold)).foregroundColor(DSColor.textFaint)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(DSColor.border, lineWidth: 1))
            .dsShadow(.sm)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Отдых

struct RouteRestSheet: View {
    let rest: RestUi?
    let isPointOfTurnover: Bool
    let onChange: (Bool) -> Void
    let onRestSettings: () -> Void
    let onClose: () -> Void

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Отдых", subtitle: "Тип и длительность отдыха"), onClose: onClose) {
            segmented
                .padding(.bottom, 14)

            if let rest = rest {
                if let message = rest.unavailableMessage {
                    DSCard {
                        Text(message)
                            .font(DSFont.sans(14))
                            .foregroundColor(DSColor.textMuted)
                            .padding(.horizontal, DSMetrics.rowPadX)
                            .padding(.vertical, 18)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                } else {
                    if rest.isSecondTurnaround {
                        DSWarningBlock(
                            title: "Второй отдых в ПО подряд",
                            message: "Предыдущий маршрут тоже завершился отдыхом в пункте оборота. Нормативы ниже посчитаны от минимума для второго отдыха.",
                            icon: .bed
                        )
                        .padding(.bottom, 10)
                    }
                    let items = (rest.items as? [RestItemUi]) ?? []
                    DSCard {
                        ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                            if index > 0 { DSDivider() }
                            restItem(item)
                        }
                    }
                    if let footnote = rest.footnote {
                        HStack(spacing: 8) {
                            DSIcon.info.image.font(.system(size: 14))
                            Text(footnote).font(DSFont.sans(13))
                        }
                        .foregroundColor(DSColor.textMuted)
                        .padding(.horizontal, 6)
                        .padding(.top, 12)
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            // Кнопка настроек закреплена в самом низу шторки.
            RouteSheetSettingsRow(title: "Настройки времени отдыха", action: onRestSettings)
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.top, 8)
                .padding(.bottom, 12)
                .background(DSColor.bg)
        }
    }

    private var segmented: some View {
        HStack(spacing: 4) {
            segment("Домашний", active: !isPointOfTurnover) { onChange(false) }
            segment("В пункте оборота", active: isPointOfTurnover) { onChange(true) }
        }
        .padding(4)
        .background(DSColor.bgSubtle)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func segment(_ title: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(DSFont.sans(14, .semibold))
                .foregroundColor(active ? DSColor.text : DSColor.textMuted)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 9)
                .background {
                    if active {
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .fill(DSColor.surface)
                            .dsShadow(.sm)
                    }
                }
        }
        .buttonStyle(.plain)
    }

    private func restItem(_ item: RestItemUi) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            HStack(spacing: 0) {
                Text(item.title).font(DSFont.sans(14)).foregroundColor(DSColor.textMuted)
                Text(" · \(item.duration)").font(DSFont.mono(14)).foregroundColor(DSColor.textMuted)
            }
            Text("до \(item.until)")
                .font(DSFont.mono(22, .bold))
                .tracking(-0.3)
                .foregroundColor(DSColor.text)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - «Вторая ночь подряд»

struct RouteNightWarnView: View {
    let rows: [NightWarnRowUi]
    let timeFormat: RouteTimeFormat
    let onClose: () -> Void

    var body: some View {
        DSWarningBlock(
            title: "Вторая ночь подряд",
            message: "Третья ночь подряд не допускается.",
            icon: .moon,
            onClose: onClose,
            content: { timeline }
        )
    }

    /// Ось — от начала самой ранней строки до конца самой поздней (в поясе отображения).
    private var timeline: some View {
        let minStart = rows.map { $0.start }.min() ?? 0
        let maxEnd = rows.map { $0.end }.max() ?? 1
        let span = Double(max(maxEnd - minStart, 1))
        return VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                VStack(alignment: .leading, spacing: 4) {
                    HStack(alignment: .firstTextBaseline) {
                        Text(row.label).font(DSFont.sans(11.5, .semibold)).foregroundColor(DSColor.text)
                        Spacer(minLength: 6)
                        Text(row.nightText).font(DSFont.mono(11, .bold)).foregroundColor(DSColor.accent)
                    }
                    Text(row.rangeText).font(DSFont.mono(10)).foregroundColor(DSColor.textMuted)
                    GeometryReader { geo in
                        let w = geo.size.width
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 5)
                                .fill(DSColor.borderStrong)
                                .frame(width: w * CGFloat(Double(row.end - row.start) / span), height: 14)
                                .offset(x: w * CGFloat(Double(row.start - minStart) / span))
                            ForEach(Array(((row.nightIntervals as? [NightIntervalUi]) ?? []).enumerated()), id: \.offset) { _, iv in
                                Rectangle()
                                    .fill(DSColor.accent)
                                    .frame(width: w * CGFloat(Double(iv.end - iv.start) / span), height: 14)
                                    .offset(x: w * CGFloat(Double(iv.start - minStart) / span))
                            }
                        }
                    }
                    .frame(height: 14)
                }
            }
            HStack(spacing: 14) {
                legend(color: DSColor.borderStrong, text: "Маршрут")
                legend(color: DSColor.accent, text: "Ночь · 00:00–05:00")
            }
            .padding(.top, 4)
        }
        .padding(.top, 14)
    }

    private func legend(color: Color, text: String) -> some View {
        HStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 2).fill(color).frame(width: 14, height: 8)
            Text(text).font(DSFont.sans(11)).foregroundColor(DSColor.textMuted)
        }
    }
}

// MARK: - Shared preview (§5.6)

struct RouteSharedPreviewSheet: View {
    let onView: () -> Void
    let onSave: () -> Void
    let onCancel: () -> Void

    var body: some View {
        DSBottomSheet(header: .none, onClose: onView) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Получен новый маршрут").dsTextStyle(.sectionH2)
                Text("Маршрут отправлен вам по ссылке. Можно сначала посмотреть его или сразу сохранить в свой список.")
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.textMuted)
            }
            .padding(.top, 18)
            .padding(.horizontal, 4)
            VStack(spacing: 10) {
                DSCTAButton(title: "Сохранить", action: onSave)
                DSTonalButton(title: "Просмотр", action: onView)
                DSTextAction(title: "Отмена", action: onCancel)
            }
            .padding(.top, 20)
        }
    }
}

// MARK: - Passenger12h (§5.5)

struct RoutePassenger12hSheet: View {
    let suggest: (String) -> [String]
    let initialDeparture: Date
    let initialArrival: Date
    let initialStation: String
    let workStart: Date?
    let workEnd: Date?
    let timeZone: TimeZone
    let onSettings: () -> Void
    let onNo: () -> Void
    let onSave: (String?, String?, Int64, Int64) -> Void

    @State private var step = 1
    @State private var stationDeparture: String
    @State private var stationArrival = ""
    @State private var departure: Date
    @State private var arrival: Date
    @FocusState private var focus: Field?

    private enum Field { case departure, arrival }

    init(suggest: @escaping (String) -> [String], initialDeparture: Date, initialArrival: Date, initialStation: String,
         workStart: Date?, workEnd: Date?, timeZone: TimeZone,
         onSettings: @escaping () -> Void, onNo: @escaping () -> Void,
         onSave: @escaping (String?, String?, Int64, Int64) -> Void) {
        self.suggest = suggest
        self.initialDeparture = initialDeparture
        self.initialArrival = initialArrival
        self.initialStation = initialStation
        self.workStart = workStart
        self.workEnd = workEnd
        self.timeZone = timeZone
        self.onSettings = onSettings
        self.onNo = onNo
        self.onSave = onSave
        _stationDeparture = State(initialValue: initialStation)
        _departure = State(initialValue: initialDeparture)
        _arrival = State(initialValue: initialArrival)
    }

    /// «Время следования должно быть в пределах рабочего времени» (§5.5).
    private var hasError: Bool {
        if let s = workStart, departure < s { return true }
        if let e = workEnd, arrival > e { return true }
        return departure >= arrival
    }

    var body: some View {
        DSBottomSheet(header: .none, onClose: onNo) {
            if step == 1 { stepOne } else { stepTwo }
        }
        .interactiveDismissDisabled(false)
    }

    private var stepOne: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Время работы превышает 12 часов").dsTextStyle(.sectionH2).padding(.top, 18)
            Text("Записать время свыше 12 часов как следование пассажиром?")
                .font(DSFont.sans(15)).foregroundColor(DSColor.text)
            Button(action: onSettings) {
                Text("Изменить в настройках")
                    .font(DSFont.sans(14, .medium)).foregroundColor(DSColor.accent)
            }
            .buttonStyle(.plain)
            HStack(spacing: 10) {
                DSTonalButton(title: "Нет", action: onNo)
                DSCTAButton(title: "Да") { step = 2 }
            }
            .padding(.top, 8)
        }
        .padding(.horizontal, 4)
    }

    private var stepTwo: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Пассажиром").dsTextStyle(.sectionH2).padding(.top, 18)
            stationField(title: "Станция отправления", text: $stationDeparture)
            stationField(title: "Станция прибытия", text: $stationArrival)
            timeRow(title: "Время отправления", pickerTitle: "Отправление пассажиром", date: $departure)
            timeRow(title: "Время прибытия", pickerTitle: "Прибытие пассажиром", date: $arrival)
            if hasError {
                Text("Время следования должно быть в пределах рабочего времени")
                    .font(DSFont.sans(13)).foregroundColor(DSColor.danger)
            }
            DSCTAButton(title: "Сохранить", isEnabled: !hasError) {
                let dep = TimeFormatter.dateToMs(departure)
                let arr = TimeFormatter.dateToMs(arrival)
                onSave(stationDeparture, stationArrival, dep, arr)
            }
            .padding(.top, 4)
        }
        .padding(.horizontal, 4)
    }

    private func stationField(title: String, text: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).dsTextStyle(.label)
            TextField("Станция", text: text)
                .font(DSFont.sans(16))
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
                .background(DSColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.sm, style: .continuous))
            let hints = suggest(text.wrappedValue).filter { $0 != text.wrappedValue }
            if !hints.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(hints, id: \.self) { name in
                            DSChip(title: name) { text.wrappedValue = name }
                        }
                    }
                }
            }
        }
    }

    private func timeRow(title: String, pickerTitle: String, date: Binding<Date>) -> some View {
        HStack {
            Text(title).dsTextStyle(.label)
            Spacer()
            DatePicker(pickerTitle, selection: date, displayedComponents: [.date, .hourAndMinute])
                .labelsHidden()
                .environment(\.timeZone, timeZone)
                .environment(\.locale, Locale(identifier: "ru_RU"))
        }
    }
}
