import SwiftUI

// Макет: design/src/ios-screens.jsx → TimeRow («Явка» / «Сдача»: label + tag,
//        ниже две плашки bgSubtle r10 — дата и время, mono 15);
//        design/src/time-sheet.jsx → SheetTimeRow (шторка времени: аватар-часы,
//        label 14/500, дельта нормы, плашка времени mono 18/600).

/// Строка времени в карточке формы: подпись (+ тег) и плашки «дата» / «время».
/// Тап по плашке — `onTap`; долгое нажатие по заполненной строке — `onLongPress`
/// (SCREEN_SPECS §24.2: долгое нажатие открывает шторку «Удалить значение»;
/// для пустого значения long-press ничего не делает — не передавайте обработчик).
struct DSTimeRow: View {
    let label: String
    /// Дата («02 июн 2026») или nil → плейсхолдер «Выбрать».
    let date: String?
    /// Время «19:09» или nil → «—:—».
    let time: String?
    var tag: String? = nil
    var onTap: (() -> Void)? = nil
    var onLongPress: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: DSSpacing.sm) {
            HStack(spacing: DSSpacing.sm) {
                Text(label).dsTextStyle(.label)
                if let tag = tag {
                    DSTag(text: tag)
                }
            }
            HStack(spacing: DSSpacing.sm) {
                chip(date ?? "Выбрать", isEmpty: date == nil, weight: .medium)
                chip(time ?? "—:—", isEmpty: time == nil, weight: .semibold)
            }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, DSMetrics.rowPadY)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture { onTap?() }
        .onLongPressGesture { onLongPress?() }
    }

    private func chip(_ text: String, isEmpty: Bool, weight: Font.Weight) -> some View {
        Text(text)
            .font(DSFont.mono(15, weight))
            .foregroundColor(isEmpty ? DSColor.textFaint : DSColor.text)
            .lineLimit(1)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(DSColor.bgSubtle)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.sm, style: .continuous))
    }
}

/// Строка операции в шторке времени (`SheetTimeRow`): статус-аватар, название,
/// дельта нормы, плашка времени справа. Вертикальная линия-связка между строками
/// рисуется, если `isFirst == false`.
struct DSSheetTimeRow: View {
    let label: String
    /// «08:20» или nil → «—:—»
    let time: String?
    /// «+20 мин» — дельта по норме
    var delta: String? = nil
    var isFirst: Bool = false
    /// Время уже наступило (зелёная галочка)
    var isDone: Bool = false
    var isPreviousDone: Bool = false
    /// Значение пришло из маршрута и не редактируется («ИЗ МАРШРУТА»)
    var isLocked: Bool = false
    var onTap: (() -> Void)? = nil

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: isDone ? DSIcon.check.rawValue : DSIcon.clock.rawValue)
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(isDone ? DSColor.success : DSColor.textMuted)
                .frame(width: DSMetrics.avatarMd, height: DSMetrics.avatarMd)
                .background(isDone ? DSColor.success.opacity(0.14) : DSColor.bgSubtle)
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Text(label)
                        .font(DSFont.sans(14, .medium))
                        .foregroundColor(DSColor.text)
                    if isLocked {
                        DSTag(text: "из маршрута", color: DSColor.textMuted, background: DSColor.bgSubtle)
                    }
                }
                if let delta = delta {
                    HStack(spacing: 4) {
                        DSIcon.flash.image.font(.system(size: 9))
                        Text(delta)
                        Text("норма").foregroundColor(DSColor.textFaint)
                    }
                    .font(DSFont.mono(12))
                    .foregroundColor(DSColor.textMuted)
                }
            }
            Spacer(minLength: DSSpacing.sm)
            Button(action: { onTap?() }) {
                Text(time ?? "—:—")
                    .dsTextStyle(.timeMono, color: time == nil ? DSColor.textFaint : DSColor.text)
                    .frame(minWidth: 76)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(time == nil ? DSColor.bgSubtle : DSColor.bg)
                    .overlay(
                        RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous)
                            .stroke(time == nil ? Color.clear : DSColor.border, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
            }
            .buttonStyle(DSPressableStyle())
            .disabled(isLocked)
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, DSMetrics.rowPadYTouch)
        .overlay(alignment: .topLeading) {
            if !isFirst {
                Rectangle()
                    .fill(connectorColor)
                    .frame(width: 2, height: DSMetrics.rowPadYTouch)
                    .offset(x: DSMetrics.rowPadX + DSMetrics.avatarMd / 2 - 1, y: -DSMetrics.rowPadYTouch)
            }
        }
    }

    private var connectorColor: Color {
        (isDone && isPreviousDone) ? DSColor.success : DSColor.borderStrong
    }
}

#Preview("Время — light") {
    DSTimeRowPreview().preferredColorScheme(.light)
}

#Preview("Время — dark") {
    DSTimeRowPreview().preferredColorScheme(.dark)
}

private struct DSTimeRowPreview: View {
    var body: some View {
        DSPreviewCanvas {
            DSCard {
                DSTimeRow(label: "Явка", date: "02 июн 2026", time: "19:09", tag: "по прибытию пассажиром")
                DSDivider()
                DSTimeRow(label: "Сдача", date: nil, time: nil)
            }
            DSCard {
                DSSheetTimeRow(label: "Явка", time: "08:00", isFirst: true, isDone: true, isLocked: true)
                DSSheetTimeRow(label: "Начало приёмки", time: "08:20", delta: "+20 мин", isDone: true, isPreviousDone: true)
                DSSheetTimeRow(label: "Выход на КП", time: nil, delta: "+5 мин")
            }
        }
    }
}
