import SwiftUI

/// Иконки дизайн-кита.
///
/// В макетах — собственный SVG-набор (`design/src/icons.jsx`, компоненты `Ic*`).
/// На iOS они заменены ближайшими SF Symbols (iOS 16+, SF Symbols 4) с сохранением
/// смысла и размеров (22 pt в таб-баре/навбаре, 18–20 pt в строках).
/// Если нужна иконка, которой здесь нет, — добавить кейс сюда (общая зона, только добавление).
enum DSIcon: String {
    // Навигация
    case chevronLeft = "chevron.left"
    case chevronRight = "chevron.right"
    case chevronDown = "chevron.down"
    case close = "xmark"
    case plus = "plus"
    case ellipsis = "ellipsis"

    // Таб-бар
    case document = "doc.text"            // IcDocument — «Главная»
    case ruble = "rublesign.circle"       // IcRuble — «Зарплата»
    case sliders = "slider.horizontal.3"  // IcSliders — «Настройки»
    case profile = "person.crop.circle"   // IcProfile — «Профиль»

    // Сущности маршрута
    case locomotive = "tram"                    // IcLocomotive
    case train = "train.side.front.car"         // IcRails
    case passenger = "person.2"                 // IcPassenger
    case mapPin = "mappin.and.ellipse"          // IcMapPin
    case home = "house"                         // IcHome

    // Инструменты и действия
    case calendar = "calendar"            // IcCalendar
    case armchair = "sofa"                // IcArmchair — «Отвлечения»
    case search = "magnifyingglass"       // IcSearch
    case pdf = "doc.richtext"             // IcPdf
    case trash = "trash"                  // IcTrash
    case heart = "heart"                  // IcHeart
    case share = "square.and.arrow.up"    // IcShare
    case copy = "doc.on.doc"              // IcCopy
    case gear = "gearshape"               // IcGear
    case info = "info.circle"             // IcInfo
    case sync = "arrow.triangle.2.circlepath" // IcSync
    case clock = "clock"                  // IcClock
    case check = "checkmark"              // IcCheck
    case flash = "bolt.fill"              // IcFlash
    case moon = "moon"                    // NW_IcMoon
    case filter = "line.3.horizontal.decrease" // ArFilter
    case sort = "arrow.up.arrow.down"     // ArSort
    case warning = "exclamationmark"

    var image: Image { Image(systemName: rawValue) }
}
