import SwiftUI
import ComposeApp

// Мастер «Заполнить месяц» (SCREEN_SPECS §15). Макет: schedule-wizard.jsx
// (ScheduleWizardScreen platform=ios), schedule-wizard-step1/step2.jsx.
// Логика — ScheduleWizardIosViewModel (Kotlin).

struct ScheduleWizardView: View {
    @StateObject private var vm = ScheduleWizardViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared

    @State private var snackQueue: [DSSnackbarItem] = []
    @State private var deletePatternRequest: DSDeleteRequest? = nil
    @State private var timePickerTarget: TimeTarget? = nil

    enum TimeTarget: String, Identifiable {
        case dayStart, dayEnd, nightStart, nightEnd
        var id: String { rawValue }
    }

    var body: some View {
        let ui = vm.ui
        ScrollView {
            VStack(alignment: .leading, spacing: DSSpacing.md) {
                stepIndicator(ui)
                if ui?.step == 1 {
                    step1(ui)
                } else {
                    step2(ui)
                }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.bottom, 120)
        }
        .background(DSColor.bg.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) { bottomBar(ui) }
        .navigationBarBackButtonHidden(true)
        .overlay(alignment: .top) {
            DSNavBar(title: "Заполнить месяц", style: .text(backLabel: backLabel(ui)), onBack: {
                if ui?.step == 2 { vm.goToStep(1) } else { router.back() }
            })
        }
        .onAppear { vm.prepareScreen() }
        .onChange(of: ui?.applyMessage) { text in
            guard let text = text, !text.isEmpty else { return }
            snackQueue.append(DSSnackbarItem(text: text))
            vm.consumeApplyMessage()
        }
        .dsSnackbarQueue(snackQueue, bottomPadding: 16) {
            if !snackQueue.isEmpty { snackQueue.removeFirst() }
        }
        .dsDeleteAlert($deletePatternRequest)
        .alert("Продолжить график прошлого месяца?", isPresented: Binding(
            get: { vm.ui?.showContinuePreviousSheet == true },
            set: { if !$0 { vm.declineContinuePrevious() } }
        )) {
            Button("Продолжить") { vm.continuePreviousSchedule() }
            Button("Выбрать заново") { vm.declineContinuePrevious() }
        } message: {
            Text("Цикл продолжится с той же фазы, с 1 числа.")
        }
        .alert("Не хватает бесплатных маршрутов", isPresented: Binding(
            get: { vm.ui?.showLimitDialog == true },
            set: { if !$0 { vm.dismissSubscriptionLimit() } }
        )) {
            Button("Оформить подписку") {
                vm.dismissSubscriptionLimit()
                router.showPurchases(isAuthorized: true)
            }
            Button("Отмена", role: .cancel) { vm.dismissSubscriptionLimit() }
        } message: {
            Text(ui?.limitMessage ?? "")
        }
        .dsSheet(item: $timePickerTarget, detents: [.height(320)]) { target in
            PlanCustomTimeSheet { hour, minute in
                switch target {
                case .dayStart: vm.setDayStart(hour: hour, minute: minute)
                case .dayEnd: vm.setDayEnd(hour: hour, minute: minute)
                case .nightStart: vm.setNightStart(hour: hour, minute: minute)
                case .nightEnd: vm.setNightEnd(hour: hour, minute: minute)
                }
            }
        }
        .overlay {
            if ui?.isSaving == true {
                BlockingProgressView(message: "Создаём маршруты\nЭто может занять несколько секунд")
            }
        }
    }

    private func backLabel(_ ui: WizardIosUi?) -> String {
        let month = ui?.monthName ?? ""
        return month.isEmpty ? "Назад" : "‹ \(month)"
    }

    private func stepIndicator(_ ui: WizardIosUi?) -> some View {
        HStack(spacing: 20) {
            stepBadge(index: 1, title: "График", done: (ui?.step ?? 1) > 1, active: ui?.step == 1)
            stepBadge(index: 2, title: "Старт и предпросмотр", done: false, active: ui?.step == 2)
            Spacer()
        }
        .padding(.top, 56)
    }

