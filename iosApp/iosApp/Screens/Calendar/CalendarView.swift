import SwiftUI
import ComposeApp

// Экран «Календарь» (SCREEN_SPECS §14). Макет: design/src/calendar-variants-de.jsx
// (CalendarVariantE platform=ios, AddEventSheetE). Логика — CalendarIosViewModel (Kotlin).

struct CalendarView: View {
    @StateObject private var vm = CalendarViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared

    @State private var selectedDay: Int? = nil
    @State private var isWeekCollapsed = false
    /// Сегмент-фильтр: "all" | "trips" | "absences" (§14.2 п.3).
    @State private var filter: String = "all"
    @State private var showAddEvent = false
    @State private var showTechStudy = false
    @State private var showDurationSheet = false
    @State private var showCustomTimePicker = false
    @State private var snackQueue: [DSSnackbarItem] = []
    @State private var deleteRequest: DSDeleteRequest? = nil
    @State private var absenceDelete: AbsenceDeleteChoice? = nil
    @State private var shareText: String? = nil

    var body: some View {
        let ui = vm.ui
        ScrollView {
            VStack(alignment: .leading, spacing: DSSpacing.md) {
                header(ui)
                if planActive {
                    planHint
                    planTimeChips
                } else if !isWeekCollapsed {
                    filterBar
                }
                weekdayRow
                grid(ui)
                collapseButton
                if planActive {
                    planBottomBar
                } else {
                    actionButtons
                    dayDetails(ui)
                }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.bottom, 96)
        }
        .background(DSColor.bg.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .overlay(alignment: .top) { navBarOverlay }
        .onAppear {
            vm.prepareScreen()
            if selectedDay == nil { selectedDay = vm.ui?.today?.intValue ?? 1 }
        }
        .onChange(of: vm.message) { text in
            guard let text = text, !text.isEmpty else { return }
            snackQueue.append(DSSnackbarItem(text: text))
            vm.consumeMessage()
        }
        .onChange(of: vm.shareLink) { url in
            guard let url = url else { return }
            shareText = url
            vm.consumeShareLink()
        }
        .onChange(of: vm.openRouteFormId) { basicId in
            guard let basicId = basicId else { return }
            vm.consumeOpenRouteForm()
            router.show(.routeFormCopy(basicId: basicId))
        }
        .dsSnackbarQueue(snackQueue, bottomPadding: 24) {
            if !snackQueue.isEmpty { snackQueue.removeFirst() }
        }
        .dsDeleteAlert($deleteRequest)
        .dsSheet(isPresented: $showAddEvent, detents: [.medium]) {
            AddEventSheetView(
                day: selectedDay ?? 1,
                monthLabel: ui?.monthLabelNormal ?? "",
                onRoute: {
                    vm.enterRoutePlan(initialDay: selectedDay ?? 1)
                },
                onAbsence: {
                    router.showAbsence()
                },
                onDayOff: {
                    if let d = selectedDay { vm.addDayOff(d) }
                },
                onTechStudy: {
                    showTechStudy = true
                }
            )
        }
        .dsSheet(isPresented: $showTechStudy, detents: [.height(340)]) {
            TechnicalStudyDialog(
                day: selectedDay ?? 1,
                monthLabel: ui?.monthLabelNormal ?? "",
                averagePayText: { hours in vm.techStudyPayText(hours: hours) },
                onSave: { hours in
                    if let d = selectedDay { vm.addTechnicalStudy(d, hours: hours) }
                }
            )
        }
        .dsSheet(isPresented: $showDurationSheet, detents: [.height(380)]) {
            PlanDurationSheet(
                suggestedMillis: vm.plan?.suggestedWorkDuration ?? 43_200_000,
                onConfirm: { millis in
                    vm.createPlannedRoutes(durationMillis: millis)
                },
                onWithoutEnd: {
                    vm.createPlannedRoutes(durationMillis: nil)
                }
            )
        }
        .dsSheet(isPresented: $showCustomTimePicker, detents: [.height(320)]) {
            PlanCustomTimeSheet { hour, minute in
                vm.addCustomPlanTime(hour: hour, minute: minute)
            }
        }
        .sheet(item: $absenceDelete) { request in
            AbsenceDeleteSheet(request: request) { mode in
                if mode == "day" {
                    vm.deleteAbsenceDay(request.day)
                } else {
                    vm.deleteAbsencePeriod(start: request.periodStart, end: request.periodEnd)
                }
            }
            .presentationDetents([.height(280)])
        }
        .alert("Маршрут с такой явкой уже сохранён.", isPresented: Binding(
            get: { vm.duplicate != nil },
            set: { if !$0 { vm.dismissDuplicate() } }
        )) {
            Button("Заменить") { vm.replaceDuplicateAndCreate() }
            Button("Оставить оба") { vm.keepDuplicateAndCreate() }
        } message: {
            Text(vm.duplicate?.existingLabel ?? "")
        }
        .alert("Не хватает бесплатных маршрутов", isPresented: Binding(
            get: { vm.limit != nil },
            set: { if !$0 { vm.dismissLimit() } }
        )) {
            Button("Оформить подписку") {
                vm.dismissLimit()
                router.showPurchases(isAuthorized: true)
            }
            Button("Отмена", role: .cancel) { vm.dismissLimit() }
        } message: {
            Text(vm.limit?.message ?? "")
        }
        .overlay {
            if let op = vm.ui?.operationMessage {
                BlockingProgressView(message: op)
            }
        }
    }

    private var planActive: Bool { vm.plan?.active == true }

    // MARK: - Шапка (§14.2 п.2)

    private func header(_ ui: CalendarIosUi?) -> some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 4) {
                Text(ui?.monthLabelUpper ?? "")
                    .dsTextStyle(.group)
                HStack(alignment: .firstTextBaseline, spacing: 8) {
                    Text("Отработано")
                        .font(DSFont.sans(13))
                        .foregroundColor(DSColor.textMuted)
                    Text(ui?.totalWorkedText ?? "00:00")
                        .dsTextStyle(.metricLg)
                }
            }
            Spacer()
            if planActive {
                Button("Отмена") { vm.exitRoutePlan() }
                    .font(DSFont.sans(15, .semibold))
                    .foregroundColor(DSColor.accent)
            } else {
                HStack(spacing: 12) {
                    Button { vm.shiftMonth(-1) } label: {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(DSColor.text)
                    }
                    Button { vm.shiftMonth(1) } label: {
                        Image(systemName: "chevron.right")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(DSColor.text)
                    }
                }
            }
        }
    }

    private var filterBar: some View {
        HStack(spacing: 8) {
            ForEach([("all", "Всё"), ("trips", "Поездки"), ("absences", "Отвлечения")], id: \.0) { item in
                DSChip(title: item.1, style: .choice(isSelected: filter == item.0)) {
                    filter = item.0
                }
            }
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

    // MARK: - Сетка месяца (§14.2 п.5)

    private func grid(_ ui: CalendarIosUi?) -> some View {
        let days = (ui?.days as? [CalendarIosDay]) ?? []
        let shown = visibleDays(days)
        let leading = leadingBlanks(shown)
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
            ForEach(0..<leading, id: \.self) { _ in Color.clear.frame(height: 64) }
            ForEach(shown, id: \.day) { day in
                DayCellView(
                    day: day,
                    isSelected: selectedDay == Int(day.day),
                    isPlanActive: planActive,
                    planTimeText: planTime(for: Int(day.day)),
                    isPlanMarked: isPlanMarked(Int(day.day)),
                    isPlanDisabled: isPlanDisabled(day),
                    filter: filter
                ) {
                    if planActive {
                        vm.toggleDayPlan(Int(day.day))
                    } else {
                        selectedDay = Int(day.day)
                    }
                }
            }
        }
    }

    private func visibleDays(_ days: [CalendarIosDay]) -> [CalendarIosDay] {
        guard isWeekCollapsed, let sel = selectedDay, let first = days.first else { return days }
        // Одна неделя выбранного дня (неделя с понедельника).
        let selDay = days.first { Int($0.day) == sel } ?? first
        let weekStart = Int(selDay.day) - Int(selDay.weekdayIndex)
        return days.filter { weekStart <= Int($0.day) && Int($0.day) < weekStart + 7 }
    }

    private func leadingBlanks(_ shown: [CalendarIosDay]) -> Int {
        guard isWeekCollapsed, let first = shown.first else {
            // Первый день месяца: пустые ячейки до его дня недели.
            return 0
        }
        return Int(first.weekdayIndex)
    }

    private func planTime(for day: Int) -> String? {
        guard let days = vm.plan?.plannedDays else { return nil }
        return days.first { Int($0.day) == day }?.timeText
    }

    private func isPlanMarked(_ day: Int) -> Bool {
        guard let days = vm.plan?.plannedDays else { return false }
        return days.contains { Int($0.day) == day }
    }

    private func isPlanDisabled(_ day: CalendarIosDay) -> Bool {
        guard let absence = day.absence else { return false }
        return !absence.isDayOff
    }

    private var collapseButton: some View {
        Button {
            withAnimation(.easeOut(duration: 0.2)) { isWeekCollapsed.toggle() }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: isWeekCollapsed ? "chevron.down" : "chevron.up")
                    .font(.system(size: 12, weight: .bold))
                Text(isWeekCollapsed ? "Развернуть в месяц" : "Свернуть в неделю")
                    .font(DSFont.sans(13, .semibold))
            }
            .foregroundColor(DSColor.accent)
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: - Действия (§14.2 п.7–8)

    private var actionButtons: some View {
        VStack(spacing: 10) {
            DSCTAButton(title: addEventTitle, icon: .plus) {
                showAddEvent = true
            }
            DSTonalButton(title: "Заполнить месяц по графику", icon: .calendar, tone: .neutral) {
                router.showScheduleWizard()
            }
        }
    }

    private var addEventTitle: String {
        let day = selectedDay ?? 1
        let monthLabel = (vm.ui?.monthLabelNormal ?? "").split(separator: " ").first.map(String.init) ?? ""
        return "+ Добавить событие на \(day) \(monthLabel.lowercased())"
    }

    // MARK: - Режим планирования (§14.4)

    private var planHint: some View {
        let step = vm.plan?.hintStep ?? 0
        return VStack(alignment: .leading, spacing: 4) {
            Text(step == 0 ? "ШАГ 1 · ВРЕМЯ ЯВКИ" : (step == 1 ? "ШАГ 2 · ДАТЫ" : "✓ Готово — можно создавать маршруты"))
                .dsTextStyle(.group)
            if step < 2 {
                Text(step == 0 ? "Выберите время явки из списка ниже" : "Отметьте дни в календаре")
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.textMuted)
            }
        }
    }

    private var planTimeChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(vm.plan?.timeOptions as? [CalendarIosTimeOption] ?? [], id: \.millis) { option in
                    DSChip(title: option.label, style: .choice(isSelected: vm.plan?.activeTimeText == option.label)) {
                        vm.pickPlanTime(millis: option.millis)
                    }
                }
                DSChip(title: "+ Своё время", style: .assist) {
                    showCustomTimePicker = true
                }
            }
        }
    }

    private var planBottomBar: some View {
        VStack(spacing: 12) {
            Text(vm.plan?.bottomText ?? "")
                .font(DSFont.sans(13, .medium))
                .foregroundColor(DSColor.textMuted)
                .frame(maxWidth: .infinity)
            DSCTAButton(
                title: "Создать маршруты",
                isEnabled: (vm.plan?.plannedDays.count ?? 0) > 0
            ) {
                showDurationSheet = true
            }
        }
    }

    // MARK: - Детали выбранного дня (§14.2 п.9)

    @ViewBuilder
    private func dayDetails(_ ui: CalendarIosUi?) -> some View {
        let day = (ui?.days as? [CalendarIosDay])?.first { selectedDay == nil || Int($0.day) == selectedDay }
        VStack(alignment: .leading, spacing: DSSpacing.sm) {
            if let day = day {
                Text(dayTitle(day))
                    .dsTextStyle(.sectionH2)
                if day.isHoliday {
                    holidayPlate(day)
                }
                if let absence = day.absence {
                    AbsenceCardView(absence: absence) {
                        if absence.periodDays > 1 {
                            absenceDelete = AbsenceDeleteChoice(
                                day: Int(day.day),
                                periodStart: Int(absence.periodStart),
                                periodEnd: Int(absence.periodEnd),
                                title: "Удалить «\(absence.label)»?",
                                subtitle: "Занимает \(absence.periodStart)–\(absence.periodEnd) · \(absence.periodDays) дней"
                            )
                        } else {
                            deleteRequest = DSDeleteRequest(
                                title: "Удалить «\(absence.label)»?",
                                message: "Это действие нельзя отменить."
                            ) {
                                vm.deleteAbsenceDay(Int(day.day))
                            }
                        }
                    }
                }
                let trips = filteredTrips(day)
                if trips.isEmpty && day.absence == nil {
                    Text("Нет событий в этот день.")
                        .font(DSFont.sans(14))
                        .foregroundColor(DSColor.textMuted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 12)
                }
                ForEach(trips, id: \.basicId) { trip in
                    HomeRouteCardRow(
                        card: trip.card,
                        onTap: { router.show(.routeForm(basicId: trip.basicId)) },
                        onLongPress: { router.present(.routeQuickView(basicId: trip.basicId)) },
                        onLegend: { router.present(.routeLegend) },
                        onDelete: {
                            deleteRequest = DSDeleteRequest(
                                title: "Удалить маршрут?",
                                message: tripLabel(trip)
                            ) {
                                vm.deleteRoute(trip.basicId)
                            }
                        }
                    )
                }
            }
        }
    }

    private func filteredTrips(_ day: CalendarIosDay) -> [CalendarIosTrip] {
        let trips = (day.trips as? [CalendarIosTrip]) ?? []
        if filter == "absences" { return [] }
        return trips
    }

    private func dayTitle(_ day: CalendarIosDay) -> String {
        let monthLabel = (vm.ui?.monthLabelNormal ?? "").split(separator: " ").first.map(String.init) ?? ""
        let weekdays = ["понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"]
        let wd = weekdays[Int(day.weekdayIndex) % 7]
        return "\(day.day) \(monthLabel.lowercased()) · \(wd)"
    }

    private func tripLabel(_ trip: CalendarIosTrip) -> String {
        "\(trip.timeText) · \(trip.numberText)"
    }

    private func holidayPlate(_ day: CalendarIosDay) -> some View {
        HStack(spacing: 8) {
            Image(systemName: "star.fill")
                .font(.system(size: 12))
                .foregroundColor(DSColor.danger)
            Text(day.holidayName ?? "Праздничный день")
                .font(DSFont.sans(13, .semibold))
                .foregroundColor(DSColor.danger)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.danger.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var navBarOverlay: some View {
        DSNavBar(title: "Календарь", style: .text(backLabel: "Назад"), onBack: { router.back() })
    }
}

// MARK: - Ячейка дня

private struct DayCellView: View {
    let day: CalendarIosDay
    let isSelected: Bool
    let isPlanActive: Bool
    let planTimeText: String?
    let isPlanMarked: Bool
    let isPlanDisabled: Bool
    let filter: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 2) {
                ZStack {
                    if day.isToday {
                        Circle()
                            .fill(DSColor.accent)
                            .frame(width: 26, height: 26)
                    }
                    Text("\(day.day)")
                        .font(DSFont.mono(14, .semibold))
                        .foregroundColor(numberColor)
                }
                if isPlanActive && isPlanMarked {
                    Text(planTimeText ?? "")
                        .font(DSFont.mono(9, .semibold))
                        .foregroundColor(DSColor.accent)
                } else {
                    tripTimes
                    if filter != "trips", let absence = day.absence {
                        Text(absence.badge)
                            .font(DSFont.mono(8, .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 4)
                            .padding(.vertical, 1)
                            .background(badgeColor(absence.kind))
                            .clipShape(Capsule())
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 64)
            .background(isPlanMarked ? DSColor.accentSoft : DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(isSelected ? DSColor.accent : DSColor.border, lineWidth: isSelected ? 2 : 1)
            )
            .opacity(isPlanDisabled ? 0.45 : 1)
        }
        .buttonStyle(DSPressableStyle())
    }

    private var numberColor: Color {
        if day.isToday { return DSColor.accentInk }
        if day.isHoliday { return DSColor.danger }
        if day.isWeekend { return DSColor.textFaint }
        return DSColor.text
    }

    @ViewBuilder
    private var tripTimes: some View {
        let trips = (day.trips as? [CalendarIosTrip]) ?? []
        if filter != "absences" && !trips.isEmpty {
            Text(trips[0].timeText)
                .font(DSFont.mono(9, .medium))
                .foregroundColor(DSColor.textMuted)
            if trips.count > 1 {
                Text(trips.count == 2 ? trips[1].timeText : "+\(trips.count - 1)")
                    .font(DSFont.mono(9, .medium))
                    .foregroundColor(DSColor.textFaint)
            }
        }
    }

    private func badgeColor(_ kind: String) -> Color {
        switch kind {
        case "vacation": return DSColor.accent
        case "sick": return DSColor.warning
        case "courses": return DSColor.success
        case "dayOff": return DSColor.textMuted
        case "technicalStudy": return DSColor.text
        default: return DSColor.textMuted
        }
    }
}

// MARK: - Карточка отвлечения (§14.2)

private struct AbsenceCardView: View {
    let absence: CalendarIosAbsence
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                Text(absence.label)
                    .font(DSFont.sans(15, .semibold))
                    .foregroundColor(DSColor.text)
                Text(subtitle)
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 2) {
                Text(absence.hoursValueText)
                    .dsTextStyle(.valueMono)
                Text(absence.hoursCaptionText)
                    .font(DSFont.sans(10))
                    .foregroundColor(DSColor.textMuted)
            }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, DSMetrics.rowPadY)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .swipeToDelete(action: onDelete)
    }

    private var subtitle: String {
        if absence.isTechnicalStudy {
            return "Оплата по среднему часу"
        }
        if absence.periodDays > 1 {
            return "\(absence.periodStart)–\(absence.periodEnd) · \(absence.periodDays) дней"
        }
        return "Весь день"
    }
}

