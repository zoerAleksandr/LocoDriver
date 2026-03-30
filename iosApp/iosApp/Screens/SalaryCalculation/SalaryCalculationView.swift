import SwiftUI
import ComposeApp

struct SalaryCalculationView: View {
    @StateObject private var vm = SalaryCalculationViewModelWrapper()

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView("Загрузка...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let summary = vm.summary {
                ScrollView {
                    VStack(spacing: 16) {
                        summaryCard(summary: summary)
                    }
                    .padding()
                }
            } else {
                Text("Нет данных")
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle("Зарплата")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func summaryCard(summary: SalaryCalculationIosViewModel.MonthlySummary) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(summary.month)
                .font(.headline)

            Divider()

            HStack {
                Label("\(summary.routeCount) маршрутов", systemImage: "list.bullet")
                Spacer()
                Label("\(summary.totalWorkHours)ч \(summary.totalWorkMinutes)м", systemImage: "clock")
            }
            .font(.subheadline)
            .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(UIColor.secondarySystemBackground))
        .cornerRadius(12)
    }
}