    private func stepBadge(index: Int, title: String, done: Bool, active: Bool) -> some View {
        VStack(spacing: 4) {
            Text(done ? "✓" : "\(index)")
                .font(DSFont.mono(14, .bold))
                .foregroundColor(active ? DSColor.accent : DSColor.textFaint)
            Text(title)
                .font(DSFont.sans(11, .medium))
                .foregroundColor(active ? DSColor.text : DSColor.textFaint)
        }
    }

    // MARK: - Шаг 1

    @ViewBuilder
    private func step1(_ ui: WizardIosUi?) -> some View {
        if ui?.canContinuePrevious == true {
            VStack(alignment: .leading, spacing: 4) {
                DSTonalButton(title: "Продолжить график прошлого месяца", tone: .accent) {
                    vm.continuePreviousSchedule()
                }
                Text("Цикл продолжится с той же фазы, с 1 числа")
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
            }
        }

        DSGroupHeader("ВАРИАНТЫ ГРАФИКА")
        patternGrid(ui)
        Text("Удерживайте график, чтобы удалить его.")
            .font(DSFont.sans(12))
            .foregroundColor(DSColor.textMuted)

        if ui?.isCustom == true {
            DSGroupHeader("ЦИКЛ СМЕН")
            customCycle(ui)
        }

        DSGroupHeader("ВРЕМЯ СМЕНЫ")
        shiftTimeCards(ui)
        Text("Смены разложатся по всем рабочим дням паттерна.")
            .font(DSFont.sans(12))
            .foregroundColor(DSColor.textMuted)
    }

