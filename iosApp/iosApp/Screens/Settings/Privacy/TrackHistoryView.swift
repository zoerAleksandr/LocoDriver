import SwiftUI
import ComposeApp

// «Мои треки» (история) — SCREEN_SPECS §17.8.
// Логика — Kotlin `TrackHistoryIosViewModel` (обёртка `TrackHistoryViewModelWrapper`).
// Карточка — паттерн карточки маршрута §9: мини-карта 72dp, название, «Явка: …»,
// моно-строка «N км · в пути …», внизу справа звезда и индикатор синхронизации.
// Жесты: свайп влево — «Удалить» (с подтверждением), свайп вправо — звезда
// (без подтверждения), долгое нажатие — контекстное меню (документированное
// дополнение: удобный доступ к тем же действиям).

struct TrackHistoryView: View {
    @StateObject private var vm = TrackHistoryViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared

    @State private var deleteRequest: DSDeleteRequest?
    @State private var deleteAllRequest: DSDeleteRequest?
    /// Открытая свайпом карточка (одна на экран).
    @State private var openSwipeId: String?
    @State private var lockScroll = false

    var body: some View {
        VStack(spacing: 0) {
            navBar
            ScrollView {
                VStack(alignment: .leading, spacing: DSSpacing.md) {
                    if vm.state?.consent != true {
                        consentOffCard
                    }
                    if vm.state?.remoteError == true {
                        Text("Треки с сервера не загрузились. Показаны треки этого устройства.")
                            .dsTextStyle(.captionMuted)
                            .padding(.horizontal, 2)
                    }
                    trackCards
                    if vm.state?.canLoadMore == true {
                        loadMoreButton
                    }
                    if let items = vm.state?.items, !items.isEmpty {
                        deleteAllButton
                    }
                }
                .padding(DSMetrics.screenPadX)
                .padding(.bottom, 32)
            }
            .scrollDisabled(lockScroll)
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .dsSnackbarQueue(vm.messages) { vm.popMessage() }
        .dsDeleteAlert($deleteRequest)
        .dsDeleteAlert($deleteAllRequest)
        .onAppear { vm.onScreenOpened() }
    }

    // ── Топбар ─────────────────────────────────────────────────────────

    private var navBar: some View {
        DSNavBar(title: "Мои треки", onBack: { router.back() })
    }

    // ── Состояния списка ───────────────────────────────────────────────

    /// Вверху — предупреждение, пока согласие №1 не дано (§17.8).
    private var consentOffCard: some View {
        Button {
            router.show(.dataPrivacy)
        } label: {
            VStack(alignment: .leading, spacing: DSSpacing.sm) {
                Text("Треки не сохраняются")
                    .font(DSFont.sans(15, .semibold))
                    .foregroundColor(DSColor.text)
                Text("Чтобы приложение записывало путь, включите «Сохранять треки» в «Данных и конфиденциальности».")
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
                Text("Открыть настройки")
                    .font(DSFont.sans(14, .semibold))
                    .foregroundColor(DSColor.accent)
            }
            .padding(DSSpacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DSColor.accentSoft)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous)
                    .stroke(DSColor.accent.opacity(0.35), lineWidth: 1)
            )
        }
        .buttonStyle(DSPressableStyle())
    }

    @ViewBuilder
    private var trackCards: some View {
        let items = vm.state?.items ?? []
        if vm.state?.loading == true && items.isEmpty {
            HStack(spacing: DSSpacing.sm) {
                ProgressView()
                Text("Загружаем треки…")
                    .font(DSFont.sans(13))
                    .foregroundColor(DSColor.textMuted)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, 40)
        } else if items.isEmpty {
            DSEmptyState(
                icon: .mapPin,
                title: "Треков пока нет",
                message: "Записанные поездки появятся здесь, когда включён режим «Автозапись» и дано согласие на сохранение треков."
            )
            .padding(.top, 32)
        } else {
            ForEach(items, id: \.id) { card in
                TrackHistoryCardRow(
                    card: card,
                    isOpen: openSwipeId == card.id,
                    lockScroll: $lockScroll,
                    onDelete: {
                        deleteRequest = DSDeleteRequest(
                            title: "Удалить трек?",
                            message: deleteMessage(card),
                            confirmTitle: "Удалить"
                        ) { vm.delete(card) }
                    },
                    onToggleFavorite: {
                        vm.toggleFavorite(card)
                    },
                    onSetOpen: { open in
                        openSwipeId = open ? card.id : nil
                    }
                )
            }
        }
    }

    private func deleteMessage(_ card: TrackHistoryCard) -> String {
        card.isLocal
            ? "Трек будет удалён с этого устройства и с сервера. Это действие нельзя отменить."
            : "Трек будет удалён с сервера. Это действие нельзя отменить."
    }

    private var loadMoreButton: some View {
        Button {
            vm.loadMore()
        } label: {
            HStack(spacing: DSSpacing.sm) {
                if vm.state?.remoteLoading == true {
                    ProgressView().controlSize(.small)
                }
                Text("Показать ещё")
                    .font(DSFont.sans(15, .semibold))
            }
            .foregroundColor(DSColor.accent)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .background(DSColor.surfaceAlt, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(DSPressableStyle())
        .disabled(vm.state?.remoteLoading == true)
    }

    /// «Удалить всю историю» (§17.7): локальные треки и серверные — после подтверждения.
    private var deleteAllButton: some View {
        Button {
            deleteAllRequest = DSDeleteRequest(
                title: "Удалить всю историю?",
                message: "Все треки будут удалены с этого устройства и с сервера. Это действие нельзя отменить.",
                confirmTitle: "Удалить"
            ) { vm.deleteAllHistory() }
        } label: {
            Text(vm.state?.clearingAll == true ? "Удаляем…" : "Удалить всю историю")
                .font(DSFont.sans(15, .medium))
                .foregroundColor(vm.state?.clearingAll == true ? DSColor.textFaint : DSColor.danger)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
        }
        .buttonStyle(DSPressableStyle())
        .disabled(vm.state?.clearingAll == true)
        .padding(.top, DSSpacing.lg)
    }
}

// MARK: - Карточка трека

private struct TrackHistoryCardRow: View {
    let card: TrackHistoryCard
    let isOpen: Bool
    @Binding var lockScroll: Bool
    let onDelete: () -> Void
    let onToggleFavorite: () -> Void
    let onSetOpen: (Bool) -> Void

    var body: some View {
        TrackSwipeReveal(
            id: card.id,
            isOpen: isOpen,
            favoriteLabel: card.isFavorite ? "Убрать из избранного" : "В избранное",
            lockScroll: $lockScroll,
            onDelete: onDelete,
            onToggleFavorite: onToggleFavorite,
            onSetOpen: onSetOpen
        ) {
            cardBody
        }
        .contextMenu {
            Button {
                onToggleFavorite()
            } label: {
                Label(
                    card.isFavorite ? "Убрать из избранного" : "В избранное",
                    systemImage: card.isFavorite ? "star.slash" : "star"
                )
            }
            Button(role: .destructive) {
                onDelete()
            } label: {
                Label("Удалить", systemImage: "trash")
            }
        }
    }

    private var cardBody: some View {
        DSCard {
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .top, spacing: 12) {
                    TrackMiniMap(preview: card.preview)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(card.title)
                            .font(DSFont.sans(15, .semibold))
                            .foregroundColor(DSColor.text)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                        if card.status == "REJECTED" {
                            Text("Не отправлен: лимит хранилища на сервере")
                                .font(DSFont.sans(12))
                                .foregroundColor(DSColor.danger)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        if !card.workStartLabel.isEmpty {
                            Text(card.workStartLabel)
                                .font(DSFont.sans(13))
                                .foregroundColor(DSColor.textMuted)
                        }
                        Text(card.distanceDurationLabel)
                            .font(DSFont.sans(13, .medium).monospaced())
                            .foregroundColor(DSColor.text)
                    }
                    Spacer(minLength: 0)
                }
                HStack {
                    Spacer()
                    HStack(spacing: 8) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 14))
                            .foregroundColor(card.isFavorite ? TrackHistoryPalette.star : DSColor.textFaint.opacity(0.35))
                            .frame(width: 20, height: 20)
                        syncIndicator
                    }
                }
            }
        }
    }

    /// Индикатор синхронизации (§17.8): зелёные стрелки — отправлен/серверный,
    /// серый — не отправлен, красный — отказ сервера (лимит).
    @ViewBuilder
    private var syncIndicator: some View {
        Group {
            switch card.status {
            case "SENT", "SERVER_ONLY":
                DSIcon.sync.image
                    .foregroundColor(DSColor.success)
            case "REJECTED":
                DSIcon.sync.image
                    .foregroundColor(DSColor.danger)
            default:
                DSIcon.sync.image
                    .foregroundColor(DSColor.textFaint)
            }
        }
        .font(.system(size: 16, weight: .semibold))
        .frame(width: 20, height: 20)
    }
}

