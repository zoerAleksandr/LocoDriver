import SwiftUI
import ComposeApp

// Форма маршрута — SCREEN_SPECS §5.
// Макет: design/src/ios-screens.jsx → IOSScreenRoute; night-warn.jsx → RouteNightWarn.
// Логика и расчёты — Kotlin `RouteFormIosViewModel` (аналог Android FormViewModel).
// По спеке (макет противоречит): топ-бар «Готово» слева и шестерёнка справа, без «Сохранить»
// (автосейв, §3.3/§5.2); секции единственного числа («ЛОКОМОТИВ», «ПОЕЗД», …).

struct FormView: View {
    let routeId: String?
    var isCopy: Bool = false
    var isSharedPreview: Bool = false

    @StateObject private var vm = RouteFormViewModelWrapper()
    @Environment(\.dynamicTypeSize) private var typeSize

    private enum ActiveSheet: Identifiable {
        case calc, rest
        case picker(RouteTimeField)
        var id: String {
            switch self {
            case .calc: return "calc"
            case .rest: return "rest"
            case let .picker(f): return "picker-\(f.rawValue)"
            }
        }
    }

    private struct PendingDelete: Identifiable {
        let id = UUID()
        let title: String
        let message: String
        let action: () -> Void
    }

    @State private var sheet: ActiveSheet?
    @State private var pendingDelete: PendingDelete?
    @State private var confirm: RouteConfirmModel?
    @State private var showArrivalConfirm = false
    @State private var sharePayload: SharePayload?
    @State private var nightWarnHidden = false
    @State private var numberText = ""
    @State private var notesText = ""
    @State private var textsLoaded = false
    @State private var started = false
    @State private var onePerson = false
    @State private var barHidden = false

    var body: some View { allDialogs }

