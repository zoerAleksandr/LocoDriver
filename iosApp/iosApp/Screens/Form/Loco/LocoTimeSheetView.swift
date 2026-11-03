import SwiftUI
import ComposeApp

// Шторка установки времени приёмки/сдачи — SCREEN_SPECS §2.
// Макет: design/src/time-sheet.jsx → TimeSheetIOS. Расчёты — Kotlin `LocoTimeSheetIosViewModel`.

struct LocoTimeSheetView: View {
    let kind: String
    let state: LocoScreenState
    let routeId: String
    let vm: LocoScreenViewModelWrapper

    @StateObject private var sheet = LocoTimeSheetViewModelWrapper()
    @Environment(\.dismiss) private var dismiss
    @State private var started = false
    @State private var blink = false
    @State private var pickerRow: PickerRow?
    @State private var deleteRow: TimeRowUi?
    @State private var refPicker: RefPickerRequest?

    struct PickerRow: Identifiable {
        let row: TimeRowUi
        var id: String { row.id }
    }

    struct RefPickerRequest: Identifiable {
        enum Kind { case series, station }
        let kind: Kind
        let editId: String?
        var id: String { "\(kind)-\(editId ?? "")" }
    }

    private var isAcceptance: Bool { kind == "acceptance" }
    private var fmt: RouteTimeFormat { RouteTimeFormat(offsetSeconds: Int(state.displayOffsetSeconds)) }

    var body: some View {
        let s = sheet.state
        VStack(spacing: 0) {
            header(s)
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    contextFields(s)
                    if s.showNormMode { normMode(s) }
                    ForEach(Array(sheet.warnings.enumerated()), id: \.offset) { _, w in warning(w) }
                    if s.isAsking { askingBanner }
                    rowsCard(s)
                    saveButtons(s)
                    calculateButton(s)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 32)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .onAppear { startIfNeeded() }
        .onAppear {
            withAnimation(.easeInOut(duration: 0.4).repeatForever(autoreverses: true)) { blink = true }
        }
        .sheet(item: $pickerRow) { picked in
            let row = picked.row
            DSDateTimeSheet(
                title: row.label,
                timeZone: fmt.timeZone,
                initial: row.timeMs.ms.map { TimeFormatter.msToDate($0) } ?? Date(),
                onDone: { ms in
                    sheet.setRowTime(row.id, ms)
                    pickerRow = nil
                },
                onCancel: { pickerRow = nil }
            )
            .presentationDetents([.height(380)])
        }
        .confirmationDialog(deleteRow?.label ?? "", isPresented: Binding(get: { deleteRow != nil }, set: { if !$0 { deleteRow = nil } }), titleVisibility: .visible) {
            Button("Удалить значение", role: .destructive) {
                if let r = deleteRow { sheet.clearRow(r.id) }
                deleteRow = nil
            }
            Button("Отмена", role: .cancel) { deleteRow = nil }
        }
        .alert("Другая норма", isPresented: Binding(get: { sheet.state.showRecalcDialog }, set: { _ in })) {
            Button("Пересчитать") { sheet.answerRecalc(true) }
            Button("Оставить", role: .cancel) { sheet.answerRecalc(false) }
        } message: {
            Text("Для условия «\(sheet.state.recalcVariantText)» действует другая норма длительности. Пересчитать время по ней?")
        }
        .sheet(item: $refPicker) { req in
            LocoRefPickerSheet(request: req, sheet: sheet, onClose: { refPicker = nil })
                .presentationDetents([.fraction(0.85)])
        }
    }

    private func startIfNeeded() {
        guard !started else { return }
        started = true
        sheet.setCallbacks(
            onSave: { r in
                if isAcceptance { vm.saveAcceptance(r) } else { vm.saveDelivery(r) }
            },
            onSeriesChanged: { name in vm.setSeries(name) },
            onTimeEndWork: { ms in vm.setTimeEndWork(ms) }
        )
        sheet.start(
            kind: kind,
            seriesName: state.series.isEmpty ? nil : state.series,
            isElectric: state.isElectric,
            initialStart: (isAcceptance ? state.timeStartOfAcceptance : state.timeStartOfDelivery).ms,
            initialEnd: (isAcceptance ? state.timeEndOfAcceptance : state.timeEndOfDelivery).ms,
            initialBarrierOut: state.timeBarrierOut.ms,
            initialBarrierIn: state.timeBarrierIn.ms,
            routeStartWork: state.routeStartWork.ms,
            routeEndWork: state.routeEndWork.ms,
            initialStationId: isAcceptance ? state.acceptanceStationId : state.deliveryStationId,
            offsetSeconds: Int(state.displayOffsetSeconds)
        )
    }