// MARK: - Мини-карта (ломаная превью ≤60 точек, без тайлов)

private struct TrackMiniMap: View {
    let preview: [TrackPreviewPoint]

    private static let size: CGFloat = 72
    private static let inset: CGFloat = 8

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous)
                .fill(DSColor.bgSubtle)
            if preview.isEmpty {
                // Нет точек — пустая плитка (§17.8).
                DSIcon.mapPin.image
                    .font(.system(size: 18))
                    .foregroundColor(DSColor.textFaint.opacity(0.5))
            } else {
                GeometryReader { proxy in
                    let pts = Self.project(preview, in: proxy.size)
                    ZStack {
                        polyline(pts)
                        if let start = pts.first {
                            Circle()
                                .fill(DSColor.accent)
                                .frame(width: 6, height: 6)
                                .position(start)
                        }
                        if let end = pts.last, pts.count > 1 {
                            Circle()
                                .strokeBorder(DSColor.accent, lineWidth: 2)
                                .frame(width: 8, height: 8)
                                .position(end)
                        }
                    }
                }
                .padding(Self.inset)
            }
        }
        .frame(width: Self.size, height: Self.size)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
    }

    private func polyline(_ pts: [CGPoint]) -> some View {
        Path { path in
            guard let first = pts.first else { return }
            path.move(to: first)
            for p in pts.dropFirst() { path.addLine(to: p) }
        }
        .stroke(DSColor.accent, style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))
    }

    /// Проецирование широты/долготы в плитку с сохранением пропорций (≤60 точек).
    static func project(_ points: [TrackPreviewPoint], in size: CGSize) -> [CGPoint] {
        let capped = Array(points.prefix(60))
        guard capped.count > 1 else {
            return capped.map { _ in CGPoint(x: size.width / 2, y: size.height / 2) }
        }
        let midLat = (capped.map { $0.lat }.min()! + capped.map { $0.lat }.max()!) / 2
        let cosLat = cos(midLat * .pi / 180)
        let xs = capped.map { $0.lon * cosLat }
        let ys = capped.map { $0.lat }
        let minX = xs.min()!, maxX = xs.max()!
        let minY = ys.min()!, maxY = ys.max()!
        let dx = max(maxX - minX, 1e-6)
        let dy = max(maxY - minY, 1e-6)
        let scale = min(size.width / dx, size.height / dy)
        let offsetX = (size.width - dx * scale) / 2
        let offsetY = (size.height - dy * scale) / 2
        return capped.enumerated().map { i, p in
            CGPoint(
                x: offsetX + (xs[i] - minX) * scale,
                // Экранная Y развёрнута: север вверху.
                y: size.height - (offsetY + (p.lat - minY) * scale)
            )
        }
    }
}

