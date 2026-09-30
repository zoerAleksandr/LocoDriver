import SwiftUI
import ComposeApp

/// Карточка маршрута `ItemHomeScreen` в свёрнутом виде (§4.5). Макет: `TripRow`.
///
/// Жесты: тап → форма маршрута; долгое нажатие → быстрый просмотр (§9.1);
/// долгое нажатие на значки → «Обозначения» (§9.3); свайп влево → красная кнопка удаления.
struct HomeRouteCardRow: View {
    let card: HomeIosRouteCard
    let onTap: () -> Void
    let onLongPress: () -> Void
    let onLegend: () -> Void
    let onDelete: () -> Void

    @State private var revealed = false
    @GestureState private var dragX: CGFloat = 0

    private let deleteWidth: CGFloat = 88

    private var rowOffset: CGFloat {
        let base: CGFloat = revealed ? -deleteWidth : 0
        return min(0, max(-deleteWidth * 1.3, base + dragX))
    }

    var body: some View {
        ZStack(alignment: .trailing) {
            Button {
                withAnimation(.easeOut(duration: 0.2)) { revealed = false }
                onDelete()
            } label: {
                DSIcon.trash.image
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(DSColor.accentInk)
                    .frame(width: deleteWidth)
                    .frame(maxHeight: .infinity)
                    .background(DSColor.danger)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("Удалить маршрут"))
            .opacity(rowOffset < 0 ? 1 : 0)

            content
                .background(background)
                .offset(x: rowOffset)
                .simultaneousGesture(swipe)
        }
        .clipped()
    }

    /// Будущий маршрут — светлее обычного, переходный — приглушённый фон (§4.5).
    private var background: Color {
        if card.isFuture { return DSColor.surfaceAlt }
        if card.isTransition { return DSColor.bgSubtle }
        return DSColor.surface
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: 6) {
            header
            if !card.summary.isEmpty {
                Text(card.summary)
                    .font(DSFont.sans(15))
                    .foregroundColor(DSColor.text)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            HStack(alignment: .center, spacing: DSSpacing.md) {
                Text(card.numberText)
                    .font(DSFont.mono(13))
                    .foregroundColor(DSColor.textMuted)
                Spacer(minLength: DSSpacing.sm)
                icons
            }
        }
        .padding(.horizontal, DSMetrics.rowPadX)
        .padding(.vertical, DSMetrics.rowPadY)
        .frame(maxWidth: .infinity, minHeight: 65, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture {
            if revealed {
                withAnimation(.easeOut(duration: 0.2)) { revealed = false }
            } else {
                onTap()
            }
        }
        .onLongPressGesture(minimumDuration: 0.5) {
            if !revealed { onLongPress() }
        }
    }

    /// «явка — сдача» и продолжительность; не помещается — явка и сдача столбцом (обе с датой).
    private var header: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: DSSpacing.md) {
                Text(card.endText.isEmpty ? card.startText : "\(card.startText) — \(card.endText)")
                    .lineLimit(1)
                    .fixedSize()
                Spacer(minLength: DSSpacing.sm)
                Text(card.durationText).fixedSize()
            }
            HStack(alignment: .center, spacing: DSSpacing.md) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(card.startText)
                    if !card.endTextWithDate.isEmpty { Text(card.endTextWithDate) }
                }
                .lineLimit(1)
                .minimumScaleFactor(10.0 / 17.0)
                Spacer(minLength: DSSpacing.sm)
                Text(card.durationText).lineLimit(1).fixedSize()
            }
        }
        .font(DSFont.mono(17, .semibold))
        .foregroundColor(DSColor.text)
    }

    private var icons: some View {
        HStack(spacing: 10) {
            ForEach(card.icons, id: \.self) { key in
                HomeRouteAttributeIcon(key: key)
            }
            if card.isFavorite {
                Image(systemName: "heart.fill")
                    .font(.system(size: 15))
                    .foregroundColor(DSColor.danger)
            }
            DSIcon.sync.image
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(card.isSynchronized ? DSColor.success : DSColor.textFaint)
                .accessibilityLabel(Text(card.isSynchronized ? "Синхронизирован" : "Не синхронизирован"))
        }
        .contentShape(Rectangle())
        .onLongPressGesture(minimumDuration: 0.5) { onLegend() }
    }

    private var swipe: some Gesture {
        DragGesture(minimumDistance: 20)
            .updating($dragX) { value, state, _ in
                if abs(value.translation.width) > abs(value.translation.height) * 1.5 {
                    state = value.translation.width
                }
            }
            .onEnded { value in
                guard abs(value.translation.width) > abs(value.translation.height) * 1.5 else { return }
                withAnimation(.easeOut(duration: 0.2)) {
                    let final = (revealed ? -deleteWidth : 0) + value.translation.width
                    revealed = final < -deleteWidth / 2
                }
            }
    }
}

/// Значок признака маршрута (порядок и смысл — §4.5, легенда — §9.3).
struct HomeRouteAttributeIcon: View {
    let key: String

    private var systemName: String {
        switch key {
        case "holiday": return "star.circle"
        case "break": return "pause.circle"
        case "length": return "ruler"
        case "weight": return "scalemass"
        case "shoulder": return "arrow.left.and.right"
        case "solo": return "person"
        case "passenger": return DSIcon.passenger.rawValue
        case "over12": return "rosette"
        case "pusher": return "arrow.right.to.line"
        case "double": return "square.on.square"
        case "coupled": return "rectangle.on.rectangle"
        default: return "circle"
        }
    }

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: 15, weight: .medium))
            .foregroundColor(DSColor.text)
    }
}