    private func patternGrid(_ ui: WizardIosUi?) -> some View {
        let patterns = (ui?.patterns as? [WizardIosPattern]) ?? []
        return LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
            ForEach(patterns, id: \.id) { pattern in
                patternTile(
                    id: pattern.id,
                    title: pattern.title,
                    subtitle: pattern.subtitle,
                    isSelected: ui?.selectedId == pattern.id
                )
            }
            patternTile(
                id: "custom",
                title: "Свой",
                subtitle: "Настроить вручную",
                isSelected: ui?.isCustom == true
            )
        }
    }

    private func patternTile(id: String, title: String, subtitle: String, isSelected: Bool) -> some View {
        Button {
            vm.selectPattern(id)
        } label: {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(DSFont.sans(17, .bold))
                    .foregroundColor(isSelected ? DSColor.accent : DSColor.text)
                Text(subtitle)
                    .font(DSFont.sans(11))
                    .foregroundColor(DSColor.textMuted)
                    .lineLimit(2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(isSelected ? DSColor.accentSoft : DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(isSelected ? DSColor.accent : DSColor.border, lineWidth: 1)
            )
        }
        .buttonStyle(DSPressableStyle())
        .onLongPressGesture {
            guard id != "custom" else { return }
            deletePatternRequest = DSDeleteRequest(title: "Удалить график «\(title)»?") {
                vm.deletePattern(id)
            }
        }
    }

    private func customCycle(_ ui: WizardIosUi?) -> some View {
        let cycle = ui?.customCycle as? [String] ?? []
        return VStack(alignment: .leading, spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(cycle.enumerated()), id: \.offset) { index, kind in
                        Button {
                            vm.openTypePicker(index)
                        } label: {
                            VStack(spacing: 2) {
                                Text(shiftWord(kind))
                                    .font(DSFont.sans(12, .semibold))
                                    .foregroundColor(kind == "off" ? DSColor.textMuted : DSColor.text)
                                if ui?.pickerIndex == Int32(index) {
                                    typePicker(index: index, dayNumber: index + 1, canRemove: cycle.count > 1)
                                        .frame(width: 160)
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(ui?.pickerIndex == Int32(index) ? DSColor.accentSoft : DSColor.chipBg)
                            .clipShape(Capsule())
                        }
                        .buttonStyle(DSPressableStyle())
                    }
                    Button {
                        vm.addCycleDay()
                    } label: {
                        Image(systemName: "plus")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(DSColor.accent)
                            .frame(width: 36, height: 36)
                            .background(DSColor.accentSoft)
                            .clipShape(Circle())
                    }
                    .buttonStyle(DSPressableStyle())
                }
            }
            Text(ui?.cycleCaption ?? "")
                .font(DSFont.sans(12))
                .foregroundColor(DSColor.textMuted)
        }
    }

    private func typePicker(index: Int, dayNumber: Int, canRemove: Bool) -> some View {
        VStack(spacing: 6) {
            Text("День \(dayNumber) — выберите тип")
                .font(DSFont.sans(11, .semibold))
                .foregroundColor(DSColor.textMuted)
            ForEach([("day", "День"), ("night", "Ночь"), ("off", "Выходной")], id: \.0) { item in
                Button {
                    vm.setCycleDayType(index, kind: item.0)
                } label: {
                    Text(item.1)
                        .font(DSFont.sans(13))
                        .foregroundColor(DSColor.text)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(DSRowPressStyle())
            }
            if canRemove {
                Button {
                    vm.removeCycleDay(index)
                } label: {
                    Text("Удалить день \(dayNumber) из цикла")
                        .font(DSFont.sans(12))
                        .foregroundColor(DSColor.danger)
                }
                .buttonStyle(DSRowPressStyle())
            }
            Button {
                vm.closeTypePicker()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(DSColor.textFaint)
            }
        }
        .padding(10)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func shiftTimeCards(_ ui: WizardIosUi?) -> some View {
        VStack(spacing: 10) {
            if ui?.hasDayShift == true {
                timeCard(
                    title: ui?.hasNightShift == true ? "Дневная" : "",
                    start: ui?.dayStartText ?? "08:00",
                    end: ui?.dayEndText ?? "20:00",
                    startTarget: .dayStart,
                    endTarget: .dayEnd
                )
            }
            if ui?.hasNightShift == true {
                timeCard(
                    title: "Ночная",
                    start: ui?.nightStartText ?? "20:00",
                    end: ui?.nightEndText ?? "08:00",
                    startTarget: .nightStart,
                    endTarget: .nightEnd
                )
            }
        }
    }

    private func timeCard(title: String, start: String, end: String, startTarget: TimeTarget, endTarget: TimeTarget) -> some View {
        DSCard {
            VStack(alignment: .leading, spacing: 8) {
                if !title.isEmpty {
                    Text(title)
                        .font(DSFont.sans(13, .semibold))
                        .foregroundColor(DSColor.textMuted)
                }
                HStack(spacing: 12) {
                    timeField(label: "Начало", value: start) { timePickerTarget = startTarget }
                    timeField(label: "Конец", value: end) { timePickerTarget = endTarget }
                }
            }
            .padding(12)
        }
    }

    private func timeField(label: String, value: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(DSFont.sans(11))
                    .foregroundColor(DSColor.textMuted)
                Text(value)
                    .dsTextStyle(.valueMono)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
            .background(DSColor.surfaceAlt)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(DSPressableStyle())
    }

    // MARK: - Шаг 2

    @ViewBuilder
    private func step2(_ ui: WizardIosUi?) -> some View {
        DSGroupHeader("ПЕРВЫЙ ДЕНЬ ЦИКЛА")
        HStack {
            Button { vm.shiftMonth(-1) } label: {
                Image(systemName: "chevron.left")
                    .foregroundColor(DSColor.text)
            }
            Text("\(ui?.monthName ?? "") \(String(ui?.year ?? 0))")
                .font(DSFont.sans(15, .semibold))
                .foregroundColor(DSColor.text)
            Button { vm.shiftMonth(1) } label: {
                Image(systemName: "chevron.right")
                    .foregroundColor(DSColor.text)
            }
            Spacer()
        }
        Text("Число месяца, с которого начинается ваш цикл смен.")
            .font(DSFont.sans(12))
            .foregroundColor(DSColor.textMuted)

        firstDayGrid(ui)

        Button {
            vm.setExtendToNextMonth(!(ui?.extendToNextMonth ?? false))
        } label: {
            HStack(spacing: 10) {
                Image(systemName: (ui?.extendToNextMonth ?? false) ? "checkmark.square.fill" : "square")
                    .font(.system(size: 18))
                    .foregroundColor((ui?.extendToNextMonth ?? false) ? DSColor.accent : DSColor.textFaint)
                Text("Продлить на следующий месяц")
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.text)
                Spacer()
            }
        }
        .buttonStyle(DSPressableStyle())

        DSGroupHeader("ПРЕДПРОСМОТР · \((ui?.monthName ?? "").uppercased())")
        previewGrid(ui)
        Text("\(ui?.previewShiftCount ?? 0) смен")
            .font(DSFont.sans(13, .semibold))
            .foregroundColor(DSColor.textMuted)
    }

    private func firstDayGrid(_ ui: WizardIosUi?) -> some View {
        let days = Int(ui?.daysInMonth ?? 0)
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
            ForEach(1...max(days, 1), id: \.self) { day in
                if day <= days {
                    Button {
                        vm.setFirstDay(day)
                    } label: {
                        Text("\(day)")
                            .font(DSFont.mono(13, .semibold))
                            .foregroundColor(ui?.firstDay == Int32(day) ? DSColor.accentInk : DSColor.text)
                            .frame(maxWidth: .infinity)
                            .frame(height: 36)
                            .background(ui?.firstDay == Int32(day) ? DSColor.accent : DSColor.surface)
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                    .buttonStyle(DSPressableStyle())
                }
            }
        }
    }

    private func previewGrid(_ ui: WizardIosUi?) -> some View {
        let preview = ui?.preview as? [String] ?? []
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 7), spacing: 4) {
            ForEach(Array(preview.enumerated()), id: \.offset) { index, kind in
                Text(shortKind(kind))
                    .font(DSFont.mono(9, .bold))
                    .foregroundColor(kindColor(kind))
                    .frame(maxWidth: .infinity)
                    .frame(height: 26)
                    .background(kindBackground(kind))
                    .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                    .overlay(Text("\(index + 1)").font(DSFont.mono(7)).foregroundColor(DSColor.textFaint), alignment: .topTrailing)
            }
        }
    }

    private func shortKind(_ kind: String) -> String {
        switch kind {
        case "day": return "Дн"
        case "night": return "Ноч"
        default: return "Вых"
        }
    }

    private func shiftWord(_ kind: String) -> String {
        switch kind {
        case "day": return "Дн"
        case "night": return "Ноч"
        default: return "Вых"
        }
    }

    private func kindColor(_ kind: String) -> Color {
        switch kind {
        case "day": return DSColor.text
        case "night": return DSColor.accent
        default: return DSColor.textFaint
        }
    }

    private func kindBackground(_ kind: String) -> Color {
        switch kind {
        case "day": return DSColor.chipBg
        case "night": return DSColor.accentSoft
        default: return DSColor.surfaceAlt
        }
    }

    // MARK: - Нижняя панель

    private func bottomBar(_ ui: WizardIosUi?) -> some View {
        HStack(spacing: 10) {
            if ui?.step == 2 {
                DSTonalButton(title: "‹ Назад", tone: .neutral) {
                    vm.goToStep(1)
                }
                .frame(width: 120)
            }
            if ui?.step == 1 {
                DSCTAButton(title: "Далее →") {
                    vm.goToStep(2)
                }
            } else {
                DSCTAButton(title: "Применить", isLoading: ui?.isSaving == true) {
                    vm.apply()
                }
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.top, 10)
        .padding(.bottom, 8)
        .background(DSColor.bg)
    }
}
