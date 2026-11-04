import SwiftUI
import ComposeApp

// Карточки секций, «ДРУГОЕ» и «ИТОГО» экрана «Локомотив» (SCREEN_SPECS §1.3 п.4–6).
// Все числа посчитаны в Kotlin (`LocoScreenIosViewModel`); здесь только отображение и ввод.

/// Шапка карточки секции: «Секция N» (тап переключает нумерацию) и необязательная пилюля справа.
private struct SectionHeader<Trailing: View>: View {
    let label: String
    let onTap: () -> Void
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack {
            Text(label).font(DSFont.sans(15, .medium)).foregroundColor(DSColor.text)
            Spacer(minLength: 8)
            trailing()
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: onTap)
    }
}

/// Пара «Принял → Сдал».
private struct PairRow: View {
    let firstTitle: String
    let secondTitle: String
    let first: String
    let second: String
    var suffix: String? = nil
    var firstHint: String = ""
    var secondHint: String = ""
    let onFirst: (String) -> Void
    let onSecond: (String) -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            column(firstTitle, first, firstHint, onFirst)
            Text("→").font(DSFont.sans(15)).foregroundColor(DSColor.textMuted).frame(height: DSMetrics.inputMinH).padding(.top, 18)
            column(secondTitle, second, secondHint, onSecond)
        }
    }

    private func column(_ title: String, _ value: String, _ hint: String, _ onChange: @escaping (String) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            LocoFieldLabel(text: title)
            LocoField(placeholder: "0", external: value, suffix: suffix, onChange: onChange)
            Text(hint).font(DSFont.mono(11)).foregroundColor(DSColor.textMuted).frame(minHeight: 14)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: Тепловоз

struct LocoDieselSectionCard: View {
    let section: LocoDieselSectionUi
    let index: Int
    let isKiloMode: Bool
    let vm: LocoScreenViewModelWrapper
    @Binding var openSwipeId: String?
    @Binding var swipeLock: Bool
    let onCoeff: () -> Void
    let onSupplyCoeff: () -> Void
    let onDelete: () -> Void

    @State private var supplyExpanded: Bool?

    private var expanded: Bool { supplyExpanded ?? section.hasSupplyData }

    var body: some View {
        LocoSwipeDelete(id: section.id, openId: $openSwipeId, lockScroll: $swipeLock, onDelete: onDelete) {
            DSCard {
                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader(label: section.label, onTap: { vm.toggleNumbering() }) {
                        LocoCoeffPill(title: "k секции", value: section.coefficientPill, action: onCoeff)
                    }
                    HStack {
                        Text("ТОПЛИВО").dsTextStyle(.group)
                        Spacer()
                        unitToggle
                    }
                    .padding(.top, 4)
                    PairRow(
                        firstTitle: "Принял", secondTitle: "Сдал",
                        first: section.acceptedText, second: section.deliveryText,
                        suffix: isKiloMode ? "кг." : "л.",
                        firstHint: section.acceptedHint, secondHint: section.deliveryHint,
                        onFirst: { vm.setFuel(index, "accepted", $0) },
                        onSecond: { vm.setFuel(index, "delivery", $0) }
                    )
                    supplyBlock
                    LocoDashedDivider()
                    HStack {
                        Text("Расход").font(DSFont.sans(14)).foregroundColor(DSColor.textMuted)
                        Spacer()
                        Text(section.resultText).font(DSFont.mono(15, .semibold)).foregroundColor(DSColor.text)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }
        }
        .contextMenu { moveMenu }
    }

    @ViewBuilder
    private var moveMenu: some View {
        Button { vm.moveSection(from: index, to: index - 1) } label: { Label("Выше", systemImage: "arrow.up") }
        Button { vm.moveSection(from: index, to: index + 1) } label: { Label("Ниже", systemImage: "arrow.down") }
    }

    private var unitToggle: some View {
        HStack(spacing: 0) {
            seg("л", active: !isKiloMode) { if isKiloMode { vm.toggleKiloMode() } }
            seg("кг", active: isKiloMode) { if !isKiloMode { vm.toggleKiloMode() } }
        }
        .padding(2)
        .background(DSColor.bgSubtle)
        .clipShape(Capsule())
    }

    private func seg(_ title: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(DSFont.sans(13, .semibold))
                .foregroundColor(active ? DSColor.accentInk : DSColor.textMuted)
                .padding(.horizontal, 14)
                .padding(.vertical, 5)
                .background(active ? DSColor.accent : Color.clear)
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    private var supplyBlock: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Button { withAnimation(.easeInOut(duration: 0.25)) { supplyExpanded = !expanded } } label: {
                    HStack(spacing: 6) {
                        Text("ЭКИПИРОВКА").dsTextStyle(.group)
                        (expanded ? DSIcon.chevronDown : DSIcon.chevronRight).image
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(DSColor.textFaint)
                    }
                }
                .buttonStyle(.plain)
                Spacer()
                if expanded {
                    LocoCoeffPill(title: "k экипировки", value: section.refuelCoefficientPill, action: onSupplyCoeff)
                }
            }
            if expanded {
                HStack(alignment: .top, spacing: 8) {
                    VStack(alignment: .leading, spacing: 4) {
                        LocoFieldLabel(text: "Объём")
                        LocoField(placeholder: "0", external: section.refuelText, suffix: "л.", onChange: { vm.setRefuel(index, $0) })
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        LocoFieldLabel(text: "Масса")
                        LocoField(placeholder: "0", external: section.refuelKiloText, suffix: "кг.", onChange: { vm.setRefuelKilo(index, $0) })
                    }
                }
            }
        }
        .padding(.top, 4)
        .animation(.easeInOut(duration: 0.25), value: expanded)
    }
}

