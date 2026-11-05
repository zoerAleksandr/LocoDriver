import SwiftUI
import ComposeApp

/// Обёртка Int для `.dsSheet(item:)` (Identifiable).
struct NormSheetItem: Identifiable {
    let id: Int
}

// «Норма по месяцам» / «Своя настройка» (SCREEN_SPECS §16.3, §16.3.1).
// Открывается кнопкой «Изменить норму» на «Норма и регион» (§17.2).
// Логика профиля недели и customDays — NormMonthIosViewModel (Kotlin).

struct NormMonthView: View {
    @StateObject private var vm = NormMonthViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared

    @State private var snackQueue: [DSSnackbarItem] = []
    @State private var daySheetDay: NormSheetItem? = nil
    @State private var weekdaySheetIndex: NormSheetItem? = nil
    @State private var showTour = false
    @State private var tourStep = 0

    var body: some View {
        let ui = vm.ui
        ScrollView {
            VStack(alignment: .leading, spacing: DSSpacing.md) {
                monthHeader(ui)
                normaCard(ui)
                weekSection(ui)
                weekdayRow
                grid(ui)
                normaCard(ui)
                Text("Месячная норма рабочего времени. По ней считается переработка и недоработка.")
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.bottom, 40)
        }
        .background(DSColor.bg.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .overlay(alignment: .top) {
            HStack {
                DSNavBar(title: "Норма по месяцам", style: .text(backLabel: "‹ Норма/Регион"), onBack: {
                    router.back()
                })
                Spacer()
                Button {
                    tourStep = 0
                    showTour = true
                } label: {
                    Text("?")
                        .font(DSFont.sans(16, .bold))
                        .foregroundColor(DSColor.accent)
                        .frame(width: 40, height: 40)
                        .background(DSColor.surface)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(DSColor.border, lineWidth: 1))
                }
                .padding(.trailing, DSMetrics.screenPadX)
                .padding(.top, 8)
            }
        }
        .onAppear { vm.prepareScreen() }
        .onChange(of: ui?.message) { text in
            guard let text = text, !text.isEmpty else { return }
            snackQueue.append(DSSnackbarItem(text: text))
            vm.consumeMessage()
        }
        .dsSnackbarQueue(snackQueue, bottomPadding: 16) {
            if !snackQueue.isEmpty { snackQueue.removeFirst() }
        }
        .dsSheet(item: $daySheetDay, detents: [.height(420)]) { item in
            NormDaySheet(day: item.id, ui: ui, vm: vm)
        }
        .dsSheet(item: $weekdaySheetIndex, detents: [.height(360)]) { item in
            NormWeekdaySheet(index: item.id, ui: ui, vm: vm)
        }
        .overlay {
            if showTour {
                NormTourOverlay(step: tourStep, isLast: tourStep >= 4) {
                    if tourStep >= 4 {
                        showTour = false
                    } else {
                        tourStep += 1
                    }
                } onClose: {
                    showTour = false
                }
            }
        }
    }

    // MARK: - Шапка месяца (§16.3.1)

    private func monthHeader(_ ui: NormMonthIosUi?) -> some View {
        HStack {
            Button { vm.shiftMonth(-1) } label: {
                Image(systemName: "chevron.left").foregroundColor(DSColor.text)
            }
            Text("\(ui?.monthLabel ?? "")")
                .font(DSFont.sans(17, .bold))
                .foregroundColor(DSColor.text)
            Button { vm.shiftMonth(1) } label: {
                Image(systemName: "chevron.right").foregroundColor(DSColor.text)
            }
            Spacer()
        }
        .padding(.top, 56)
    }

