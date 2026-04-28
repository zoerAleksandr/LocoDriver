import SwiftUI
import ComposeApp

struct FormView: View {
    let routeId: String?
    @StateObject private var vm = FormViewModelWrapper()
    @Environment(\.dismiss) private var dismiss

    // DatePicker state — nil пока пользователь не указал время явно.
    @State private var startWorkDate: Date? = nil
    @State private var endWorkDate: Date? = nil

    // Шторки
    @State private var salaryDetailsShown: Bool = false
    @State private var restDetailsShown: Bool = false
    @State private var settingsSheetShown: Bool = false

    // Share Sheet
    @State private var showShareSheet: Bool = false
    @State private var shareURL: URL? = nil

    // Alert: удаление
    @State private var confirmDelete: Bool = false

    // Предупреждение о некорректных временах работы
    @State private var timeWarning: String? = nil

    // Видимость плавающего тулбара: прячем при скролле вниз, показываем при
    // скролле вверх и на старте / в самом верху.
    @State private var toolbarVisible: Bool = true
    @State private var lastScrollOffset: CGFloat = 0
    /// Накопленное смещение в одном направлении. Сбрасывается при смене
    /// направления скролла, позволяет срабатывать и на медленный скролл
    /// (мелкие дельты суммируются), и раньше — на быстром (одно большое
    /// событие сразу превышает порог).
    @State private var scrollAccumulator: CGFloat = 0

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.appBg)
            } else {
                formContent
            }
        }
        .navigationTitle(routeId == nil ? "Новый маршрут" : "Маршрут")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .toolbar {
            // Для нового маршрута форма — корень модального sheet'а,
            // системного back-chevron нет, добавляем явную кнопку «Отмена».
            if routeId == nil {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Отмена") { dismiss() }
                        .font(.system(size: 15))
                        .foregroundStyle(Color.appAccent)
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("Сохранить") { vm.saveRoute() }
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(Color.appAccent)
            }
        }
        .onAppear { vm.loadRoute(id: routeId) }
        .onChange(of: vm.route) { route in
            // Поля остаются nil, если сервер/БД не вернули значение —
            // не подставляем текущее время как placeholder.
            if let startMs = route?.basicData.timeStartWork?.int64Value, startMs > 0 {
                startWorkDate = TimeFormatter.msToDate(startMs)
            } else {
                startWorkDate = nil
            }
            if let endMs = route?.basicData.timeEndWork?.int64Value, endMs > 0 {
                endWorkDate = TimeFormatter.msToDate(endMs)
            } else {
                endWorkDate = nil
            }
        }
        .onChange(of: vm.isSaved) { saved in
            if saved { dismiss() }
        }
        .onChange(of: vm.isDeleted) { deleted in
            if deleted { dismiss() }
        }
        .onChange(of: vm.shareLink) { link in
            if let link = link, let url = URL(string: link) {
                shareURL = url
                showShareSheet = true
                vm.consumeShareLink()
            }
        }
        // Объединённый alert: vm.error (типизированный AppError) + legacy
        // vm.errorMessage. Кнопка «Повторить» — только при vm.error.canRetry.
        // Set-binding обнуляет ОБА state'а через consumeError (он же
        // делегирует на clearError).
        .alert(
            vm.error?.userMessage ?? vm.errorMessage ?? "Ошибка",
            isPresented: Binding(
                get: { vm.error != nil || vm.errorMessage != nil },
                set: { if !$0 { vm.consumeError() } }
            )
        ) {
            if vm.error?.canRetry == true {
                Button("Повторить") { vm.retry() }
            }
            Button("OK", role: .cancel) { vm.consumeError() }
        }
        .alert("Некорректное время", isPresented: Binding(
            get: { timeWarning != nil },
            set: { if !$0 { timeWarning = nil } }
        )) {
            Button("OK") { timeWarning = nil }
        } message: {
            Text(timeWarning ?? "")
        }
        .alert("Удалить маршрут?", isPresented: $confirmDelete) {
            Button("Отмена", role: .cancel) {}
            Button("Удалить", role: .destructive) { vm.deleteRoute() }
        } message: {
            Text("Маршрут будет помечен как удалённый и убран из списка.")
        }
        .sheet(isPresented: $showShareSheet, onDismiss: { shareURL = nil }) {
            if let url = shareURL {
                ShareSheet(items: [url])
            }
        }
        .sheet(isPresented: $salaryDetailsShown) {
            SalaryDetailsSheet(vm: vm)
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $restDetailsShown) {
            RestDetailsSheet(vm: vm)
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        }
        .sheet(isPresented: $settingsSheetShown) {
            RouteSettingsSheet(vm: vm)
                .presentationDetents([.large])
                .presentationDragIndicator(.visible)
        }
    }

    // MARK: - Content

    private var formContent: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                VStack(spacing: 18) {

                    // 1. Основные данные + бейдж
                    basicDataSection

                    // 2. Плитки «Расчёт» + «Отдых»
                    SummaryTilesRow(
                        totalPayment: vm.salaryTotal,
                        isRestAtPO: vm.isRestAtPO,
                        onPaymentTap: { salaryDetailsShown = true },
                        onRestToggle: { vm.toggleRestType() },
                        onRestDetailsTap: { restDetailsShown = true }
                    )

                    // 3. Время работы
                    workingTimeSection

                    // 4. Составы (локомотивы / поезда / пассажиром) — оставлены как отдельные карточки
                    locomotivesCard
                    trainsCard
                    passengersCard

                    // 5. Заметки
                    notesCard

                    Color.clear.frame(height: 90) // место под плавающий тулбар
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            .background(Color.appBg)
            .modifier(ScrollOffsetObserver { offset in
                handleContentOffset(offset)
            })

            // Плавающий тулбар iOS Photos style — прячется при скролле вниз.
            if toolbarVisible {
                RouteFormToolbar(
                    isFavorite: vm.isFavorite,
                    canDelete: routeId != nil,
                    onSettings: { settingsSheetShown = true },
                    onFavoriteToggle: { vm.toggleFavorite() },
                    onShare: { vm.shareRoute() },
                    onDuplicate: { /* TODO */ },
                    onDelete: { confirmDelete = true }
                )
                .transition(.move(edge: .bottom))
            }
        }
    }

    /// Решает, показывать ли тулбар, по contentOffset.y ScrollView.
    /// В самом верху offset ≈ 0, при скролле вниз (контент уходит вверх)
    /// offset растёт, при скролле вверх — уменьшается.
    private func handleContentOffset(_ newOffset: CGFloat) {
        let delta = newOffset - lastScrollOffset
        lastScrollOffset = newOffset

        // У самого верха — всегда показываем и сбрасываем аккумулятор.
        if newOffset <= 4 {
            scrollAccumulator = 0
            if !toolbarVisible {
                withAnimation(.easeInOut(duration: 0.22)) { toolbarVisible = true }
            }
            return
        }

        // Смена направления скролла — обнуляем аккумулятор, чтобы старые
        // движения в противоположную сторону не влияли.
        if (delta > 0 && scrollAccumulator < 0) || (delta < 0 && scrollAccumulator > 0) {
            scrollAccumulator = 0
        }
        scrollAccumulator += delta

        let threshold: CGFloat = 12

        if scrollAccumulator >= threshold, toolbarVisible {
            // Скролл вниз накопился — прячем.
            withAnimation(.easeInOut(duration: 0.22)) { toolbarVisible = false }
            scrollAccumulator = 0
        } else if scrollAccumulator <= -threshold, !toolbarVisible {
            // Скролл вверх накопился — показываем.
            withAnimation(.easeInOut(duration: 0.22)) { toolbarVisible = true }
            scrollAccumulator = 0
        }
    }

    // MARK: - Basic data

    private var basicDataSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("Основные данные")
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
                Spacer()
                if vm.isOnePersonOperation {
                    OneManBadge { settingsSheetShown = true }
                }
            }
            .padding(.bottom, 2)

            HStack(spacing: 6) {
                let currentNumber = vm.route?.basicData.number ?? ""
                if !currentNumber.isEmpty {
                    Text("№")
                        .font(.system(size: 15))
                        .foregroundStyle(.secondary)
                }
                TextField("Номер маршрута", text: Binding(
                    get: { vm.route?.basicData.number ?? "" },
                    set: { vm.updateNumber($0) }
                ))
                .font(.system(size: 15))
            }
            .padding(14)
            .background(Color.appCard)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    // MARK: - Working time

    private var workingTimeSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("Время работы")
                    .font(.system(size: 13))
                    .foregroundStyle(.secondary)
                Spacer()
                if let startMs = vm.route?.basicData.timeStartWork?.int64Value,
                   let endMs = vm.route?.basicData.timeEndWork?.int64Value,
                   startMs > 0, endMs > startMs {
                    Text(TimeFormatter.formatDuration(ms: endMs - startMs))
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
            }
            .padding(.bottom, 2)

            VStack(spacing: 0) {
                workingTimeRow(
                    title: "Начало",
                    date: $startWorkDate,
                    defaultProvider: { Date() },
                    onSet: { newStart in
                        // Если выставлено окончание и оно оказалось раньше
                        // нового начала — отказываем, ревертим дату.
                        if let end = endWorkDate, end <= newStart {
                            timeWarning = "Начало работы не может быть позже или равно окончанию."
                            return false
                        }
                        vm.setTimeStartWork(TimeFormatter.dateToMs(newStart))
                        return true
                    },
                    onClear: { vm.setTimeStartWork(nil) }
                )
                Divider().padding(.leading, 14)
                workingTimeRow(
                    title: "Окончание",
                    date: $endWorkDate,
                    defaultProvider: {
                        // +12 часов ко времени начала, если оно указано;
                        // иначе — 12 часов от текущего момента.
                        let base = startWorkDate ?? Date()
                        return base.addingTimeInterval(12 * 3600)
                    },
                    onSet: { newEnd in
                        if let start = startWorkDate, newEnd <= start {
                            timeWarning = "Окончание работы не может быть раньше или равным началу."
                            return false
                        }
                        vm.setTimeEndWork(TimeFormatter.dateToMs(newEnd))
                        return true
                    },
                    onClear: { vm.setTimeEndWork(nil) }
                )
            }
            .background(Color.appCard)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    /// Строка DatePicker-а времени работы.
    ///
    /// - `defaultProvider`: какое время подставить при первом нажатии «Указать».
    /// - `onSet`: возвращает `true`, если значение принято; `false` — если
    ///   валидация не прошла, тогда локальный `date` откатывается.
    @ViewBuilder
    private func workingTimeRow(
        title: String,
        date: Binding<Date?>,
        defaultProvider: @escaping () -> Date,
        onSet: @escaping (Date) -> Bool,
        onClear: @escaping () -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(title)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                if date.wrappedValue != nil {
                    Button(action: {
                        date.wrappedValue = nil
                        onClear()
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 14))
                            .foregroundStyle(.tertiary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Очистить")
                }
            }

            if let current = date.wrappedValue {
                DatePicker(
                    "",
                    selection: Binding(
                        get: { current },
                        set: { newValue in
                            let previous = current
                            // Оптимистично обновляем локально, чтобы DatePicker
                            // отразил выбор. Если валидация не прошла —
                            // откатываем назад.
                            date.wrappedValue = newValue
                            if !onSet(newValue) {
                                DispatchQueue.main.async {
                                    date.wrappedValue = previous
                                }
                            }
                        }
                    ),
                    displayedComponents: [.date, .hourAndMinute]
                )
                .labelsHidden()
                .datePickerStyle(.compact)
            } else {
                Button(action: {
                    let defaultDate = defaultProvider()
                    date.wrappedValue = defaultDate
                    if !onSet(defaultDate) {
                        // Маловероятно, но на всякий случай откатываем.
                        date.wrappedValue = nil
                    }
                }) {
                    HStack(spacing: 12) {
                        Image(systemName: "plus.circle")
                            .font(.system(size: 14))
                            .frame(width: 20)
                        Text("Указать")
                    }
                    .foregroundStyle(Color.appAccent)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Consist (loco / train / passenger)

    private var locomotivesCard: some View {
        sectionCard(title: "Локомотивы") {
            let locos = vm.route?.locomotives as? [DomainLocomotive] ?? []
            VStack(spacing: 0) {
                ForEach(locos, id: \.locoId) { loco in
                    NavigationLink(destination: FormLocoView(
                        routeId: vm.route?.basicData.id ?? "",
                        locoId: loco.locoId
                    )) {
                        consistRow(
                            icon: "tram.fill",
                            text: locoTitle(loco)
                        )
                    }
                    .buttonStyle(.plain)
                    Divider().padding(.leading, 44)
                }
                NavigationLink(destination: FormLocoView(
                    routeId: vm.route?.basicData.id ?? "",
                    locoId: nil
                )) {
                    addRow(text: "Добавить локомотив")
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var trainsCard: some View {
        sectionCard(title: "Поезда") {
            let trains = vm.route?.trains as? [DomainTrain] ?? []
            VStack(spacing: 0) {
                ForEach(trains, id: \.trainId) { train in
                    NavigationLink(destination: FormTrainView(
                        routeId: vm.route?.basicData.id ?? "",
                        trainId: train.trainId
                    )) {
                        consistRow(
                            icon: "car.2.fill",
                            text: "Поезд \(train.number ?? "—")"
                        )
                    }
                    .buttonStyle(.plain)
                    Divider().padding(.leading, 44)
                }
                NavigationLink(destination: FormTrainView(
                    routeId: vm.route?.basicData.id ?? "",
                    trainId: nil
                )) {
                    addRow(text: "Добавить поезд")
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var passengersCard: some View {
        sectionCard(title: "Пассажиром") {
            let passengers = vm.route?.passengers as? [DomainPassenger] ?? []
            VStack(spacing: 0) {
                ForEach(passengers, id: \.passengerId) { passenger in
                    NavigationLink(destination: FormPassengerView(
                        routeId: vm.route?.basicData.id ?? "",
                        passengerId: passenger.passengerId
                    )) {
                        consistRow(
                            icon: "person.2.fill",
                            text: "Поезд \(passenger.trainNumber ?? "—")"
                        )
                    }
                    .buttonStyle(.plain)
                    Divider().padding(.leading, 44)
                }
                NavigationLink(destination: FormPassengerView(
                    routeId: vm.route?.basicData.id ?? "",
                    passengerId: nil
                )) {
                    addRow(text: "Добавить пассажиром")
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func consistRow(icon: String, text: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 14))
                .foregroundStyle(Color.appAccent)
                .frame(width: 20)
            Text(text)
                .foregroundStyle(.primary)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }

    private func addRow(text: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: "plus.circle")
                .font(.system(size: 14))
                .foregroundStyle(Color.appAccent)
                .frame(width: 20)
            Text(text)
                .foregroundStyle(Color.appAccent)
            Spacer()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }

    private func locoTitle(_ loco: DomainLocomotive) -> String {
        let joined = [loco.series, loco.number]
            .compactMap { $0 }
            .joined(separator: " ")
        return joined.isEmpty ? "Локомотив" : joined
    }

    // MARK: - Notes

    private var notesCard: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Заметки")
                .font(.system(size: 13))
                .foregroundStyle(.secondary)
                .padding(.bottom, 2)

            TextField("Добавить заметку…", text: Binding(
                get: { vm.route?.basicData.notes ?? "" },
                set: { vm.updateNotes($0) }
            ), axis: .vertical)
            .lineLimit(3...6)
            .font(.system(size: 15))
            .padding(14)
            .background(Color.appCard)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    // MARK: - Helpers

    @ViewBuilder
    private func sectionCard<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 13))
                .foregroundStyle(.secondary)
                .padding(.bottom, 2)
            content()
                .background(Color.appCard)
                .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

}

// MARK: - OneManBadge

struct OneManBadge: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Image(systemName: "person.fill")
                    .font(.system(size: 10, weight: .semibold))
                Text("В одно лицо")
                    .font(.system(size: 12, weight: .semibold))
            }
            .foregroundStyle(Color.appAccent)
            .padding(.horizontal, 9)
            .padding(.vertical, 4)
            .background(Color.appAccent.opacity(0.12))
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - SummaryTilesRow

struct SummaryTilesRow: View {
    let totalPayment: Double?
    let isRestAtPO: Bool
    let onPaymentTap: () -> Void
    let onRestToggle: () -> Void
    let onRestDetailsTap: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            // Плитка расчёта — целиком открывает шторку с расчётом.
            Button(action: onPaymentTap) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 6) {
                        Image(systemName: "rublesign")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Color.appAccent)
                        Text("Расчёт")
                            .font(.system(size: 11))
                            .foregroundStyle(.secondary)
                        Spacer(minLength: 4)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundStyle(.tertiary)
                    }
                    Text(FormatMoney.rub(totalPayment))
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(12)
                .background(Color.appCard)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)

            // Плитка отдыха:
            //  - Шеврон справа сверху — кнопка, открывает шторку деталей.
            //  - Вся остальная область (иконка + заголовок + тип) — переключает
            //    «Домашний» ↔ «Отдых в ПО».
            HStack(alignment: .top, spacing: 6) {
                Button(action: onRestToggle) {
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 6) {
                            if isRestAtPO {
                                Image("ic_bed_side")
                                    .renderingMode(.template)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 22, height: 22)
                                    .foregroundStyle(Color.appAccent)
                            } else {
                                Image(systemName: "house.fill")
                                    .font(.system(size: 18))
                                    .foregroundStyle(Color.appAccent)
                            }
                            Text("Отдых")
                                .font(.system(size: 11))
                                .foregroundStyle(.secondary)
                        }
                        Text(isRestAtPO ? "Отдых в ПО" : "Домашний")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(.primary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)

                Button(action: onRestDetailsTap) {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(.tertiary)
                        .padding(.leading, 6)
                        .padding(.vertical, 2)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            .padding(12)
            .background(Color.appCard)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }
}

// MARK: - Scroll offset observer
//
// iOS 18+: используем системный onScrollGeometryChange — он точно и быстро
// сообщает об изменении contentOffset ScrollView.
// iOS 16–17: падаем на PreferenceKey + GeometryReader внутри overlay.

private struct ScrollOffsetKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private struct ScrollOffsetObserver: ViewModifier {
    let action: (CGFloat) -> Void

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *) {
            content.onScrollGeometryChange(for: CGFloat.self) { geometry in
                geometry.contentOffset.y
            } action: { _, newValue in
                action(newValue)
            }
        } else {
            content
                .coordinateSpace(name: "formScroll")
                .overlay(
                    GeometryReader { outer in
                        // Позиция ScrollView на экране; изменение minY контента
                        // относительно неё эквивалентно contentOffset.y
                        // (с инвертированным знаком).
                        Color.clear.preference(
                            key: ScrollOffsetKey.self,
                            value: -outer.frame(in: .named("formScroll")).minY
                        )
                    }
                    .frame(width: 0, height: 0)
                )
                .onPreferenceChange(ScrollOffsetKey.self) { value in
                    action(value)
                }
        }
    }
}

// MARK: - RouteFormToolbar

struct RouteFormToolbar: View {
    let isFavorite: Bool
    let canDelete: Bool
    let onSettings: () -> Void
    let onFavoriteToggle: () -> Void
    let onShare: () -> Void
    let onDuplicate: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            ToolbarPillButton(
                systemImage: "slider.horizontal.3",
                tint: .white,
                background: Color.appAccent,
                action: onSettings
            )

            // Центральная группа: «В избранное», «Поделиться», «Дублировать»
            // — объединены в одну капсулу с тонкими разделителями.
            ToolbarPillGroup {
                ToolbarGroupButton(
                    systemImage: isFavorite ? "heart.fill" : "heart",
                    tint: isFavorite ? Color.appDanger : .primary,
                    action: onFavoriteToggle
                )
                ToolbarGroupDivider()
                ToolbarGroupButton(
                    systemImage: "square.and.arrow.up",
                    tint: .primary,
                    action: onShare
                )
                ToolbarGroupDivider()
                ToolbarGroupButton(
                    systemImage: "doc.on.doc",
                    tint: .primary,
                    action: onDuplicate
                )
            }

            if canDelete {
                ToolbarPillButton(
                    systemImage: "trash",
                    tint: Color.appDanger,
                    background: Color.appCard,
                    action: onDelete
                )
            }
        }
        .padding(.horizontal, 14)
        .padding(.bottom, 18)
    }
}

struct ToolbarPillButton: View {
    let systemImage: String
    let tint: Color
    let background: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(tint)
                // Крайние кнопки — только необходимое место (иконка +
                // горизонтальный отступ). Свободное место отдаём центральной
                // группе.
                .padding(.horizontal, 20)
                .frame(height: 44)
                .background(background)
                .clipShape(Capsule())
                .shadow(color: .black.opacity(0.08), radius: 4, y: 1)
        }
        .buttonStyle(.plain)
    }
}

