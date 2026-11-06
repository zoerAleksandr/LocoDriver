import SwiftUI
import ComposeApp

// Корзина маршрутов — SCREEN_SPECS §9.4.
// Логика — Kotlin `TrashIosViewModel` (обёртка `TrashViewModelWrapper`).
// Soft-delete маршруты хранятся 30 дней, затем удаляются автоматически.

struct TrashView: View {
    @StateObject private var vm = TrashViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared

    @State private var showClearConfirm = false

    var body: some View {
        VStack(spacing: 0) {
            navBar
            ScrollView {
                VStack(alignment: .leading, spacing: DSSpacing.lg) {
                    if vm.ui?.isSyncing == true {
                        HStack(spacing: DSSpacing.sm) {
                            ProgressView()
                            Text("Синхронизация корзины…")
                                .font(DSFont.sans(13))
                                .foregroundColor(DSColor.textMuted)
                        }
                    }
                    infoBlock
                    restoreAllButton
                    routeCards
                }
                .padding(DSMetrics.screenPadX)
                .padding(.bottom, 32)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .dsSnackbarQueue(vm.messages) { vm.popMessage() }
        .onAppear { vm.onScreenOpened() }
        .alert("Очистить корзину?", isPresented: $showClearConfirm) {
            Button("Удалить", role: .destructive) { vm.emptyTrash() }
            Button("Отмена", role: .cancel) {}
        } message: {
            Text(clearMessage)
        }
    }

    private var clearMessage: String {
        let count = (vm.ui?.routes ?? []).filter { $0.canPurge }.count
        var text = vm.emptyTrashConfirmText(count: count)
        if let note = vm.purgeTracksNote(), !note.isEmpty {
            text += "\n" + note
        }
        return text
    }

    // ── Топбар ─────────────────────────────────────────────────────────
    private var navBar: some View {
        DSNavBar(title: "Корзина", onBack: { router.back() }) {
            if vm.ui?.canClear == true {
                Button {
                    showClearConfirm = true
                } label: {
                    Text(vm.ui?.isPurging == true ? "Очистка…" : "Очистить")
                        .font(DSFont.sans(15, .medium))
                        .foregroundColor(vm.ui?.isPurging == true ? DSColor.textFaint : DSColor.accent)
                }
                .disabled(vm.ui?.isPurging == true || vm.ui?.isRestoringAll == true)
            }
        }
    }

    // ── Инфо-блок ──────────────────────────────────────────────────────
    private var infoBlock: some View {
        VStack(alignment: .leading, spacing: DSSpacing.sm) {
            Text("Здесь маршруты хранятся 30 дней, затем удаляются автоматически. До этого их можно восстановить.")
                .font(DSFont.sans(13))
                .foregroundColor(DSColor.text)
                .fixedSize(horizontal: false, vertical: true)
            if let pending = vm.ui?.pendingCheckCount, pending > 0 {
                Text("Проверяем статус: \(Int(pending))")
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
            } else if let waiting = vm.ui?.waitingSyncCount, waiting > 0 {
                Text("Ожидают синхронизации: \(Int(waiting))")
                    .font(DSFont.sans(12))
                    .foregroundColor(DSColor.textMuted)
            }
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

    private var restoreAllButton: some View {
        Button {
            vm.restoreAll()
        } label: {
            HStack(spacing: DSSpacing.sm) {
                if vm.ui?.isRestoringAll == true {
                    ProgressView().tint(DSColor.success)
                }
                Text(vm.ui?.isRestoringAll == true ? "Восстановление…" : "Восстановить всё")
                    .font(DSFont.sans(15, .semibold))
            }
            .foregroundColor(DSColor.success)
            .frame(maxWidth: .infinity)
            .padding(14)
            .background(DSColor.success.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous)
                    .stroke(DSColor.success.opacity(0.4), lineWidth: 1)
            )
        }
        .buttonStyle(DSPressableStyle())
        .disabled(vm.ui?.isRestoringAll == true || vm.ui?.isPurging == true || (vm.ui?.routes.isEmpty ?? true))
    }

    // ── Карточки удалённых маршрутов ───────────────────────────────────
    @ViewBuilder
    private var routeCards: some View {
        if vm.ui?.routes.isEmpty ?? true {
            DSEmptyState(
                icon: .trash,
                title: "Корзина пуста",
                message: "Удалённые маршруты появятся здесь"
            )
            .frame(maxWidth: .infinity)
            .padding(.top, 40)
        } else {
            ForEach(vm.ui?.routes ?? [], id: \.basicId) { route in
                trashCard(route)
            }
        }
    }

    private func trashCard(_ route: TrashRouteUi) -> some View {
        DSCard {
            VStack(alignment: .leading, spacing: DSSpacing.sm) {
                HStack(alignment: .top) {
                    Text(route.dateText)
                        .font(DSFont.mono(15, .semibold))
                        .foregroundColor(DSColor.text)
                    Spacer(minLength: DSSpacing.md)
                    restoreButton(route)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(route.deletedAtText)
                        .font(DSFont.sans(12))
                        .foregroundColor(DSColor.textMuted)
                    if route.isWaitingServer {
                        Text("Ожидает удаления с сервера")
                            .font(DSFont.sans(12))
                            .foregroundColor(DSColor.warning)
                    } else {
                        Text(route.daysLeftText)
                            .font(DSFont.sans(12))
                            .foregroundColor(DSColor.textMuted)
                    }
                }
            }
            .padding(DSMetrics.rowPadX)
        }
    }

    private func restoreButton(_ route: TrashRouteUi) -> some View {
        Button {
            vm.restore(routeId: route.basicId)
        } label: {
            Text(vm.restoringId == route.basicId ? "Восстановление…" : "Восстановить")
                .font(DSFont.sans(13, .semibold))
                .foregroundColor(DSColor.accent)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(DSColor.accentSoft)
                .clipShape(Capsule())
        }
        .buttonStyle(DSPressableStyle())
        .disabled(vm.ui?.isRestoringAll == true || vm.ui?.restoringRouteId != nil)
    }
}

private extension TrashViewModelWrapper {
    /// id маршрута, который сейчас восстанавливается (для анимации кнопки).
    var restoringId: String? { ui?.restoringRouteId }
}
