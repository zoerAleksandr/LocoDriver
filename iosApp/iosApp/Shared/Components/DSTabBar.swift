import SwiftUI

// Макет: design/src/ios-screens.jsx → таб-бар IOSScreenTrips + TabBtn
//        (плавающая панель: bottom 22, left/right 10, surface, радиус 30,
//        рамка border, тень floating, padding 10/14; вкладка — иконка 22 +
//        подпись 10/600, активная accent, неактивная textMuted; центральная
//        «+» 46×46 фон cta, тень 0 8 18 rgba(0,0,0,.25));
//        design/src/settings-screens.jsx → FauxTabBarIOS; ScrimBottom (градиент под панелью).
// Спека: SCREEN_SPECS §3.1 — 4 вкладки «Главная · Зарплата · Настройки · Профиль»
// + отдельная кнопка «+» (действие «новый маршрут», §3.4). Тексты — по спеке
// («Главная»), а не по макету («Главный»/«Поездки»).

/// Плавающий нижний таб-бар корневых экранов.
struct DSTabBar: View {
    @Binding var selection: AppTab
    let onAdd: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            tabButton(.home)
            tabButton(.salary)
            Button(action: onAdd) {
                DSIcon.plus.image
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(DSColor.ctaInk)
                    .frame(width: 46, height: 46)
                    .background(DSColor.cta)
                    .clipShape(Circle())
                    .shadow(color: Color.black.opacity(0.25), radius: 9, x: 0, y: 8)
            }
            .buttonStyle(DSPressableStyle())
            .accessibilityLabel(Text("Добавить маршрут"))
            .frame(maxWidth: .infinity)
            tabButton(.settings)
            tabButton(.profile)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(DSColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 30, style: .continuous)
                .stroke(DSColor.border, lineWidth: 1)
        )
        .dsShadow(.floating)
        .padding(.horizontal, 10)
    }

    private func tabButton(_ tab: AppTab) -> some View {
        let isActive = selection == tab
        return Button {
            // Повторный тап по текущей вкладке ничего не делает (§3.1).
            if !isActive { selection = tab }
        } label: {
            VStack(spacing: 2) {
                tab.icon.image
                    .font(.system(size: 20, weight: .regular))
                    .frame(height: 24)
                Text(tab.title)
                    .font(DSFont.sans(10, .semibold))
                    .lineLimit(1)
            }
            .foregroundColor(isActive ? DSColor.accent : DSColor.textMuted)
            .padding(.vertical, 4)
            .frame(maxWidth: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }
}

/// Градиент-скрим под плавающей панелью: контент мягко уходит под таб-бар/тулбар
/// (`ScrimBottom`: bg 0 % → 85 % на 45 % → 100 %).
struct DSBottomScrim: View {
    var height: CGFloat = 140

    var body: some View {
        LinearGradient(
            stops: [
                .init(color: DSColor.bg.opacity(0), location: 0),
                .init(color: DSColor.bg.opacity(0.85), location: 0.45),
                .init(color: DSColor.bg, location: 1),
            ],
            startPoint: .top,
            endPoint: .bottom
        )
        .frame(height: height)
        .allowsHitTesting(false)
    }
}

#Preview("Таб-бар — light") {
    DSTabBarPreview().preferredColorScheme(.light)
}

#Preview("Таб-бар — dark") {
    DSTabBarPreview().preferredColorScheme(.dark)
}

private struct DSTabBarPreview: View {
    @State private var tab: AppTab = .home

    var body: some View {
        ZStack(alignment: .bottom) {
            DSColor.bg.ignoresSafeArea()
            DSBottomScrim()
            DSTabBar(selection: $tab, onAdd: {})
                .padding(.bottom, 8)
        }
    }
}