    private var mainView: some View {
        VStack(spacing: 0) {
            navBar
            if let state = vm.state, !state.isLoading {
                content(state)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .overlay(alignment: .bottom) { if let state = vm.state, !state.isLoading { bottomBar(state) } }
        .onAppear {
            vm.eventHandler = handle
            // onAppear повторяется при возврате с дочернего экрана — запускаем загрузку один раз.
            guard !started else { return }
            started = true
            vm.start(routeId: routeId, isCopy: isCopy, sharedPreview: isSharedPreview)
        }
        .onChange(of: vm.state?.isLoading) { _ in loadTextsIfNeeded() }
        .onChange(of: onePerson) { value in
            if value != (vm.state?.isOnePerson ?? false) { vm.setOnePerson(value) }
        }
        .onChange(of: vm.state?.isOnePerson) { value in
            if let value = value, value != onePerson { onePerson = value }
        }
        .onChange(of: vm.state?.timeStartWork) { _ in nightWarnHidden = false }
        .onChange(of: vm.state?.timeEndWork) { _ in nightWarnHidden = false }
        .sheet(item: $sheet) { active in sheetContent(active) }
        .sheet(item: $sharePayload) { RouteShareSheet(text: $0.text, subject: $0.subject) }
        .background(passenger12hHost)
        .background(sharedPreviewHost)
    }

    private var withDuplicateAndCopy: some View {
        mainView
            .overlay {
                if let model = confirm {
                    RouteConfirmDialog(model: model, onCancel: cancelConfirm) { action in
                        confirm = nil
                        action.perform()
                    }
                }
            }
            .animation(.easeInOut(duration: 0.15), value: confirm?.id)
            .onChange(of: vm.state?.duplicate != nil) { has in
                if has { showDuplicateConfirm() }
            }
    }

    private var allDialogs: some View { withDuplicateAndCopy
        .alert(pendingDelete?.title ?? "", isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } })) {
            Button("Да, удалить", role: .destructive) { pendingDelete?.action(); pendingDelete = nil }
            Button("Отмена", role: .cancel) { pendingDelete = nil }
        } message: {
            Text(pendingDelete?.message ?? "")
        }
        .alert("Изменить время явки?", isPresented: $showArrivalConfirm) {
            Button("Изменить") {
                vm.disableWorkStartByArrival()
                sheet = .picker(.start)
            }
            Button("Отмена", role: .cancel) {}
        } message: {
            Text("Сейчас явка определяется прибытием пассажиром. Если задать время вручную, режим «явка по прибытию» отключится.")
        }
    }

    // MARK: Подтверждения (§24.3)

    private func showCopyConfirmSheet() {
        confirm = RouteConfirmModel(
            title: "Создать копию маршрута?",
            message: "Откроется редактирование копии — исходный маршрут не изменится.",
            actions: [.init(title: "Создать копию", role: .normal) {
                if let id = vm.state?.basicId { AppRouter.shared.showRouteForm(basicId: id, isMakeCopy: true) }
            }]
        )
    }

    private func showDeleteRouteSheet() {
        confirm = RouteConfirmModel(
            title: "Удалить маршрут?",
            message: deleteRouteMessage,
            actions: [.init(title: "Да, удалить", role: .destructive) { vm.deleteRoute() }]
        )
    }

    private func showClearSheet(_ field: RouteTimeField) {
        confirm = RouteConfirmModel(
            title: field.deleteTitle,
            message: currentValueText(field),
            actions: [.init(title: "Удалить значение", role: .destructive) { setTime(field, nil) }]
        )
    }

    /// «Маршрут с такой явкой уже сохранён» (§5.6): «Отмена»/тап вне диалога = ничего не сохраняется.
    private func showDuplicateConfirm() {
        confirm = RouteConfirmModel(
            title: "Маршрут с такой явкой уже сохранён.",
            message: nil,
            actions: [
                .init(title: "Заменить", role: .destructive) { vm.confirmReplaceDuplicate() },
                .init(title: "Оставить оба", role: .normal) { vm.confirmKeepBothDuplicates() },
            ]
        )
    }

    private func cancelConfirm() {
        confirm = nil
        if vm.state?.duplicate != nil { vm.dismissDuplicate() }
    }

    // MARK: События

    private func handle(_ event: RouteFormEvent) {
        switch event.kind {
        case "saved", "exit":
            AppRouter.shared.back()
        case "snackbar":
            if let text = event.text { ToastCenter.shared.show(text) }
        case "share":
            if let text = event.text { sharePayload = SharePayload(text: text, subject: event.subject ?? "Маршрут") }
        default:
            break
        }
    }

    private func loadTextsIfNeeded() {
        guard !textsLoaded, let state = vm.state, !state.isLoading else { return }
        textsLoaded = true
        numberText = state.number
        notesText = state.notes
        onePerson = state.isOnePerson
    }

    // MARK: Топ-бар (§5.3 п.1)

    private var navBar: some View {
        ZStack {
            Text(vm.state?.title ?? (routeId == nil ? "Новый маршрут" : "Маршрут"))
                .dsTextStyle(.navTitle)
                .lineLimit(1)
                .padding(.horizontal, 96)
            HStack {
                DSNavPillButton(title: "Готово") { vm.onSaveClick() }
                Spacer()
                DSNavPillButton(icon: .gear, accessibilityLabel: "Настройки маршрута") {
                    AppRouter.shared.showSettingsSection(.routeForm)
                }
            }
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.vertical, 12)
        .background(DSColor.bg)
    }

    // MARK: Содержимое

    private func content(_ state: RouteFormState) -> some View {
        let timeFormat = RouteTimeFormat(offsetSeconds: Int(state.displayOffsetSeconds))
        return ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if let error = state.errorMessage { errorBanner(error) }

                DSGroupHeader(title: "ОСНОВНЫЕ ДАННЫЕ", topSpacing: DSMetrics.groupHeadFirst) {
                    if state.isBusinessTrip {
                        DSTag(text: "КОМ", color: DSColor.businessTrip, background: DSColor.businessTrip.opacity(0.14))
                    }
                }
                numberCard
                if state.showOnePersonSwitch { onePersonCard(state).padding(.top, DSMetrics.cardGap) }
                tiles(state).padding(.top, DSMetrics.cardGap)

                if state.showPartner { partnersBlock(state) }
                timeBlock(state, timeFormat)

                if state.showLocomotive {
                    childSection(
                        title: "ЛОКОМОТИВ", addTitle: "Добавить локомотив", icon: .locomotive,
                        rows: (state.locomotives as? [RouteChildRow]) ?? [],
                        open: { id in AppRouter.shared.showLocoForm(basicId: state.basicId, locoId: id) },
                        add: { AppRouter.shared.showLocoForm(basicId: state.basicId, locoId: nil) },
                        confirm: { id in
                            pendingDelete = PendingDelete(title: "Удалить локомотив?", message: "Локомотив будет убран из маршрута. Это действие нельзя отменить.") { vm.deleteLoco(id) }
                        }
                    )
                }
                if state.showTrain {
                    childSection(
                        title: "ПОЕЗД", addTitle: "Добавить поезд", icon: .train,
                        rows: (state.trains as? [RouteChildRow]) ?? [],
                        open: { id in AppRouter.shared.showTrainForm(basicId: state.basicId, trainId: id) },
                        add: { AppRouter.shared.showTrainForm(basicId: state.basicId, trainId: nil) },
                        confirm: { id in
                            pendingDelete = PendingDelete(title: "Удалить поезд?", message: "Поезд будет убран из маршрута. Это действие нельзя отменить.") { vm.deleteTrain(id) }
                        }
                    )
                }
                if state.showOtherWork {
                    // Текст «Добавить прочая работа» — как в Android (`title.lowercase()`), по спеке §5.3 п.9.
                    childSection(
                        title: "ПРОЧАЯ РАБОТА", addTitle: "Добавить прочая работа", icon: .mapPin,
                        rows: (state.otherWorks as? [RouteChildRow]) ?? [],
                        open: { id in AppRouter.shared.showOtherWorkForm(basicId: state.basicId, otherWorkId: id) },
                        add: { AppRouter.shared.showOtherWorkForm(basicId: state.basicId, otherWorkId: nil) },
                        confirm: { id in
                            pendingDelete = PendingDelete(title: "Удалить запись?", message: "Запись прочей работы будет убрана из маршрута. Это действие нельзя отменить.") { vm.deleteOtherWork(id) }
                        }
                    )
                }
                if state.showPassenger {
                    childSection(
                        title: "ПАССАЖИРОМ", addTitle: "Добавить пассажиром", icon: .passenger,
                        rows: (state.passengers as? [RouteChildRow]) ?? [],
                        open: { id in AppRouter.shared.showPassengerForm(basicId: state.basicId, passengerId: id) },
                        add: { AppRouter.shared.showPassengerForm(basicId: state.basicId, passengerId: nil) },
                        confirm: { id in
                            pendingDelete = PendingDelete(title: "Удалить поездку пассажиром?", message: "Запись будет убрана из маршрута. Это действие нельзя отменить.") { vm.deletePassenger(id) }
                        }
                    )
                }

                DSGroupHeader("ЗАМЕТКИ")
                notesCard
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.bottom, 120) // место под плавающую нижнюю панель
        }
        // Панель уезжает вниз при прокрутке вниз и возвращается при прокрутке вверх (§5.3 п.11).
        .simultaneousGesture(
            DragGesture(minimumDistance: 16)
                .onChanged { value in
                    let hide = value.translation.height < 0
                    if hide != barHidden { withAnimation(.easeInOut(duration: 0.6)) { barHidden = hide } }
                }
        )
    }

    private func errorBanner(_ text: String) -> some View {
        Text(text)
            .font(DSFont.sans(14, .medium))
            .foregroundColor(DSColor.accentInk)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 14)
            .background(LinearGradient(colors: [DSColor.danger.opacity(0.9), DSColor.danger.opacity(0.72)], startPoint: .leading, endPoint: .trailing))
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
            .padding(.top, 8)
    }

    // MARK: Номер и «в одно лицо»

    private var numberCard: some View {
        DSCard {
            HStack(spacing: 12) {
                Text("Номер").font(DSFont.sans(15)).foregroundColor(DSColor.textMuted)
                TextField("Введите номер", text: $numberText)
                    .font(DSFont.mono(20, .semibold))
                    .multilineTextAlignment(.trailing)
                    .keyboardType(.numberPad)
                    .onChange(of: numberText) { vm.setNumber($0) }
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 16)
        }
    }

    private func onePersonCard(_ state: RouteFormState) -> some View {
        DSCard {
            // Вся строка кликабельна (§5.3 п.5); сам переключатель только отображает состояние.
            HStack {
                Text("Работа в одно лицо").font(DSFont.sans(16, .medium)).foregroundColor(DSColor.text)
                Spacer(minLength: 8)
                Toggle("", isOn: $onePerson)
                    .labelsHidden()
                    .tint(DSColor.accent)
                    .allowsHitTesting(false)
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
            .onTapGesture { onePerson.toggle() }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("Работа в одно лицо"))
            .accessibilityValue(Text(onePerson ? "Включено" : "Выключено"))
            .accessibilityAddTraits(.isButton)
        }
    }

    // MARK: Плитки «Расчёт» / «Отдых»

    private func tiles(_ state: RouteFormState) -> some View {
        let calc = tile(
            icon: Text(state.currencySymbol).font(DSFont.mono(15, .bold)).foregroundColor(DSColor.textMuted),
            label: "Расчёт",
            value: state.salaryTileText,
            isMono: true,
            isMuted: !state.salaryTileIsCalculated
        ) { sheet = .calc }
        let restTile = tile(
            icon: (state.restPointOfTurnover ? DSIcon.bed : DSIcon.home).image.font(.system(size: 15)).foregroundColor(DSColor.textMuted),
            label: "Отдых",
            value: state.restPointOfTurnover ? "В пункте оборота" : "Домашний",
            isMono: false,
            isMuted: false
        ) { sheet = .rest }
        return Group {
            if typeSize >= .xxxLarge {
                VStack(spacing: 10) { calc; restTile }
            } else {
                HStack(spacing: 10) { calc; restTile }
            }
        }
    }

    private func tile<Icon: View>(icon: Icon, label: String, value: String, isMono: Bool, isMuted: Bool, action: @escaping () -> Void) -> some View {
        DSCard {
            Button(action: action) {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        HStack(spacing: 6) {
                            icon
                            Text(label).font(DSFont.sans(13)).foregroundColor(DSColor.textMuted)
                        }
                        Spacer(minLength: 4)
                        DSIcon.chevronRight.image.font(.system(size: 12, weight: .semibold)).foregroundColor(DSColor.textFaint)
                    }
                    Text(value)
                        .font(isMono ? DSFont.mono(22, .heavy) : DSFont.sans(18, .bold))
                        .tracking(isMono ? -0.5 : 0)
                        .foregroundColor(isMuted ? DSColor.textFaint : DSColor.text)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
                .padding(18)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: Напарники (§5.3 п.7)

    private func partnersBlock(_ state: RouteFormState) -> some View {
        let partners = (state.partners as? [RoutePartnerRow]) ?? []
        return VStack(alignment: .leading, spacing: 0) {
            DSGroupHeader("НАПАРНИКИ")
            DSCard {
                ForEach(Array(partners.enumerated()), id: \.element.id) { _, partner in
                    PartnerRowView(partner: partner) {
                        if let source = partner.sourcePartnerId {
                            AppRouter.shared.show(.partnerEditor(partnerId: source))
                        }
                    } onDelete: {
                        pendingDelete = PendingDelete(title: "Удалить напарника?", message: partner.shortName) { vm.deletePartner(partner.id) }
                    }
                    DSDivider(inset: 56)
                }
                DSAddRow(title: "Добавить напарника", icon: .plus) {
                    let id = state.basicId
                    vm.preSave { AppRouter.shared.show(.partnerPicker(basicId: id)) }
                }
            }
        }
    }

    // MARK: Время работы (§5.3 п.8)

    private func timeBlock(_ state: RouteFormState, _ f: RouteTimeFormat) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            DSGroupHeader("ВРЕМЯ РАБОТЫ")
            DSCard {
                if let s = state.summary { summary(s, decimal: false) }
                timeRow(.start, state.timeStartWork.ms, f, tag: state.startWorkByArrival ? "по прибытию пассажиром" : nil, state: state)
                DSDivider()
                timeRow(.end, state.timeEndWork.ms, f, tag: nil, state: state)
                if state.showBreak {
                    DSDivider()
                    timeRow(.breakStart, state.timeStartBreak.ms, f, tag: nil, state: state)
                    DSDivider()
                    timeRow(.breakEnd, state.timeEndBreak.ms, f, tag: nil, state: state)
                }
            }
            if let warn = (state.nightWarn as? [NightWarnRowUi]), !warn.isEmpty, !nightWarnHidden {
                RouteNightWarnView(rows: warn, timeFormat: f) { nightWarnHidden = true }
                    .padding(.top, 10)
            }
        }
    }

    private func summary(_ s: WorkSummaryUi, decimal: Bool) -> some View {
        VStack(spacing: 8) {
            if let v = s.passengerBeforeStart {
                summaryMain("Пассажиром до явки", v)
                Rectangle().fill(DSColor.accent.opacity(0.15)).frame(height: 1)
            }
            if let v = s.waitingBeforeStart {
                summaryMain("Ожидание до явки", v)
                Rectangle().fill(DSColor.accent.opacity(0.15)).frame(height: 1)
            }
            summaryMain("Отработано", s.worked)
            if let v = s.night { summarySub(.moon, "В ночное время", v) }
            if let v = s.passengerInside { summarySub(.passenger, "Пассажиром", v) }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(DSColor.accentSoft)
    }

    private func summaryMain(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title).font(DSFont.sans(15, .semibold)).foregroundColor(DSColor.accent)
            Spacer(minLength: 8)
            Text(value).font(DSFont.mono(20, .bold)).foregroundColor(DSColor.accent)
        }
    }

    private func summarySub(_ icon: DSIcon, _ title: String, _ value: String) -> some View {
        HStack {
            HStack(spacing: 8) {
                icon.image.font(.system(size: 13)).foregroundColor(DSColor.text)
                Text(title).font(DSFont.sans(14)).foregroundColor(DSColor.text)
            }
            Spacer(minLength: 8)
            Text(value).font(DSFont.mono(14, .bold)).foregroundColor(DSColor.text)
        }
    }

    /// Строка времени как в PWA: подпись слева, значение справа (пусто — «Выбрать», заполнено — акцентный mono).
    private func timeRow(_ field: RouteTimeField, _ value: Int64?, _ f: RouteTimeFormat, tag: String?, state: RouteFormState) -> some View {
        HStack(alignment: .center, spacing: DSSpacing.sm) {
            VStack(alignment: .leading, spacing: 4) {
                Text(field.rowTitle).font(DSFont.sans(16)).foregroundColor(DSColor.text)
                if let tag = tag { DSTag(text: tag) }
            }
            Spacer(minLength: 8)
            if let value = value {
                Text(f.dateTimeFull(value))
                    .font(DSFont.mono(16, .medium))
                    .foregroundColor(DSColor.accent)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            } else {
                Text("Выбрать")
                    .font(DSFont.sans(16))
                    .foregroundColor(DSColor.textFaint)
            }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture {
            if field == .start && state.startWorkByArrival {
                showArrivalConfirm = true
            } else {
                sheet = .picker(field)
            }
        }
        .onLongPressGesture {
            if value != nil { showClearSheet(field) }
        }
    }

    private func setTime(_ field: RouteTimeField, _ ms: Int64?) {
        switch field {
        case .start: vm.setTimeStartWork(ms)
        case .end: vm.setTimeEndWork(ms)
        case .breakStart: vm.setTimeStartBreak(ms)
        case .breakEnd: vm.setTimeEndBreak(ms)
        }
    }

    private func currentValue(_ field: RouteTimeField) -> Int64? {
        guard let s = vm.state else { return nil }
        switch field {
        case .start: return s.timeStartWork.ms
        case .end: return s.timeEndWork.ms
        case .breakStart: return s.timeStartBreak.ms
        case .breakEnd: return s.timeEndBreak.ms
        }
    }

    private func currentValueText(_ field: RouteTimeField) -> String? {
        guard let s = vm.state, let v = currentValue(field) else { return nil }
        return RouteTimeFormat(offsetSeconds: Int(s.displayOffsetSeconds)).dateTime(v)
    }

    /// Начальное значение пикера: текущее; иначе для сдачи — явка, для перерыва — начало перерыва/явка, иначе «сейчас» (§5.3 п.8).
    private func pickerInitial(_ field: RouteTimeField) -> Date {
        if let v = currentValue(field) { return TimeFormatter.msToDate(v) }
        let start = vm.state?.timeStartWork.ms
        let breakStart = vm.state?.timeStartBreak.ms
        switch field {
        case .start: return Date()
        case .end, .breakStart: return start.map(TimeFormatter.msToDate) ?? Date()
        case .breakEnd: return (breakStart ?? start).map(TimeFormatter.msToDate) ?? Date()
        }
    }

    // MARK: Дочерние разделы (§5.3 п.9)

    private func childSection(
        title: String, addTitle: String, icon: DSIcon, rows: [RouteChildRow],
        open: @escaping (String) -> Void, add: @escaping () -> Void, confirm: @escaping (String) -> Void
    ) -> some View {
        let basicId = vm.state?.basicId ?? ""
        _ = basicId
        return VStack(alignment: .leading, spacing: 0) {
            DSGroupHeader(title)
            DSCard {
                ForEach(rows, id: \.id) { row in
                    HStack(spacing: DSMetrics.rowGap) {
                        DSIconAvatar(icon: icon, size: .lg, tone: .accent, shape: .square)
                        rowText(row)
                        Spacer(minLength: 4)
                        Button { confirm(row.id) } label: {
                            DSIcon.trash.image
                                .font(.system(size: 16))
                                .foregroundColor(DSColor.danger)
                                .frame(width: 34, height: 34)
                                .background(DSColor.danger.opacity(0.10))
                                .clipShape(Circle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(Text("Удалить"))
                    }
                    .padding(.leading, DSMetrics.rowPadX)
                    .padding(.trailing, 12)
                    .padding(.vertical, 12)
                    .contentShape(Rectangle())
                    .onTapGesture { open(row.id) }
                    DSDivider(inset: 56)
                }
                // «Добавить» → сначала сохранить маршрут, затем пустая форма (§5.2)
                DSAddRow(title: addTitle, icon: icon) { vm.preSave(add) }
            }
        }
    }

    private func rowText(_ row: RouteChildRow) -> some View {
        Group {
            if row.isMono {
                Text(row.title).font(DSFont.mono(15, .semibold)).foregroundColor(DSColor.text)
                    + Text(row.tail).font(DSFont.sans(15, .medium)).foregroundColor(DSColor.text)
            } else {
                Text(row.title + row.tail).font(DSFont.sans(15, .medium)).foregroundColor(DSColor.text)
            }
        }
        .lineLimit(1)
    }

    // MARK: Заметки

    private var notesCard: some View {
        DSCard {
            TextField("Опиши смену, если нужно…", text: $notesText, axis: .vertical)
                .font(DSFont.sans(15))
                .lineLimit(3...12)
                .padding(20)
                .frame(minHeight: 64, alignment: .topLeading)
                .onChange(of: notesText) { vm.setNotes($0) }
        }
    }

    // MARK: Нижняя панель (§5.3 п.11)

    private func bottomBar(_ state: RouteFormState) -> some View {
        ZStack(alignment: .bottom) {
            LinearGradient(colors: [DSColor.bg.opacity(0), DSColor.bg.opacity(0.85), DSColor.bg], startPoint: .top, endPoint: .bottom)
                .frame(height: 120)
                .allowsHitTesting(false)
            HStack(spacing: 8) {
                HStack {
                    barButton(.heart, filled: state.isFavorite, label: "Избранное") {
                        ToastCenter.shared.show(state.isFavorite ? "Убрали из избранного" : "Маршрут добавлен в избранное")
                        vm.toggleFavorite()
                    }
                    Spacer()
                    barButton(.share, filled: false, label: "Поделиться") { vm.share() }
                    Spacer()
                    barButton(.copy, filled: false, label: "Копировать") { showCopyConfirmSheet() }
                }
                .padding(.horizontal, 22)
                .frame(height: 52)
                .background(DSColor.surface)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(DSColor.border, lineWidth: 1))
                .dsShadow(.floating)

                Button { showDeleteRouteSheet() } label: {
                    DSIcon.trash.image
                        .font(.system(size: 20))
                        .foregroundColor(DSColor.danger)
                        .frame(width: 52, height: 52)
                        .background(DSColor.surface)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(DSColor.border, lineWidth: 1))
                        .dsShadow(.floating)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("Удалить маршрут"))
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.bottom, 12)
        }
        .ignoresSafeArea(edges: .bottom)
        .offset(y: barHidden ? 160 : 0)
    }

    private func barButton(_ icon: DSIcon, filled: Bool, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            (filled ? Image(systemName: "heart.fill") : icon.image)
                .font(.system(size: 20))
                .foregroundColor(filled ? DSColor.danger : DSColor.text)
                .frame(width: 40, height: 40)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(label))
    }

    private var deleteRouteMessage: String {
        guard let s = vm.state, let start = s.timeStartWork.ms else { return "" }
        return "от " + RouteTimeFormat(offsetSeconds: Int(s.displayOffsetSeconds)).dateTime(start)
    }

    // MARK: Шторки

    @ViewBuilder
    private func sheetContent(_ active: ActiveSheet) -> some View {
        switch active {
        case .calc:
            RouteCalcSheet(
                salary: vm.salary,
                onSalarySettings: { sheet = nil; AppRouter.shared.showSettingSalary() },
                onClose: { sheet = nil }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.hidden)
        case .rest:
            RouteRestSheet(
                rest: vm.rest,
                isPointOfTurnover: vm.state?.restPointOfTurnover ?? false,
                onChange: { vm.setRestPointOfTurnover($0) },
                onRestSettings: { sheet = nil; AppRouter.shared.showSettingsSection(.rest) },
                onClose: { sheet = nil }
            )
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.hidden)
        case let .picker(field):
            DSDateTimeSheet(
                title: field.pickerTitle,
                timeZone: RouteTimeFormat(offsetSeconds: Int(vm.state?.displayOffsetSeconds ?? 10800)).timeZone,
                initial: pickerInitial(field),
                recent: vm.recentTimes(field.recentKey).map(TimeFormatter.msToDate),
                onDone: { ms in
                    vm.rememberTime(field.recentKey, ms)
                    setTime(field, ms)
                    sheet = nil
                },
                onCancel: { sheet = nil }
            )
            .presentationDetents([.height(380)])
            .presentationDragIndicator(.hidden)
        }
    }

    /// Passenger12h (§5.5) — состояние во ViewModel; закрытие свайпом = «Нет».
    private var passenger12hHost: some View {
        Color.clear.sheet(isPresented: Binding(
            get: { vm.state?.showPassenger12h ?? false },
            set: { if !$0 && (vm.state?.showPassenger12h ?? false) { vm.dismissPassenger12h() } }
        )) {
            if let s = vm.state {
                let tz = RouteTimeFormat(offsetSeconds: Int(s.displayOffsetSeconds)).timeZone
                RoutePassenger12hSheet(
                    suggest: { vm.suggestStations($0) },
                    initialDeparture: TimeFormatter.msToDate(s.p12Departure),
                    initialArrival: TimeFormatter.msToDate(s.p12Arrival),
                    initialStation: s.p12StationDeparture,
                    workStart: s.timeStartWork.ms.map(TimeFormatter.msToDate),
                    workEnd: s.timeEndWork.ms.map(TimeFormatter.msToDate),
                    timeZone: tz,
                    onSettings: { vm.dismissPassenger12h(); AppRouter.shared.showSettingsSection(.route) },
                    onNo: { vm.dismissPassenger12h() },
                    onSave: { dep, arr, td, ta in vm.savePassenger12h(departure: dep, arrival: arr, timeDeparture: td, timeArrival: ta) }
                )
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.hidden)
            }
        }
    }

    /// «Получен новый маршрут» (§5.6).
    private var sharedPreviewHost: some View {
        Color.clear.sheet(isPresented: Binding(
            get: { vm.state?.isSharedPreview ?? false },
            set: { if !$0 && (vm.state?.isSharedPreview ?? false) { vm.hideSharedSheet() } }
        )) {
            RouteSharedPreviewSheet(
                onView: { vm.hideSharedSheet() },
                onSave: { vm.saveSharedRouteAndExit() },
                onCancel: { vm.cancelSharedPreview() }
            )
            .presentationDetents([.height(300)])
            .presentationDragIndicator(.hidden)
        }
    }
}

