import SwiftUI
import ComposeApp

struct FormView: View {
    let routeId: String?
    @StateObject private var vm = FormViewModelWrapper()
    @Environment(\.dismiss) private var dismiss

    // DatePicker state – always present, defaulting to now
    @State private var startWorkDate: Date = Date()
    @State private var endWorkDate: Date = Date()

    var body: some View {
        Group {
            if vm.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                formContent
            }
        }
        .navigationTitle(routeId == nil ? "Новый маршрут" : "Маршрут")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Сохранить") { vm.saveRoute() }
            }
        }
        .onAppear { vm.loadRoute(id: routeId) }
        // Sync DatePicker state when route loads
        .onChange(of: vm.route) { route in
            if let startMs = route?.basicData.timeStartWork?.int64Value, startMs > 0 {
                startWorkDate = TimeFormatter.msToDate(startMs)
            }
            if let endMs = route?.basicData.timeEndWork?.int64Value, endMs > 0 {
                endWorkDate = TimeFormatter.msToDate(endMs)
            }
        }
        .onChange(of: vm.isSaved) { saved in
            if saved { dismiss() }
        }
        .alert("Ошибка", isPresented: .constant(vm.errorMessage != nil)) {
            Button("OK") {}
        } message: {
            Text(vm.errorMessage ?? "")
        }
    }

    private var formContent: some View {
        Form {
            // ── Основные данные ──────────────────────────────────────────
            Section("Основные данные") {
                TextField("Номер маршрута", text: Binding(
                    get: { vm.route?.basicData.number ?? "" },
                    set: { vm.updateNumber($0) }
                ))
            }

            // ── Время работы ─────────────────────────────────────────────
            Section(header: Text("Время работы")) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Начало работы")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    DatePicker(
                        "",
                        selection: Binding(
                            get: { startWorkDate },
                            set: { date in
                                startWorkDate = date
                                vm.setTimeStartWork(TimeFormatter.dateToMs(date))
                            }
                        ),
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    .labelsHidden()
                    .datePickerStyle(.compact)
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text("Конец работы")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    DatePicker(
                        "",
                        selection: Binding(
                            get: { endWorkDate },
                            set: { date in
                                endWorkDate = date
                                vm.setTimeEndWork(TimeFormatter.dateToMs(date))
                            }
                        ),
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    .labelsHidden()
                    .datePickerStyle(.compact)
                }
            }

            // ── Локомотивы ───────────────────────────────────────────────
            Section("Локомотивы") {
                let locos = vm.route?.locomotives as? [DomainLocomotive] ?? []
                ForEach(locos, id: \.locoId) { loco in
                    NavigationLink(destination: FormLocoView(
                        routeId: vm.route?.basicData.id ?? "",
                        locoId: loco.locoId
                    )) {
                        HStack {
                            Image(systemName: "tram.fill")
                                .foregroundColor(.accentColor)
                            Text([loco.series, loco.number]
                                .compactMap { $0 }
                                .joined(separator: " ")
                                .isEmpty ? "Локомотив" :
                                [loco.series, loco.number]
                                    .compactMap { $0 }
                                    .joined(separator: " "))
                        }
                    }
                }
                NavigationLink(destination: FormLocoView(
                    routeId: vm.route?.basicData.id ?? "",
                    locoId: nil
                )) {
                    Label("Добавить локомотив", systemImage: "plus.circle")
                        .foregroundColor(.accentColor)
                }
            }

            // ── Поезда ───────────────────────────────────────────────────
            Section("Поезда") {
                let trains = vm.route?.trains as? [DomainTrain] ?? []
                ForEach(trains, id: \.trainId) { train in
                    NavigationLink(destination: FormTrainView(
                        routeId: vm.route?.basicData.id ?? "",
                        trainId: train.trainId
                    )) {
                        HStack {
                            Image(systemName: "car.2.fill")
                                .foregroundColor(.accentColor)
                            Text("Поезд \(train.number ?? "—")")
                        }
                    }
                }
                NavigationLink(destination: FormTrainView(
                    routeId: vm.route?.basicData.id ?? "",
                    trainId: nil
                )) {
                    Label("Добавить поезд", systemImage: "plus.circle")
                        .foregroundColor(.accentColor)
                }
            }

            // ── Пассажирские ─────────────────────────────────────────────
            Section("Пассажирские") {
                let passengers = vm.route?.passengers as? [DomainPassenger] ?? []
                ForEach(passengers, id: \.passengerId) { passenger in
                    NavigationLink(destination: FormPassengerView(
                        routeId: vm.route?.basicData.id ?? "",
                        passengerId: passenger.passengerId
                    )) {
                        HStack {
                            Image(systemName: "person.2.fill")
                                .foregroundColor(.accentColor)
                            Text("Поезд \(passenger.trainNumber ?? "—")")
                        }
                    }
                }
                NavigationLink(destination: FormPassengerView(
                    routeId: vm.route?.basicData.id ?? "",
                    passengerId: nil
                )) {
                    Label("Добавить пассажирский", systemImage: "plus.circle")
                        .foregroundColor(.accentColor)
                }
            }

            // ── Заметки ──────────────────────────────────────────────────
            Section("Заметки") {
                TextField("Заметки", text: Binding(
                    get: { vm.route?.basicData.notes ?? "" },
                    set: { vm.updateNotes($0) }
                ), axis: .vertical)
                .lineLimit(3...6)
            }
        }
    }
}