// MARK: Электровоз

struct LocoElectricSectionCard: View {
    let section: LocoElectricSectionUi
    let index: Int
    let showOtherCurrent: Bool
    let vm: LocoScreenViewModelWrapper
    @Binding var openSwipeId: String?
    @Binding var swipeLock: Bool
    let onDelete: () -> Void

    @State private var recoveryExpanded: Bool?

    private var expanded: Bool { recoveryExpanded ?? section.hasRecoveryData }

    var body: some View {
        LocoSwipeDelete(id: section.id, openId: $openSwipeId, lockScroll: $swipeLock, onDelete: onDelete) {
            DSCard {
                VStack(alignment: .leading, spacing: 8) {
                    SectionHeader(label: section.label, onTap: { vm.toggleNumbering() }) { EmptyView() }
                    Text("РАСХОД").dsTextStyle(.group).padding(.top, 4)
                    PairRow(
                        firstTitle: showOtherCurrent ? "Ток 1 принял" : "Принял",
                        secondTitle: showOtherCurrent ? "Ток 1 сдал" : "Сдал",
                        first: section.accepted, second: section.delivery,
                        onFirst: { vm.setEnergy(index, "accepted", $0) },
                        onSecond: { vm.setEnergy(index, "delivery", $0) }
                    )
                    if showOtherCurrent {
                        PairRow(
                            firstTitle: "Ток 2 принял", secondTitle: "Ток 2 сдал",
                            first: section.accepted2, second: section.delivery2,
                            onFirst: { vm.setEnergy(index, "accepted2", $0) },
                            onSecond: { vm.setEnergy(index, "delivery2", $0) }
                        )
                    }
                    Button { withAnimation(.easeInOut(duration: 0.25)) { recoveryExpanded = !expanded } } label: {
                        HStack(spacing: 6) {
                            Text("РЕКУПЕРАЦИЯ").dsTextStyle(.group)
                            (expanded ? DSIcon.chevronDown : DSIcon.chevronRight).image
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(DSColor.textFaint)
                        }
                    }
                    .buttonStyle(.plain)
                    .padding(.top, 4)
                    if expanded {
                        PairRow(
                            firstTitle: showOtherCurrent ? "Ток 1 принял" : "Принял",
                            secondTitle: showOtherCurrent ? "Ток 1 сдал" : "Сдал",
                            first: section.recoveryAccepted, second: section.recoveryDelivery,
                            onFirst: { vm.setEnergy(index, "recoveryAccepted", $0) },
                            onSecond: { vm.setEnergy(index, "recoveryDelivery", $0) }
                        )
                        if showOtherCurrent {
                            PairRow(
                                firstTitle: "Ток 2 принял", secondTitle: "Ток 2 сдал",
                                first: section.recoveryAccepted2, second: section.recoveryDelivery2,
                                onFirst: { vm.setEnergy(index, "recoveryAccepted2", $0) },
                                onSecond: { vm.setEnergy(index, "recoveryDelivery2", $0) }
                            )
                        }
                    }
                    LocoDashedDivider()
                    summary("Расход", section.consumption)
                    if let v = section.recovery { summary("Рекуперация", v) }
                    if showOtherCurrent, let v = section.consumption2 { summary("Расход (ток 2)", v) }
                    if showOtherCurrent, let v = section.recovery2 { summary("Рекуперация (ток 2)", v) }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .animation(.easeInOut(duration: 0.25), value: expanded)
                .animation(.easeInOut(duration: 0.25), value: showOtherCurrent)
                .animation(.easeInOut(duration: 0.25), value: section.recovery != nil)
            }
        }
        .contextMenu {
            Button { vm.moveSection(from: index, to: index - 1) } label: { Label("Выше", systemImage: "arrow.up") }
            Button { vm.moveSection(from: index, to: index + 1) } label: { Label("Ниже", systemImage: "arrow.down") }
        }
    }

    private func summary(_ title: String, _ value: String) -> some View {
        HStack {
            Text(title).font(DSFont.sans(14)).foregroundColor(DSColor.textMuted)
            Spacer()
            Text(value).font(DSFont.mono(15, .semibold)).foregroundColor(DSColor.text)
        }
    }
}

// MARK: ДРУГОЕ

struct LocoCountersCard: View {
    let state: LocoScreenState
    let vm: LocoScreenViewModelWrapper

