import SwiftUI
import ComposeApp

struct FormPassengerView: View {
    let routeId: String
    let passengerId: String?

    @StateObject private var vm = PassengerFormViewModelWrapper()
    @Environment(\.dismiss) private var dismiss

    @State private var showDeparture = false
    @State private var showArrival = false

    // Local Date state for DatePicker binding; kept in sync with vm.passenger
    @State private var timeDepartureDate: Date = Date()
    @State private var timeArrivalDate: Date = Date()

    private var durationMs: Int64 {
        guard let dep = vm.passenger?.timeDeparture as? Int64,
              let arr = vm.passenger?.timeArrival as? Int64,
              arr > dep else { return 0 }
        return arr - dep
    }

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                formContent
            }
        }
        .navigationTitle(passengerId == nil ? "Новый пассажирский" : "Пассажирский")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Сохранить") { vm.savePassenger() }
            }
        }
        .onAppear {
            vm.load(routeId: routeId, passengerId: passengerId)
        }
        .onChange(of: vm.passenger) { p in
            // Sync DatePicker state from viewModel when passenger first loads
            if let depMs = p?.timeDeparture as? Int64, depMs > 0 {
                timeDepartureDate = TimeFormatter.msToDate(depMs)
            }
            if let arrMs = p?.timeArrival as? Int64, arrMs > 0 {
                timeArrivalDate = TimeFormatter.msToDate(arrMs)
            }
        }
        .onChange(of: vm.isSaved) { saved in
            if saved { dismiss() }
        }
        .alert(
            vm.error?.userMessage ?? "Ошибка",
            isPresented: Binding(
                get: { vm.error != nil },
                set: { if !$0 { vm.clearError() } }
            )
        ) {
            if vm.error?.canRetry == true {
                Button("Повторить") { vm.retry() }
            }
            Button("OK", role: .cancel) {}
        }
    }

    private var formContent: some View {
        Form {
            Section("Поезд") {
                TextField("Номер поезда", text: Binding(
                    get: { vm.passenger?.trainNumber ?? "" },
                    set: { vm.setTrainNumber($0) }
                ))
                .keyboardType(.numberPad)
            }

            Section("Маршрут") {
                TextField("Станция отправления", text: Binding(
                    get: { vm.passenger?.stationDeparture ?? "" },
                    set: { vm.setDepartureStation($0) }
                ))

                TextField("Станция прибытия", text: Binding(
                    get: { vm.passenger?.stationArrival ?? "" },
                    set: { vm.setArrivalStation($0) }
                ))
            }

            Section("Время") {
                Button {
                    showDeparture.toggle()
                    if showArrival { showArrival = false }
                } label: {
                    HStack {
                        Text("Отправление").foregroundColor(.primary)
                        Spacer()
                        if let depMs = vm.passenger?.timeDeparture as? Int64, depMs > 0 {
                            Text(TimeFormatter.formatDateTime(ms: depMs))
                                .foregroundColor(.secondary)
                        } else {
                            Text("Не задано").foregroundColor(.secondary)
                        }
                    }
                }
                if showDeparture {
                    DatePicker(
                        "",
                        selection: Binding(
                            get: { timeDepartureDate },
                            set: { date in
                                timeDepartureDate = date
                                vm.setTimeDeparture(TimeFormatter.dateToMs(date))
                            }
                        ),
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                }

                Button {
                    showArrival.toggle()
                    if showDeparture { showDeparture = false }
                } label: {
                    HStack {
                        Text("Прибытие").foregroundColor(.primary)
                        Spacer()
                        if let arrMs = vm.passenger?.timeArrival as? Int64, arrMs > 0 {
                            Text(TimeFormatter.formatDateTime(ms: arrMs))
                                .foregroundColor(.secondary)
                        } else {
                            Text("Не задано").foregroundColor(.secondary)
                        }
                    }
                }
                if showArrival {
                    DatePicker(
                        "",
                        selection: Binding(
                            get: { timeArrivalDate },
                            set: { date in
                                timeArrivalDate = date
                                vm.setTimeArrival(TimeFormatter.dateToMs(date))
                            }
                        ),
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                }
            }

            if durationMs > 0 {
                Section {
                    LabeledContent("В пути", value: TimeFormatter.formatDuration(ms: durationMs))
                }
            }

            Section("Заметки") {
                TextField("Заметки", text: Binding(
                    get: { vm.passenger?.notes ?? "" },
                    set: { vm.setNotes($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
            }
        }
    }
}
