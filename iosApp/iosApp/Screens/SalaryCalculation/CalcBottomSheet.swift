import SwiftUI
import ComposeApp

// MARK: - CalcBottomSheet (SCREEN_SPECS §11.9)
// «Расчёт за смену»: шторка поверх маршрута. Считает общий Kotlin
// `SalaryCalculationHelper` через `CalcSheetIosViewModel` (паритет с формой §5.4).

struct CalcBottomSheet: View {
    let basicId: String
    let onClose: () -> Void

    @StateObject private var vm: CalcSheetViewModelWrapper

    init(basicId: String, onClose: @escaping () -> Void) {
        self.basicId = basicId
        self.onClose = onClose
        _vm = StateObject(wrappedValue: CalcSheetViewModelWrapper(basicId: basicId))
    }

    private var router: AppRouter { AppRouter.shared }

    var body: some View {
        DSBottomSheet(
            header: .titleClose(title: "Расчёт за смену", subtitle: vm.salary.subtitle),
            onClose: onClose
        ) {
            if vm.salary.isCalculated {
                calculated
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
    private var calculated: some View {
        HStack(alignment: .bottom) {
            Text("Заработано")
                .font(DSFont.sans(13, .semibold))
                .foregroundColor(DSColor.accent)
            Spacer(minLength: 8)
            Text(vm.salary.total)
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

        // Командировка без среднего часа (§11.9)
        if vm.salary.averageHourMissing {
            Button(action: openSalarySettings) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Маршрут в командировке")
                        .font(DSFont.sans(15, .semibold))
                        .foregroundColor(DSColor.warning)
                    Text("Оплачивается только по среднему часу, без надбавок. Средний час не указан — поэтому сумма 0. Нажмите, чтобы задать его в настройках зарплаты.")
                        .font(DSFont.sans(14))
                        .foregroundColor(DSColor.text)
                        .multilineTextAlignment(.leading)
                }
                .padding(.horizontal, DSMetrics.rowPadX)
                .padding(.vertical, 16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(DSColor.warning.opacity(0.14))
                .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
            }
            .buttonStyle(.plain)
            .padding(.bottom, 12)
        }

        let rows = (vm.salary.rows as? [SalaryRowUi]) ?? vm.salary.rows
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

        // Тарифная ставка 0 — курсивная подчёркнутая ссылка (§11.9).
        if vm.salary.tariffMissing {
            Button(action: openSalarySettings) {
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
    }

    /// Внизу всегда строка «Настройки зарплаты» (§11.9).
    private var pinnedSettings: some View {
        VStack(spacing: 0) {
            DSDivider(inset: 0)
            Button(action: openSalarySettings) {
                HStack {
                    Text("Настройки зарплаты")
                        .font(DSFont.sans(15, .semibold))
                        .foregroundColor(DSColor.accent)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(DSColor.textFaint)
                }
                .padding(.horizontal, DSMetrics.rowPadX)
                .padding(.vertical, 16)
            }
            .buttonStyle(.plain)
        }
        .background(DSColor.bg)
    }

    /// Любой переход в настройки сначала закрывает шторку (§11.9).
    private func openSalarySettings() {
        onClose()
        router.showSettingSalary()
    }
}
