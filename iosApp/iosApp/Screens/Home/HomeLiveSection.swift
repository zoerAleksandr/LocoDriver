import SwiftUI
import UIKit
import ComposeApp

/// Живые блоки Главной (§4.3): «Текущий маршрут» (плитки), «Следующий маршрут»,
/// «Отдых в пункте оборота», «Домашний отдых». Все счётчики и подписи считает Kotlin
/// (`HomeIosLive`), тикер — в ViewModel (граница минуты / секунда для отдыха).
struct HomeLiveSection: View {
    let live: HomeIosLive
    @Binding var isReorderMode: Bool
    let onOpenRoute: (String) -> Void
    let onTile: (HomeIosUnitTile) -> Void
    let onAddUnit: (String) -> Void

    var body: some View {
        switch live.kind {
        case "current":
            VStack(alignment: .leading, spacing: 0) {
                sectionHeader("Текущий маршрут")
                HomeCurrentTiles(
                    live: live,
                    isReorderMode: $isReorderMode,
                    onOpenRoute: { onOpenRoute(live.basicId) },
                    onTile: onTile,
                    onAddUnit: onAddUnit
                )
            }
        case "next":
            VStack(alignment: .leading, spacing: 0) {
                sectionHeader("Следующий маршрут")
                    .allowsHitTesting(!isReorderMode)
                HomeUpcomingCard(live: live) { onOpenRoute(live.basicId) }
            }
        case "restTurnover", "restHome":
            if let rest = live.rest {
                VStack(alignment: .leading, spacing: 0) {
                    DSSectionHeader(live.kind == "restTurnover" ? "Отдых в пункте оборота" : "Домашний отдых")
                        .padding(.top, DSSpacing.sm)
                    HomeRestCard(rest: rest)
                }
            }
        default:
            EmptyView()
        }
    }

    /// Заголовок секции; тап → форма маршрута.
    private func sectionHeader(_ title: String) -> some View {
        Button { if !isReorderMode { onOpenRoute(live.basicId) } } label: {
            DSSectionHeader(title: title) {
                DSIcon.chevronRight.image
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(DSColor.textFaint)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.top, DSSpacing.sm)
    }
}

// MARK: - Плитки текущего маршрута

private enum HomeTileMetrics {
    static let size: CGFloat = 150
    static let spacing: CGFloat = 12
    /// Сдвиг, после которого плитка меняется местами с соседней (§4.3: ≥ 81dp).
    static let swapThreshold: CGFloat = 81
}

/// Ряд плиток «На работе / Локомотив / Поезд / Пассажиром» с перестановкой долгим нажатием.
struct HomeCurrentTiles: View {
    let live: HomeIosLive
    @Binding var isReorderMode: Bool
    let onOpenRoute: () -> Void
    let onTile: (HomeIosUnitTile) -> Void
    let onAddUnit: (String) -> Void

    /// Порядок хранится локально (не синхронизируется), восстанавливается после перезапуска.
    @AppStorage("home.currentRouteBlockOrder") private var storedOrder: String = ""
    @State private var dragged: String? = nil
    @State private var dragOffset: CGFloat = 0
    @State private var dragShift: CGFloat = 0
    @State private var wiggle = false

    private static let allKeys: Set<String> = ["work", "loco", "train", "passenger"]

