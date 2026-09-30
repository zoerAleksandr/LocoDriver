import SwiftUI
import ComposeApp

/// Карточки-уведомления Главной (§4.4 п.5): подписка / бесплатный период / не синхронизировано.
/// Если карточек больше одной — карусель со свайпом и индикатором.
/// Нет в iOS-макете — собрано из кита (карточка, `DSIconAvatar`, `DSTonalButton`/`DSCTAButton`).
struct HomeNoticeCarousel: View {
    let notices: [HomeIosNotice]
    let onDismiss: (String) -> Void
    let onAction: (HomeIosNotice) -> Void

    @State private var index = 0
    @GestureState private var dragX: CGFloat = 0

    var body: some View {
        let safeIndex = min(index, max(notices.count - 1, 0))
        VStack(spacing: DSSpacing.md) {
            if notices.indices.contains(safeIndex) {
                HomeNoticeCard(
                    notice: notices[safeIndex],
                    onDismiss: { onDismiss(notices[safeIndex].dismissKey) },
                    onAction: { onAction(notices[safeIndex]) }
                )
                .id(notices[safeIndex].dismissKey)
                .offset(x: dragX * 0.4)
                .transition(.opacity)
                .gesture(swipe)
            }
            if notices.count > 1 {
                HStack(spacing: 6) {
                    ForEach(0..<notices.count, id: \.self) { i in
                        Capsule()
                            .fill(i == safeIndex ? DSColor.textMuted : DSColor.borderStrong)
                            .frame(width: i == safeIndex ? 18 : 6, height: 6)
                    }
                }
                .animation(.easeInOut(duration: 0.2), value: safeIndex)
            }
        }
        .onChange(of: notices.count) { count in
            if index >= count { index = max(count - 1, 0) }
        }
    }

    private var swipe: some Gesture {
        DragGesture(minimumDistance: 20)
            .updating($dragX) { value, state, _ in
                if abs(value.translation.width) > abs(value.translation.height) {
                    state = value.translation.width
                }
            }
            .onEnded { value in
                guard notices.count > 1,
                      abs(value.translation.width) > abs(value.translation.height),
                      abs(value.translation.width) > 50 else { return }
                withAnimation(.easeInOut(duration: 0.2)) {
                    if value.translation.width < 0 {
                        index = min(index + 1, notices.count - 1)
                    } else {
                        index = max(index - 1, 0)
                    }
                }
            }
    }
}

/// Одна карточка-уведомление: иконка, заголовок, «Закрыть», текст, подсказка, кнопка.
struct HomeNoticeCard: View {
    let notice: HomeIosNotice
    let onDismiss: () -> Void
    let onAction: () -> Void

    private var toneColor: Color? {
        switch notice.tone {
        case "warning": return DSColor.warning
        case "danger": return DSColor.danger
        default: return nil
        }
    }

    private var iconName: String? {
        switch notice.icon {
        case "crown": return "crown"
        case "schedule": return "clock"
        case "alert": return "exclamationmark.triangle"
        default: return nil
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DSSpacing.sm) {
            HStack(alignment: .center, spacing: 10) {
                if let iconName = iconName {
                    Image(systemName: iconName)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(toneColor ?? DSColor.accent)
                        .frame(width: 38, height: 38)
                        .background((toneColor ?? DSColor.accent).opacity(0.16))
                        .clipShape(RoundedRectangle(cornerRadius: 11, style: .continuous))
                }
                Text(notice.title)
                    .font(DSFont.sans(16, .bold))
                    .foregroundColor(DSColor.text)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: DSSpacing.sm)
                Button(action: onDismiss) {
                    DSIcon.close.image
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(DSColor.textMuted)
                        .frame(width: 28, height: 28)
                        .background(DSColor.bgSubtle)
                        .clipShape(Circle())
                }
                .buttonStyle(DSPressableStyle())
                .accessibilityLabel(Text("Закрыть"))
            }
            Text(notice.message)
                .font(DSFont.sans(14, .semibold))
                .foregroundColor(DSColor.text)
                .fixedSize(horizontal: false, vertical: true)
            Text(notice.hint)
                .dsTextStyle(.hint)
                .fixedSize(horizontal: false, vertical: true)
            if notice.progress >= 0 {
                HomeProgressBar(
                    progress: Double(notice.progress),
                    height: 4,
                    fill: toneColor ?? DSColor.accent,
                    track: (toneColor ?? DSColor.accent).opacity(0.16)
                )
                .padding(.top, 2)
            }
            actionButton
                .padding(.top, DSSpacing.xs)
        }
        .padding(DSSpacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(background)
        .overlay(
            RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous)
                .stroke(toneColor.map { $0.opacity(0.4) } ?? Color.clear, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: DSRadius.card, style: .continuous))
        .modifier(HomeNoticeShadow(isNeutral: toneColor == nil))
    }

    @ViewBuilder
    private var background: some View {
        if let toneColor = toneColor {
            toneColor.opacity(0.10)
        } else {
            DSColor.surface
        }
    }

    @ViewBuilder
    private var actionButton: some View {
        if toneColor == nil {
            DSCTAButton(title: notice.buttonText, action: onAction)
        } else {
            Button(action: onAction) {
                Text(notice.buttonText)
                    .font(DSFont.sans(15, .bold))
                    .foregroundColor(DSColor.accentInk)
                    .frame(maxWidth: .infinity, minHeight: 50)
                    .background(toneColor ?? DSColor.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .buttonStyle(DSPressableStyle())
        }
    }
}

private struct HomeNoticeShadow: ViewModifier {
    let isNeutral: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if isNeutral {
            content.dsShadow(.sm)
        } else {
            content
        }
    }
}

#Preview("Уведомления — light") {
    HomeNoticePreview().preferredColorScheme(.light)
}

#Preview("Уведомления — dark") {
    HomeNoticePreview().preferredColorScheme(.dark)
}

private struct HomeNoticePreview: View {
    var body: some View {
        VStack(spacing: 16) {
            HomeNoticeCard(
                notice: HomeIosNotice(
                    dismissKey: "a", tone: "warning", icon: "schedule",
                    title: "Бесплатный период", message: "Осталось 3 из 20 бесплатных маршрутов",
                    hint: "Оформите подписку заранее, чтобы не потерять возможность добавлять маршруты.",
                    buttonText: "Оформить подписку", action: "purchases", progress: 0.85
                ),
                onDismiss: {}, onAction: {}
            )
            HomeNoticeCard(
                notice: HomeIosNotice(
                    dismissKey: "b", tone: "neutral", icon: "none",
                    title: "Внимание!", message: "Не синхронизировано маршрутов: 4",
                    hint: "Проверьте подключение к интернету и выполните синхронизацию.",
                    buttonText: "Синхронизировать", action: "sync", progress: -1
                ),
                onDismiss: {}, onAction: {}
            )
        }
        .padding()
        .background(DSColor.bg)
    }
}
