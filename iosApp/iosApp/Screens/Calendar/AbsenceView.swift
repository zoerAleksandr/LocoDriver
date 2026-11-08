import SwiftUI
import ComposeApp

// «Новое отвлечение» (SCREEN_SPECS §16.1, §16.4). Макет: calendar-variants-de.jsx
// (AbsenceFlowScreen platform=ios). Логика — AbsenceIosViewModel (Kotlin).

struct AbsenceView: View {
    @StateObject private var vm = AbsenceViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared

    @State private var snackQueue: [DSSnackbarItem] = []

    var body: some View {
        let ui = vm.ui
        ScrollView {
            VStack(alignment: .leading, spacing: DSSpacing.md) {
                subtitleHeader(ui)
                monthHeader(ui)
                weekdayRow
                grid(ui)
                Text("Нажмите дату для одного дня или две даты — начало и конец диапазона.")
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
                typeSection(ui)
                summaryCard(ui)
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.bottom, 120)
        }
        .background(DSColor.bg.ignoresSafeArea())
        .safeAreaInset(edge: .bottom) {
            DSCTAButton(
                title: "Сохранить",
                isLoading: ui?.isSaving == true,
                isEnabled: ui?.selectionKind != "none" && ui?.isSaving != true
            ) {
                vm.save()
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.top, 10)
            .padding(.bottom, 8)
            .background(DSColor.bg)
        }
        .navigationBarBackButtonHidden(true)
        .overlay(alignment: .top) {
            DSNavBar(title: "Новое отвлечение", style: .text(backLabel: "‹ Назад"), onBack: {
                router.back()
            })
        }
        .onAppear { vm.prepareScreen() }
        .onChange(of: ui?.done) { done in
            if done == true { router.back() }
        }
        .onChange(of: ui?.errorMessage) { text in
            guard let text = text, !text.isEmpty else { return }
            snackQueue.append(DSSnackbarItem(text: text))
            vm.consumeError()
        }
        .dsSnackbarQueue(snackQueue, bottomPadding: 16) {
            if !snackQueue.isEmpty { snackQueue.removeFirst() }
        }
    }

    private func subtitleHeader(_ ui: AbsenceIosUi?) -> some View {
        Text(subtitleText(ui))
            .font(DSFont.sans(13, .medium))
            .foregroundColor(DSColor.textMuted)
            .padding(.top, 56)
    }

    private func subtitleText(_ ui: AbsenceIosUi?) -> String {
        switch ui?.selectionKind {
        case "single": return "Выбрана 1 дата"
        case "range": return "Выбран диапазон дат"
        default: return "Выберите даты"
        }
    }

    private func monthHeader(_ ui: AbsenceIosUi?) -> some View {
        HStack {
            Button { vm.shiftMonth(-1) } label: {
                Image(systemName: "chevron.left").foregroundColor(DSColor.text)
            }
            Text(ui?.monthLabel ?? "")
                .font(DSFont.sans(17, .bold))
                .foregroundColor(DSColor.text)
            Button { vm.shiftMonth(1) } label: {
                Image(systemName: "chevron.right").foregroundColor(DSColor.text)
            }
            Spacer()
        }
    }

    private var weekdayRow: some View {
        HStack(spacing: 0) {
            ForEach(0..<7, id: \.self) { i in
                let labels = ["ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"]
                Text(labels[i])
                    .font(DSFont.mono(11, .semibold))
                    .foregroundColor(i >= 5 ? DSColor.accent : DSColor.textMuted)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private func grid(_ ui: AbsenceIosUi?) -> some View {
        let days = (ui?.days as? [AbsenceIosDay]) ?? []
        let leading = days.first.map { Int($0.weekdayIndex) } ?? 0
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
            ForEach(0..<leading, id: \.self) { _ in Color.clear.frame(height: 52) }
            ForEach(days, id: \.day) { day in
                dayCell(day)
            }
        }
    }

    private func dayCell(_ day: AbsenceIosDay) -> some View {
        let selection = day.selection
        return Button {
            vm.onDayTap(Int(day.day))
        } label: {
            VStack(spacing: 2) {
                Text("\(day.day)")
                    .font(DSFont.mono(13, .semibold))
                    .foregroundColor(foreground(selection, day))
                if let badge = day.existingBadge {
                    Text(badge)
                        .font(DSFont.mono(7, .bold))
                        .foregroundColor(DSColor.textMuted)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(cellBackground(selection, day))
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(DSPressableStyle())
    }

    private func foreground(_ selection: String?, _ day: AbsenceIosDay) -> Color {
        if selection == "start" || selection == "end" || selection == "single" { return DSColor.accentInk }
        if day.isWeekend { return DSColor.textFaint }
        return DSColor.text
    }

    private func cellBackground(_ selection: String?, _ day: AbsenceIosDay) -> Color {
        switch selection {
        case "start", "end", "single": return DSColor.accent
        case "inside": return DSColor.accent.opacity(0.30)
        default: return DSColor.surface
        }
    }

    // MARK: - Тип отвлечения (§16.4)

    private func typeSection(_ ui: AbsenceIosUi?) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Button {
                vm.toggleTypePicker()
            } label: {
                HStack {
                    Text("Тип отвлечения")
                        .font(DSFont.sans(13))
                        .foregroundColor(DSColor.textMuted)
                    Spacer()
                    Text(selectedTypeLabel(ui))
                        .font(DSFont.sans(15, .medium))
                        .foregroundColor(DSColor.text)
                    Image(systemName: ui?.typePickerOpen == true ? "chevron.up" : "chevron.down")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(DSColor.textFaint)
                }
                .padding(.vertical, 14)
            }
            .buttonStyle(DSRowPressStyle())

            if ui?.typePickerOpen == true {
                ForEach(vm.typeOptions, id: \.kind) { option in
                    Button {
                        vm.setType(option.kind)
                    } label: {
                        HStack {
                            Text(option.label)
                                .font(DSFont.sans(15))
                                .foregroundColor(DSColor.text)
                            Spacer()
                            if ui?.selectedKind == option.kind {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(DSColor.accent)
                            }
                        }
                        .padding(.vertical, 12)
                    }
                    .buttonStyle(DSRowPressStyle())
                }
            }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func selectedTypeLabel(_ ui: AbsenceIosUi?) -> String {
        vm.typeOptions.first { $0.kind == ui?.selectedKind }?.label ?? "Отпуск"
    }

    // MARK: - Карточка-сводка (§16.4)

    private func summaryCard(_ ui: AbsenceIosUi?) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 2)
                .fill(DSColor.accent)
                .frame(width: 4, height: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(selectedTypeLabel(ui))
                    .font(DSFont.sans(15, .semibold))
                    .foregroundColor(DSColor.text)
                Text(ui?.summaryRangeText ?? "Не выбрано")
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text("ДНЕЙ")
                    .font(DSFont.mono(9, .bold))
                    .foregroundColor(DSColor.textFaint)
                Text("\(ui?.summaryDays ?? 0)")
                    .dsTextStyle(.valueMono)
            }
            VStack(alignment: .trailing, spacing: 2) {
                Text("ЧАСОВ")
                    .font(DSFont.mono(9, .bold))
                    .foregroundColor(DSColor.textFaint)
                Text(ui?.summaryHoursText ?? "0:00")
                    .dsTextStyle(.valueMono)
                Text(ui?.summaryHoursCaption ?? "по рабочим дням")
                    .font(DSFont.sans(9))
                    .foregroundColor(DSColor.textMuted)
            }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, DSMetrics.rowPadY)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}