    private var order: [String] {
        let saved = storedOrder.split(separator: ",").map(String.init)
        if saved.count == 4, Set(saved) == Self.allKeys { return saved }
        let fallback = live.defaultOrder
        return fallback.count == 4 ? fallback : ["work", "loco", "train", "passenger"]
    }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: HomeTileMetrics.spacing) {
                ForEach(order, id: \.self) { key in
                    tile(for: key)
                        .rotationEffect(.degrees(isReorderMode ? (wiggle ? 1.2 : -1.2) : 0))
                        .scaleEffect(dragged == key ? 1.04 : 1)
                        .offset(x: dragged == key ? dragOffset - dragShift : 0)
                        .zIndex(dragged == key ? 1 : 0)
                        .gesture(reorderGesture(for: key))
                }
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.top, 9)
            .padding(.bottom, 8)
        }
        .scrollDisabled(isReorderMode)
        .padding(.horizontal, -DSMetrics.screenPadX)
        .onChange(of: isReorderMode) { active in
            if active {
                withAnimation(.easeInOut(duration: 0.11).repeatForever(autoreverses: true)) { wiggle = true }
            } else {
                withAnimation(.easeOut(duration: 0.1)) { wiggle = false }
                dragged = nil
            }
        }
    }

    @ViewBuilder
    private func tile(for key: String) -> some View {
        switch key {
        case "work":
            HomeWorkTile(live: live, isEnabled: !isReorderMode, onTap: onOpenRoute)
        case "loco":
            unitTile(live.loco, type: "loco")
        case "train":
            unitTile(live.train, type: "train")
        default:
            unitTile(live.passenger, type: "passenger")
        }
    }

    @ViewBuilder
    private func unitTile(_ tile: HomeIosUnitTile?, type: String) -> some View {
        if let tile = tile {
            HomeUnitTileView(
                tile: tile,
                isEnabled: !isReorderMode,
                onTap: { onTile(tile) },
                onAdd: { onAddUnit(type) }
            )
        }
    }

    // MARK: Перестановка

    private func reorderGesture(for key: String) -> AnyGesture<Void> {
        if isReorderMode {
            // Режим уже включён: плитки можно брать сразу, без долгого нажатия.
            return AnyGesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in updateDrag(key, value.translation.width) }
                    .onEnded { _ in endDrag() }
                    .map { _ in () }
            )
        }
        return AnyGesture(
            LongPressGesture(minimumDuration: 0.45)
                .sequenced(before: DragGesture(minimumDistance: 0))
                .onChanged { value in
                    switch value {
                    case .second(true, let drag):
                        if !isReorderMode { isReorderMode = true }
                        updateDrag(key, drag?.translation.width ?? 0)
                    default:
                        break
                    }
                }
                .onEnded { _ in endDrag() }
                .map { _ in () }
        )
    }

    private func updateDrag(_ key: String, _ translation: CGFloat) {
        if dragged != key {
            dragged = key
            dragShift = 0
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        }
        dragOffset = translation
        var current = order
        guard let index = current.firstIndex(of: key) else { return }
        let step = HomeTileMetrics.size + HomeTileMetrics.spacing
        let visual = translation - dragShift
        if visual >= HomeTileMetrics.swapThreshold, index < current.count - 1 {
            current.swapAt(index, index + 1)
            dragShift += step
            storedOrder = current.joined(separator: ",")
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        } else if visual <= -HomeTileMetrics.swapThreshold, index > 0 {
            current.swapAt(index, index - 1)
            dragShift -= step
            storedOrder = current.joined(separator: ",")
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        }
    }

    private func endDrag() {
        withAnimation(.spring(response: 0.25, dampingFraction: 0.8)) {
            dragged = nil
            dragOffset = 0
            dragShift = 0
        }
    }
}

/// Плитка «НА РАБОТЕ»: секундомер ЧЧ:ММ и полоса `часы/12` (красная после 12 ч).
struct HomeWorkTile: View {
    let live: HomeIosLive
    let isEnabled: Bool
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("На работе").dsTextStyle(.group)
            Spacer(minLength: 0)
            Text(live.workText)
                .font(DSFont.mono(28, .heavy))
                .tracking(-1)
                .foregroundColor(DSColor.text)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            HomeProgressBar(
                progress: Double(live.workProgress),
                height: 3,
                fill: live.isOver12 ? DSColor.danger : DSColor.accent,
                track: DSColor.accentSoft
            )
            .padding(.top, DSSpacing.sm)
        }
        .padding(18)
        .frame(width: HomeTileMetrics.size, height: HomeTileMetrics.size, alignment: .leading)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .dsShadow(.sm)
        .contentShape(Rectangle())
        .onTapGesture { if isEnabled { onTap() } }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

