import SwiftUI

/// Корневой вид приложения.
/// Пока `AppInitIosViewModel` выполняет первичную инициализацию
/// (определение страны/таймзоны, загрузка производственного календаря,
/// миграции) — показываем splash. Потом переключаемся на AppCoordinator.
struct ContentView: View {
    @StateObject private var appInit = AppInitViewModelWrapper()

    var body: some View {
        ZStack {
            if appInit.appInitialized {
                AppCoordinator()
                    .transition(.opacity)
            } else {
                SplashView()
                    .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.25), value: appInit.appInitialized)
        .syncToastOverlay()
    }
}

private struct SplashView: View {
    var body: some View {
        ZStack {
            Color.appBg.ignoresSafeArea()
            VStack(spacing: 16) {
                Image(systemName: "train.side.front.car")
                    .font(.system(size: 56, weight: .regular))
                    .foregroundStyle(Color.appAccent)
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: Color.appAccent))
                Text("Загрузка…")
                    .font(.system(size: 14))
                    .foregroundStyle(Color.appSecondary)
            }
        }
    }
}