// MARK: - Строка напарника

private struct PartnerRowView: View {
    let partner: RoutePartnerRow
    let onTap: () -> Void
    let onDelete: () -> Void

    @State private var offset: CGFloat = 0

    var body: some View {
        HStack(spacing: DSMetrics.rowGap) {
            Text(partner.initials)
                .font(DSFont.sans(13, .semibold))
                .foregroundColor(DSColor.accent)
                .frame(width: 32, height: 32)
                .background(DSColor.accentSoft)
                .clipShape(Circle())
            Text(partner.shortName).font(DSFont.sans(15, .medium)).foregroundColor(DSColor.text).lineLimit(1)
            Spacer(minLength: 4)
            if let tab = partner.tabNumber {
                Text("таб. \(tab)").font(DSFont.mono(13)).foregroundColor(DSColor.textMuted)
            }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, 10)
        .contentShape(Rectangle())
        .offset(x: offset)
        .onTapGesture(perform: onTap)
        // Свайп влево → подтверждение «Удалить напарника?» (§5.3 п.7)
        .simultaneousGesture(
            DragGesture(minimumDistance: 24)
                .onChanged { value in
                    if value.translation.width < 0 { offset = max(value.translation.width, -80) }
                }
                .onEnded { value in
                    if value.translation.width < -60 { onDelete() }
                    withAnimation(.easeOut(duration: 0.2)) { offset = 0 }
                }
        )
    }
}