/// Плитка единицы (макет `FilledTile` / `AddTile`): иконка, подпись, имя, станции,
/// бейдж-счётчик и «стопка» при нескольких единицах, круглая «+».
struct HomeUnitTileView: View {
    let tile: HomeIosUnitTile
    let isEnabled: Bool
    let onTap: () -> Void
    let onAdd: () -> Void

    private var count: Int { Int(tile.count) }
    private var isEmpty: Bool { count == 0 }

    private var icon: DSIcon {
        switch tile.type {
        case "loco": return .locomotive
        case "train": return .train
        default: return .passenger
        }
    }

    private var label: String {
        switch tile.type {
        case "loco": return "Локомотив"
        case "train": return "Поезд"
        default: return "Пассажиром"
        }
    }

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            if count > 1 {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(DSColor.surface)
                    .dsShadow(.sm)
                    .frame(width: HomeTileMetrics.size, height: HomeTileMetrics.size)
                    .offset(x: 7, y: -7)
            }
            card
            Button(action: { if isEnabled { onAdd() } }) {
                DSIcon.plus.image
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(DSColor.accent)
                    .frame(width: 32, height: 32)
                    .background(DSColor.accentSoft)
                    .clipShape(Circle())
            }
            .buttonStyle(DSPressableStyle())
            .padding(14)
            .accessibilityLabel(Text(isEmpty ? "Добавить: \(label)" : "Добавить ещё: \(label)"))
        }
        .frame(width: HomeTileMetrics.size, height: HomeTileMetrics.size)
    }

    private var card: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top) {
                icon.image
                    .font(.system(size: 26, weight: .regular))
                    .foregroundColor(isEmpty ? DSColor.textFaint : DSColor.accent)
                    .frame(height: 30)
                Spacer(minLength: 0)
                if count > 1 {
                    Text("\(count)")
                        .font(DSFont.mono(11, .bold))
                        .foregroundColor(DSColor.accentInk)
                        .padding(.horizontal, 5)
                        .frame(minWidth: 19, minHeight: 19)
                        .background(DSColor.accent)
                        .clipShape(Capsule())
                }
            }
            .padding(.bottom, 6)
            Text(label).dsTextStyle(.group).padding(.bottom, 3)
            if let title = tile.title {
                Text(title)
                    .font(DSFont.mono(17, .bold))
                    .foregroundColor(DSColor.text)
                    .lineLimit(1)
                    .minimumScaleFactor(11.0 / 17.0)
            }
            if let subtitle = tile.subtitle {
                Text(subtitle)
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.textMuted)
                    .lineLimit(2)
                    .padding(.top, 3)
                    .padding(.trailing, 36)
            }
            Spacer(minLength: 0)
        }
        .padding(18)
        .frame(width: HomeTileMetrics.size, height: HomeTileMetrics.size, alignment: .topLeading)
        .background(isEmpty ? Color.clear : DSColor.surface)
        .overlay(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(
                    isEmpty ? DSColor.borderStrong : (count > 1 ? DSColor.border : Color.clear),
                    style: StrokeStyle(lineWidth: isEmpty ? 1.5 : 1, dash: isEmpty ? [6, 4] : [])
                )
        )
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .modifier(HomeTileShadow(isVisible: !isEmpty))
        .contentShape(Rectangle())
        .onTapGesture { if isEnabled { onTap() } }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
    }
}

private struct HomeTileShadow: ViewModifier {
    let isVisible: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if isVisible { content.dsShadow(.sm) } else { content }
    }
}

// MARK: - Следующий маршрут

