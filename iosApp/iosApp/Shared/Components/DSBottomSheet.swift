import SwiftUI

// Макет: design/src/ios-screens.jsx → RouteSheetShell / IOSUnitsSheet / IOSLegendSheet
//        (фон bg, грэббер 40×5 borderStrong, заголовок 20/700 tracking −0.3,
//        подзаголовок 13 textMuted, кнопка ✕ 30×30 bgSubtle, тело с padding 8 16 28);
//        design/src/shared-ui.jsx → BottomSheet / SheetGrabber / SheetHeader;
//        design/src/time-sheet.jsx → TimeSheetIOS (шапка «Отмена · Заголовок · Готово»).
// Контракт: SCREEN_SPECS §24.1 (AppBottomSheet: закрытие свайпом, тапом вне и Back,
// действие сначала закрывает шторку, затем выполняется) и §24.2 (шторка даты/времени
// НЕ закрывается свайпом и не имеет грэббера — используйте `.interactiveDismissDisabled()`
// и `showsGrabber: false`).

/// Каркас содержимого нижней шторки. Показывается системным `.sheet`
/// (см. `View.dsSheet`), сам каркас отвечает за шапку, фон и прокрутку.
struct DSBottomSheet<Content: View>: View {
    enum Header {
        /// Заголовок слева + крестик справа (`RouteSheetShell`)
        case titleClose(title: String, subtitle: String?)
        /// «Отмена · Заголовок · Готово» (`TimeSheetIOS`)
        case cancelDone(title: String, cancel: String, done: String, onDone: () -> Void)
        /// Без шапки — содержимое рисует своё
        case none
    }

    let header: Header
    var showsGrabber: Bool = true
    var onClose: () -> Void
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(spacing: 0) {
            if showsGrabber {
                Capsule()
                    .fill(DSColor.borderStrong)
                    .frame(width: 40, height: 5)
                    .padding(.top, 10)
            }
            headerView
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    content()
                }
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.top, DSSpacing.sm)
                .padding(.bottom, 28)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
    }

    @ViewBuilder
    private var headerView: some View {
        switch header {
        case let .titleClose(title, subtitle):
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).dsTextStyle(.sectionH2)
                    if let subtitle = subtitle {
                        Text(subtitle).font(DSFont.sans(13)).foregroundColor(DSColor.textMuted)
                    }
                }
                Spacer(minLength: DSSpacing.md)
                DSIconButton(icon: .close, variant: .subtle, size: 30, accessibilityLabel: "Закрыть", action: onClose)
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.top, 12)
            .padding(.bottom, 8)
        case let .cancelDone(title, cancel, done, onDone):
            VStack(spacing: 0) {
                HStack {
                    Button(cancel, action: onClose)
                        .font(DSTextStyle.navAction.font)
                        .foregroundColor(DSColor.accent)
                    Spacer()
                    Text(title).dsTextStyle(.navTitle)
                    Spacer()
                    Button(done, action: onDone)
                        .font(DSTextStyle.navActionAccent.font)
                        .foregroundColor(DSColor.accent)
                }
                .padding(.horizontal, DSMetrics.screenPadX)
                .padding(.top, 8)
                .padding(.bottom, 14)
                DSDivider(inset: 0)
            }
        case .none:
            EmptyView()
        }
    }
}

extension View {
    /// Показывает шторку кита через системный `.sheet` (iOS 16: detents).
    /// Системный грэббер скрыт — каркас рисует свой.
    /// ⚠️ Радиус системной шторки на iOS 16 изменить нельзя
    /// (`presentationCornerRadius` — iOS 16.4+); используется системный.
    func dsSheet<Item: Identifiable, SheetContent: View>(
        item: Binding<Item?>,
        detents: Set<PresentationDetent> = [.medium, .large],
        @ViewBuilder content: @escaping (Item) -> SheetContent
    ) -> some View {
        sheet(item: item) { value in
            content(value)
                .presentationDetents(detents)
                .presentationDragIndicator(.hidden)
        }
    }

    func dsSheet<SheetContent: View>(
        isPresented: Binding<Bool>,
        detents: Set<PresentationDetent> = [.medium, .large],
        @ViewBuilder content: @escaping () -> SheetContent
    ) -> some View {
        sheet(isPresented: isPresented) {
            content()
                .presentationDetents(detents)
                .presentationDragIndicator(.hidden)
        }
    }
}

#Preview("Шторка — light") {
    DSBottomSheetPreview().preferredColorScheme(.light)
}

#Preview("Шторка — dark") {
    DSBottomSheetPreview().preferredColorScheme(.dark)
}

private struct DSBottomSheetPreview: View {
    var body: some View {
        DSBottomSheet(header: .titleClose(title: "Расчёт за смену", subtitle: "Маршрут №112 · 08:00"), onClose: {}) {
            DSCard {
                DSFieldRow(label: "Почасовая оплата", value: "3 000,00 ₽", valueIsMono: true)
                DSFieldRow(label: "Ночные", value: "1 177,50 ₽", valueIsMono: true, showsDivider: false)
            }
        }
    }
}