/// Контейнер для группы кнопок в одной капсуле с общей тенью.
/// Разворачивается на всё свободное место, крайние пилюли остаются компактными.
struct ToolbarPillGroup<Content: View>: View {
    @ViewBuilder let content: () -> Content

    var body: some View {
        HStack(spacing: 0) {
            content()
        }
        .frame(maxWidth: .infinity)
        .frame(height: 44)
        .background(Color.appCard)
        .clipShape(Capsule())
        .shadow(color: .black.opacity(0.08), radius: 4, y: 1)
    }
}

struct ToolbarGroupButton: View {
    let systemImage: String
    let tint: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(tint)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

struct ToolbarGroupDivider: View {
    var body: some View {
        Rectangle()
            .fill(Color.primary.opacity(0.12))
            .frame(width: 0.5, height: 22)
    }
}

// MARK: - ShareSheet wrapper

private struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }
    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}

// MARK: - Money formatter

enum FormatMoney {
    static func rub(_ value: Double?) -> String {
        guard let value = value else { return "—" }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
        formatter.groupingSeparator = " "
        formatter.decimalSeparator = ","
        return (formatter.string(from: NSNumber(value: value)) ?? "0,00") + " ₽"
    }
}

// MARK: - Salary details sheet

private struct SalaryDetailsSheet: View {
    @ObservedObject var vm: FormViewModelWrapper
    @Environment(\.dismiss) private var dismiss
    @State private var showSettingsStub: Bool = false