// MARK: - Диалоги и шторки

struct AbsenceDeleteChoice: Identifiable {
    let id = UUID()
    let day: Int
    let periodStart: Int
    let periodEnd: Int
    let title: String
    let subtitle: String
}

private struct AbsenceDeleteSheet: View {
    let request: AbsenceDeleteChoice
    let onChoose: (String) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        DSBottomSheet(header: .titleClose(title: request.title, subtitle: request.subtitle), onClose: { dismiss() }) {
            VStack(spacing: 10) {
                DSTonalButton(title: "Только этот день", tone: .neutral) {
                    dismiss()
                    onChoose("day")
                }
                DSDangerButton(title: "Весь период · \(request.periodEnd - request.periodStart + 1) дней") {
                    dismiss()
                    onChoose("period")
                }
            }
            .padding(.top, 8)
        }
    }
}

struct BlockingProgressView: View {
    let message: String

    var body: some View {
        ZStack {
            DSColor.scrim.ignoresSafeArea()
            VStack(spacing: 14) {
                ProgressView()
                    .tint(DSColor.accent)
                Text(message)
                    .font(DSFont.sans(14, .medium))
                    .foregroundColor(DSColor.text)
                    .multilineTextAlignment(.center)
            }
            .padding(24)
            .background(DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        }
    }
}

// MARK: - Свайп влево → удаление (паттерн HomeRouteCardRow)

struct SwipeToDeleteModifier: ViewModifier {
    let action: () -> Void
    @State private var revealed = false
    @GestureState private var dragX: CGFloat = 0

    private let deleteWidth: CGFloat = 80

    func body(content: Content) -> some View {
        let offset = min(0, max(-deleteWidth * 1.3, (revealed ? -deleteWidth : 0) + dragX))
        ZStack(alignment: .trailing) {
            Button {
                withAnimation(.easeOut(duration: 0.2)) { revealed = false }
                action()
            } label: {
                DSIcon.trash.image
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(DSColor.accentInk)
                    .frame(width: deleteWidth)
                    .frame(maxHeight: .infinity)
                    .background(DSColor.danger)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Удалить"))
            .opacity(offset < 0 ? 1 : 0)

            content
                .background(DSColor.surface)
                .offset(x: offset)
                .simultaneousGesture(
                    DragGesture(minimumDistance: 20)
                        .updating($dragX) { value, state, _ in
                            state = value.translation.width
                        }
                        .onEnded { value in
                            withAnimation(.easeOut(duration: 0.2)) {
                                revealed = value.translation.width < -40
                            }
                        }
                )
        }
        .clipped()
    }
}

extension View {
    func swipeToDelete(action: @escaping () -> Void) -> some View {
        modifier(SwipeToDeleteModifier(action: action))
    }
}