// MARK: - Свайп карточки: влево — «Удалить», вправо — звезда (§17.8)

private struct TrackSwipeReveal<Content: View>: View {
    let id: String
    let isOpen: Bool
    /// «В избранное» / «Убрать из избранного» (§17.8).
    let favoriteLabel: String
    @Binding var lockScroll: Bool
    let onDelete: () -> Void
    let onToggleFavorite: () -> Void
    let onSetOpen: (Bool) -> Void
    @ViewBuilder let content: () -> Content

    @State private var drag: CGFloat = 0
    /// Направление раскрытия открытой карточки: −1 — «Удалить», +1 — звезда.
    @State private var openAnchor: CGFloat = 0

    private let revealWidth: CGFloat = 84
    private let animation = Animation.easeOut(duration: 0.22)

    private var offset: CGFloat {
        let base = isOpen ? openAnchor * revealWidth : 0
        return max(-revealWidth, min(revealWidth, base + drag))
    }

    var body: some View {
        content()
            .offset(x: offset)
            .overlay {
                // Открытая карточка по тапу возвращается на место.
                if isOpen {
                    Color.clear.contentShape(Rectangle())
                        .onTapGesture {
                            withAnimation(animation) {
                                openAnchor = 0
                                onSetOpen(false)
                            }
                        }
                }
            }
            .background(
                TrackHistoryHorizontalPan(
                    onChanged: { value in
                        drag = value
                        // Пока идёт горизонтальный свайп, прокрутка экрана блокируется.
                        if abs(value) > 10 { lockScroll = true }
                    },
                    onEnded: { translation in
                        lockScroll = false
                        let final = (isOpen ? openAnchor * revealWidth : 0) + translation
                        withAnimation(animation) {
                            drag = 0
                            if final < -revealWidth / 2 {
                                openAnchor = -1
                                onSetOpen(true)
                            } else if final > revealWidth / 2 {
                                // Свайп вправо раскрывает кнопку-звезду и фиксирует
                                // карточку открытой; действие — по нажатию кнопки.
                                openAnchor = 1
                                onSetOpen(true)
                            } else {
                                openAnchor = 0
                                onSetOpen(false)
                            }
                        }
                    }
                )
            )
            .background(alignment: .trailing) {
                // «Удалить» — свайп влево; подтверждение диалогом.
                Button(action: {
                    withAnimation(animation) {
                        openAnchor = 0
                        onSetOpen(false)
                    }
                    onDelete()
                }) {
                    DSIcon.trash.image
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: revealWidth)
                        .frame(maxHeight: .infinity)
                        .background(DSColor.danger)
                        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
                }
                .buttonStyle(.plain)
                .opacity(offset < -4 ? 1 : 0)
                .allowsHitTesting(offset < -4)
            }
            .background(alignment: .leading) {
                // Звезда — свайп вправо; действие без подтверждения + снекбар.
                Button(action: {
                    withAnimation(animation) {
                        openAnchor = 0
                        onSetOpen(false)
                    }
                    onToggleFavorite()
                }) {
                    VStack(spacing: 4) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 18, weight: .semibold))
                        Text(favoriteLabel)
                            .font(DSFont.sans(10, .semibold))
                            .multilineTextAlignment(.center)
                            .lineLimit(2)
                    }
                    .foregroundColor(TrackHistoryPalette.starInk)
                    .padding(.horizontal, 6)
                    .frame(width: revealWidth)
                    .frame(maxHeight: .infinity)
                    .background(TrackHistoryPalette.star)
                    .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
                }
                .buttonStyle(.plain)
                .opacity(offset > 4 ? 1 : 0)
                .allowsHitTesting(offset > 4)
            }
            .onChange(of: isOpen) { open in
                if !open { openAnchor = 0; drag = 0 }
            }
            // Контейнер фиксированной ширины: смещённая карточка не расширяет прокрутку.
            .padding(.vertical, 6)
            .clipped()
            .padding(.vertical, -6)
    }
}

