import SwiftUI
import ComposeApp

struct FormTrainView: View {
    let routeId: String
    let trainId: String?
    @StateObject private var vm = TrainFormViewModelWrapper()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Form {
            Section("Поезд") {
                TextField("Номер поезда", text: Binding(
                    get: { vm.train?.number ?? "" },
                    set: { vm.setNumber($0) }
                ))
                .keyboardType(.numberPad)
            }

            Section("Характеристики") {
                TextField("Вес, т", text: Binding(
                    get: { vm.train?.weight ?? "" },
                    set: { vm.setWeight($0) }
                ))
                .keyboardType(.decimalPad)

                TextField("Осей", text: Binding(
                    get: { vm.train?.axle ?? "" },
                    set: { vm.setAxle($0) }
                ))
                .keyboardType(.numberPad)

                TextField("Расстояние, км", text: Binding(
                    get: { vm.train?.distance ?? "" },
                    set: { vm.setDistance($0) }
                ))
                .keyboardType(.decimalPad)

                TextField("Длина (усл. ваг.)", text: Binding(
                    get: { vm.train?.conditionalLength ?? "" },
                    set: { vm.setLength($0) }
                ))
                .keyboardType(.decimalPad)
            }
        }
        .navigationTitle(trainId == nil ? "Новый поезд" : "Поезд")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { vm.load(routeId: routeId, trainId: trainId) }
        .onChange(of: vm.isSaved) { if $0 { dismiss() } }
    }
}
