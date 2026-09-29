import SwiftUI
import UIKit

/// Шрифты дизайн-системы.
///
/// Два семейства (`design/tokens.js` → `fontSans` / `fontMono`):
/// - **Inter** (sans) — язык: заголовки, лейблы, кнопки, названия станций, заметки;
/// - **JetBrains Mono** (mono) — идентификаторы и значения: время, деньги, нормы,
///   счётчики, номера маршрута/поезда/вагона, серия+номер локомотива («ВЛ10-1456»),
///   UPPERCASE-заголовки групп.
///
/// Выбор семейства — по ТИПУ контента, не по тому, кто ввёл значение
/// (см. «Правила шрифтов.md» в корне репозитория). Признак mono: значение можно
/// перепутать посимвольно (0/O, 1/l) или оно выравнивается в столбик.
///
/// Файлы шрифтов в бандл пока НЕ добавлены (нужен .ttf + `UIAppFonts` в Info.plist).
/// Пока их нет, используется системный фолбэк: SF Pro вместо Inter и
/// SF Mono (`design: .monospaced`) вместо JetBrains Mono — с теми же кеглями и весами.
/// После добавления файлов код менять не нужно: имена PostScript уже прописаны.
enum DSFont {
    static func sans(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        if let name = postScriptName(family: "Inter", weight: weight) {
            return .custom(name, fixedSize: size)
        }
        return .system(size: size, weight: weight, design: .default)
    }

    static func mono(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        if let name = postScriptName(family: "JetBrainsMono", weight: weight) {
            return .custom(name, fixedSize: size)
        }
        return .system(size: size, weight: weight, design: .monospaced)
    }

    // MARK: - Private

    /// Возвращает PostScript-имя начертания, если файл шрифта есть в бандле.
    private static func postScriptName(family: String, weight: Font.Weight) -> String? {
        let name = "\(family)-\(styleSuffix(weight))"
        return availableNames.contains(name) ? name : nil
    }

    private static func styleSuffix(_ weight: Font.Weight) -> String {
        switch weight {
        case .medium: return "Medium"
        case .semibold: return "SemiBold"
        case .bold: return "Bold"
        case .heavy, .black: return "ExtraBold"
        default: return "Regular"
        }
    }

    /// Проверяется один раз при первом обращении.
    private static let availableNames: Set<String> = {
        let families = ["Inter", "JetBrainsMono"]
        let suffixes = ["Regular", "Medium", "SemiBold", "Bold", "ExtraBold"]
        var result = Set<String>()
        for family in families {
            for suffix in suffixes {
                let name = "\(family)-\(suffix)"
                if UIFont(name: name, size: 12) != nil { result.insert(name) }
            }
        }
        return result
    }()
}

/// Готовые текстовые стили — 1:1 с `M.t.*` из `design/tokens.js`.
///
/// Использование: `Text("Серия").dsTextStyle(.label)`.
/// Цвет задаётся стилем; если нужен другой — `.dsTextStyle(.value, color: DSColor.textFaint)`.
enum DSTextStyle {
    /// mono 11/400, tracking 1.4, UPPERCASE, textMuted — «ОСНОВНЫЕ ДАННЫЕ»
    case group
    /// mono 11/600, tracking 1.4, UPPERCASE, textMuted — бейдж «ИЗ МАРШРУТА»
    case tag
    /// sans 13/400, textMuted — подпись над значением («Серия», «Начало»)
    case label
    /// sans 14/400, textMuted
    case labelMuted
    /// sans 12/400, textMuted — подстрока («Новосибирск — Омск»)
    case captionMuted
    /// sans 13/400, textMuted, line-height 1.45 — пояснения
    case hint
    /// sans 15/500, text — текст строки
    case body
    /// sans 17/600, text — основное значение
    case value
    /// mono 17/600, text — числовое значение / идентификатор
    case valueMono
    /// sans 17/500, text — название станции
    case stationName
    /// sans 17/700, text — заголовок навбара
    case navTitle
    /// sans 16/400, accent — текстовая кнопка навбара («Отмена»)
    case navAction
    /// sans 16/600, accent — акцентная текстовая кнопка навбара («Готово»)
    case navActionAccent
    /// sans 20/700, tracking −0.3 — крупный заголовок секции
    case sectionH2
    /// mono 32/700, tracking −0.8 — крупная метрика
    case metricLg
    /// mono 48/800, tracking −1.5 — hero-цифра
    case metricXL
    /// mono 18/600 — время в строках
    case timeMono