/// Карточка «ДО ЯВКИ ОСТАЛОСЬ» (макет `UpcomingRoute`): обратный отсчёт и строка «Явка».
struct HomeUpcomingCard: View {
    let live: HomeIosLive
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 0) {
                Text("До явки осталось").dsTextStyle(.group).padding(.bottom, 4)
                Text(live.countdownText)
                    .font(DSFont.mono(28, .heavy))
                    .tracking(-1)
                    .foregroundColor(DSColor.text)
                DSDivider(inset: 0).padding(.top, 20)
                HStack(spacing: 14) {
                    DSIconAvatar(icon: .calendar, size: .lg, tone: .neutral, shape: .square)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Явка").font(DSFont.sans(12)).foregroundColor(DSColor.textMuted)
                        Text(live.appearanceText)
                            .font(DSFont.mono(16, .bold))
                            .foregroundColor(DSColor.text)
                    }
                    Spacer(minLength: 0)
                }
                .padding(.top, 18)
            }
            .padding(22)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DSColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.xl, style: .continuous))
            .dsShadow(.md)
        }
        .buttonStyle(DSPressableStyle())
    }
}

// MARK: - Отдых

/// Карточка отдыха (макет `RestAtTurnaround`): «ОТДЫХАЕТЕ», счётчик, шкала, строки окончаний.
struct HomeRestCard: View {
    let rest: HomeIosRest

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Отдыхаете").dsTextStyle(.group)
            Text(rest.elapsedText)
                .font(DSFont.mono(28, .heavy))
                .tracking(-1)
                .foregroundColor(DSColor.text)
                .padding(.top, 7)
            Text(rest.startedText)
                .dsTextStyle(.captionMuted)
                .padding(.top, 8)
            scale.padding(.top, 20)
            labels.padding(.top, 7)
            DSDivider(inset: 0).padding(.top, 14)
            ForEach(Array(rest.rows.enumerated()), id: \.offset) { index, row in
                HStack(alignment: .center, spacing: 14) {
                    VStack(alignment: .leading, spacing: 1) {
                        Text(row.title)
                            .font(DSFont.sans(15, .bold))
                            .foregroundColor(DSColor.text)
                        Text(row.untilText)
                            .font(DSFont.mono(12))
                            .foregroundColor(DSColor.textMuted)
                    }
                    Spacer(minLength: DSSpacing.sm)
                    HStack(spacing: 4) {
                        Text("осталось").font(DSFont.sans(13)).foregroundColor(DSColor.textMuted)
                        Text(row.leftText).font(DSFont.mono(13, .bold)).foregroundColor(DSColor.text)
                    }
                    .fixedSize()
                }
                .padding(.vertical, 15)
                .padding(.horizontal, 2)
                if index < rest.rows.count - 1 { DSDivider(inset: 0) }
            }
        }
        .padding(22)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.xl, style: .continuous))
        .dsShadow(.md)
    }

    private var scale: some View {
        GeometryReader { geo in
            let width = geo.size.width
            ZStack(alignment: .leading) {
                Capsule().fill(DSColor.accentSoft).frame(height: 3)
                Capsule()
                    .fill(DSColor.accent)
                    .frame(width: width * CGFloat(rest.progress), height: 3)
                if rest.markerFraction >= 0 {
                    Circle()
                        .fill(rest.markerIsSuccess ? DSColor.success : DSColor.warning)
                        .frame(width: 9, height: 9)
                        .offset(x: width * CGFloat(rest.markerFraction) - 4.5)
                }
                Circle()
                    .fill(DSColor.warning)
                    .frame(width: 9, height: 9)
                    .offset(x: width - 4.5)
            }
            .frame(height: 9)
        }
        .frame(height: 9)
    }

    private var labels: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Text(rest.startLabel)
                    .font(DSFont.mono(11))
                    .foregroundColor(DSColor.textFaint)
                if rest.markerFraction >= 0, !rest.markerLabel.isEmpty {
                    Text(rest.markerLabel)
                        .font(DSFont.mono(11, .bold))
                        .foregroundColor(DSColor.textFaint)
                        .fixedSize()
                        .position(x: geo.size.width * CGFloat(rest.markerFraction), y: 7)
                }
                Text(rest.endLabel)
                    .font(DSFont.mono(11))
                    .foregroundColor(DSColor.textFaint)
                    .frame(maxWidth: .infinity, alignment: .trailing)
            }
        }
        .frame(height: 14)
    }
}
