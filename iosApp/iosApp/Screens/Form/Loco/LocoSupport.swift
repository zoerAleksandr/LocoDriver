import SwiftUI
import ComposeApp

// Вспомогательные элементы экрана «Локомотив» (SCREEN_SPECS §1).

/// Числовое/текстовое поле экрана. Хранит локальный текст, чтобы набор не «прыгал» от
/// обратной записи из Kotlin: внешнее значение подхватывается, только когда поле не в фокусе.
struct LocoField: View {
    let placeholder: String
    let external: String
    var suffix: String? = nil
    var keyboard: UIKeyboardType = .decimalPad
    var isMono: Bool = true
    var alignment: TextAlignment = .leading
    let onChange: (String) -> Void

    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        HStack(spacing: 4) {
            TextField(placeholder, text: $text)
                .keyboardType(keyboard)
                .multilineTextAlignment(alignment)
                .focused($focused)
                .font(isMono ? DSFont.mono(17, .medium) : DSFont.sans(17, .medium))
                .foregroundColor(DSColor.text)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
            if let suffix = suffix {
                Text(suffix).font(DSFont.sans(14)).foregroundColor(DSColor.textFaint)
            }
        }
        .padding(.horizontal, DSMetrics.inputPadX)
        .frame(minHeight: DSMetrics.inputMinH)
        .background(DSColor.bgSubtle)
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
        .onAppear { text = external }
        // Не гейтим по `focused`: при первом тапе вставка/ввод может прийти раньше, чем обновится
        // фокус, и значение терялось (в модель не уходило).
        .onChange(of: text) { value in
            if value != external { onChange(value) }
        }
        .onChange(of: external) { value in
            if !focused { text = value }
        }
        .onChange(of: focused) { isFocused in
            if !isFocused && text != external { onChange(text) }
        }
    }
}

/// Подпись над полем («Принял» / «Сдал»).
struct LocoFieldLabel: View {
    let text: String
    var body: some View {
        Text(text).font(DSFont.sans(12)).foregroundColor(DSColor.textMuted)
    }
}

/// Свайп карточки влево открывает красную кнопку удаления (`SwipeToRevealDelete`, §1.3 п.4).
/// Открытая карточка одна (`openId` у родителя): тап в любом другом месте экрана или по другой
/// карточке закрывает её с анимацией; возврат тоже анимируется.
struct LocoSwipeDelete<Content: View>: View {
    let id: String
    @Binding var openId: String?
    /// Пока идёт горизонтальный свайп, прокрутка экрана блокируется (иначе тянется весь экран).
    @Binding var lockScroll: Bool
    let onDelete: () -> Void
    @ViewBuilder let content: () -> Content

    @State private var drag: CGFloat = 0
    private let revealWidth: CGFloat = 84
    private let animation = Animation.easeOut(duration: 0.22)

    private var isOpen: Bool { openId == id }
    private var offset: CGFloat {
        max(-revealWidth, min(0, (isOpen ? -revealWidth : 0) + drag))
    }