    var font: Font {
        switch self {
        case .group: return DSFont.mono(11, .regular)
        case .tag: return DSFont.mono(11, .semibold)
        case .label: return DSFont.sans(13)
        case .labelMuted: return DSFont.sans(14)
        case .captionMuted: return DSFont.sans(12)
        case .hint: return DSFont.sans(13)
        case .body: return DSFont.sans(15, .medium)
        case .value: return DSFont.sans(17, .semibold)
        case .valueMono: return DSFont.mono(17, .semibold)
        case .stationName: return DSFont.sans(17, .medium)
        case .navTitle: return DSFont.sans(17, .bold)
        case .navAction: return DSFont.sans(16)
        case .navActionAccent: return DSFont.sans(16, .semibold)
        case .sectionH2: return DSFont.sans(20, .bold)
        case .metricLg: return DSFont.mono(32, .bold)
        case .metricXL: return DSFont.mono(48, .heavy)
        case .timeMono: return DSFont.mono(18, .semibold)
        }
    }

    var color: Color {
        switch self {
        case .group, .tag, .label, .labelMuted, .captionMuted, .hint: return DSColor.textMuted
        case .navAction, .navActionAccent: return DSColor.accent
        default: return DSColor.text
        }
    }

    /// letter-spacing из токенов (px = pt).
    var tracking: CGFloat {
        switch self {
        case .group, .tag: return 1.4
        case .sectionH2: return -0.3
        case .metricLg: return -0.8
        case .metricXL: return -1.5
        default: return 0
        }
    }

    var isUppercase: Bool {
        self == .group || self == .tag
    }

    /// Доп. межстрочный интервал: CSS line-height × size − size.
    var lineSpacing: CGFloat {
        switch self {
        case .hint: return 13 * 0.45
        case .stationName: return 17 * 0.2
        default: return 0
        }
    }
}

extension View {
    /// Применяет текстовый стиль дизайн-кита: шрифт, цвет, трекинг, регистр.
    /// `color` переопределяет цвет стиля. Внешний `.foregroundColor(...)` поверх
    /// этого модификатора НЕ сработает — внутренний цвет ближе к `Text` и побеждает.
    func dsTextStyle(_ style: DSTextStyle, color: Color? = nil) -> some View {
        self
            .font(style.font)
            .foregroundColor(color ?? style.color)
            .tracking(style.tracking)
            .textCase(style.isUppercase ? .uppercase : nil)
            .lineSpacing(style.lineSpacing)
    }
}

#Preview("Типографика — light") {
    DSTypographyPreview().preferredColorScheme(.light)
}

#Preview("Типографика — dark") {
    DSTypographyPreview().preferredColorScheme(.dark)
}

private struct DSTypographyPreview: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 10) {
                Text("Основные данные").dsTextStyle(.group)
                Text("Из маршрута").dsTextStyle(.tag)
                Text("Серия").dsTextStyle(.label)
                Text("Новосибирск — Омск").dsTextStyle(.captionMuted)
                Text("Добавить локомотив").dsTextStyle(.body)
                Text("Электровоз").dsTextStyle(.value)
                Text("ВЛ10-1456").dsTextStyle(.valueMono)
                Text("Москва-Товарная").dsTextStyle(.stationName)
                Text("Маршрут").dsTextStyle(.navTitle)
                Text("Готово").dsTextStyle(.navActionAccent)
                Text("Текущий маршрут").dsTextStyle(.sectionH2)
                Text("5 027,50 ₽").dsTextStyle(.metricLg)
                Text("205:05").dsTextStyle(.metricXL)
                Text("08:20").dsTextStyle(.timeMono)
            }
            .padding(DSSpacing.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(DSColor.bg)
    }
}