    private struct Row: Identifiable {
        let id = UUID()
        let title: String
        let value: Double
    }

    /// Ненулевые составляющие ЗП — не показываем поля, где 0 или nil.
    private var nonZeroRows: [Row] {
        let all: [(String, Double?)] = [
            ("По тарифу",          vm.salaryTariff),
            ("Ночные",             vm.salaryNight),
            ("Зональная надбавка", vm.salaryZonal),
            ("Пассажирские",       vm.salaryPassenger),
            ("Праздничные",        vm.salaryHoliday),
            ("Надбавки за поезда", vm.salaryTrains),
            ("В одно лицо",        vm.salaryOnePerson),
            ("Прочие надбавки",    vm.salaryOther),
            ("Сверхотдых",         vm.salaryOverRest),
        ]
        return all.compactMap { title, value in
            guard let v = value, v != 0 else { return nil }
            return Row(title: title, value: v)
        }
    }

    // MARK: - Состояние расчёта

    private var hasWorkTime: Bool {
        let start = vm.route?.basicData.timeStartWork?.int64Value ?? 0
        let end   = vm.route?.basicData.timeEndWork?.int64Value ?? 0
        return start > 0 && end > 0
    }

    /// Эвристика: нет тарифной ставки → тариф за тарифное время = 0/nil,
    /// хотя время работы указано (тогда вкладчиком расчёта быть нечему).
    private var hasTariffRate: Bool {
        guard hasWorkTime else { return true } // первым пропустим вопрос времени
        let tariff = vm.salaryTariff ?? 0
        return tariff > 0
    }