    var body: some View {
        ZStack(alignment: .trailing) {
            Button(action: {
                // Карточка остаётся открытой: её закрытие/исчезновение ведёт экран.
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
            .opacity(offset < 0 ? 1 : 0)
            .allowsHitTesting(isOpen)
            .accessibilityLabel(Text("Удалить секцию"))

            content()
                .offset(x: offset)
                .allowsHitTesting(!isOpen)
                .overlay {
                    // Открытая карточка по тапу возвращается на место, а не реагирует на поля.
                    if isOpen {
                        Color.clear.contentShape(Rectangle()).offset(x: offset)
                            .onTapGesture { withAnimation(animation) { openId = nil } }
                    }
                }
                // Горизонтальный pan на UIKit: вертикальные жесты (прокрутка экрана) не трогает,
                // горизонтальные — забирает и на время жеста отключает прокрутку.
                .background(
                    LocoHorizontalPan(
                        onChanged: { drag = $0 },
                        onEnded: { translation in
                            let final = (isOpen ? -revealWidth : 0) + translation
                            withAnimation(animation) {
                                drag = 0
                                if final < -revealWidth / 2 {
                                    openId = id
                                } else if openId == id {
                                    openId = nil
                                }
                            }
                        }
                    )
                )
        }
        // Контейнер фиксированной ширины: смещённая карточка не расширяет область прокрутки
        // (иначе при свайпе тянулся весь экран). Вертикальный запас сохраняет тень карточки.
        .padding(.vertical, 12)
        .clipped()
        .padding(.vertical, -12)
    }
}

/// Горизонтальный pan-жест поверх `UIPanGestureRecognizer`, который начинается только для
/// преимущественно горизонтального движения внутри собственных границ. Распознаватель вешается на
/// окно и работает одновременно с прокруткой; пока жест идёт, pan прокрутки отключается.
struct LocoHorizontalPan: UIViewRepresentable {
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
        var parent: LocoHorizontalPan
        weak var view: UIView?
        private var pan: UIPanGestureRecognizer?
        private weak var scroll: UIScrollView?

        init(_ parent: LocoHorizontalPan) { self.parent = parent }

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
            // Касание должно приходиться на содержимое той же прокрутки (не на шторку/алерт поверх).
            if let scroll = enclosingScroll() {
                let hit = window.hitTest(pan.location(in: window), with: nil)
                guard let hit = hit, hit.isDescendant(of: scroll) else { return false }
            }
            let velocity = pan.velocity(in: view)
            return abs(velocity.x) > abs(velocity.y) * 1.2
        }

        func gestureRecognizer(_ g: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith other: UIGestureRecognizer) -> Bool { true }

        @objc private func handle(_ pan: UIPanGestureRecognizer) {
            guard let view = view else { return }
            switch pan.state {
            case .began:
                scroll = enclosingScroll()
                scroll?.panGestureRecognizer.isEnabled = false
            case .changed:
                parent.onChanged(pan.translation(in: view).x)
            case .ended, .cancelled, .failed:
                scroll?.panGestureRecognizer.isEnabled = true
                scroll = nil
                parent.onEnded(pan.translation(in: view).x)
            default:
                break
            }
        }
    }
}

/// Плашка «↓ ПЕРЕРАСХОД −X (−P%)» / «↑ ЭКОНОМИЯ +X (+P%)» (§1.3 п.6).
struct LocoResultCard: View {
    let result: LocoResultUi
    var unit: String = ""

    var body: some View {
        let color = result.isOver ? DSColor.danger : DSColor.success
        let sign = result.isOver ? "−" : "+"
        HStack(spacing: 12) {
            Text(result.isOver ? "↓" : "↑")
                .font(DSFont.sans(18, .bold))
                .foregroundColor(color)
                .frame(width: 34, height: 34)
                .background(color.opacity(0.20))
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(result.isOver ? "ПЕРЕРАСХОД" : "ЭКОНОМИЯ")
                    .dsTextStyle(.group, color: color)
                HStack(alignment: .firstTextBaseline, spacing: 0) {
                    Text("\(sign)\(result.magnitude)")
                        .font(DSFont.mono(20, .bold))
                        .foregroundColor(DSColor.text)
                        .lineLimit(1).minimumScaleFactor(0.5)
                    Text((unit.isEmpty ? " " : " \(unit)  ") + "(\(sign)\(result.percent)%)")
                        .font(DSFont.mono(13))
                        .foregroundColor(DSColor.textMuted)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(color.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
    }
}

/// Пилюля «k секции 0.83» (тап открывает шторку коэффициента).
struct LocoCoeffPill: View {
    let title: String
    let value: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(title).font(DSFont.sans(12)).foregroundColor(DSColor.textMuted)
                Text(value).font(DSFont.mono(13, .semibold)).foregroundColor(DSColor.text)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(DSColor.bgSubtle)
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

/// Пунктирный разделитель перед итогами секции.
struct LocoDashedDivider: View {
    var body: some View {
        Rectangle()
            .stroke(DSColor.borderStrong, style: StrokeStyle(lineWidth: 1, dash: [3, 3]))
            .frame(height: 1)
            .padding(.vertical, 6)
    }
}
