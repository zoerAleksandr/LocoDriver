import SwiftUI
import ComposeApp

// MARK: - SalaryCalculationView (SCREEN_SPECS §11.6)
// Раскладка по design/src/salary-screen.jsx и §11.6. Логика — в Kotlin VM.

struct SalaryCalculationView: View {
    @StateObject private var vm = SalaryCalculationViewModelWrapper()
    @State private var isMonthSheetShown = false

    private var router: AppRouter { AppRouter.shared }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            if vm.isLoading {
                VStack(spacing: 12) {
                    ProgressView()
                    Text("Пересчет...")
                        .font(DSFont.sans(14))
                        .foregroundColor(DSColor.textMuted)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        monthSwitcher
                        heroBlock
                        if vm.statement.tariffMissing {
                            tariffWarningCard
                        }
                        accrualsCard
                        deductionsCard
                        payoutBlock
                        disclaimer
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 24)
                }
                .background(DSColor.bg)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .navigationBarHidden(true)
        .dsSheet(isPresented: $isMonthSheetShown, detents: [.medium, .large]) {
            SalaryMonthSheet(
                months: vm.monthYearList,
                selectedYear: Int(vm.statement.year),
                selectedMonth: Int(vm.statement.monthIndex),
                onApply: { year, month in
                    isMonthSheetShown = false
                    vm.selectYearAndMonth(year: year, month: month)
                },
                onClose: { isMonthSheetShown = false }
            )
        }
        .alert("Оплата недоработки", isPresented: underworkAlertBinding) {
            Button("В настройки") {
                vm.hideUnderworkInfoForSession()
                router.showSettingSalary()
            }
            Button("Понятно", role: .cancel) {
                vm.dismissUnderworkInfoForever()
            }
        } message: {
            Text("За выбранный период отработано меньше нормы. Укажите средний час в настройках зарплаты — и приложение рассчитает оплату недоработки за недостающие часы.")
        }
    }

    private var underworkAlertBinding: Binding<Bool> {
        Binding(
            get: { vm.statement.showUnderworkInfo },
            set: { if !$0 { vm.hideUnderworkInfoForSession() } }
        )
    }

    // MARK: Топбар (стрелка · поле «Расшифровать код» · настройки)

    private var topBar: some View {
        HStack(spacing: 10) {
            Button { router.back() } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(DSColor.text)
                    .frame(width: 40, height: 40)
                    .background(DSColor.bgSubtle)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Назад"))

            Button { router.show(.payrollCodeSearch) } label: {
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(DSColor.textFaint)
                    Text("Расшифровать код")
                        .font(DSFont.sans(14))
                        .foregroundColor(DSColor.textFaint)
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, 12)
                .frame(height: 40)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(DSColor.borderStrong, lineWidth: 1)
                )
            }
            .buttonStyle(.plain)

            Button { router.showSettingSalary() } label: {
                Image(systemName: "gearshape")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(DSColor.text)
                    .frame(width: 40, height: 40)
                    .background(DSColor.bgSubtle)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Настройки зарплаты"))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    // MARK: 1. Переключатель месяца

    private var monthSwitcher: some View {
        HStack(spacing: 6) {
            Button { vm.previousMonth() } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(DSColor.accent)
                    .frame(width: 36, height: 36)
            }
            .buttonStyle(.plain)
            .opacity(vm.monthYearList.first { $0.year == vm.statement.year && Int($0.month) == Int(vm.statement.monthIndex) } == vm.monthYearList.first ? 0.35 : 1)

            Button { isMonthSheetShown = true } label: {
                Text("\(vm.statement.monthLabel)")
                    .font(DSFont.sans(17, .semibold))
                    .foregroundColor(DSColor.text)
            }
            .buttonStyle(.plain)

            Button { vm.nextMonth() } label: {
                Image(systemName: "chevron.right")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(DSColor.accent)
                    .frame(width: 36, height: 36)
            }
            .buttonStyle(.plain)
            .opacity(vm.monthYearList.last { $0.year == vm.statement.year && Int($0.month) == Int(vm.statement.monthIndex) } == vm.monthYearList.last ? 0.35 : 1)
        }
        .padding(.top, 4)
        .padding(.bottom, 10)
    }

    // MARK: 2. Hero «К ВЫДАЧЕ» / «ВСЕГО ОТРАБОТАНО»

    private var heroBlock: some View {
        HStack(alignment: .bottom, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text("К ВЫДАЧЕ")
                    .dsTextStyle(.group)
                HStack(alignment: .lastTextBaseline, spacing: 4) {
                    Text(vm.statement.toBeCreditedText)
                        .font(DSFont.mono(34, .heavy))
                        .tracking(-1.5)
                        .foregroundColor(DSColor.text)
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                    Text(vm.statement.currency)
                        .font(DSFont.mono(20, .bold))
                        .foregroundColor(DSColor.textMuted)
                }
            }
            Spacer(minLength: 8)
            VStack(alignment: .trailing, spacing: 6) {
                Text("ВСЕГО ОТРАБОТАНО")
                    .dsTextStyle(.group)
                Text(vm.statement.totalWorkText)
                    .font(DSFont.mono(17, .bold))
                    .foregroundColor(DSColor.text)
            }
        }
        .padding(.bottom, 14)
    }

    // MARK: 3. Предупреждение о тарифной ставке (BUG-38 — см. VM)

    private var tariffWarningCard: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "info.circle")
                .font(.system(size: 16))
                .foregroundColor(DSColor.warning)
            Text("Не установлена тарифная ставка. Перейдите в настройки для её указания.")
                .font(DSFont.sans(14))
                .foregroundColor(DSColor.text)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.warning.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        .padding(.bottom, 12)
    }

    // MARK: 4. Начисления

    private var accrualsCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("НАЧИСЛЕНИЯ")
                .dsTextStyle(.group)
                .padding(.bottom, 8)
            DSCard {
                payHead(cols: ["КОД", "ВИД ВЫПЛАТЫ", "ЧАСЫ", "%", "СУММА"], isAccrual: true)
                ForEach(Array(vm.statement.accrualRows.enumerated()), id: \.offset) { index, row in
                    if index > 0 { DSDivider(inset: 12) }
                    payRow(row, isAccrual: true)
                }
                DSDivider(inset: 0)
                payTotalRow(name: "Всего начислено", amount: vm.statement.totalChargedText)
            }
        }
        .padding(.top, 6)
    }

    // MARK: 5. Удержания

    private var deductionsCard: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("УДЕРЖАНИЯ")
                .dsTextStyle(.group)
                .padding(.top, 18)
                .padding(.bottom, 8)
            DSCard {
                payHead(cols: ["КОД", "ВИД УДЕРЖАНИЯ", "%", "СУММА"], isAccrual: false)
                ForEach(Array(vm.statement.deductionRows.enumerated()), id: \.offset) { index, row in
                    if index > 0 { DSDivider(inset: 12) }
                    payRow(row, isAccrual: false)
                }
                DSDivider(inset: 0)
                payTotalRow(name: "Всего удержано", amount: vm.statement.totalRetentionText)
            }
        }
    }

    // Таблица: сетка столбцов согласована с Android (КОД | … | СУММА).
    private func payHead(cols: [String], isAccrual: Bool) -> some View {
        HStack(spacing: 8) {
            Text(cols[0])
                .frame(width: 52, alignment: .leading)
            Text(cols[1])
                .frame(maxWidth: .infinity, alignment: .leading)
            if isAccrual {
                Text(cols[2]).frame(width: 48, alignment: .trailing)
                Text(cols[3]).frame(width: 42, alignment: .trailing)
                Text(cols[4]).frame(width: 78, alignment: .trailing)
            } else {
                Text(cols[2]).frame(width: 42, alignment: .trailing)
                Text(cols[3]).frame(width: 78, alignment: .trailing)
            }
        }
        .dsTextStyle(.labelMuted)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
    }

    private func payRow(_ row: SalaryStatementRow, isAccrual: Bool) -> some View {
        HStack(spacing: 8) {
            Text(row.code)
                .font(DSFont.mono(12, .semibold))
                .foregroundColor(row.code == "—" ? DSColor.textFaint : DSColor.textMuted)
                .frame(width: 52, alignment: .leading)
            Text(row.title)
                .font(DSFont.sans(14, .medium))
                .foregroundColor(DSColor.text)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)
            if isAccrual {
                valueCell(row.hoursText, width: 48)
                valueCell(row.percentText, width: 42)
            } else {
                valueCell(row.percentText, width: 42)
            }
            Text(row.moneyText)
                .font(DSFont.mono(14, .semibold))
                .foregroundColor(DSColor.text)
                .frame(width: 78, alignment: .trailing)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 11)
    }

    @ViewBuilder
    private func valueCell(_ text: String?, width: CGFloat) -> some View {
        Text(text ?? "—")
            .font(DSFont.mono(12, .semibold))
            .foregroundColor(text == nil ? DSColor.textFaint : DSColor.textMuted)
            .frame(width: width, alignment: .trailing)
    }

    private func payTotalRow(name: String, amount: String) -> some View {
        HStack {
            Text(name)
                .font(DSFont.sans(15, .bold))
                .foregroundColor(DSColor.accent)
            Spacer(minLength: 8)
            Text(amount)
                .font(DSFont.mono(16, .bold))
                .foregroundColor(DSColor.accent)
                .lineLimit(1)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 13)
        .background(DSColor.accentSoft)
    }

    // MARK: 6. Тёмный блок «К ВЫДАЧЕ»

    private var payoutBlock: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("К ВЫДАЧЕ")
                    .font(DSFont.mono(11, .semibold))
                    .tracking(1.4)
                    .foregroundColor(DSColor.accentInk.opacity(0.6))
                Text("\(vm.statement.toBeCreditedText) \(vm.statement.currency)")
                    .font(DSFont.mono(26, .heavy))
                    .tracking(-1)
                    .foregroundColor(DSColor.accentInk)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
            }
            Spacer(minLength: 8)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 20)
        .background(DSColor.accent)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        .padding(.top, 20)
    }

    // MARK: 7. Пояснение

    private var disclaimer: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "info.circle")
                .font(.system(size: 14))
                .foregroundColor(DSColor.textMuted)
            Text("Расчёт носит информационный характер: некоторые виды выплат могут отличаться в зависимости от нормативных документов вашего депо.")
                .font(DSFont.sans(12))
                .foregroundColor(DSColor.textMuted)
        }
        .padding(.top, 14)
        .padding(.horizontal, 4)
    }
}
