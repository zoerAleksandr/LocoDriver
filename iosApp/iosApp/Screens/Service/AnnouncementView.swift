import SwiftUI
import ComposeApp

// Контракт: SCREEN_SPECS §23.1 (типы «Новость» и «Обновление», §23.3 «видел/не видел»).
// Android-эталон: app/src/main/java/com/z_company/loco_driver/ui/AnnouncementScreen.kt.
// Макета в design/src нет (docs/IOS_UI_GUIDE.md) — внешний вид собран из дизайн-кита.
// Общее правило: нет картинки ⇒ текст по центру.

/// Полноэкранное сообщение «Новость при запуске» (§23). Показывается overlay'ем
/// поверх приложения; закрывается кнопкой или крестиком — `onClose`.
struct AnnouncementView: View {
    @ObservedObject var store: AnnouncementViewModelWrapper
    var onClose: () -> Void = { LaunchOverlays.shared.closeCurrent() }

    var body: some View {
        ZStack {
            DSColor.bg.ignoresSafeArea()
            if let data = store.current {
                if data.isUpdateType {
                    AnnouncementUpdateContent(data: data, onClose: onClose)
                } else {
                    AnnouncementNewsContent(data: data, onClose: onClose)
                }
            }
        }
    }
}

// MARK: - Общие элементы

/// Крестик закрытия — верхний правый угол (§23.1).
private struct AnnouncementCloseButton: View {
    let onClose: () -> Void
    var body: some View {
        DSIconButton(
            icon: .close,
            variant: .subtle,
            size: 30,
            accessibilityLabel: "Закрыть",
            action: onClose
        )
    }
}

/// Pill-кнопка во всю ширину внизу экрана («Понятно» / «Далее»).
private struct AnnouncementBottomPill: View {
    let label: String
    let action: () -> Void
    var body: some View {
        DSCTAButton(title: label, action: action)
            .frame(minHeight: 54)
    }
}

/// Блок «картинка сверху → заголовок → описание» (когда картинка есть).
/// Картинка и текст делят высоту через weight 1.9 : 0.55 (§23.1); описание
/// при нехватке места обрезается (ellipsis).
private struct AnnouncementImageTitleTextBlock: View {
    let imageUrl: String?
    let title: String
    let text: String

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                DSColor.bgSubtle
                if let imageUrl, let url = URL(string: imageUrl) {
                    AsyncImage(url: url) { phase in
                        if let image = phase.image {
                            image.resizable().scaledToFit()
                        } else if phase.error != nil {
                            DSIcon.info.image
                                .font(.system(size: 28))
                                .foregroundColor(DSColor.textFaint)
                        } else {
                            ProgressView()
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .layoutPriority(1.9)

            Text(title)
                .font(DSFont.sans(22, .bold))
                .foregroundColor(DSColor.text)
                .multilineTextAlignment(.leading)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.top, 28)

            Text(text)
                .font(DSFont.sans(15))
                .foregroundColor(DSColor.textMuted)
                .multilineTextAlignment(.leading)
                .lineLimit(3)
                .truncationMode(.tail)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                .padding(.top, 12)
                .layoutPriority(0.55)
        }
    }
}

