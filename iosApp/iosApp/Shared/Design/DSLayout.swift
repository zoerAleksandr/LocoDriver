import SwiftUI

/// Радиусы — `M.r` из `design/tokens.js`. На iOS используются iOS-варианты
/// (`cardIOS`, `sheetIOS`), Android-варианты сюда не переносятся.
enum DSRadius {
    /// Пилюли-метки внутри строк
    static let xs: CGFloat = 6
    /// Мелкие чипы, бейджи
    static let sm: CGFloat = 10
    /// Поля ввода, кнопки в строках
    static let md: CGFloat = 12
    static let input: CGFloat = 12
    static let btn: CGFloat = 12
    /// Карточки (iOS)
    static let card: CGFloat = 18
    static let lg: CGFloat = 18
    /// Крупные hero-блоки
    static let xl: CGFloat = 24
    /// Шторки (iOS)
    static let sheet: CGFloat = 18
    /// Полностью круглые элементы
    static let pill: CGFloat = 999
}

/// Отступы — `M.s` / `M.space`.
enum DSSpacing {
    static let xs: CGFloat = 4
    static let sm: CGFloat = 8
    static let md: CGFloat = 12
    static let lg: CGFloat = 16
    static let xl: CGFloat = 20
    static let xxl: CGFloat = 24
    static let xxxl: CGFloat = 32
    static let card: CGFloat = 20
    static let section: CGFloat = 32
}

/// Семантические «строительные» размеры — `M.semantic`.
enum DSMetrics {
    // Строки в карточках
    static let rowPadX: CGFloat = 20
    static let rowPadY: CGFloat = 14
    static let rowPadYTouch: CGFloat = 16
    static let rowGap: CGFloat = 12
    // Карточки
    static let cardPadX: CGFloat = 20
    static let cardPadY: CGFloat = 18
    static let cardGap: CGFloat = 12
    // Заголовки групп: отступ перед заголовком и padding `10px 4px 8px`
    static let groupHeadMt: CGFloat = 22
    static let groupHeadFirst: CGFloat = 4
    static let groupHeadPadTop: CGFloat = 10
    static let groupHeadPadX: CGFloat = 4
    static let groupHeadPadBottom: CGFloat = 8
    // Инпуты
    static let inputMinH: CGFloat = 44
    static let inputPadX: CGFloat = 14
    static let inputPadY: CGFloat = 10
    // Кнопки навбара
    static let navBtnSize: CGFloat = 40
    static let navBtnPadX: CGFloat = 14
    // Иконки-аватары в начале строки
    static let avatarSm: CGFloat = 28
    static let avatarMd: CGFloat = 32
    static let avatarLg: CGFloat = 36
    static let avatarRadiusSm: CGFloat = 8
    static let avatarRadiusMd: CGFloat = 16
    static let avatarRadiusLg: CGFloat = 10
    /// Горизонтальный отступ контента экрана от краёв (все iOS-макеты: `padding: 0 16px`)
    static let screenPadX: CGFloat = 16
}

/// Тени — `M.shadow`. CSS `box-shadow: X Y BLUR rgba(...)` →
/// SwiftUI `.shadow(color:radius:x:y:)`, где `radius ≈ BLUR / 2`.
/// Многослойные тени применяются последовательно.
enum DSShadow {
    case sm, md, lg, floating, fab, sheet

    struct Layer {
        let color: Color
        let radius: CGFloat
        let x: CGFloat
        let y: CGFloat
    }

    var layers: [Layer] {
        switch self {
        case .sm:
            return [
                Layer(color: Self.warm(0.04), radius: 1, x: 0, y: 1),
                Layer(color: Self.warm(0.06), radius: 1.5, x: 0, y: 1),
            ]
        case .md:
            return [
                Layer(color: Self.warm(0.05), radius: 3, x: 0, y: 2),
                Layer(color: Self.warm(0.06), radius: 12, x: 0, y: 8),
            ]
        case .lg:
            return [Layer(color: Self.warm(0.12), radius: 20, x: 0, y: 12)]
        case .floating:
            return [
                Layer(color: Color.black.opacity(0.14), radius: 16, x: 0, y: 12),
                Layer(color: Color.black.opacity(0.06), radius: 3, x: 0, y: 2),
            ]
        case .fab:
            return [
                Layer(color: Color.black.opacity(0.18), radius: 6, x: 0, y: 4),
                Layer(color: Color.black.opacity(0.08), radius: 2, x: 0, y: 2),
            ]
        case .sheet:
            return [Layer(color: Color.black.opacity(0.18), radius: 12, x: 0, y: -8)]
        }
    }

    /// rgba(20,18,14,a) — «тёплая» тень из токенов.
    private static func warm(_ alpha: Double) -> Color {
        Color(red: 20 / 255, green: 18 / 255, blue: 14 / 255).opacity(alpha)
    }
}

extension View {
    func dsShadow(_ shadow: DSShadow) -> some View {
        modifier(DSShadowModifier(layers: shadow.layers))
    }
}

private struct DSShadowModifier: ViewModifier {
    let layers: [DSShadow.Layer]

    func body(content: Content) -> some View {
        // Максимум два слоя в токенах — раскрываем явно, без рекурсии.
        let first = layers.first
        let second = layers.count > 1 ? layers[1] : nil
        return content
            .shadow(color: first?.color ?? .clear, radius: first?.radius ?? 0, x: first?.x ?? 0, y: first?.y ?? 0)
            .shadow(color: second?.color ?? .clear, radius: second?.radius ?? 0, x: second?.x ?? 0, y: second?.y ?? 0)
    }
}

#Preview("Радиусы и тени — light") {
    DSLayoutPreview().preferredColorScheme(.light)
}

#Preview("Радиусы и тени — dark") {
    DSLayoutPreview().preferredColorScheme(.dark)
}

private struct DSLayoutPreview: View {
    private let shadows: [(String, DSShadow)] = [
        ("sm", .sm), ("md", .md), ("lg", .lg), ("floating", .floating), ("fab", .fab), ("sheet", .sheet),
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                ForEach(shadows, id: \.0) { name, shadow in
                    Text(name)
                        .dsTextStyle(.valueMono)
                        .frame(maxWidth: .infinity, minHeight: 64)
                        .background(DSColor.surface)
                        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
                        .dsShadow(shadow)
                }
            }
            .padding(DSSpacing.xxl)
        }
        .background(DSColor.bg)
    }
}
