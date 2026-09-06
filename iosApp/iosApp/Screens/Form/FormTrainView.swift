import SwiftUI
import ComposeApp

struct FormTrainView: View {
    let routeId: String
    let trainId: String?
    @StateObject private var vm = TrainFormViewModelWrapper()
    @Environment(\.dismiss) private var dismiss

    // Локальное состояние пикера: значение из модели приходит асинхронно,
    // поэтому синхронизируем его в onChange, как в FormLocoView.
    @State private var couplingDate = Date()

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

            carInspectorSection
        }
        .navigationTitle(trainId == nil ? "Новый поезд" : "Поезд")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Сохранить") { vm.save() }
            }
        }
        .onAppear { vm.load(routeId: routeId, trainId: trainId) }
        .onChange(of: vm.couplingTimeMs) { ms in
            if ms > 0 { couplingDate = TimeFormatter.msToDate(ms) }
        }
        .onChange(of: vm.isSaved) { saved in
            if saved { dismiss() }
        }
        .alert(
            "Не удалось сохранить",
            isPresented: Binding(
                get: { vm.errorMessage != nil },
                set: { if !$0 { vm.clearError() } }
            )
        ) {
            Button("Понятно", role: .cancel) { vm.clearError() }
        } message: {
            Text(vm.errorMessage ?? "")
        }
    }

    /// Вагонник — осматривает и закрепляет состав перед прицепкой.
    /// Опционален: пока не добавлен, показываем одну кнопку.
    @ViewBuilder
    private var carInspectorSection: some View {
        Section("Вагонник") {
            if let inspector = vm.carInspector {
                TextField("ФИО", text: Binding(
                    get: { inspector.fullName ?? "" },
                    set: { vm.setCarInspectorFullName($0) }
                ))

                TextField("Табельный номер", text: Binding(
                    get: { inspector.tabNumber ?? "" },
                    set: { vm.setCarInspectorTabNumber($0) }
                ))
                .keyboardType(.numbersAndPunctuation)

                if vm.hasCouplingTime {
                    DatePicker(
                        "Время прицепки",
                        selection: Binding(
                            get: { couplingDate },
                            set: { couplingDate = $0; vm.setCouplingTime($0) }
                        ),
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    Button("Убрать время прицепки", role: .destructive) {
                        vm.clearCouplingTime()
                    }
                } else {
                    Button("Указать время прицепки") {
                        couplingDate = Date()
                        vm.setCouplingTime(couplingDate)
                    }
                }

                Button("Удалить вагонника", role: .destructive) {
                    vm.removeCarInspector()
                }
            } else {
                Button("Добавить вагонника") { vm.addCarInspector() }
            }
        }
    }
}
