import SwiftUI
import ComposeApp

struct FormLocoView: View {
    let routeId: String
    let locoId: String?
    @StateObject private var vm = LocoFormViewModelWrapper()
    @Environment(\.dismiss) private var dismiss

    @State private var acceptanceStart: Date = Date()
    @State private var acceptanceEnd: Date = Date()
    @State private var deliveryStart: Date = Date()
    @State private var deliveryEnd: Date = Date()
    @State private var showAcceptanceStart = false
    @State private var showAcceptanceEnd = false
    @State private var showDeliveryStart = false
    @State private var showDeliveryEnd = false

    var body: some View {
        Form {
            Section("Локомотив") {
                TextField("Серия", text: Binding(
                    get: { vm.loco?.series ?? "" },
                    set: { vm.setSeries($0) }
                ))
                TextField("Номер", text: Binding(
                    get: { vm.loco?.number ?? "" },
                    set: { vm.setNumber($0) }
                ))
                .keyboardType(.numberPad)

                Picker("Тип", selection: Binding(
                    get: { vm.loco?.type ?? LocoType.electric },
                    set: { vm.setType($0) }
                )) {
                    Text("Электровоз").tag(LocoType.electric)
                    Text("Тепловоз").tag(LocoType.diesel)
                }
            }

            Section("Приёмка") {
                dateRow(label: "Начало", date: $acceptanceStart, show: $showAcceptanceStart) {
                    vm.setTimeStartAcceptance(TimeFormatter.dateToMs(acceptanceStart))
                }
                dateRow(label: "Окончание", date: $acceptanceEnd, show: $showAcceptanceEnd) {
                    vm.setTimeEndAcceptance(TimeFormatter.dateToMs(acceptanceEnd))
                }
            }

            Section("Сдача") {
                dateRow(label: "Начало", date: $deliveryStart, show: $showDeliveryStart) {
                    vm.setTimeStartDelivery(TimeFormatter.dateToMs(deliveryStart))
                }
                dateRow(label: "Окончание", date: $deliveryEnd, show: $showDeliveryEnd) {
                    vm.setTimeEndDelivery(TimeFormatter.dateToMs(deliveryEnd))
                }
            }

            if let loco = vm.loco {
                if loco.type == LocoType.electric, !loco.electricSectionList.isEmpty {
                    Section("Секции (электроэнергия)") {
                        ForEach(loco.electricSectionList as! [SectionElectric], id: \.sectionId) { section in
                            VStack(alignment: .leading, spacing: 4) {
                                Text("Секция")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                HStack {
                                    LabeledContent("Принято") {
                                        Text(section.acceptedEnergy.map { String(format: "%.1f", $0) } ?? "—")
                                    }
                                    LabeledContent("Сдано") {
                                        Text(section.deliveryEnergy.map { String(format: "%.1f", $0) } ?? "—")
                                    }
                                }
                                .font(.subheadline)
                            }
                        }
                    }
                }
                if loco.type == LocoType.diesel, !loco.dieselSectionList.isEmpty {
                    Section("Секции (топливо)") {
                        ForEach(loco.dieselSectionList as! [SectionDiesel], id: \.sectionId) { section in
                            HStack {
                                LabeledContent("Принято") {
                                    Text(section.acceptedFuel.map { String(format: "%.1f", $0) } ?? "—")
                                }
                                LabeledContent("Сдано") {
                                    Text(section.deliveryFuel.map { String(format: "%.1f", $0) } ?? "—")
                                }
                            }
                            .font(.subheadline)
                        }
                    }
                }
            }
        }
        .navigationTitle(locoId == nil ? "Новый локомотив" : "Локомотив")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { vm.load(routeId: routeId, locoId: locoId) }
        .onChange(of: vm.isSaved) { if $0 { dismiss() } }
    }

    private func dateRow(label: String, date: Binding<Date>, show: Binding<Bool>, onChange: @escaping () -> Void) -> some View {
        VStack(alignment: .leading) {
            Button {
                show.wrappedValue.toggle()
            } label: {
                HStack {
                    Text(label)
                        .foregroundColor(.primary)
                    Spacer()
                    Text(TimeFormatter.formatDateTime(ms: TimeFormatter.dateToMs(date.wrappedValue)))
                        .foregroundColor(.secondary)
                }
            }
            if show.wrappedValue {
                DatePicker("", selection: date, displayedComponents: [.date, .hourAndMinute])
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                    .onChange(of: date.wrappedValue) { _ in onChange() }
            }
        }
    }
}
