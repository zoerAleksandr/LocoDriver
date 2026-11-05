import SwiftUI
import ComposeApp

// MARK: - SalaryMonthSheet (SCREEN_SPECS §11.7)
// Нижняя шторка выбора месяца расчёта: чипы месяцев и лет, «Применить».

struct SalaryMonthSheet: View {
    let months: [MonthYearItem]
    let selectedYear: Int
    let selectedMonth: Int
    let onApply: (Int, Int) -> Void
    let onClose: () -> Void

    @State private var month: Int
    @State private var year: Int

    init(
        months: [MonthYearItem],
        selectedYear: Int,
        selectedMonth: Int,
        onApply: @escaping (Int, Int) -> Void,
        onClose: @escaping () -> Void
    ) {
        self.months = months
        self.selectedYear = selectedYear
        self.selectedMonth = selectedMonth
        self.onApply = onApply
        self.onClose = onClose
        _month = State(initialValue: selectedMonth)
        _year = State(initialValue: selectedYear)
    }

    private var monthNumbers: [Int] {
        Array(Set(months.map { Int($0.month) })).sorted()
    }

    private var years: [Int] {
        Array(Set(months.map { Int($0.year) })).sorted()
    }

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Выберите месяц и год", subtitle: nil), onClose: onClose) {
            VStack(alignment: .leading, spacing: 14) {
                flowChips(
                    title: "Месяц",
                    items: monthNumbers,
                    label: { SalaryMonthSheet.monthNames[$0 % 12] },
                    isSelected: { $0 == month },
                    onSelect: { month = $0 }
                )
                flowChips(
                    title: "Год",
                    items: years,
                    label: { "\($0)" },
                    isSelected: { $0 == year },
                    onSelect: { year = $0 }
                )
                DSCTAButton(title: "Применить") {
                    // Если пары нет в календаре — выбор молча игнорируется (§11.7).
                    onApply(year, month)
                }
                .padding(.top, 8)
            }
        }
    }

    private func flowChips(
        title: String,
        items: [Int],
        label: @escaping (Int) -> String,
        isSelected: @escaping (Int) -> Bool,
        onSelect: @escaping (Int) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .dsTextStyle(.labelMuted)
            // Простая переносимая сетка чипов (iOS 16 — без LazyVGrid-ячеек-бинов).
            FlexibleChipWrap(items: items, label: label, isSelected: isSelected, onSelect: onSelect)
        }
    }

    static let monthNames = [
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
    ]
}

/// Обёртка чипов с переносом строк (§11.7: все месяцы календаря + все годы).
private struct FlexibleChipWrap: View {
    let items: [Int]
    let label: (Int) -> String
    let isSelected: (Int) -> Bool
    let onSelect: (Int) -> Void

    private let columns = [GridItem(.adaptive(minimum: 88), spacing: 8)]

    var body: some View {
        LazyVGrid(columns: columns, alignment: .leading, spacing: 8) {
            ForEach(items, id: \.self) { item in
                DSChip(title: label(item), style: .choice(isSelected: isSelected(item))) {
                    onSelect(item)
                }
            }
        }
    }
}
