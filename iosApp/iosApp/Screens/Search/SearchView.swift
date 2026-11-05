import SwiftUI
import ComposeApp

// Экран «Поиск» — SCREEN_SPECS §10. Поиск по всем маршрутам с историей,
// подсказками и параметрами. Логика — Kotlin `SearchIosViewModel`.
// Макет: design/src (SearchScreen / SearchSettingBottomSheet).

struct SearchView: View {
    @StateObject private var vm = SearchViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared

    @State private var query = ""
    @State private var showSettings = false

    var body: some View {
        VStack(spacing: 0) {
            searchBar
            content
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .dsSheet(isPresented: $showSettings, detents: [.large]) {
            SearchSettingsSheet(vm: vm) {
                vm.closeSettings()
                showSettings = false
            }
        }
        .onAppear {
            if query.isEmpty { query = vm.ui?.query ?? "" }
        }
    }

    // ── Строка поиска (§10.3) ─────────────────────────────────────────
    private var searchBar: some View {
        HStack(spacing: DSSpacing.sm) {
            Button {
                router.back()
            } label: {
                DSIcon.chevronLeft.image
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(DSColor.text)
            }
            .buttonStyle(.plain)
            HStack(spacing: DSSpacing.sm) {
                TextField("Я хочу найти...", text: $query)
                    .font(DSFont.sans(15))
                    .foregroundColor(DSColor.text)
                    .submitLabel(.search)
                    .onSubmit { vm.onSearch() }
                    .onChange(of: query) { newValue in
                        vm.setQueryValue(newValue)
                    }
                if !query.isEmpty {
                    Button {
                        vm.onSearch()
                    } label: {
                        DSIcon.search.image
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(DSColor.accent)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, DSSpacing.md)
            .frame(height: 44)
            .background(DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous)
                    .stroke(DSColor.border, lineWidth: 1)
            )
            Button {
                showSettings = true
            } label: {
                DSIcon.sliders.image
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(DSColor.text)
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, DSMetrics.screenPadX)
        .padding(.vertical, DSSpacing.sm)
    }

    // ── Контент ────────────────────────────────────────────────────────
    @ViewBuilder
    private var content: some View {
        if vm.ui?.isLoading == true {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if vm.ui?.isVisibleResult == true {
            resultsList
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: DSSpacing.md) {
                    if vm.ui?.isVisibleHints == true {
                        suggestions
                    }
                    if vm.ui?.isVisibleHistory == true {
                        history
                    }
                }
                .padding(DSMetrics.screenPadX)
            }
        }
    }

    private var suggestions: some View {
        FlowLayout(spacing: DSSpacing.sm) {
            ForEach(vm.ui?.suggestions ?? [], id: \.self) { suggestion in
                Button {
                    vm.applySuggestion(suggestion)
                    query = vm.ui?.query ?? query
                } label: {
                    Text(suggestion)
                        .font(DSFont.sans(13, .medium))
                        .foregroundColor(DSColor.text)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(DSColor.chipBg)
                        .clipShape(Capsule())
                }
                .buttonStyle(DSPressableStyle())
            }
        }
    }

    private var history: some View {
        VStack(alignment: .leading, spacing: DSSpacing.sm) {
            if let hist = vm.ui?.history, !hist.isEmpty {
                Text("НЕДАВНИЕ ЗАПРОСЫ")
                    .font(DSFont.mono(11))
                    .tracking(1.4)
                    .textCase(.uppercase)
                    .foregroundColor(DSColor.textMuted)
                ForEach(hist, id: \.self) { item in
                    HStack {
                        Button {
                            query = item
                            vm.searchHistoryItem(item)
                        } label: {
                            HStack(spacing: DSSpacing.sm) {
                                DSIcon.clock.image
                                    .font(.system(size: 14))
                                    .foregroundColor(DSColor.textFaint)
                                Text(item)
                                    .font(DSFont.sans(14))
                                    .foregroundColor(DSColor.text)
                            }
                        }
                        .buttonStyle(.plain)
                        Spacer()
                        Button {
                            vm.removeHistoryItem(item)
                        } label: {
                            DSIcon.close.image
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(DSColor.textFaint)
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.vertical, 6)
                }
            }
        }
    }

    private var resultsList: some View {
        ScrollView {
            if let results = vm.ui?.results, results.isEmpty {
                DSEmptyState(
                    icon: .search,
                    title: "Ничего не найдено",
                    message: "Измените запрос или проверьте фильтры"
                )
                .frame(maxWidth: .infinity)
                .padding(.top, 40)
            } else {
                LazyVStack(spacing: DSSpacing.md) {
                    ForEach(vm.ui?.results ?? [], id: \.basicId) { result in
                        resultCard(result)
                    }
                }
                .padding(DSMetrics.screenPadX)
            }
        }
    }

    private func resultCard(_ result: SearchResultUi) -> some View {
        DSCard {
            VStack(alignment: .leading, spacing: DSSpacing.sm) {
                HStack {
                    Text(result.routeNumberText)
                        .font(DSFont.mono(15, .semibold))
                        .foregroundColor(DSColor.text)
                    Spacer()
                    Text(result.dateText)
                        .font(DSFont.mono(12))
                        .foregroundColor(DSColor.textMuted)
                }
                Text(result.tagLabel)
                    .font(DSFont.mono(11))
                    .tracking(1.2)
                    .textCase(.uppercase)
                    .foregroundColor(DSColor.accent)
                Text(SearchMatchHighlight.build(text: result.text, ranges: result.matchRanges))
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.text)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(DSMetrics.rowPadX)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            router.showRouteForm(basicId: result.basicId)
        }
    }
}

// MARK: - Подсветка совпадений (§10.2)

enum SearchMatchHighlight {
    /// `matchRanges` — UTF-16 индексы в `text`; подсвечивает вхождения фоном.
    static func build(text: String, ranges: [MatchRangeUi]) -> AttributedString {
        var attributed = AttributedString(text)
        for range in ranges {
            let loc = Int(range.start)
            let len = Int(range.length)
            guard loc >= 0, len > 0, loc + len <= text.utf16.count else { continue }
            let utf16 = text.utf16
            guard
                let uLower = utf16.index(utf16.startIndex, offsetBy: loc, limitedBy: utf16.endIndex),
                let uUpper = utf16.index(uLower, offsetBy: len, limitedBy: utf16.endIndex),
                let sLower = uLower.samePosition(in: text),
                let sUpper = uUpper.samePosition(in: text),
                let aLower = AttributedString.Index(sLower, within: attributed),
                let aUpper = AttributedString.Index(sUpper, within: attributed),
                aLower < aUpper
            else { continue }
            attributed[aLower..<aUpper].backgroundColor = DSColor.chipBgActive
        }
        return attributed
    }
}

// MARK: - Шторка «Параметры поиска» (§10.3)

struct SearchSettingsSheet: View {
    @ObservedObject var vm: SearchViewModelWrapper
    let onClose: () -> Void

    @State private var showPeriodPicker = false
    @State private var periodIsStart = true
    @State private var periodDate = Date()

    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Параметры", subtitle: nil), onClose: onClose) {
            HStack {
                Spacer()
                Button {
                    vm.clearFilter()
                } label: {
                    Text("Сбросить")
                        .font(DSFont.sans(15, .medium))
                        .foregroundColor(DSColor.accent)
                }
                .buttonStyle(.plain)
            }
            .padding(.bottom, DSSpacing.sm)
            Text("ГДЕ ИСКАТЬ")
                .font(DSFont.mono(11))
                .tracking(1.4)
                .textCase(.uppercase)
                .foregroundColor(DSColor.textMuted)
            FlowLayout(spacing: DSSpacing.sm) {
                ForEach(vm.ui?.chips ?? [], id: \.key) { chip in
                    Button {
                        vm.setFilterEnabled(key: chip.key, enabled: !chip.isEnabled)
                    } label: {
                        Text(chip.label)
                            .font(DSFont.sans(13, .semibold))
                            .foregroundColor(chip.isEnabled ? DSColor.accent : DSColor.text)
                            .padding(.horizontal, 12)
                            .frame(height: 34)
                            .background(chip.isEnabled ? DSColor.chipBgActive : DSColor.chipBg)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(DSPressableStyle())
                }
            }
            Text("ПЕРИОД ВРЕМЕНИ")
                .font(DSFont.mono(11))
                .tracking(1.4)
                .textCase(.uppercase)
                .foregroundColor(DSColor.textMuted)
                .padding(.top, DSSpacing.lg)
            periodRow(label: "с", text: vm.ui?.periodStartText ?? "", isStart: true)
            DSDivider(inset: 0)
            periodRow(label: "по", text: vm.ui?.periodEndText ?? "", isStart: false)
        }
        .dsSheet(isPresented: $showPeriodPicker, detents: [.height(340)]) {
            DSBottomSheet(
                header: .cancelDone(
                    title: periodIsStart ? "Начало периода" : "Конец периода",
                    cancel: "Отмена",
                    done: "Готово",
                    onDone: {
                        let ms = Int64(periodDate.timeIntervalSince1970 * 1000)
                        if periodIsStart {
                            vm.setPeriodStart(ms)
                        } else {
                            vm.setPeriodEnd(ms)
                        }
                        showPeriodPicker = false
                    }
                ),
                showsGrabber: false,
                onClose: { showPeriodPicker = false }
            ) {
                DatePicker("", selection: $periodDate, displayedComponents: [.date])
                    .datePickerStyle(.wheel)
                    .labelsHidden()
            }
        }
    }

    private func periodRow(label: String, text: String, isStart: Bool) -> some View {
        Button {
            periodIsStart = isStart
            showPeriodPicker = true
        } label: {
            HStack {
                Text(label)
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.textMuted)
                Spacer()
                Text(text.isEmpty ? "Не задано" : text)
                    .font(DSFont.mono(14))
                    .foregroundColor(text.isEmpty ? DSColor.textFaint : DSColor.text)
            }
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
