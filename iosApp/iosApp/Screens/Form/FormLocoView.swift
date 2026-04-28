import SwiftUI
import ComposeApp

struct FormLocoView: View {
    let routeId: String
    let locoId: String?
    @StateObject private var vm = LocoFormViewModelWrapper()
    @Environment(\.dismiss) private var dismiss

    // Локальные даты для DatePicker-ов; nil — время не указано.
    @State private var acceptanceStart: Date? = nil
    @State private var acceptanceEnd: Date? = nil
    @State private var deliveryStart: Date? = nil
    @State private var deliveryEnd: Date? = nil

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                locoTypePicker
                basicDataSection
                acceptanceSection
                deliverySection
                sectionsSection
                Color.clear.frame(height: 32)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .background(Color.appBg)
        .navigationTitle(locoId == nil ? "Новый локомотив" : "Локомотив")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Сохранить") { vm.saveLoco() }
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(Color.appAccent)
            }
        }
        .onAppear { vm.load(routeId: routeId, locoId: locoId) }
        .onChange(of: vm.isSaved) { if $0 { dismiss() } }
        .onChange(of: vm.loco) { loco in
            // Синхронизируем локальные Date-стейты с доменной моделью
            // (может поменяться при первой загрузке или после сохранения).
            acceptanceStart = msToDateOrNil(loco?.timeStartOfAcceptance?.int64Value)
            acceptanceEnd   = msToDateOrNil(loco?.timeEndOfAcceptance?.int64Value)
            deliveryStart   = msToDateOrNil(loco?.timeStartOfDelivery?.int64Value)
            deliveryEnd     = msToDateOrNil(loco?.timeEndOfDelivery?.int64Value)
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

    // MARK: - Вид тяги

    private var locoTypePicker: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Вид тяги")
                .font(.system(size: 13))
                .foregroundStyle(.secondary)
                .padding(.bottom, 2)

            Picker("Вид тяги", selection: Binding(
                get: { vm.loco?.type ?? DomainLocoType.electric },
                set: { vm.setType($0) }
            )) {
                Text("Электротяга").tag(DomainLocoType.electric)
                Text("Теплотяга").tag(DomainLocoType.diesel)
            }
            .pickerStyle(.segmented)
        }
    }

    // MARK: - Серия + номер

    private var basicDataSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Локомотив")
                .font(.system(size: 13))
                .foregroundStyle(.secondary)
                .padding(.bottom, 2)

            VStack(spacing: 0) {
                TextField("Серия", text: Binding(
                    get: { vm.loco?.series ?? "" },
                    set: { vm.setSeries($0) }
                ))
                .font(.system(size: 15))
                .padding(14)

                Divider().padding(.leading, 14)

                TextField("Номер", text: Binding(
                    get: { vm.loco?.number ?? "" },
                    set: { vm.setNumber($0) }
                ))
                .font(.system(size: 15))
                .keyboardType(.numberPad)
                .padding(14)
            }
            .background(Color.appCard)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    // MARK: - Приёмка / Сдача

    private var acceptanceSection: some View {
        timeRangeCard(
            title: "Приёмка",
            startDate: $acceptanceStart,
            endDate: $acceptanceEnd,
            onStartChanged: { vm.setTimeStartAcceptance($0.map { TimeFormatter.dateToMs($0) }) },
            onEndChanged:   { vm.setTimeEndAcceptance($0.map { TimeFormatter.dateToMs($0) }) }
        )
    }

    private var deliverySection: some View {
        timeRangeCard(
            title: "Сдача",
            startDate: $deliveryStart,
            endDate: $deliveryEnd,
            onStartChanged: { vm.setTimeStartDelivery($0.map { TimeFormatter.dateToMs($0) }) },
            onEndChanged:   { vm.setTimeEndDelivery($0.map { TimeFormatter.dateToMs($0) }) }
        )
    }

    /// Карточка с парой «Начало» + «Окончание». На iPhone DatePicker.compact
    /// занимает почти всю ширину, поэтому пара упакована в одну карточку
    /// двумя строками (а не двумя колонками), разделёнными Divider'ом.
    @ViewBuilder
    private func timeRangeCard(
        title: String,
        startDate: Binding<Date?>,
        endDate: Binding<Date?>,
        onStartChanged: @escaping (Date?) -> Void,
        onEndChanged: @escaping (Date?) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 13))
                .foregroundStyle(.secondary)
                .padding(.bottom, 2)

            VStack(spacing: 0) {
                timeCell(
                    label: "Начало",
                    date: startDate,
                    onChanged: onStartChanged
                )
                Divider().padding(.leading, 14)
                timeCell(
                    label: "Окончание",
                    date: endDate,
                    defaultFrom: { startDate.wrappedValue?.addingTimeInterval(3600) ?? Date() },
                    onChanged: onEndChanged
                )
            }
            .background(Color.appCard)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    @ViewBuilder
    private func timeCell(
        label: String,
        date: Binding<Date?>,
        defaultFrom: (() -> Date)? = nil,
        onChanged: @escaping (Date?) -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)

            if let current = date.wrappedValue {
                DatePicker(
                    "",
                    selection: Binding(
                        get: { current },
                        set: { newValue in
                            date.wrappedValue = newValue
                            onChanged(newValue)
                        }
                    ),
                    displayedComponents: [.date, .hourAndMinute]
                )
                .labelsHidden()
                .datePickerStyle(.compact)
            } else {
                Button(action: {
                    let newDate = defaultFrom?() ?? Date()
                    date.wrappedValue = newDate
                    onChanged(newDate)
                }) {
                    HStack(spacing: 6) {
                        Image(systemName: "plus.circle")
                            .font(.system(size: 14))
                        Text("Указать")
                    }
                    .foregroundStyle(Color.appAccent)
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
    }

    // MARK: - Секции

    @ViewBuilder
    private var sectionsSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Секции")
                .font(.system(size: 13))
                .foregroundStyle(.secondary)
                .padding(.bottom, 2)

            VStack(spacing: 0) {
                sectionsContent

                // «Добавить секцию» — общая строка-кнопка под списком.
                Divider().padding(.leading, 14)
                Button(action: { vm.addSection() }) {
                    HStack(spacing: 12) {
                        Image(systemName: "plus.circle")
                            .font(.system(size: 14))
                            .foregroundStyle(Color.appAccent)
                            .frame(width: 20)
                        Text(addSectionTitle)
                            .foregroundStyle(Color.appAccent)
                        Spacer()
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            .background(Color.appCard)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    @ViewBuilder
    private var sectionsContent: some View {
        if let loco = vm.loco {
            if loco.type == DomainLocoType.electric {
                let sections = (loco.electricSectionList as? [DomainSectionElectric]) ?? []
                ForEach(Array(sections.enumerated()), id: \.element.sectionId) { idx, section in
                    electricSectionRow(index: idx, section: section)
                    if idx < sections.count - 1 {
                        Divider().padding(.leading, 14)
                    }
                }
            } else {
                let sections = (loco.dieselSectionList as? [DomainSectionDiesel]) ?? []
                ForEach(Array(sections.enumerated()), id: \.element.sectionId) { idx, section in
                    dieselSectionRow(index: idx, section: section)
                    if idx < sections.count - 1 {
                        Divider().padding(.leading, 14)
                    }
                }
            }
        }
    }

    private func electricSectionRow(index: Int, section: DomainSectionElectric) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Электросекция \(index + 1)")
                .font(.system(size: 13, weight: .semibold))
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Принято").font(.caption).foregroundStyle(.secondary)
                    Text(section.acceptedEnergy.map { String(format: "%.1f кВт·ч", $0.doubleValue) } ?? "—")
                        .font(.subheadline)
                }
                Divider().frame(height: 28)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Сдано").font(.caption).foregroundStyle(.secondary)
                    Text(section.deliveryEnergy.map { String(format: "%.1f кВт·ч", $0.doubleValue) } ?? "—")
                        .font(.subheadline)
                }
                Spacer()
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func dieselSectionRow(index: Int, section: DomainSectionDiesel) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Дизельная секция \(index + 1)")
                .font(.system(size: 13, weight: .semibold))
            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Принято").font(.caption).foregroundStyle(.secondary)
                    Text(section.acceptedFuel.map { String(format: "%.1f л", $0.doubleValue) } ?? "—")
                        .font(.subheadline)
                }
                Divider().frame(height: 28)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Сдано").font(.caption).foregroundStyle(.secondary)
                    Text(section.deliveryFuel.map { String(format: "%.1f л", $0.doubleValue) } ?? "—")
                        .font(.subheadline)
                }
                Spacer()
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var addSectionTitle: String {
        let type = vm.loco?.type ?? DomainLocoType.electric
        return type == DomainLocoType.electric
            ? "Добавить электросекцию"
            : "Добавить дизельную секцию"
    }

    // MARK: - Helpers

    private func msToDateOrNil(_ ms: Int64?) -> Date? {
        guard let ms = ms, ms > 0 else { return nil }
        return TimeFormatter.msToDate(ms)
    }
}
