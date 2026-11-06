import SwiftUI
import ComposeApp

// MARK: - SettingSalaryView (SCREEN_SPECS §12.1)
// Все параметры расчёта: тариф (MonthOfYear), поля SalarySetting, пороговые
// доплаты и «По плечам». Валидация и автосохранение — в Kotlin VM (§12.2).

struct SettingSalaryView: View {
    @StateObject private var vm = SettingSalaryViewModelWrapper()
    @State private var deleteRequest: DSDeleteRequest?
    @State private var helpSheet: HelpSheet?
    @State private var expandedPhases: Set<String> = []
    @State private var showPhaseList: [String: Bool] = [:]

    private var router: AppRouter { AppRouter.shared }

    private enum HelpSheet: Identifiable {
        case longTrain
        case extendedPhase
        var id: Int { self == .longTrain ? 0 : 1 }
    }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("Начисления")
                        .dsTextStyle(.sectionH2)
                        .padding(.top, 10)
                        .padding(.bottom, 12)

                    tariffCard
                    surchargeCard
                    pdmCard
                    heavyTrainCard
                    longTrainCard
                    extendedPhaseCard
                    otherSurchargeCard
                    underworkToggleCard

                    Text("Удержания")
                        .dsTextStyle(.sectionH2)
                        .padding(.top, 24)
                        .padding(.bottom, 12)
                    deductionsCard
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 32)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .navigationBarHidden(true)
        .dsDeleteAlert($deleteRequest)
        .dsSheet(item: $helpSheet, detents: [.height(360)]) { sheet in
            helpSheetView(sheet)
        }
        .onDisappear { vm.onScreenExit() }
        // Шторка §12.3 (AppSheet.tariffChanged) — по флагу VM.
        .onChange(of: vm.state.isTariffDialogShown) { shown in
            if shown {
                router.present(.tariffChanged)
            } else {
                router.dismissSheet()
            }
        }
    }

    // MARK: Топбар (выход с проверкой смены тарифа, §12)

    private var topBar: some View {
        HStack(spacing: 10) {
            Button {
                vm.onBackPressed(onExit: { router.back() })
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(DSColor.text)
                    .frame(width: 40, height: 40)
                    .background(DSColor.bgSubtle)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Назад"))

            Text("Зарплата")
                .font(DSFont.sans(17, .semibold))
                .foregroundColor(DSColor.text)
            Spacer(minLength: 0)
            Color.clear.frame(width: 40, height: 40)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    // MARK: 1. Тарифная ставка

    private var tariffCard: some View {
        DSCard {
            VStack(alignment: .leading, spacing: 0) {
                salaryFieldRow(
                    label: "Тарифная ставка",
                    suffix: "₽",
                    field: vm.state.tariffRate,
                    trailing: AnyView(
                        Button {
                            vm.showDialogTariffRate()
                        } label: {
                            Text(vm.state.dateLabel)
                                .font(DSFont.sans(12, .semibold))
                                .foregroundColor(DSColor.accent)
                        }
                        .buttonStyle(.plain)
                    ),
                    onChange: { vm.setTariffRate($0) }
                )
                if vm.state.hasDateSetTariffRate {
                    DSDivider()
                    // BUG-42 (паритет с Android): ошибка старой ставки подсвечивается
                    // по флагу новой ставки.
                    salaryFieldRow(
                        label: "Тарифная ставка",
                        suffix: "₽",
                        field: SettingFieldUi(
                            value: vm.state.oldTariffRate.value,
                            isError: vm.state.tariffRate.isError
                        ),
                        trailing: AnyView(
                            Button {
                                vm.showDialogTariffRate()
                            } label: {
                                Text("до " + vm.state.oldDateLabel.dropFirst(3))
                                    .font(DSFont.sans(12, .semibold))
                                    .foregroundColor(DSColor.accent)
                            }
                            .buttonStyle(.plain)
                        ),
                        onChange: { vm.setOldTariffRate($0) }
                    )
                }
                DSDivider()
                salaryFieldRow(
                    label: "Средний час",
                    suffix: "₽",
                    field: vm.state.averagePaymentHour,
                    onChange: { vm.setAveragePaymentHour($0) }
                )
            }
        }
        .padding(.bottom, 12)
    }

    // MARK: 2. Процентные надбавки

    private var surchargeCard: some View {
        DSCard {
            VStack(alignment: .leading, spacing: 0) {
                salaryFieldRow(label: "Зональная надбавка", suffix: "%", field: vm.state.zonalSurcharge) {
                    vm.setZonalSurcharge($0)
                }
                DSDivider()
                salaryFieldRow(label: "Доплаты за класс и права", suffix: "%", field: vm.state.surchargeQualificationClass) {
                    vm.setSurchargeQualificationClass($0)
                }
                DSDivider()
                salaryFieldRow(label: "Работа в одно лицо", suffix: "%", field: vm.state.onePersonOperationPercent) {
                    vm.setOnePersonOperationPercent($0)
                }
                DSDivider()
                salaryFieldRow(label: "Работа в одно лицо (пассажирский)", suffix: "%", field: vm.state.onePersonOperationPassengerTrainPercent) {
                    vm.setOnePersonOperationPassengerTrainPercent($0)
                }
                DSDivider()
                salaryFieldRow(label: "Доплата за вредность", suffix: "%", field: vm.state.harmfulnessPercent) {
                    vm.setHarmfulnessPercent($0)
                }
                DSDivider()
                salaryFieldRow(label: "Северная надбавка", suffix: "%", field: vm.state.nordicCoefficient) {
                    vm.setNordicCoefficient($0)
                }
                DSDivider()
                salaryFieldRow(label: "Районный коэффициент", suffix: "%", field: vm.state.districtCoefficient) {
                    vm.setDistrictCoefficient($0)
                }
            }
        }
        .padding(.bottom, 12)
    }

    // MARK: 3. ПДМ

    private var pdmCard: some View {
        DSCard {
            salaryFieldRow(
                label: "Доплата за ПДМ (>6000 т. и >350 осей)",
                suffix: "%",
                field: vm.state.surchargeHeavyLongDistanceTrains
            ) {
                vm.setSurchargeHeavyLongDistanceTrains($0)
            }
        }
        .padding(.bottom, 12)
    }

    // MARK: 4a. Тяжеловесные (+ «По плечам»)

    private var heavyTrainCard: some View {
        thresholdCard(
            title: "Доплата за тяж. поезда",
            thresholdUnit: "т.",
            rows: vm.state.heavyRows,
            showHelp: false,
            onAdd: { vm.addSurchargeHeavyTrain() },
            onThreshold: { vm.setSurchargeHeavyTrainWeight(id: $0, $1) },
            onPercent: { vm.setSurchargeHeavyTrainPercent(id: $0, $1) },
            onDelete: { rowId in
                deleteRequest = DSDeleteRequest(title: "Удалить доплату?") {
                    vm.deleteSurchargeHeavyTrain(id: rowId)
                }
            },
            phaseKey: "heavy",
            phaseRows: { phase in phase.heavyRows },
            onPhaseAdd: { vm.addPhaseHeavyRow(phaseId: $0) },
            onPhaseThreshold: { vm.setPhaseHeavyRowThreshold(phaseId: $0, rowId: $1, $2) },
            onPhasePercent: { vm.setPhaseHeavyRowPercent(phaseId: $0, rowId: $1, $2) },
            onPhaseDelete: { phaseId, rowId in
                deleteRequest = DSDeleteRequest(title: "Удалить доплату?") {
                    vm.deletePhaseHeavyRow(phaseId: phaseId, rowId: rowId)
                }
            }
        )
    }

    // MARK: 4b. Длинносоставные (+ «По плечам», кнопка «?»)

    private var longTrainCard: some View {
        thresholdCard(
            title: "Доплата за длинносост. поезда",
            thresholdUnit: "ваг.",
            rows: vm.state.longRows,
            showHelp: true,
            onAdd: { vm.addSurchargeLongTrain() },
            onThreshold: { vm.setSurchargeLongTrainLength(id: $0, $1) },
            onPercent: { vm.setSurchargeLongTrainPercent(id: $0, $1) },
            onDelete: { rowId in
                deleteRequest = DSDeleteRequest(title: "Удалить доплату?") {
                    vm.deleteSurchargeLongTrain(id: rowId)
                }
            },
            phaseKey: "long",
            phaseRows: { phase in phase.longRows },
            onPhaseAdd: { vm.addPhaseLongRow(phaseId: $0) },
            onPhaseThreshold: { vm.setPhaseLongRowThreshold(phaseId: $0, rowId: $1, $2) },
            onPhasePercent: { vm.setPhaseLongRowPercent(phaseId: $0, rowId: $1, $2) },
            onPhaseDelete: { phaseId, rowId in
                deleteRequest = DSDeleteRequest(title: "Удалить доплату?") {
                    vm.deletePhaseLongRow(phaseId: phaseId, rowId: rowId)
                }
            }
        )
    }

    // MARK: 4c. Удлинённое плечо (кнопка «?»)

    private var extendedPhaseCard: some View {
        thresholdCard(
            title: "Доплата за удлиненное плечо",
            thresholdUnit: "км",
            rows: vm.state.extendedRows,
            showHelp: true,
            onAdd: { vm.addSurchargeExtendedServicePhase() },
            onThreshold: { vm.setSurchargeExtendedServicePhaseDistance(id: $0, $1) },
            onPercent: { vm.setSurchargeExtendedServicePhasePercent(id: $0, $1) },
            onDelete: { rowId in
                deleteRequest = DSDeleteRequest(title: "Удалить доплату?") {
                    vm.deleteSurchargeExtendedServicePhase(id: rowId)
                }
            },
            phaseKey: nil,
            phaseRows: { _ in [] },
            onPhaseAdd: { _ in },
            onPhaseThreshold: { _, _, _ in },
            onPhasePercent: { _, _, _ in },
            onPhaseDelete: { _, _ in }
        )
    }

    // MARK: Пороговая карточка [порог][%] (+ опционально «По плечам»)

    private func thresholdCard(
        title: String,
        thresholdUnit: String,
        rows: [SurchargeRowUi],
        showHelp: Bool,
        onAdd: @escaping () -> Void,
        onThreshold: @escaping (String, String) -> Void,
        onPercent: @escaping (String, String) -> Void,
        onDelete: @escaping (String) -> Void,
        phaseKey: String?,
        phaseRows: @escaping (PhaseSurchargesUi) -> [SurchargeRowUi],
        onPhaseAdd: @escaping (String) -> Void,
        onPhaseThreshold: @escaping (String, String, String) -> Void,
        onPhasePercent: @escaping (String, String, String) -> Void,
        onPhaseDelete: @escaping (String, String) -> Void
    ) -> some View {
        DSCard {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    Text(title)
                        .font(DSFont.sans(15, .semibold))
                        .foregroundColor(DSColor.text)
                    if showHelp {
                        Button {
                            helpSheet = title.contains("длинносост") ? .longTrain : .extendedPhase
                        } label: {
                            Image(systemName: "questionmark.circle")
                                .font(.system(size: 16))
                                .foregroundColor(DSColor.accent)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(Text("Справка"))
                    }
                    Spacer(minLength: 8)
                    Button(action: onAdd) {
                        Text("Добавить")
                            .font(DSFont.sans(13, .semibold))
                            .foregroundColor(DSColor.accent)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, DSMetrics.rowPadX)
                .padding(.vertical, 12)

                ForEach(Array(rows.enumerated()), id: \.offset) { index, row in
                    DSDivider()
                    thresholdRow(
                        unit: thresholdUnit,
                        row: row,
                        onThreshold: { onThreshold(row.id, $0) },
                        onPercent: { onPercent(row.id, $0) },
                        onDelete: { onDelete(row.id) }
                    )
                }

                if let phaseKey {
                    phaseSection(
                        key: phaseKey,
                        unit: thresholdUnit,
                        rowsOf: phaseRows,
                        onAdd: onPhaseAdd,
                        onThreshold: onPhaseThreshold,
                        onPercent: onPhasePercent,
                        onDelete: onPhaseDelete
                    )
                }
            }
        }
        .padding(.bottom, 12)
    }

    private func thresholdRow(
        unit: String,
        row: SurchargeRowUi,
        onThreshold: @escaping (String) -> Void,
        onPercent: @escaping (String) -> Void,
        onDelete: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 8) {
            TextField(unit, text: Binding(get: { row.threshold }, set: onThreshold))
                .font(DSFont.mono(14, .medium))
                .keyboardType(.decimalPad)
                .padding(.horizontal, 10)
                .frame(height: 40)
                .background(DSColor.bgSubtle)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                .frame(width: 96)
            TextField("%", text: Binding(get: { row.percent }, set: onPercent))
                .font(DSFont.mono(14, .medium))
                .keyboardType(.decimalPad)
                .padding(.horizontal, 10)
                .frame(height: 40)
                .background(DSColor.bgSubtle)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                .frame(width: 72)
            Spacer(minLength: 8)
            // Удаление строки — корзина справа (свайпа нет), затем подтверждение.
            Button(action: onDelete) {
                Image(systemName: "trash")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(DSColor.danger)
                    .frame(width: 40, height: 40)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Удалить доплату"))
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, 8)
    }

    // MARK: «По плечам» (§12.1): переключатель Показать/Скрыть + списки плеч

    private func phaseSection(
        key: String,
        unit: String,
        rowsOf: @escaping (PhaseSurchargesUi) -> [SurchargeRowUi],
        onAdd: @escaping (String) -> Void,
        onThreshold: @escaping (String, String, String) -> Void,
        onPercent: @escaping (String, String, String) -> Void,
        onDelete: @escaping (String, String) -> Void
    ) -> some View {
        let isShown = showPhaseList[key] ?? false
        return VStack(alignment: .leading, spacing: 0) {
            DSDivider()
            HStack {
                Text("По плечам")
                    .font(DSFont.sans(13, .semibold))
                    .foregroundColor(DSColor.textMuted)
                Spacer(minLength: 8)
                Button {
                    showPhaseList[key] = !isShown
                } label: {
                    Text(isShown ? "Скрыть" : "Показать")
                        .font(DSFont.sans(13, .semibold))
                        .foregroundColor(DSColor.accent)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 12)

            if isShown {
                ForEach(Array(vm.state.phases.enumerated()), id: \.offset) { _, phase in
                    VStack(alignment: .leading, spacing: 0) {
                        HStack {
                            Text(phase.name)
                                .font(DSFont.sans(13, .medium))
                                .foregroundColor(DSColor.text)
                            Spacer(minLength: 8)
                            let ownCount = rowsOf(phase).count
                            Text(ownCount > 0 ? "свои: \(ownCount)" : "общие")
                                .font(DSFont.sans(11))
                                .foregroundColor(DSColor.textFaint)
                        }
                        .padding(.horizontal, DSMetrics.rowPadX)
                        .padding(.top, 10)
                        .padding(.bottom, 4)

                        ForEach(Array(rowsOf(phase).enumerated()), id: \.offset) { _, row in
                            thresholdRow(
                                unit: unit,
                                row: row,
                                onThreshold: { onThreshold(phase.id, row.id, $0) },
                                onPercent: { onPercent(phase.id, row.id, $0) },
                                onDelete: { onDelete(phase.id, row.id) }
                            )
                        }
                        Button {
                            onAdd(phase.id)
                        } label: {
                            Text("Добавить")
                                .font(DSFont.sans(13, .semibold))
                                .foregroundColor(DSColor.accent)
                        }
                        .buttonStyle(.plain)
                        .padding(.horizontal, DSMetrics.rowPadX)
                        .padding(.vertical, 10)
                    }
                }
            }
        }
    }

    // MARK: 5–6. Другие надбавки + тумблер недоработки

    private var otherSurchargeCard: some View {
        DSCard {
            salaryFieldRow(label: "Другие надбавки", suffix: "%", field: vm.state.otherSurcharge) {
                vm.setOtherSurcharge($0)
            }
        }
        .padding(.bottom, 12)
    }

    private var underworkToggleCard: some View {
        DSCard {
            Toggle(isOn: Binding(
                get: { vm.state.showUnderworkPayments },
                set: { vm.setShowUnderworkPayments($0) }
            )) {
                Text("Показывать оплату недоработки")
                    .font(DSFont.sans(15))
                    .foregroundColor(DSColor.text)
            }
            .tint(DSColor.accent)
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 10)
        }
        .padding(.bottom, 12)
    }

    // MARK: Удержания

    private var deductionsCard: some View {
        DSCard {
            VStack(alignment: .leading, spacing: 0) {
                salaryFieldRow(label: "Подоходный налог", suffix: "%", field: vm.state.ndfl) { vm.setNDFL($0) }
                DSDivider()
                salaryFieldRow(label: "Профсоюз", suffix: "%", field: vm.state.unionistsRetention) { vm.setUnionistsRetention($0) }
                DSDivider()
                salaryFieldRow(label: "Прочие удержания", suffix: "%", field: vm.state.otherRetention) { vm.setOtherRetention($0) }
                DSDivider()
                salaryFieldRow(label: "Благосостояние", suffix: "%", field: vm.state.welfarePercent) { vm.setWelfarePercent($0) }
                DSDivider()
                salaryFieldRow(label: "Алименты", suffix: "%", field: vm.state.alimonyPercent) { vm.setAlimonyPercent($0) }
            }
        }
    }

    // MARK: Поле ввода (название + число с суффиксом + ошибка «Некорректные данные»)

    private func salaryFieldRow(
        label: String,
        suffix: String,
        field: SettingFieldUi,
        trailing: AnyView? = nil,
        onChange: @escaping (String) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 8) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(label)
                        .font(DSFont.sans(14, .medium))
                        .foregroundColor(DSColor.text)
                    if let trailing {
                        trailing
                    }
                }
                Spacer(minLength: 8)
                HStack(spacing: 4) {
                    TextField("0", text: Binding(get: { field.value }, set: onChange))
                        .font(DSFont.mono(15, .semibold))
                        .foregroundColor(DSColor.text)
                        .keyboardType(.decimalPad)
                        .multilineTextAlignment(.trailing)
                    Text(suffix)
                        .font(DSFont.sans(13))
                        .foregroundColor(DSColor.textMuted)
                }
                .padding(.horizontal, 10)
                .frame(height: 40)
                .frame(minWidth: 96)
                .background(DSColor.bgSubtle)
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous)
                        .stroke(field.isError ? DSColor.danger : Color.clear, lineWidth: 1)
                )
            }
            if field.isError {
                Text("Некорректные данные")
                    .font(DSFont.sans(11))
                    .foregroundColor(DSColor.danger)
                    .padding(.bottom, 4)
            }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, 10)
    }

    // MARK: Справки порогов (§12.1)

    @ViewBuilder
    private func helpSheetView(_ sheet: HelpSheet) -> some View {
        let (title, text) = sheet == .longTrain
            ? (
                "Граница длинносоставного поезда",
                "Доплата начинается только когда условная длина поезда больше указанного значения. При равенстве порогу доплата ещё не начисляется. Например: для порога 80 доплата действует с 81 условного вагона."
            )
            : (
                "Граница удлинённого плеча",
                "Доплата начинается только когда пробег больше указанного значения. При равенстве порогу доплата ещё не начисляется. Например: для порога 250 км доплата действует при пробеге свыше 250 км."
            )
        DSBottomSheet(header: .titleClose(title: title, subtitle: nil), onClose: { helpSheet = nil }) {
            Text(text)
                .font(DSFont.sans(14))
                .foregroundColor(DSColor.text)
            DSCTAButton(title: "Понятно") { helpSheet = nil }
                .padding(.top, 16)
        }
    }
}
