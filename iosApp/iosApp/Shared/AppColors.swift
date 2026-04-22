import SwiftUI

// MARK: - AppColors
// Глобальная палитра приложения. Все экраны используют только эти токены.
// Системные цвета автоматически адаптируются к Light / Dark Mode.
// Кастомные (card, elevated) используют UIColor { traits } для ручного переключения.

extension Color {

    // MARK: - Фоны · Layers

    /// Фон всех экранов
    static let appBg = Color(.systemGroupedBackground)

    /// Таббар, навбар (light: systemBackground, dark: #1C1C1F)
    static let appSurface = Color(.systemBackground)

    /// Карточка статистики, плитки инструментов, пилюля карусели
    /// Light: #FFFFFF, Dark: #242428
    static let appCard = Color(UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0x24/255, green: 0x24/255, blue: 0x28/255, alpha: 1)
            : .systemBackground
    })

    /// Фон иконок в плитках, прогресс-трек, пилюля активного месяца
    /// Light: #F2F2F7, Dark: #2E2E33
    static let appElevated = Color(UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0x2E/255, green: 0x2E/255, blue: 0x33/255, alpha: 1)
            : .systemGroupedBackground
    })

    /// Фон иконок внутри плиток (light)
    static let appIconBg = Color(.systemGroupedBackground)

    /// Прогресс-трек и разделители
    static let appSeparator = Color(.separator)

    // MARK: - Текст · Typography

    /// Основной текст — часы 101:40, заголовки, название маршрута
    static let appPrimary = Color(.label)

    /// Вторичный — лейблы прогресс-баров, «из 165 ч.», номера #17, подписи, год карусели
    static let appSecondary = Color(.secondaryLabel)

    /// Третичный — неактивные месяцы карусели, иконка повтора
    static let appTertiary = Color(.tertiaryLabel)

    // MARK: - Акцент · Accent

    /// Главный акцент — %, прогресс-бар месяца, иконки инструментов, таббар, кнопки
    /// Light: #007AFF, Dark: #4DA3FF
    static let appAccent = Color(.systemBlue)

    // MARK: - Статусы · Semantic

    /// Норма выполнена — прогресс-бар и текст «✓ выполнена»
    static let appSuccess = Color(.systemGreen)

    /// В процессе, норма дня не выполнена
    static let appWarning = Color(.systemOrange)

    /// Не выполнено, просрочено — зарезервирован
    static let appDanger = Color(.systemRed)
}
