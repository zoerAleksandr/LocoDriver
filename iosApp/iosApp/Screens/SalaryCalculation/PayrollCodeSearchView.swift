import SwiftUI
import ComposeApp

// MARK: - PayrollCodeSearchView (SCREEN_SPECS §11.8)
// Справочник кодов: поиск по коду, служебному названию, расшифровке и типу.

struct PayrollCodeSearchView: View {
    @StateObject private var vm = PayrollCodeSearchViewModelWrapper()
    @FocusState private var isSearchFocused: Bool

    private var router: AppRouter { AppRouter.shared }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    infoCard
                    Text("Найдено: \(vm.results.count)")
                        .dsTextStyle(.labelMuted)
                    if vm.results.isEmpty {
                        Text("Совпадений нет. Проверьте код или попробуйте часть названия.")
                            .font(DSFont.sans(14))
                            .foregroundColor(DSColor.textMuted)
                            .frame(maxWidth: .infinity, alignment: .center)
                            .padding(.vertical, 24)
                    } else {
                        ForEach(Array(vm.results.enumerated()), id: \.offset) { _, item in
                            codeCard(item)
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .navigationBarHidden(true)
        .onAppear {
            // При открытии поле получает фокус и показывается клавиатура (§11.8).
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { isSearchFocused = true }
        }
    }

    // MARK: Топбар: стрелка к листу · поле поиска · настройки (слоты как §11.6)

    private var topBar: some View {
        HStack(spacing: 10) {
            Button { router.back() } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(DSColor.text)
                    .frame(width: 40, height: 40)
                    .background(DSColor.bgSubtle)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Назад к расчётному листу"))

            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(DSColor.textFaint)
                TextField("Расшифровать код", text: queryBinding)
                    .font(DSFont.sans(14))
                    .foregroundColor(DSColor.text)
                    .focused($isSearchFocused)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                if !vm.query.isEmpty {
                    Button { vm.clearQuery() } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 16))
                            .foregroundColor(DSColor.textFaint)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text("Очистить поиск"))
                }
            }
            .padding(.horizontal, 12)
            .frame(height: 40)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(DSColor.borderStrong, lineWidth: 1)
            )

            Button { router.showSettingSalary() } label: {
                Image(systemName: "gearshape")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(DSColor.text)
                    .frame(width: 40, height: 40)
                    .background(DSColor.bgSubtle)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Настройки зарплаты"))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }

    private var queryBinding: Binding<String> {
        Binding(
            get: { vm.query },
            set: { vm.setQuery($0) }
        )
    }

    private var infoCard: some View {
        Text("Введите код, служебное название или слова из расшифровки любого удержания или начисления.")
            .font(DSFont.sans(13))
            .foregroundColor(DSColor.text)
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DSColor.accentSoft)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
            .padding(.top, 4)
    }

    private func codeCard(_ item: PayrollCodeItemUi) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text(item.code)
                    .font(DSFont.mono(20, .heavy))
                    .foregroundColor(DSColor.text)
                Spacer(minLength: 8)
                // Чипы «Начисление»/«Удержание» — мягкий синий фон, синий акцент (§11.8).
                Text(item.isAccrual ? "Начисление" : "Удержание")
                    .font(DSFont.sans(11, .semibold))
                    .foregroundColor(DSColor.accent)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(DSColor.accentSoft)
                    .clipShape(Capsule())
            }
            Text(item.shortName)
                .font(DSFont.mono(12, .medium))
                .foregroundColor(DSColor.textMuted)
            Text(item.description)
                .font(DSFont.sans(13))
                .foregroundColor(DSColor.text)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        .dsShadow(.sm)
    }
}