    private func normaCard(_ ui: NormMonthIosUi?) -> some View {
        HStack {
            Text("Норма за месяц")
                .font(DSFont.sans(15, .medium))
                .foregroundColor(DSColor.text)
            Spacer()
            Text(ui?.normaMonthText ?? "0:00")
                .dsTextStyle(.metricLg)
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, DSMetrics.rowPadY)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    // MARK: - Рабочая неделя (§16.3)

    private func weekSection(_ ui: NormMonthIosUi?) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            DSGroupHeader("РАБОЧАЯ НЕДЕЛЯ")
            DSCard {
                VStack(spacing: 0) {
                    weekRow(title: ui?.standardTitle ?? "Стандартная · 8 ч, Пн–Пт",
                            selected: ui?.weekMode == "standard", emphasized: ui?.weekMode == "standard") {
                        vm.setWeekMode("standard")
                    }
                    DSDivider(inset: 18)
                    weekRow(title: ui?.sixDayTitle ?? "Шестидневная · 7 ч + Сб 5 ч",
                            selected: ui?.weekMode == "sixDay", emphasized: ui?.weekMode == "sixDay") {
                        vm.setWeekMode("sixDay")
                    }
                    DSDivider(inset: 18)
                    weekRow(title: ui?.customTitle ?? "По неделям",
                            selected: ui?.weekMode == "custom",
                            emphasized: ui?.weekMode == "custom") {
                        vm.toggleWeekTemplate()
                    }
                    if ui?.isWeekTemplateExpanded == true {
                        weekTemplate(ui)
                    }
                }
            }
        }
    }

    private func weekRow(title: String, selected: Bool, emphasized: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Text(emphasized ? "✓ " : "")
                    .font(DSFont.sans(15, .bold))
                    .foregroundColor(DSColor.accent)
                Text(title)
                    .font(DSFont.sans(15, emphasized ? .bold : .regular))
                    .foregroundColor(emphasized ? DSColor.accent : DSColor.text)
                Spacer()
            }
            .padding(.vertical, 14)
            .padding(.horizontal, DSMetrics.rowPadX)
            .contentShape(Rectangle())
        }
        .buttonStyle(DSRowPressStyle())
    }

    private func weekTemplate(_ ui: NormMonthIosUi?) -> some View {
        let template = (ui?.weekTemplate as? [NormMonthIosWeekday]) ?? []
        return VStack(spacing: 10) {
            HStack(spacing: 6) {
                ForEach(template, id: \.index) { slot in
                    Button {
                        weekdaySheetIndex = NormSheetItem(id: Int(slot.index))
                    } label: {
                        VStack(spacing: 2) {
                            Text(slot.label)
                                .font(DSFont.mono(9, .semibold))
                                .foregroundColor(DSColor.textMuted)
                            Text(slot.hoursText)
                                .font(DSFont.mono(11, .bold))
                                .foregroundColor(slot.isDayOff ? DSColor.textMuted : DSColor.text)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(slot.isDayOff ? DSColor.surfaceAlt : DSColor.chipBg)
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                    .buttonStyle(DSPressableStyle())
                }
            }
            DSTonalButton(title: "Применить к месяцу", tone: .accent) {
                vm.applyTemplateToMonth()
            }
        }
        .padding(12)
    }

    private var weekdayRow: some View {
        HStack(spacing: 0) {
            ForEach(0..<7, id: \.self) { i in
                let labels = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]
                Text(labels[i])
                    .font(DSFont.mono(11, .semibold))
                    .foregroundColor(i >= 5 ? DSColor.accent : DSColor.textMuted)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    // MARK: - Сетка нормы

    private func grid(_ ui: NormMonthIosUi?) -> some View {
        let days = (ui?.days as? [NormMonthIosDay]) ?? []
        let leading = days.first.map { Int($0.weekdayIndex) } ?? 0
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
            ForEach(0..<leading, id: \.self) { _ in Color.clear.frame(height: 64) }
            ForEach(days, id: \.day) { day in
                Button {
                    daySheetDay = NormSheetItem(id: Int(day.day))
                } label: {
                    VStack(spacing: 1) {
                        Text("\(day.day)")
                            .font(DSFont.mono(11, .medium))
                            .foregroundColor(dayNumberColor(day))
                        if let label = day.label {
                            Text(label)
                                .font(DSFont.mono(9, .bold))
                                .foregroundColor(day.labelIsHoliday ? DSColor.danger : DSColor.textMuted)
                        } else {
                            Text(day.normText)
                                .font(DSFont.mono(18, .semibold))
                                .foregroundColor(DSColor.text)
                        }
                        Text(day.cumulativeText)
                            .font(DSFont.mono(10))
                            .foregroundColor(DSColor.textFaint)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 64)
                    .background(DSColor.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .stroke(DSColor.border, lineWidth: 1)
                    )
                }
                .buttonStyle(DSPressableStyle())
            }
        }
    }

    private func dayNumberColor(_ day: NormMonthIosDay) -> Color {
        if day.isHoliday { return DSColor.danger }
        if day.isWeekend { return DSColor.textFaint }
        return DSColor.text
    }
}

// MARK: - Шторка дня (§16.3.1)

private struct NormDaySheet: View {
    let day: Int
    let ui: NormMonthIosUi?
    let vm: NormMonthViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    private var model: NormMonthIosDay? {
        (ui?.days as? [NormMonthIosDay])?.first { Int($0.day) == day }
    }

    @State private var hoursText: String = ""

    var body: some View {
        DSBottomSheet(
            header: .cancelDone(
                title: dayTitle,
                cancel: "",
                done: "Готово",
                onDone: { dismiss() }
            ),
            showsGrabber: false,
            onClose: { dismiss() }
        ) {
            VStack(alignment: .leading, spacing: 14) {
                if let m = model, !m.isEditable {
                    if m.isHoliday {
                        Text(m.holidayName ?? "Праздничный день")
                            .font(DSFont.sans(15, .semibold))
                            .foregroundColor(DSColor.danger)
                    }
                    if let title = m.absenceTitle {
                        Text("\(title), \(m.absencePeriodText ?? "")")
                            .font(DSFont.sans(15, .semibold))
                            .foregroundColor(DSColor.text)
                    }
                    Text("Норма и ВЫХ недоступны — меняются в Календаре.")
                        .font(DSFont.sans(12))
                        .foregroundColor(DSColor.textMuted)
                } else {
                    counterBlock
                    HStack {
                        Text("С начала месяца: \(model?.cumulativeText.isEmpty == false ? (model?.cumulativeText ?? "—") + " ч" : "—")")
                            .font(DSFont.sans(13))
                            .foregroundColor(DSColor.textMuted)
                        Spacer()
                    }
                    Toggle(isOn: Binding(
                        get: { model?.isDayOff ?? false },
                        set: { _ in vm.toggleDayOff(day); dismiss() }
                    )) {
                        Text("Выходной (ВЫХ)")
                            .font(DSFont.sans(15))
                            .foregroundColor(DSColor.text)
                    }
                    .tint(DSColor.accent)

                    if model?.isCustom == true {
                        DSTonalButton(title: "Вернуть по умолчанию (\(model?.defaultHoursText ?? "8 ч"))", tone: .neutral) {
                            vm.resetDayHours(day)
                            dismiss()
                        }
                    }
                }
            }
            .padding(.top, 8)
        }
        .onAppear {
            hoursText = model?.normText.isEmpty == false ? (model?.normText ?? "0") : "0"
        }
    }

    private var dayTitle: String {
        let weekdays = ["понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"]
        let wd = weekdays[Int(model?.weekdayIndex ?? 0) % 7]
        return "\(day) · \(wd)"
    }

    private var counterBlock: some View {
        HStack(spacing: 14) {
            Button {
                let current = Int(hoursText) ?? 0
                hoursText = String(max(0, current - 1))
                vm.setDayHours(day, hours: max(0, current - 1))
            } label: {
                Text("−")
                    .font(DSFont.sans(20, .bold))
                    .foregroundColor(DSColor.text)
                    .frame(width: 44, height: 44)
                    .background(DSColor.bgSubtle)
                    .clipShape(Circle())
            }
            .buttonStyle(DSPressableStyle())

            TextField("0", text: Binding(
                get: { hoursText },
                set: { newValue in
                    let filtered = String(newValue.filter { $0.isNumber }.prefix(2))
                    let value = min(Int(filtered) ?? 0, 12)
                    hoursText = String(value)
                    vm.setDayHours(day, hours: value)
                }
            ))
            .keyboardType(.numberPad)
            .font(DSFont.mono(24, .semibold))
            .multilineTextAlignment(.center)
            .frame(height: 60)
            .frame(maxWidth: 120)
            .background(DSColor.surfaceAlt)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            Button {
                let current = Int(hoursText) ?? 0
                let value = min(12, current + 1)
                hoursText = String(value)
                vm.setDayHours(day, hours: value)
            } label: {
                Text("+")
                    .font(DSFont.sans(20, .bold))
                    .foregroundColor(DSColor.text)
                    .frame(width: 44, height: 44)
                    .background(DSColor.bgSubtle)
                    .clipShape(Circle())
            }
            .buttonStyle(DSPressableStyle())
            Spacer()
        }
    }
}

// MARK: - Шторка дня недели шаблона (§16.3.1)

private struct NormWeekdaySheet: View {
    let index: Int
    let ui: NormMonthIosUi?
    let vm: NormMonthViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    private var slot: NormMonthIosWeekday? {
        (ui?.weekTemplate as? [NormMonthIosWeekday])?.first { Int($0.index) == index }
    }

    @State private var hours: Int = 8

    var body: some View {
        DSBottomSheet(
            header: .cancelDone(
                title: slot?.label ?? "День",
                cancel: "",
                done: "Готово",
                onDone: { dismiss() }
            ),
            showsGrabber: false,
            onClose: { dismiss() }
        ) {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 14) {
                    Button {
                        hours = max(0, hours - 1)
                        vm.setWeekdayHours(index, hours: hours)
                    } label: {
                        Text("−")
                            .font(DSFont.sans(20, .bold))
                            .foregroundColor(DSColor.text)
                            .frame(width: 44, height: 44)
                            .background(DSColor.bgSubtle)
                            .clipShape(Circle())
                    }
                    .buttonStyle(DSPressableStyle())
                    Text("\(hours)")
                        .font(DSFont.mono(24, .semibold))
                        .frame(maxWidth: 120)
                    Button {
                        hours = min(12, hours + 1)
                        vm.setWeekdayHours(index, hours: hours)
                    } label: {
                        Text("+")
                            .font(DSFont.sans(20, .bold))
                            .foregroundColor(DSColor.text)
                            .frame(width: 44, height: 44)
                            .background(DSColor.bgSubtle)
                            .clipShape(Circle())
                    }
                    .buttonStyle(DSPressableStyle())
                    Spacer()
                }
                Toggle(isOn: Binding(
                    get: { slot?.isDayOff ?? false },
                    set: { value in
                        vm.setWeekdayDayOff(index, isDayOff: value)
                    }
                )) {
                    Text("Выходной (ВЫХ)")
                        .font(DSFont.sans(15))
                        .foregroundColor(DSColor.text)
                }
                .tint(DSColor.accent)
                Text("Шаблон действует до выхода с экрана и в профиль не пишется.")
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
            }
            .padding(.top, 8)
        }
        .onAppear { hours = Int(slot?.hours ?? 8) }
    }
}

