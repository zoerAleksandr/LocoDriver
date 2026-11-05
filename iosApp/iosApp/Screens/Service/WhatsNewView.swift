import SwiftUI
import ComposeApp

// Контракт: SCREEN_SPECS §22 («Что нового» после обновления, компонент
// PresentationBlock §20): 2 слайда `OnBoardingItems.getDataUpdatePresentation()`,
// «Пропустить»/«Начать» → закрыть. Android-эталон:
// features/route/.../ui/UpdatePresentationBlockScreen.kt + core/.../PresentationBlock.kt.
// Отклонение: drawable-слайдов в iOS-бандле нет — вместо них тональные иконки кита.

/// Слайд «Что нового».
private struct WhatsNewSlide: Identifiable {
    let id: Int
    let icon: DSIcon
    let title: String
    let text: String
}

/// Контент легаси-блока «Что нового» (§22). Два слайда, листание свайпом или
/// кнопкой «Далее»; «Пропустить»/«Начать» закрывают блок (`onClose`).
struct WhatsNewView: View {
    var onClose: () -> Void = { LaunchOverlays.shared.closeCurrent() }

    @State private var page = 0

    // Контент зашит в приложение (как на Android в OnBoardingItems).
    private let slides: [WhatsNewSlide] = [
        WhatsNewSlide(
            id: 0,
            icon: .clock,
            title: "Часовой пояс",
            text: "Для учета переходных маршрутов, ночных и праздничных часов установите свой часовой пояс"
        ),
        WhatsNewSlide(
            id: 1,
            icon: .document,
            title: "Главный экран",
            text: "Добавили обшее отработанное время на главный экран"
        ),
    ]

    private var isLast: Bool { page >= slides.count - 1 }

    var body: some View {
        ZStack {
            DSColor.bg.ignoresSafeArea()
            VStack(spacing: 0) {
                topBar
                TabView(selection: $page) {
                    ForEach(slides) { slide in
                        slideView(slide)
                            .tag(slide.id)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                bottomBar
            }
            .padding(.top, 8)
        }
    }

    // MARK: Шапка: «Назад» (кроме первого слайда) и «Пропустить» (§20 PresentationBlock).

    private var topBar: some View {
        HStack {
            if page > 0 {
                DSIconButton(
                    icon: .chevronLeft,
                    variant: .ghost,
                    size: 40,
                    accessibilityLabel: "Назад",
                    action: { withAnimation { page -= 1 } }
                )
            } else {
                Color.clear.frame(width: 40, height: 40)
            }
            Spacer()
            Button("Пропустить", action: onClose)
                .font(DSFont.sans(15))
                .foregroundColor(DSColor.text)
                .padding(.trailing, 8)
        }
        .padding(.horizontal, 12)
    }

    // MARK: Слайд: иконка сверху, заголовок и описание по центру.

    private func slideView(_ slide: WhatsNewSlide) -> some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            ZStack {
                DSColor.accentSoft
                slide.icon.image
                    .font(.system(size: 44))
                    .foregroundColor(DSColor.accent)
            }
            .frame(maxWidth: 180, maxHeight: 180)
            .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
            .layoutPriority(0.6)

            Text(slide.title)
                .font(DSFont.sans(22, .bold))
                .foregroundColor(DSColor.text)
                .multilineTextAlignment(.center)
                .padding(.top, 25)

            Text(slide.text)
                .font(DSFont.sans(15))
                .foregroundColor(DSColor.textMuted)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 10)
                .padding(.top, 8)
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: Низ: индикаторы и кнопка «Далее»/«Начать».

    private var bottomBar: some View {
        HStack {
            HStack(spacing: 12) {
                ForEach(slides.indices, id: \.self) { i in
                    Capsule()
                        .fill(i == page ? DSColor.accent : DSColor.bgSubtle)
                        .frame(width: i == page ? 25 : 10, height: 10)
                }
            }
            Spacer()
            DSCTAButton(
                title: isLast ? "Начать" : "Далее",
                icon: .chevronRight
            ) {
                if isLast {
                    onClose()
                } else {
                    withAnimation { page += 1 }
                }
            }
            .frame(width: 150)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 12)
    }
}