    var body: some View {
        NavigationStack {
            Group {
                if !hasWorkTime {
                    emptyState(
                        icon: "clock.badge.exclamationmark",
                        title: "Время работы не указано",
                        message: "Укажите начало и окончание работы — расчёт выполнится автоматически.",
                        actionTitle: "Закрыть"
                    ) { dismiss() }
                } else if !hasTariffRate {
                    emptyState(
                        icon: "rublesign.circle",
                        title: "Не указана тарифная ставка",
                        message: "Для расчёта нужна часовая тарифная ставка. Откройте настройки заработной платы и заполните её.",
                        actionTitle: "Настройки ЗП"
                    ) { showSettingsStub = true }
                } else {
                    detailsList
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.appBg)
            .navigationTitle("Расчёт за маршрут")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: { showSettingsStub = true }) {
                        Image(systemName: "gearshape")
                            .foregroundStyle(Color.appAccent)
                    }
                    .accessibilityLabel("Настройки ЗП")
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Готово") { dismiss() }
                }
            }
            .alert("Раздел в разработке", isPresented: $showSettingsStub) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("Экран настроек заработной платы появится в следующих обновлениях.")
            }
        }
    }

    // MARK: - Детализация

    private var detailsList: some View {
        List {
            Section {
                HStack {
                    Text("Итого").font(.headline)
                    Spacer()
                    Text(FormatMoney.rub(vm.salaryTotal))
                        .font(.title3.bold())
                        .foregroundStyle(Color.appAccent)
                }
            }

            if nonZeroRows.isEmpty {
                Section {
                    Text("Все составляющие расчёта равны нулю.")
                        .foregroundStyle(.secondary)
                        .font(.footnote)
                }
            } else {
                Section("Детализация") {
                    ForEach(nonZeroRows) { row in
                        HStack {
                            Text(row.title).foregroundStyle(.secondary)
                            Spacer()
                            Text(FormatMoney.rub(row.value))
                        }
                    }
                }
            }
        }
    }

    // MARK: - Empty state

    @ViewBuilder
    private func emptyState(
        icon: String,
        title: String,
        message: String,
        actionTitle: String,
        action: @escaping () -> Void
    ) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 44, weight: .regular))
                    .foregroundStyle(Color.appAccent)
                    .padding(.top, 32)
                Text(title)
                    .font(.headline)
                    .multilineTextAlignment(.center)
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                Button(action: action) {
                    Text(actionTitle)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 11)
                        .background(Color.appAccent)
                        .clipShape(Capsule())
                }
                .padding(.top, 8)
                Spacer(minLength: 24)
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 20)
        }
    }
}

