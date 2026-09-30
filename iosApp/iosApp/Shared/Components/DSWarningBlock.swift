import SwiftUI

// Макет: design/src/night-warn.jsx → RouteNightWarn (плашка «Вторая ночь подряд»:
//        фон warning 10 %, рамка warning 55 %, радиус 14, аватар 26 с иконкой,
//        заголовок 13.5/700, текст 12 textMuted, крестик «Скрыть»);
//        design/src/time-sheet.jsx → NormWarn (CTA-кнопка «Настроить серию ›»).
// Спека: SCREEN_SPECS §2.7 — оранжевые инфо-блоки (WarnItem).

/// Оранжевый блок предупреждения.
/// Дополнительное содержимое (например, временная шкала ночных часов) передаётся
/// через `content:` — ВСЕГДА с меткой, не trailing-замыканием (иначе замыкание может
/// уйти в `onClose`); для простого варианта используйте инициализатор без него.
struct DSWarningBlock<Extra: View>: View {
    let title: String
    var message: String? = nil
    var icon: DSIcon = .warning
    var ctaTitle: String? = nil
    var onCTA: (() -> Void)? = nil
    var onClose: (() -> Void)? = nil
    @ViewBuilder var content: () -> Extra

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 10) {
                icon.image
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(DSColor.warning)
                    .frame(width: 26, height: 26)
                    .background(DSColor.warning.opacity(0.18))
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(DSFont.sans(13.5, .bold))
                        .foregroundColor(DSColor.text)
                        .fixedSize(horizontal: false, vertical: true)
                    if let message = message {
                        Text(message)
                            .font(DSFont.sans(12))
                            .foregroundColor(DSColor.textMuted)
                            .lineSpacing(12 * 0.4)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    if let ctaTitle = ctaTitle {
                        Button(action: { onCTA?() }) {
                            HStack(spacing: 4) {
                                Text(ctaTitle).font(DSFont.sans(12, .semibold))
                                DSIcon.chevronRight.image.font(.system(size: 9, weight: .bold))
                            }
                            .foregroundColor(DSColor.accent)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(DSColor.surface)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8, style: .continuous)
                                    .stroke(DSColor.border, lineWidth: 1)
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                        }
                        .buttonStyle(DSPressableStyle())
                        .padding(.top, 6)
                    }
                }
                Spacer(minLength: 0)
                if let onClose = onClose {
                    Button(action: onClose) {
                        DSIcon.close.image
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(DSColor.textMuted)
                            .frame(width: 24, height: 24)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text("Скрыть"))
                }
            }
            content()
        }
        .padding(.horizontal, 14)
        .padding(.top, 12)
        .padding(.bottom, 13)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DSColor.warning.opacity(0.10))
        .overlay(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(DSColor.warning.opacity(0.55), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

extension DSWarningBlock where Extra == EmptyView {
    init(
        title: String,
        message: String? = nil,
        icon: DSIcon = .warning,
        ctaTitle: String? = nil,
        onCTA: (() -> Void)? = nil,
        onClose: (() -> Void)? = nil
    ) {
        self.title = title
        self.message = message
        self.icon = icon
        self.ctaTitle = ctaTitle
        self.onCTA = onCTA
        self.onClose = onClose
        self.content = { EmptyView() }
    }
}

#Preview("Предупреждение — light") {
    DSWarningPreview().preferredColorScheme(.light)
}

#Preview("Предупреждение — dark") {
    DSWarningPreview().preferredColorScheme(.dark)
}

private struct DSWarningPreview: View {
    var body: some View {
        DSPreviewCanvas {
            DSWarningBlock(title: "Вторая ночь подряд",
                           message: "Третья ночь подряд не допускается.",
                           icon: .moon,
                           onClose: {})
            DSWarningBlock(title: "Нет нормы для серии ВЛ80с",
                           ctaTitle: "Настроить серию",
                           onCTA: {})
            DSWarningBlock(title: "Станция приёмки не выбрана",
                           message: "Выберите станцию, чтобы применить нормы интервалов",
                           content: {
                               Text("Доп. содержимое")
                                   .dsTextStyle(.captionMuted)
                                   .padding(.top, 10)
                           })
        }
    }
}
