import SwiftUI
import ComposeApp

// MARK: - TariffChangedSheet (SCREEN_SPECS §12.3)
// Шторка «Изменилась тарифная ставка»: выбор области применения ставки.

struct TariffChangedSheet: View {
    let onClose: () -> Void

    @StateObject private var vm = SettingSalaryViewModelWrapper.reusingActive()
    @State private var isDatePickerShown = false
    @State private var pickedDate = Date()

    private var router: AppRouter { AppRouter.shared }

    private var dateLabelText: String {
        vm.state.dateLabel
    }

    var body: some View {
        DSBottomSheet(
            header: .titleClose(title: "Изменилась тарифная ставка", subtitle: nil),
            onClose: {
                vm.hideDialogTariffRate()
                onClose()
            }
        ) {
            if isDatePickerShown {
                datePickerContent
            } else {
                actionsContent
            }
        }
    }

    // MARK: Основное содержимое

    private var actionsContent: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Для какого месяца сохранить тариф? Прошлые месяцы останутся со старой ставкой.")
                .font(DSFont.sans(14))
                .foregroundColor(DSColor.text)

            // Синяя строка даты: тап → выбор даты (§12.3). Две модальные шторки
            // одновременно не показываются — пикер открывается на месте шторки.
            Button {
                isDatePickerShown = true
            } label: {
                Text("Новый тариф начнёт действовать с \(dateLabelText.dropFirst(3))")
                    .font(DSFont.sans(14, .semibold))
                    .foregroundColor(DSColor.accent)
            }
            .buttonStyle(.plain)

            DSCTAButton(title: "Только для этого месяца") {
                vm.saveSettingAndOnlyMonthTariffRate(onSaved: exitAfterSave)
            }
            DSCTAButton(title: "Для этого и следующих") {
                vm.saveSettingAndTariffRateCurrentAndNextMonth(onSaved: exitAfterSave)
            }
            DSTonalButton(title: "Отмена", tone: .neutral) {
                vm.hideDialogTariffRate()
                onClose()
            }
        }
    }

    // MARK: Выбор даты (DateRangePicker → одиночная дата, первая выбранная)

    private var datePickerContent: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Дата начала действия нового тарифа")
                .dsTextStyle(.sectionH2)
            DatePicker(
                "Дата",
                selection: $pickedDate,
                displayedComponents: [.date]
            )
            .datePickerStyle(.graphical)
            .tint(DSColor.accent)
            .environment(\.locale, Locale(identifier: "ru_RU"))

            HStack(spacing: 12) {
                DSTonalButton(title: "Отмена", tone: .neutral) {
                    // Отмена пикера — снова показать шторку (§12.3).
                    isDatePickerShown = false
                }
                DSCTAButton(title: "Готово") {
                    let calendar = Calendar.current
                    vm.setDateSetTariffRate(
                        year: calendar.component(.year, from: pickedDate),
                        month: calendar.component(.month, from: pickedDate) - 1,
                        day: calendar.component(.day, from: pickedDate)
                    )
                    isDatePickerShown = false
                }
            }
        }
    }

    private func exitAfterSave() {
        vm.hideDialogTariffRate()
        onClose()
        router.back()
    }
}
