import SwiftUI
import ComposeApp

struct FormView: View {
    let routeId: String?
    @StateObject private var vm = FormViewModelWrapper()
    @Environment(\.dismiss) private var dismiss

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
            Section("Основные данные") {
                TextField("Номер маршрута", text: Binding(
                    get: { vm.route?.basicData.number ?? "" },
                    set: { vm.updateNumber($0) }
                ))

                if let startMs = vm.route?.basicData.timeStartWork as? Int64, startMs > 0 {
                    LabeledContent("Начало работы", value: TimeFormatter.formatDateTime(ms: startMs))
                }
                if let endMs = vm.route?.basicData.timeEndWork as? Int64, endMs > 0 {
                    LabeledContent("Окончание работы", value: TimeFormatter.formatDateTime(ms: endMs))
                }
            }

            if let locos = vm.route?.locomotives, locos.count > 0 {
                Section("Локомотивы") {
                    ForEach(locos as! [DomainLocomotive], id: \.locoId) { loco in
                        NavigationLink(destination: Text("Локомотив").navigationTitle("Локомотив")) {
                            HStack {
                                Image(systemName: "tram.fill")
                                    .foregroundColor(.accentColor)
                                Text([loco.series, loco.number].compactMap { $0 }.joined(separator: " "))
                            }
                        }
                    }
                }
            }

            if let trains = vm.route?.trains, trains.count > 0 {
                Section("Поезда") {
                    ForEach(trains as! [DomainTrain], id: \.trainId) { train in
                        NavigationLink(destination: Text("Поезд").navigationTitle("Поезд")) {
                            HStack {
                                Image(systemName: "car.2.fill")
                                    .foregroundColor(.accentColor)
                                Text("Поезд \(train.number ?? "—")")
                            }
                        }
                    }
                }
            }

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
