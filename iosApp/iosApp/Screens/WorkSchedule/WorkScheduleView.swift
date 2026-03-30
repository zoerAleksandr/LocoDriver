import SwiftUI

struct WorkScheduleView: View {
    private let columns = Array(repeating: GridItem(.flexible(), spacing: 4), count: 7)
    private let weekdays = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                LazyVGrid(columns: columns, spacing: 4) {
                    ForEach(weekdays, id: \.self) { day in
                        Text(day)
                            .font(.caption2)
                            .fontWeight(.medium)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity)
                    }
                }

                LazyVGrid(columns: columns, spacing: 4) {
                    ForEach(1...31, id: \.self) { day in
                        let isWeekend = (day % 7 == 0 || day % 7 == 6)
                        Text("\(day)")
                            .font(.caption)
                            .frame(maxWidth: .infinity, minHeight: 34)
                            .background(isWeekend ? Color.green.opacity(0.25) : Color.blue.opacity(0.15))
                            .cornerRadius(6)
                    }
                }
            }
            .padding()
        }
        .navigationTitle("График работы")
    }
}