    var body: some View {
        DSCard {
            if state.showHeating {
                counterRow(
                    title: "Отопление", icon: .flash,
                    accepted: state.heatingAccepted, delivery: state.heatingDelivery, result: state.heatingResult,
                    onAccepted: { vm.setCounter("heatingAccepted", $0) },
                    onDelivery: { vm.setCounter("heatingDelivery", $0) }
                )
            }
            if state.showHeating && state.showAuxiliary { DSDivider() }
            if state.showAuxiliary {
                counterRow(
                    title: "Собств. нужды", icon: .gear,
                    accepted: state.auxiliaryAccepted, delivery: state.auxiliaryDelivery, result: state.auxiliaryResult,
                    onAccepted: { vm.setCounter("auxiliaryAccepted", $0) },
                    onDelivery: { vm.setCounter("auxiliaryDelivery", $0) }
                )
            }
        }
    }

    private func counterRow(title: String, icon: DSIcon, accepted: String, delivery: String, result: String?,
                            onAccepted: @escaping (String) -> Void, onDelivery: @escaping (String) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                DSIconAvatar(icon: icon, size: .sm, tone: .neutral, shape: .square)
                Text(title).font(DSFont.sans(15, .medium)).foregroundColor(DSColor.text)
            }
            PairRow(firstTitle: "Принял", secondTitle: "Сдал", first: accepted, second: delivery, onFirst: onAccepted, onSecond: onDelivery)
            if let result = result {
                HStack {
                    Text("Расход:").font(DSFont.sans(14)).foregroundColor(DSColor.textMuted)
                    Text(result).font(DSFont.mono(15, .semibold)).foregroundColor(DSColor.text)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }
}

// MARK: ИТОГО

private struct AmountLine: View {
    let label: String
    let value: String
    var unit: String = ""
    var pill: String? = nil
    var big: Bool = false
    var color: Color = DSColor.text

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            Text(label).dsTextStyle(.group).fixedSize()
            Spacer(minLength: 8)
            Text(value).font(DSFont.mono(big ? 28 : 18, .bold)).foregroundColor(color)
                .lineLimit(1).minimumScaleFactor(0.4).layoutPriority(1)
            if !unit.isEmpty { Text(unit).font(DSFont.mono(14)).foregroundColor(DSColor.textMuted).fixedSize() }
            if let pill = pill {
                Text(pill).font(DSFont.mono(13, .semibold)).foregroundColor(DSColor.text)
                    .lineLimit(1).fixedSize()
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(DSColor.bgSubtle).clipShape(Capsule())
            }
        }
    }
}

private struct NormaRow: View {
    let external: String
    var unit: String = ""
    let onChange: (String) -> Void

    var body: some View {
        HStack {
            Text("НОРМА").dsTextStyle(.group)
            Spacer()
            LocoField(placeholder: "—", external: external, suffix: unit.isEmpty ? nil : unit, alignment: .trailing, onChange: onChange)
                .frame(width: 150)
        }
    }
}

struct LocoElectricTotals: View {
    let state: LocoScreenState
    let vm: LocoScreenViewModelWrapper

    var body: some View {
        let totals = (state.electricTotals as? [LocoElectricTotalUi]) ?? []
        DSCard {
            ForEach(Array(totals.enumerated()), id: \.offset) { i, t in
                if i > 0 { DSDivider(inset: 0) }
                VStack(alignment: .leading, spacing: 10) {
                    if let label = t.label {
                        Text(label).font(DSFont.sans(15, .bold)).foregroundColor(DSColor.text)
                    }
                    AmountLine(label: "РАСХОД", value: t.consumed, big: true)
                    if let rec = t.recovery {
                        AmountLine(label: "РЕКУПЕРАЦИЯ", value: rec, color: DSColor.success)
                    }
                    if let net = t.net {
                        AmountLine(label: "ЧИСТЫЙ РАСХОД", value: net)
                    }
                    DSDivider(inset: 0)
                    NormaRow(external: i == 0 ? state.norma1 : state.norma2, onChange: { i == 0 ? vm.setNorma1($0) : vm.setNorma2($0) })
                    if let result = t.result { LocoResultCard(result: result) }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
            }
        }
    }
}

struct LocoDieselTotals: View {
    let state: LocoScreenState
    let vm: LocoScreenViewModelWrapper

    var body: some View {
        DSCard {
            if let t = state.dieselTotal {
                VStack(alignment: .leading, spacing: 10) {
                    AmountLine(label: "РАСХОД", value: t.fuelBig, unit: t.fuelUnit, pill: t.fuelPill, big: true)
                    if t.hasSupply {
                        AmountLine(label: "ЭКИПИРОВКА", value: t.supplyBig, unit: t.supplyUnit, pill: t.supplyPill, big: true)
                    }
                    DSDivider(inset: 0)
                    NormaRow(external: state.normaDiesel, unit: "кг", onChange: { vm.setNormaDiesel($0) })
                    if let result = t.result { LocoResultCard(result: result, unit: "кг") }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
            }
        }
    }
}
