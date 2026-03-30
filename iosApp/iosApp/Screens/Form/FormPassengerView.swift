import SwiftUI
import ComposeApp

struct FormPassengerView: View {
    let routeId: String
    let passengerId: String?
    @Environment(\.dismiss) private var dismiss

    @State private var trainNumber: String = ""
    @State private var stationDeparture: String = ""
    @State private var stationArrival: String = ""
    @State private var timeDeparture: Date = Date()
    @State private var timeArrival: Date = Date()
    @State private var showDeparture = false
    @State private var showArrival = false

    private var durationMs: Int64 {
        let dep = TimeFormatter.dateToMs(timeDeparture)
        let arr = TimeFormatter.dateToMs(timeArrival)
        return arr > dep ? arr - dep : 0
    }

    var body: some View {
        Form {
            Section("Поезд") {
                TextField("Номер поезда", text: $trainNumber)
                    .keyboardType(.numberPad)
            }

            Section("Маршрут") {
                TextField("Станция отправления", text: $stationDeparture)
                TextField("Станция прибытия", text: $stationArrival)
            }

            Section("Время") {
                Button { showDeparture.toggle() } label: {
                    HStack {
                        Text("Отправление").foregroundColor(.primary)
                        Spacer()
                        Text(TimeFormatter.formatDateTime(ms: TimeFormatter.dateToMs(timeDeparture)))
                            .foregroundColor(.secondary)
                    }
                }
                if showDeparture {
                    DatePicker("", selection: $timeDeparture, displayedComponents: [.date, .hourAndMinute])
                        .datePickerStyle(.wheel)
                        .labelsHidden()
                }

                Button { showArrival.toggle() } label: {
                    HStack {
                        Text("Прибытие").foregroundColor(.primary)
                        Spacer()
                        Text(TimeFormatter.formatDateTime(ms: TimeFormatter.dateToMs(timeArrival)))
                            .foregroundColor(.secondary)
                    }
                }
                if showArrival {
                    DatePicker("", selection: $timeArrival, displayedComponents: [.date, .hourAndMinute])
                        .datePickerStyle(.wheel)
                        .labelsHidden()
                }
            }

            if durationMs > 0 {
                Section {
                    LabeledContent("В пути", value: TimeFormatter.formatDuration(ms: durationMs))
                }
            }
        }
        .navigationTitle(passengerId == nil ? "Пассажирский" : "Пассажирский")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Сохранить") { dismiss() }
            }
        }
    }
}