/// Центрированный блок «заголовок + описание» (когда картинки нет).
private struct AnnouncementCenteredTitleTextBlock: View {
    let title: String
    let text: String

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            Text(title)
                .font(DSFont.sans(22, .bold))
                .foregroundColor(DSColor.text)
                .multilineTextAlignment(.center)
            Text(text)
                .font(DSFont.sans(15))
                .foregroundColor(DSColor.textMuted)
                .multilineTextAlignment(.center)
                .lineLimit(4)
                .truncationMode(.tail)
                .padding(.top, 14)
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Тип «Новость» (§23.1)

private struct AnnouncementNewsContent: View {
    let data: AnnouncementUi
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Место под крестик.
            Color.clear.frame(height: 48)
            if data.imageUrl != nil {
                AnnouncementImageTitleTextBlock(
                    imageUrl: data.imageUrl,
                    title: data.title,
                    text: data.bodyText
                )
                AnnouncementBottomPill(label: "Понятно", action: onClose)
                    .padding(.top, 16)
                    .padding(.bottom, 16)
            } else {
                // Hero-иконка и центрированный текст; скролл — для длинного сообщения.
                ScrollView {
                    VStack(spacing: 0) {
                        ZStack {
                            DSColor.accentSoft
                            DSIcon.info.image
                                .font(.system(size: 30))
                                .foregroundColor(DSColor.accent)
                        }
                        .frame(width: 64, height: 64)
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .padding(.top, 72)

                        Text(data.title)
                            .font(DSFont.sans(22, .bold))
                            .foregroundColor(DSColor.text)
                            .multilineTextAlignment(.center)
                            .padding(.top, 24)

                        Text(data.bodyText)
                            .font(DSFont.sans(15))
                            .foregroundColor(DSColor.textMuted)
                            .multilineTextAlignment(.center)
                            .padding(.top, 14)
                            .padding(.bottom, 24)
                    }
                    .frame(maxWidth: .infinity)
                }
                AnnouncementBottomPill(label: "Понятно", action: onClose)
                    .padding(.bottom, 16)
            }
        }
        .padding(.horizontal, 24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay(alignment: .topTrailing) {
            AnnouncementCloseButton(onClose: onClose)
                .padding(.top, 8)
        }
    }
}

// MARK: - Тип «Обновление» (§23.1)

/// Карусель фич обновления: свайп в обе стороны и кнопка «Далее»/«Понятно».
/// Строго на одном экране без скролла; описание при нехватке места обрезается,
/// кнопка видна всегда. Общий заголовок обновления — сверху над каждой страницей.
private struct AnnouncementUpdateContent: View {
    let data: AnnouncementUi
    let onClose: () -> Void

    @State private var page = 0

    private var features: [AnnouncementFeatureUi] { data.features }
    private var isLast: Bool { page >= features.count - 1 }

    var body: some View {
        VStack(spacing: 0) {
            Color.clear.frame(height: 48) // место под крестик

            if !data.title.isEmpty {
                Text(data.title)
                    .font(DSFont.sans(13, .semibold))
                    .foregroundColor(DSColor.textMuted)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.bottom, 12)
            }

            Color.clear.frame(height: 12)

            TabView(selection: $page) {
                ForEach(Array(features.enumerated()), id: \.offset) { index, feature in
                    featurePage(feature)
                        .tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if features.count > 1 {
                HStack(spacing: 8) {
                    ForEach(features.indices, id: \.self) { i in
                        Circle()
                            .fill(i == page ? DSColor.accent : DSColor.textFaint.opacity(0.3))
                            .frame(width: i == page ? 9 : 7, height: i == page ? 9 : 7)
                    }
                }
                .padding(.top, 12)
            }

            AnnouncementBottomPill(label: isLast ? "Понятно" : "Далее") {
                if isLast {
                    onClose()
                } else {
                    withAnimation { page += 1 }
                }
            }
            .padding(.top, 16)
            .padding(.bottom, 16)
        }
        .padding(.horizontal, 24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay(alignment: .topTrailing) {
            AnnouncementCloseButton(onClose: onClose)
                .padding(.top, 8)
        }
        .onAppear { prefetchFeatureImages() }
    }

    @ViewBuilder
    private func featurePage(_ feature: AnnouncementFeatureUi) -> some View {
        if feature.imageUrl != nil {
            AnnouncementImageTitleTextBlock(
                imageUrl: feature.imageUrl,
                title: feature.title,
                text: feature.summary
            )
        } else {
            AnnouncementCenteredTitleTextBlock(
                title: feature.title,
                text: feature.summary
            )
        }
    }

    /// Предзагрузка картинок всех фич (§23.1): при свайпе между страницами
    /// не должно быть заметной задержки на загрузку.
    private func prefetchFeatureImages() {
        for feature in features {
            guard let raw = feature.imageUrl, let url = URL(string: raw) else { continue }
            URLSession.shared.dataTask(with: url).resume()
        }
    }
}