    // MARK: Шапка

    private func header(_ s: TimeSheetState) -> some View {
        HStack {
            Text(isAcceptance ? "Приёмка" : "Сдача").font(DSFont.sans(22, .medium)).foregroundColor(DSColor.text)
            Spacer()
            Button("Готово") {
                if sheet.finish() { dismiss() }
            }
            .font(DSFont.sans(16, .semibold))
            .foregroundColor(s.hasSequenceError ? DSColor.textFaint : DSColor.accent)
            .padding(8)
        }
        .padding(.horizontal, 24)
        .padding(.top, 20)
        .padding(.bottom, 8)
    }

    // MARK: Контекст

    private func contextFields(_ s: TimeSheetState) -> some View {
        VStack(spacing: 8) {
            contextRow(label: "СЕРИЯ", value: s.seriesName, placeholder: "Выберите серию") {
                refPicker = RefPickerRequest(kind: .series, editId: nil)
            }
            contextRow(label: s.stationLabel.uppercased(), value: s.stationName, placeholder: "Выберите станцию") {
                refPicker = RefPickerRequest(kind: .station, editId: nil)
            }
        }
    }

    private func contextRow(label: String, value: String?, placeholder: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text(label).dsTextStyle(.group)
                    Text(value ?? placeholder)
                        .font(DSFont.sans(17, .medium))
                        .foregroundColor(value == nil ? DSColor.textFaint : DSColor.text)
                }
                Spacer()
                DSIcon.chevronRight.image.font(.system(size: 13, weight: .semibold)).foregroundColor(DSColor.textFaint)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
        }
        .buttonStyle(DSPressableStyle())
    }

    private func normMode(_ s: TimeSheetState) -> some View {
        HStack(spacing: 0) {
            seg("После отстоя", active: !s.handToHand) { sheet.setHandToHand(false) }
            seg("Из рук в руки", active: s.handToHand) { sheet.setHandToHand(true) }
        }
        .padding(3)
        .background(DSColor.bgSubtle)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
    }

    private func seg(_ title: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(DSFont.sans(14, .semibold))
                .foregroundColor(active ? DSColor.text : DSColor.textMuted)
                .frame(maxWidth: .infinity, minHeight: 36)
                .background(active ? DSColor.surface : Color.clear)
                .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private func warning(_ w: TimeWarnUi) -> some View {
        DSWarningBlock(
            title: w.text,
            message: w.hint,
            ctaTitle: w.cta,
            onCTA: { warningCTA(w) },
            onClose: w.dismissible ? { sheet.dismissWarning(w.text) } : nil
        )
    }

    private func warningCTA(_ w: TimeWarnUi) {
        let s = sheet.state
        if w.isSeries {
            if let id = s.seriesId {
                refPicker = RefPickerRequest(kind: .series, editId: id)
            } else {
                dismiss()
                AppRouter.shared.showSettingsSection(.seriesList)
            }
        } else {
            if let id = s.stationId {
                refPicker = RefPickerRequest(kind: .station, editId: id)
            } else {
                dismiss()
                AppRouter.shared.showSettingsSection(.stationList)
            }
        }
    }

    private var askingBanner: some View {
        Text("Выберите, от какой операции начать отсчёт времени")
            .font(DSFont.sans(13, .semibold))
            .foregroundColor(DSColor.text)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DSColor.accentSoft)
            .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(DSColor.accent.opacity(0.3), lineWidth: 1))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: Строки времени

    private func rowsCard(_ s: TimeSheetState) -> some View {
        let rows = sheet.rows
        return DSCard {
            ForEach(Array(rows.enumerated()), id: \.element.id) { i, row in
                timeRow(row, isFirst: i == 0)
            }
        }
        .padding(.vertical, 4)
    }

    private func timeRow(_ row: TimeRowUi, isFirst: Bool) -> some View {
        let hasTime = row.timeText != nil
        return VStack(spacing: 0) {
            if !isFirst {
                Rectangle().fill(DSColor.borderStrong).frame(width: 2, height: 10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.leading, DSMetrics.rowPadX + DSMetrics.avatarMd / 2 - 1)
            }
            HStack(spacing: 14) {
                Image(systemName: hasTime ? "checkmark" : "clock")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(row.isHighlighted ? DSColor.accentInk : (hasTime ? DSColor.success : DSColor.textMuted))
                    .frame(width: DSMetrics.avatarMd, height: DSMetrics.avatarMd)
                    .background(row.isHighlighted ? DSColor.accent : (hasTime ? DSColor.success.opacity(0.14) : DSColor.bgSubtle))
                    .clipShape(Circle())
                VStack(alignment: .leading, spacing: 3) {
                    Text(row.label).font(DSFont.sans(14, .medium)).foregroundColor(DSColor.text)
                    if row.isLocked {
                        Text("из маршрута").font(DSFont.sans(11)).foregroundColor(DSColor.textMuted)
                    }
                    if row.hasError {
                        Text("⛔ Время должно быть позже предыдущего").font(DSFont.sans(11)).foregroundColor(DSColor.danger)
                    }
                    if !row.deltaText.isEmpty {
                        let tone: Color = row.deltaTone == 1 ? DSColor.danger : (row.deltaTone == 2 ? DSColor.success : DSColor.textMuted)
                        HStack(spacing: 4) {
                            Text(row.deltaText)
                            if !row.deltaNote.isEmpty { Text(row.deltaNote) }
                        }
                        .font(DSFont.mono(12))
                        .foregroundColor(tone)
                    }
                }
                Spacer(minLength: 8)
                trailing(row)
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, DSMetrics.rowPadYTouch)
            .background(row.isHighlighted ? DSColor.accentSoft.opacity(blink ? 1 : 0.2) : Color.clear)
            .contentShape(Rectangle())
            .onTapGesture { tap(row) }
            .onLongPressGesture { if row.canLongPress { deleteRow = row } }
        }
    }

    @ViewBuilder
    private func trailing(_ row: TimeRowUi) -> some View {
        if let pending = row.pendingText {
            Button { sheet.acceptPendingWorkEnd() } label: {
                Text("Обновить \(pending)")
                    .font(DSFont.sans(13, .semibold))
                    .foregroundColor(DSColor.accentInk)
                    .padding(.horizontal, 12).padding(.vertical, 8)
                    .background(DSColor.accent)
                    .clipShape(Capsule())
            }
            .buttonStyle(.plain)
        } else {
            Text(row.timeText ?? "—:—")
                .dsTextStyle(.timeMono, color: row.timeText == nil ? DSColor.textFaint : DSColor.text)
                .frame(minWidth: 76)
                .padding(.horizontal, 14).padding(.vertical, 8)
                .background(row.timeText == nil ? DSColor.bgSubtle : DSColor.bg)
                .overlay(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous).stroke(row.timeText == nil ? Color.clear : DSColor.border, lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
        }
    }

    private func tap(_ row: TimeRowUi) {
        guard row.canTap else { return }
        if sheet.tapRow(row.id) { pickerRow = PickerRow(row: row) }
    }

    // MARK: Нормы и расчёт

    @ViewBuilder
    private func saveButtons(_ s: TimeSheetState) -> some View {
        if s.saveSeries != nil || s.saveStation != nil {
            VStack(spacing: 8) {
                if let b = s.saveSeries { saveButton(b) { sheet.saveSeriesNorm() } }
                if let b = s.saveStation { saveButton(b) { sheet.saveStationNorm() } }
            }
            .padding(.top, 6)
        }
    }

    private func saveButton(_ b: SaveNormUi, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 2) {
                Text(b.title).font(DSFont.sans(15, .semibold))
                Text(b.subtitle).font(DSFont.sans(12)).opacity(0.85)
            }
            .foregroundColor(DSColor.accentInk)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16).padding(.vertical, 12)
            .background(DSColor.accent)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
        }
        .buttonStyle(DSPressableStyle())
    }

    private func calculateButton(_ s: TimeSheetState) -> some View {
        Button { sheet.applyNorms() } label: {
            Text("Рассчитать время")
                .font(DSFont.sans(16, .semibold))
                .foregroundColor(s.canCalculate ? DSColor.accentInk : DSColor.textFaint)
                .frame(maxWidth: .infinity, minHeight: 50)
                .background(s.canCalculate ? DSColor.accent : DSColor.bgSubtle)
                .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(s.canCalculate ? Color.clear : DSColor.border, lineWidth: 1))
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(!s.canCalculate)
        .padding(.top, 4)
    }
}
