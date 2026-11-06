import SwiftUI
import ComposeApp

// Редактор напарника — SCREEN_SPECS §8.5.2. Макета нет: стиль справочников + кит.
// Автосейв 500 мс; выход стрелкой сохраняет немедленно. Запись с пустым ФИО не создаётся.
// Отклонение от Android (BUG-30): удаление — через стандартное подтверждение §24.3 (`.dsDeleteAlert`).

struct PartnerEditorView: View {
    let partnerId: String?

    @StateObject private var vm: PartnerEditorViewModelWrapper
    @State private var deleteRequest: DSDeleteRequest?

    init(partnerId: String?) {
        self.partnerId = partnerId
        _vm = StateObject(wrappedValue: PartnerEditorViewModelWrapper(partnerId: partnerId))
    }

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                Text("Напарник").dsTextStyle(.navTitle).lineLimit(1)
                HStack {
                    DSNavPillButton(icon: .chevronLeft, accessibilityLabel: "Назад") { leave() }
                    Spacer()
                }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.vertical, 12)
            .background(DSColor.bg)

            if let s = vm.state {
                content(s)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .dsDeleteAlert($deleteRequest)
        .onChange(of: vm.state?.deleted) { deleted in
            if deleted == true { AppRouter.shared.back() }
        }
        .alert(
            "Заменить постоянного напарника?",
            isPresented: Binding(get: { vm.state?.replaceRequest != nil }, set: { if !$0 { vm.dismissReplace() } }),
            presenting: vm.state?.replaceRequest
        ) { _ in
            Button("Отмена", role: .cancel) { vm.dismissReplace() }
            Button("Заменить") { vm.confirmReplace() }
        } message: { r in
            Text("Постоянный напарник сейчас: \(withPeriod(r.currentShort))\nПосле замены в новые маршруты будет автоматически добавляться \(withPeriod(r.candidateShort))")
        }
    }

    private func withPeriod(_ text: String) -> String { text.hasSuffix(".") ? text : text + "." }

    private func leave() {
        hideKeyboard()
        vm.saveNow()
        AppRouter.shared.back()
    }

    private func content(_ s: PartnerEditorScreenState) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                DSGroupHeader("ДАННЫЕ НАПАРНИКА", topSpacing: DSMetrics.groupHeadFirst)
                DSCard {
                    PartnerFieldRow(label: "ФИО", placeholder: "Фамилия Имя Отчество", external: s.fullName,
                                    keyboard: .default, isMono: false) { vm.setFullName($0) }
                    DSDivider()
                    PartnerFieldRow(label: "Табельный №", placeholder: "1234", external: s.tabNumber,
                                    keyboard: .numberPad, isMono: true) { vm.setTabNumber($0) }
                    DSDivider()
                    PartnerFieldRow(label: "Примечания", placeholder: "Плечи, контакты", external: s.notes,
                                    keyboard: .default, isMono: false) { vm.setNotes($0) }
                }
                Text("Напарник сохранится в справочнике — в следующий раз его можно будет быстро выбрать в маршруте.")
                    .font(DSFont.sans(13)).foregroundColor(DSColor.textMuted)
                    .padding(.horizontal, DSMetrics.groupHeadPadX)
                    .padding(.top, 8)

                DSCard {
                    HStack(spacing: 14) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Постоянный напарник").font(DSFont.sans(16, .semibold)).foregroundColor(DSColor.text)
                            Text("Автоматически добавляется в каждый новый маршрут. Постоянным может быть только один напарник.")
                                .font(DSFont.sans(13)).foregroundColor(DSColor.textMuted)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        Spacer(minLength: 8)
                        Toggle("", isOn: Binding(get: { s.isPermanent }, set: { vm.setPermanent($0) }))
                            .labelsHidden()
                            .tint(DSColor.accent)
                    }
                    .padding(.horizontal, DSMetrics.rowPadX)
                    .padding(.vertical, 14)
                }
                .padding(.top, 12)

                if !s.isNew {
                    DSDangerButton(title: "Удалить напарника") {
                        hideKeyboard()
                        deleteRequest = DSDeleteRequest(title: "Удалить напарника?") { vm.delete() }
                    }
                    .padding(.top, 20)
                }
                Spacer(minLength: 32)
            }
            .padding(.horizontal, DSMetrics.screenPadX)
        }
        .passScrollDismissesKeyboard()
    }
}

/// Строка карточки: подпись и поле (локальный текст, чтобы набор не «прыгал»).
private struct PartnerFieldRow: View {
    let label: String
    let placeholder: String
    let external: String
    let keyboard: UIKeyboardType
    let isMono: Bool
    let onChange: (String) -> Void

    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label.uppercased()).dsTextStyle(.group)
            TextField(placeholder, text: $text)
                .keyboardType(keyboard)
                .focused($focused)
                .font(isMono ? DSFont.mono(17, .medium) : DSFont.sans(17, .medium))
                .foregroundColor(DSColor.text)
                .autocorrectionDisabled()
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .onAppear { text = external }
        .onChange(of: text) { value in if value != external { onChange(value) } }
        .onChange(of: external) { value in if !focused { text = value } }
    }
}
