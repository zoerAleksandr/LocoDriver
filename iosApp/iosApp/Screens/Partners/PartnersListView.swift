import SwiftUI
import ComposeApp

// Напарники — SCREEN_SPECS §8.5.1. Макета нет: стиль справочников (norms-screens.jsx) + кит.
// Два режима одним экраном: MANAGE (из Настроек, `basicId == nil`) и SELECT (из формы маршрута).

struct PartnersListView: View {
    /// nil — MANAGE (из Настроек); иначе SELECT для маршрута `basicId`.
    let basicId: String?

    @StateObject private var vm = PartnersScreenViewModelWrapper()
    @State private var started = false
    @State private var query = ""
    @State private var deleteRequest: DSDeleteRequest?
    @State private var openSwipeId: String?
    @State private var lockScroll = false

    private var isSelect: Bool { basicId != nil }

    var body: some View {
        VStack(spacing: 0) {
            navBar
            if let s = vm.state {
                content(s)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .onAppear {
            if !started {
                started = true
                vm.start(basicId: basicId)
            }
        }
        .dsDeleteAlert($deleteRequest)
        .onChange(of: deleteRequest?.id) { id in
            if id == nil && openSwipeId != nil {
                withAnimation(.easeOut(duration: 0.22)) { openSwipeId = nil }
            }
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

    // MARK: Навбар

    @ViewBuilder
    private var navBar: some View {
        if isSelect {
            // SELECT: слева «Готово» (применяет выбор), без стрелки; свайп «назад» — выход без применения.
            ZStack {
                Text("Напарники").dsTextStyle(.navTitle).lineLimit(1)
                HStack {
                    DSNavPillButton(title: "Готово") {
                        hideKeyboard()
                        vm.confirm { AppRouter.shared.back() }
                    }
                    Spacer()
                }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.vertical, 12)
            .background(DSColor.bg)
        } else {
            DSNavBar(title: "Напарники", style: .text(backLabel: "Настройки"), onBack: { AppRouter.shared.back() })
        }
    }

    // MARK: Содержимое

    private func content(_ s: PartnersScreenState) -> some View {
        let rows = (s.rows as? [PartnerRowUi]) ?? []
        return ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if s.totalCount > 0 {
                    PartnersSearchField(text: $query)
                        .padding(.top, 12)
                        .onChange(of: query) { vm.setQuery($0) }
                    if isSelect { permanentCard(s).padding(.top, 12) }
                }

                if s.totalCount == 0 {
                    DSEmptyState(
                        icon: .passenger,
                        title: "Пока нет напарников",
                        message: "Добавьте машинистов и помощников, с которыми работаете — их можно будет быстро выбрать в маршруте."
                    )
                    .padding(.top, 40)
                } else if rows.isEmpty {
                    Text("По запросу «\(query)» ничего не найдено")
                        .font(DSFont.sans(14)).foregroundColor(DSColor.textMuted)
                        .padding(.top, 20)
                } else {
                    DSGroupHeader(query.trimmingCharacters(in: .whitespaces).isEmpty
                                  ? "ВСЕ НАПАРНИКИ · \(s.totalCount)"
                                  : "НАЙДЕНО · \(rows.count)")
                    VStack(spacing: DSMetrics.cardGap) {
                        ForEach(rows, id: \.id) { row in
                            LocoSwipeDelete(id: row.id, openId: $openSwipeId, lockScroll: $lockScroll, onDelete: {
                                deleteRequest = DSDeleteRequest(title: "Удалить напарника?", message: row.shortName.isEmpty ? row.fullName : row.shortName) {
                                    vm.deletePartner(row.id)
                                }
                            }) {
                                rowCard(row)
                            }
                        }
                    }
                }

                DSCTAButton(title: "Добавить напарника", icon: .plus) {
                    AppRouter.shared.show(.partnerEditor(partnerId: nil))
                }
                .padding(.top, 20)
                Spacer(minLength: 32)
            }
            .padding(.horizontal, DSMetrics.screenPadX)
        }
        .passScrollDismissesKeyboard()
        .simultaneousGesture(TapGesture().onEnded {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.08) {
                if openSwipeId != nil && deleteRequest == nil {
                    withAnimation(.easeOut(duration: 0.22)) { openSwipeId = nil }
                }
            }
        })
        .partnersScrollDisabled(lockScroll)
    }

    // MARK: Строка

    private func rowCard(_ row: PartnerRowUi) -> some View {
        HStack(spacing: 14) {
            Text(row.initials)
                .font(DSFont.sans(15, .semibold))
                .foregroundColor(DSColor.accent)
                .frame(width: 40, height: 40)
                .background(DSColor.accentSoft)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 3) {
                Text(row.fullName).font(DSFont.sans(16, .semibold)).foregroundColor(DSColor.text).lineLimit(2)
                if let tab = row.tabNumber {
                    Text("таб. \(tab)").font(DSFont.mono(13)).foregroundColor(DSColor.accent)
                }
                if let notes = row.notes {
                    Text(notes).font(DSFont.sans(13)).foregroundColor(DSColor.textMuted).lineLimit(2)
                }
                if row.isPermanent {
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill").font(.system(size: 10))
                        Text("Постоянный").font(DSFont.sans(11, .semibold))
                    }
                    .foregroundColor(DSColor.accent)
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(DSColor.accentSoft)
                    .clipShape(Capsule())
                    .padding(.top, 2)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Button {
                AppRouter.shared.show(.partnerEditor(partnerId: row.id))
            } label: {
                Image(systemName: "pencil").font(.system(size: 15, weight: .medium)).foregroundColor(DSColor.textMuted)
                    .frame(width: 36, height: 36)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Редактировать"))
        }
        .padding(.horizontal, 16).padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        // SELECT: выделение — цветом фона строки (без галочки).
        .background(isSelect && row.isSelected ? DSColor.accentSoft : DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        .dsShadow(.sm)
        .contentShape(Rectangle())
        .onTapGesture {
            if isSelect { vm.toggle(row.id) } else { AppRouter.shared.show(.partnerEditor(partnerId: row.id)) }
        }
    }

    // MARK: Постоянный напарник из пикера (§8.5.1)

    private func permanentCard(_ s: PartnersScreenState) -> some View {
        let candidate = s.permanentCandidateShort
        return DSCard {
            HStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Постоянный напарник").font(DSFont.sans(16, .semibold)).foregroundColor(DSColor.text)
                    Text(candidate ?? "Отметьте напарника в списке").font(DSFont.sans(13)).foregroundColor(DSColor.textMuted)
                }
                Spacer(minLength: 8)
                Toggle("", isOn: Binding(
                    get: { s.permanentCandidateIsPermanent },
                    set: { vm.setPermanent($0) }
                ))
                .labelsHidden()
                .tint(DSColor.accent)
                .disabled(candidate == nil)
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 12)
        }
    }
}

/// Однострочный поиск, иконка справа (§8.5.1).
private struct PartnersSearchField: View {
    @Binding var text: String

    var body: some View {
        HStack(spacing: 8) {
            TextField("Поиск", text: $text)
                .font(DSFont.sans(16))
                .foregroundColor(DSColor.text)
                .submitLabel(.search)
                .autocorrectionDisabled()
            DSIcon.search.image.font(.system(size: 15)).foregroundColor(DSColor.textMuted)
        }
        .padding(.horizontal, DSMetrics.inputPadX)
        .frame(minHeight: DSMetrics.inputMinH)
        .background(DSColor.bgSubtle)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
    }
}

private extension View {
    @ViewBuilder
    func partnersScrollDisabled(_ disabled: Bool) -> some View {
        if #available(iOS 16.0, *) { self.scrollDisabled(disabled) } else { self }
    }
}