// MARK: - Rest details sheet

private struct RestDetailsSheet: View {
    @ObservedObject var vm: FormViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    /// Есть ли валидные время начала и окончания работы (окончание строго позже).
    private var hasWorkTime: Bool {
        let start = vm.route?.basicData.timeStartWork?.int64Value ?? 0
        let end   = vm.route?.basicData.timeEndWork?.int64Value ?? 0
        return start > 0 && end > 0 && end > start
    }

    var body: some View {
        NavigationStack {
            Group {
                if !hasWorkTime {
                    emptyState
                } else {
                    detailsList
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.appBg)
            .navigationTitle("Отдых")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Готово") { dismiss() }
                }
            }
        }
    }

    // MARK: - Empty state (нет данных о работе)

    private var emptyState: some View {
        ScrollView {
            VStack(spacing: 16) {
                Image(systemName: "clock.badge.exclamationmark")
                    .font(.system(size: 44, weight: .regular))
                    .foregroundStyle(Color.appAccent)
                    .padding(.top, 32)
                Text("Время работы не указано")
                    .font(.headline)
                    .multilineTextAlignment(.center)
                Text("Укажите начало и окончание работы — тогда отдых будет рассчитан автоматически.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                Button(action: { dismiss() }) {
                    Text("Закрыть")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 11)
                        .background(Color.appAccent)
                        .clipShape(Capsule())
                }
                .padding(.top, 8)
                Spacer(minLength: 24)
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 20)
        }
    }

    // MARK: - Details list

    private var detailsList: some View {
        List {
            if vm.isRestAtPO {
                poHeaderSection
                restSection(
                    title: "Короткий отдых",
                    badge: "MIN",
                    duration: vm.shortRestDuration,
                    endTime: vm.shortRestEnd
                )
                restSection(
                    title: "Полный отдых",
                    badge: "MAX",
                    duration: vm.fullRestDuration,
                    endTime: vm.fullRestEnd
                )
            } else {
                if let err = vm.restErrorMessage, !err.isEmpty {
                    Section {
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundStyle(.orange)
                            Text(err)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                homeHeaderSection
                if vm.chainSize > 1, let poRest = vm.chainPORestTotal, poRest > 0 {
                    Section("Отдых в ПО") {
                        HStack {
                            Text("Суммарно").foregroundStyle(.secondary)
                            Spacer()
                            Text(TimeFormatter.formatDuration(ms: poRest))
                        }
                        HStack {
                            Text("Количество отдыхов").foregroundStyle(.secondary)
                            Spacer()
                            Text("\(max(vm.chainSize - 1, 0))")
                        }
                    }
                }
                restSection(
                    title: "Домашний отдых",
                    iconSystemName: "house.fill",
                    duration: vm.homeRestDuration,
                    endTime: vm.homeRestEnd
                )
            }
        }
    }

    // MARK: - Headers

    @ViewBuilder
    private var poHeaderSection: some View {
        Section {
            HStack {
                Text("Продолжительность работы").foregroundStyle(.secondary)
                Spacer()
                Text(vm.currentWorkDuration.map { TimeFormatter.formatDuration(ms: $0) } ?? "—")
                    .font(.headline)
                    .foregroundStyle(Color.appAccent)
            }
        } footer: {
            Text("Отдых в пункте оборота рассчитывается от продолжительности работы на текущем маршруте.")
                .font(.footnote)
        }
    }

    @ViewBuilder
    private var homeHeaderSection: some View {
        Section {
            HStack {
                Text("Суммарно").foregroundStyle(.secondary)
                Spacer()
                Text(vm.chainWorkTotal.map { TimeFormatter.formatDuration(ms: $0) } ?? "—")
                    .font(.headline)
                    .foregroundStyle(Color.appAccent)
            }
            if vm.chainSize > 1 {
                HStack {
                    Text("Маршрутов в цепочке").foregroundStyle(.secondary)
                    Spacer()
                    Text("\(vm.chainSize)")
                }
            }
        } header: {
            Text("Отработано")
        } footer: {
            Text(vm.chainSize > 1
                 ? "Домашний отдых рассчитывается по суммарной работе всей цепочки маршрутов с учётом отдыха в ПО между ними."
                 : "Домашний отдых рассчитывается по продолжительности работы текущего маршрута.")
                .font(.footnote)
        }
    }

    // MARK: - Rest block row

    @ViewBuilder
    private func restSection(
        title: String,
        badge: String? = nil,
        iconSystemName: String? = nil,
        duration: Int64?,
        endTime: Int64?
    ) -> some View {
        Section {
            HStack {
                Text("Продолжительность").foregroundStyle(.secondary)
                Spacer()
                Text(duration.map { TimeFormatter.formatDuration(ms: $0) } ?? "—")
            }
            HStack {
                Text("Окончание").foregroundStyle(.secondary)
                Spacer()
                Text(endTime.map { TimeFormatter.formatDateTime(ms: $0) } ?? "—")
            }
        } header: {
            HStack(spacing: 8) {
                if let badge = badge {
                    Text(badge)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.appAccent)
                        .clipShape(Capsule())
                }
                if let icon = iconSystemName {
                    Image(systemName: icon).foregroundStyle(Color.appAccent)
                }
                Text(title)
            }
        }
    }
}

// MARK: - Route settings sheet

private struct RouteSettingsSheet: View {
    @ObservedObject var vm: FormViewModelWrapper
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    SettingsToggleRow(
                        title: "В одно лицо",
                        subtitle: "Надбавка за работу без помощника",
                        isOn: Binding(
                            get: { vm.isOnePersonOperation },
                            set: { vm.setOnePersonOperation($0) }
                        )
                    )
                }
                .background(Color.appCard)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(.horizontal, 16)
                .padding(.top, 20)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.appBg)
            .navigationTitle("Настройки маршрута")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Готово") { dismiss() }
                }
            }
        }
    }
}

private struct SettingsToggleRow: View {
    let title: String
    let subtitle: String
    @Binding var isOn: Bool
    var disabled: Bool = false

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 15))
                    .foregroundStyle(disabled ? .secondary : .primary)
                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(Color.appSuccess)
                .disabled(disabled)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 11)
    }
}