/// Горизонтальный pan (копия `LocoHorizontalPan`, §1.3 п.4): начинается только для
/// преимущественно горизонтального движения; на время жеста прокрутка отключена.
private struct TrackHistoryHorizontalPan: UIViewRepresentable {
    let onChanged: (CGFloat) -> Void
    let onEnded: (CGFloat) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> UIView {
        let view = HostView()
        view.isUserInteractionEnabled = false
        view.backgroundColor = .clear
        view.coordinator = context.coordinator
        context.coordinator.view = view
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        context.coordinator.parent = self
    }

    final class HostView: UIView {
        weak var coordinator: Coordinator?
        override func didMoveToWindow() {
            super.didMoveToWindow()
            coordinator?.windowChanged(window)
        }
    }

    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        var parent: TrackHistoryHorizontalPan
        weak var view: UIView?
        private var pan: UIPanGestureRecognizer?
        private weak var scroll: UIScrollView?

        init(_ parent: TrackHistoryHorizontalPan) { self.parent = parent }

        deinit { if let pan = pan { pan.view?.removeGestureRecognizer(pan) } }

        func windowChanged(_ window: UIWindow?) {
            if let pan = pan { pan.view?.removeGestureRecognizer(pan); self.pan = nil }
            guard let window = window else { return }
            let recognizer = UIPanGestureRecognizer(target: self, action: #selector(handle(_:)))
            recognizer.delegate = self
            recognizer.cancelsTouchesInView = false
            window.addGestureRecognizer(recognizer)
            pan = recognizer
        }

        private func enclosingScroll() -> UIScrollView? {
            var v: UIView? = view
            while let current = v {
                if let s = current as? UIScrollView { return s }
                v = current.superview
            }
            return nil
        }

        func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
            guard let pan = gestureRecognizer as? UIPanGestureRecognizer, let view = view, let window = view.window else { return false }
            guard view.bounds.contains(pan.location(in: view)) else { return false }
            if let scroll = enclosingScroll() {
                let velocity = pan.velocity(in: window)
                guard abs(velocity.x) > abs(velocity.y) else { return false }
                self.scroll = scroll
                scroll.isScrollEnabled = false
            }
            return true
        }

        func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
            true
        }

        @objc private func handle(_ pan: UIPanGestureRecognizer) {
            guard let view = view, let window = view.window else { return }
            let translation = pan.translation(in: window).x
            switch pan.state {
            case .changed:
                parent.onChanged(translation)
            case .ended, .cancelled, .failed:
                parent.onEnded(translation)
                pan.setTranslation(.zero, in: window)
                scroll?.isScrollEnabled = true
                scroll = nil
            default:
                break
            }
        }
    }
}

/// Палитра карточки трека: золотая звезда (§17.8).
private enum TrackHistoryPalette {
    static let star = Color(red: 0.98, green: 0.75, blue: 0.14)
    static let starInk = Color(red: 0.35, green: 0.24, blue: 0.02)
}
