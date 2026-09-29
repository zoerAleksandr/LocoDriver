import SwiftUI
import UIKit

/// Цветовые токены дизайн-системы «Машинист».
///
/// Источник значений — `design/tokens.js` (`M.light` / `M.dark`), версия v3.
/// Каждый цвет — динамический `UIColor`: светлая/тёмная тема переключается
/// системой автоматически (в т.ч. в `#Preview` через `.preferredColorScheme`).
///
/// Правило: в экранах и компонентах цвета берутся ТОЛЬКО отсюда
/// (`DSColor.surface`, `DSColor.textMuted`, …), никаких `#hex` и
/// `Color(UIColor.secondarySystemBackground)`.
enum DSColor {
    // Фоны
    static let bg           = dynamic(light: 0xF2F3F5, dark: 0x0F1011)
    static let bgElevated   = dynamic(light: 0xFFFFFF, dark: 0x2A2B2D)
    static let bgSubtle     = dynamic(light: 0xE7E9EC, dark: 0x1A1B1D)
    static let surface      = dynamic(light: 0xFFFFFF, dark: 0x2A2B2D)
    static let surfaceAlt   = dynamic(light: 0xF7F8FA, dark: 0x34363A)

    // Границы
    static let border       = dynamic(light: (0x0A0E14, 0.06), dark: (0xFFFFFF, 0.08))
    static let borderStrong = dynamic(light: (0x0A0E14, 0.14), dark: (0xFFFFFF, 0.16))

    // Текст
    static let text         = dynamic(light: 0x0A0E14, dark: 0xF5F5F5)
    static let textMuted    = dynamic(light: (0x0A0E14, 0.55), dark: (0xF5F5F5, 0.60))
    static let textFaint    = dynamic(light: (0x0A0E14, 0.38), dark: (0xF5F5F5, 0.38))

    // Акцент
    static let accent       = dynamic(light: 0x00A0F5, dark: 0x33BFFF)
    static let accentInk    = dynamic(light: 0xFFFFFF, dark: 0x0A0E14)
    static let accentSoft   = dynamic(light: (0x00A0F5, 0.10), dark: (0x33BFFF, 0.16))
    static let accentHover  = dynamic(light: 0x0091D9, dark: 0x5ECFFF)

    // CTA (тёмная кнопка действия)
    static let cta          = dynamic(light: 0x0A0E14, dark: 0xF5F5F5)
    static let ctaInk       = dynamic(light: 0xFFFFFF, dark: 0x0A0E14)
    static let ctaHover     = dynamic(light: 0x242A33, dark: 0xE5E5E5)

    // Чипы
    static let chipBg       = dynamic(light: 0xEEF0F3, dark: 0x2A2A2A)
    static let chipBgActive = dynamic(light: 0xD6EEFC, dark: 0x0D2F44)

    // Статусы
    static let success      = dynamic(light: 0x00B341, dark: 0x4ADE80)
    static let warning      = dynamic(light: 0xFF8A00, dark: 0xFFB547)
    static let danger       = dynamic(light: 0xFF4053, dark: 0xFF6575)

    // Графика логотипа
    static let rail         = dynamic(light: 0x0A0E14, dark: 0xF5F5F5)
    static let ink          = dynamic(light: 0x0A0E14, dark: 0x060708)

    /// Затемнение под шторкой/алертом — `rgba(20,18,14,0.42)` из макетов
    /// (`RouteSheetShell`, `RouteDeleteAlert`). Одинаково в обеих темах.
    static let scrim        = Color(uiColor: UIColor(dsHex: 0x14120E, alpha: 0.42))

    // MARK: - Helpers

    private static func dynamic(light: UInt32, dark: UInt32) -> Color {
        dynamic(light: (light, 1), dark: (dark, 1))
    }

    private static func dynamic(light: (UInt32, CGFloat), dark: (UInt32, CGFloat)) -> Color {
        let l = UIColor(dsHex: light.0, alpha: light.1)
        let d = UIColor(dsHex: dark.0, alpha: dark.1)
        return Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? d : l
        })
    }
}

extension UIColor {
    /// `0xRRGGBB` → UIColor (sRGB).
    convenience init(dsHex hex: UInt32, alpha: CGFloat = 1) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: alpha
        )
    }
}

#Preview("Палитра — light") {
    DSColorPalettePreview().preferredColorScheme(.light)
}

#Preview("Палитра — dark") {
    DSColorPalettePreview().preferredColorScheme(.dark)
}

private struct DSColorPalettePreview: View {
    private let items: [(String, Color)] = [
        ("bg", DSColor.bg), ("bgElevated", DSColor.bgElevated), ("bgSubtle", DSColor.bgSubtle),
        ("surface", DSColor.surface), ("surfaceAlt", DSColor.surfaceAlt),
        ("border", DSColor.border), ("borderStrong", DSColor.borderStrong),
        ("text", DSColor.text), ("textMuted", DSColor.textMuted), ("textFaint", DSColor.textFaint),
        ("accent", DSColor.accent), ("accentInk", DSColor.accentInk), ("accentSoft", DSColor.accentSoft),
        ("cta", DSColor.cta), ("ctaInk", DSColor.ctaInk),
        ("chipBg", DSColor.chipBg), ("chipBgActive", DSColor.chipBgActive),
        ("success", DSColor.success), ("warning", DSColor.warning), ("danger", DSColor.danger),
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                ForEach(items, id: \.0) { name, color in
                    HStack(spacing: 12) {
                        RoundedRectangle(cornerRadius: DSRadius.sm)
                            .fill(color)
                            .overlay(RoundedRectangle(cornerRadius: DSRadius.sm).stroke(DSColor.borderStrong, lineWidth: 1))
                            .frame(width: 44, height: 44)
                        Text(name).dsTextStyle(.valueMono)
                        Spacer()
                    }
                }
            }
            .padding(DSSpacing.lg)
        }
        .background(DSColor.bg)
    }
}