// MARK: - Экскурсия (§16.3.1, упрощённая)

private struct NormTourOverlay: View {
    let step: Int
    let isLast: Bool
    let onNext: () -> Void
    let onClose: () -> Void

    private var title: String {
        switch step {
        case 0: return "Месяц"
        case 1: return "День"
        case 2: return "Норма за месяц"
        case 3: return "По неделям"
        default: return "Подсказки"
        }
    }

    private var text: String {
        switch step {
        case 0: return "Стрелками переключайте месяц. Листаются только месяцы производственного календаря."
        case 1: return "Тап по дню — правка нормы и «Выходной» (ВЫХ)."
        case 2: return "Норма за месяц считается так же, как в расчёте зарплаты."
        case 3: return "«По неделям» — шаблон рабочей недели и «Применить к месяцу»."
        default: return "Кнопка «?» открывает эту экскурсию снова."
        }
    }

    var body: some View {
        ZStack {
            DSColor.scrim.ignoresSafeArea()
            VStack(spacing: 14) {
                Text(title)
                    .dsTextStyle(.sectionH2)
                Text(text)
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.textMuted)
                    .multilineTextAlignment(.center)
                DSCTAButton(title: isLast ? "Понятно" : "Далее", action: onNext)
                DSTextAction(title: "Закрыть", action: onClose)
            }
            .padding(24)
            .background(DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .padding(.horizontal, 32)
        }
    }
}
