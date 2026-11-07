import SwiftUI
import ComposeApp

// SCREEN_SPECS §18: «Профиль» и аутентификация. Состояния экрана — §18.S:
// заглушка «Открываем профиль...» → (миграция — на iOS не выполняется, §18.5) →
// авторизованный профиль (§18.1) или форма входа/регистрации (§18.0).
// Макеты: design/src/profile-screens.jsx (IOSProfile), design/src/login-screens.jsx (IOSAuth).

struct ProfileView: View {
    @StateObject private var vm = ProfileViewModelWrapper()
    @ObservedObject private var router = AppRouter.shared

    private enum ActiveSheet: Identifiable {
        case editEmail
        case addEmail
        var id: String { self == .editEmail ? "editEmail" : "addEmail" }
    }

    @State private var activeSheet: ActiveSheet? = nil
    @State private var vkUnlinkRequest: DSDeleteRequest? = nil
    @State private var showVkAddEmailAlert = false
    @State private var editEmailText = ""
    @State private var addEmailText = ""
    @State private var addPasswordText = ""

    var body: some View {
        Group {
            if let state = vm.state {
                if !state.sessionChecked {
                    loadingPlaceholder("Открываем профиль...")
                } else if state.isAuthorized {
                    authorizedProfile(state)
                } else {
                    ProfileAuthFormView(vm: vm)
                }
            } else {
                loadingPlaceholder("Открываем профиль...")
            }
        }
        .navigationTitle("Профиль")
        .background(DSColor.bg.ignoresSafeArea())
        .dsSnackbarQueue(vm.messages) { vm.popMessage() }
        .overlay { authOverlay }
        .overlay { syncOverlay }
        .alert("Аккаунта с этим VK нет", isPresented: vkRegistrationOfferBinding) {
            Button("Создать") { vm.confirmVkRegistration() }
            Button("Отмена", role: .cancel) { vm.dismissVkRegistration() }
        } message: {
            Text("Создать аккаунт с этим VK ID? Почта и пароль не потребуются — почту можно добавить позже в профиле.")
        }
        .alert("Сначала добавьте почту", isPresented: $showVkAddEmailAlert) {
            Button("Добавить почту") { openAddEmail() }
            Button("Отмена", role: .cancel) {}
        } message: {
            Text("У аккаунта нет почты и пароля — отвязка VK означала бы потерю доступа.")
        }
        .alert("Синхронизация завершена!", isPresented: syncSuccessBinding) {
            Button("Отлично!") { vm.resetSyncState() }
        } message: {
            Text("Данные успешно синхронизированы.")
        }
        .dsDeleteAlert($vkUnlinkRequest)
        .dsSheet(item: $activeSheet) { sheet in
            switch sheet {
            case .editEmail:
                ProfileInputSheet(
                    title: "Новый email",
                    hint: nil,
                    initialValue: editEmailText,
                    confirmTitle: "Сохранить",
                    isLoading: vm.state?.updateEmailLoading ?? false,
                    errorText: vm.state?.updateEmailError,
                    isValid: { vm.isEmailValid($0) },
                    keyboard: .emailAddress,
                    onValueChange: { _ in vm.clearUpdateEmailError() },
                    onConfirm: { value in
                        editEmailText = value
                        vm.updateEmail(value)
                    },
                    onClose: {
                        vm.resetUpdateEmailState()
                        activeSheet = nil
                    }
                )
            case .addEmail:
                ProfileEmailPasswordSheet(
                    title: "Добавить почту",
                    hint: "Сейчас в аккаунт можно войти только через VK ID. Почта с паролем — второй способ входа на случай, если привязка VK отвалится.",
                    confirmTitle: "Добавить",
                    minPasswordLength: vm.minPasswordLength,
                    isLoading: vm.state?.addEmailLoading ?? false,
                    errorText: vm.state?.addEmailError,
                    isEmailValid: { vm.isEmailValid($0) },
                    onValueChange: { _, _ in vm.clearAddEmailError() },
                    onConfirm: { email, password in
                        addEmailText = email
                        addPasswordText = password
                        vm.addEmail(email, password: password)
                    },
                    onClose: {
                        vm.resetAddEmailState()
                        activeSheet = nil
                    }
                )
            }
        }
        .onAppear {
            vm.onEvent = { event in
                if event.kind == "emailUpdated" || event.kind == "emailAdded" {
                    activeSheet = nil
                }
            }
        }
    }

    // MARK: - Состояния §18.S

    private func loadingPlaceholder(_ text: String) -> some View {
        VStack(spacing: 14) {
            ProgressView()
            Text(text)
                .dsTextStyle(.captionMuted)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// Поверх любого состояния: GenericLoading входа/регистрации с крестиком отмены (§18.S).
    /// ⚠️ Поведение Android (возможный баг, BUG-51): опечатка «полььзователя» сохранена.
    @ViewBuilder
    private var authOverlay: some View {
        let state = vm.state
        if state?.authLoading == true || state?.registrationLoading == true {
            ZStack {
                DSColor.bg.opacity(0.92).ignoresSafeArea()
                VStack(spacing: 14) {
                    ProgressView()
                    Text(state?.registrationLoading == true
                         ? "Регистрируем нового полььзователя..."
                         : "Выполняется вход...")
                        .dsTextStyle(.body)
                    Button {
                        vm.cancelAuth()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(DSColor.textMuted)
                            .frame(width: 34, height: 34)
                            .background(DSColor.bgSubtle, in: Circle())
                    }
                }
            }
        }
    }

    /// SyncProgressDialog (§18.5): модальный поверх экрана; тап вне — resetSyncState.
    @ViewBuilder
    private var syncOverlay: some View {
        if let dialog = vm.state?.syncDialog, dialog.visible {
            SyncProgressDialogView(
                dialog: dialog,
                onCancel: { vm.cancelSync() },
                onDismiss: { vm.resetSyncState() }
            )
        } else if let dialog = vm.state?.syncDialog, dialog.isNetworkError {
            SyncDialogResultView(
                icon: "wifi.slash",
                title: "Нет интернета",
                buttonTitle: "Понятно",
                onButton: { vm.resetSyncState() }
            )
        } else if let dialog = vm.state?.syncDialog, dialog.isSessionExpired {
            SyncDialogResultView(
                icon: "person.crop.circle.badge.xmark",
                title: "Сессия истекла",
                message: "Войдите в аккаунт заново в разделе „Профиль“. Данные на устройстве сохранены и будут синхронизированы после входа",
                buttonTitle: "Понятно",
                onButton: { vm.resetSyncState() }
            )
        } else if let dialog = vm.state?.syncDialog, dialog.isComplete, !dialog.isSuccess {
            SyncProgressDialogView(
                dialog: dialog,
                onCancel: { vm.cancelSync() },
                onDismiss: { vm.resetSyncState() }
            )
        }
    }

    private var vkRegistrationOfferBinding: Binding<Bool> {
        Binding(
            get: { vm.state?.vkRegistrationOffer ?? false },
            set: { if !$0 { vm.dismissVkRegistration() } }
        )
    }

    private var syncSuccessBinding: Binding<Bool> {
        Binding(
            get: { vm.state?.syncDialog.isSuccess ?? false },
            set: { if !$0 { vm.resetSyncState() } }
        )
    }

    private func openAddEmail() {
        addEmailText = ""
        addPasswordText = ""
        vm.resetAddEmailState()
        activeSheet = .addEmail
    }

    // MARK: - Авторизованный профиль (§18.1)

    private func authorizedProfile(_ state: ProfileIosState) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if state.profileLoading || state.profileNetworkError {
                    dataBanner(state)
                }
                headerCard(state)
                DSGroupHeader("ПОДПИСКА", topSpacing: DSMetrics.groupHeadMt)
                subscriptionCard(state)
                referralRow(state)
                DSGroupHeader("СИНХРОНИЗАЦИЯ")
                syncSection(state)
                DSGroupHeader("ДАННЫЕ")
                dataSection(state)
                logoutButton
            }
            .padding(.horizontal, 12)
            .padding(.bottom, 50)
        }
        .refreshable { await vm.pullToSync() }
        .onChange(of: state.vkLinkMessage) { message in
            // Ошибка привязки/отвязки VK (§18.2) — snackbar, один раз.
            guard let message = message else { return }
            vm.clearVkLinkMessage()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                vm.messages.append(DSSnackbarItem(text: message))
            }
        }
    }

    /// Баннер ProfileDataBanner (§18.S).
    private func dataBanner(_ state: ProfileIosState) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if state.profileLoading {
                HStack(spacing: 8) {
                    ProgressView().controlSize(.small)
                    Text("Загружаем данные. Текущая информация могла устареть")
                        .dsTextStyle(.captionMuted)
                }
            } else {
                Text("Нет сети. Показаны сохранённые данные, они могли устареть")
                    .dsTextStyle(.captionMuted)
                Button("Повторить") { vm.refresh() }
                    .font(DSFont.sans(14, .medium))
                    .foregroundColor(DSColor.accent)
                if let vpnHint = state.vpnHint {
                    Text(vpnHint).dsTextStyle(.hint)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(DSColor.bgSubtle, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .padding(.bottom, 10)
    }

    /// Шапка (VK/имя) и почта — одна карточка с разделителем (§18.S).
    private func headerCard(_ state: ProfileIosState) -> some View {
        DSCard {
            VStack(alignment: .leading, spacing: 0) {
                vkBlock(state)
                DSDivider(inset: 0)
                emailBlock(state)
            }
        }
    }

    @ViewBuilder
    private func vkBlock(_ state: ProfileIosState) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            if state.profileLoading {
                HStack(spacing: 10) {
                    ProgressView().controlSize(.small)
                    Text("Загружаем…").dsTextStyle(.captionMuted)
                }
                .padding(.vertical, 6)
            }
            if let name = state.vkProfileName {
                HStack(alignment: .top, spacing: 12) {
                    VKAvatar(url: state.vkAvatarUrl, size: 52)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(name).font(DSFont.sans(17, .semibold))
                            .lineLimit(2)
                            .fixedSize(horizontal: false, vertical: true)
                        HStack(spacing: 6) {
                            DSPill(text: "ID", tone: .muted)
                            if let vkId = state.vkId {
                                Text(vkId)
                                    .font(DSFont.mono(13))
                                    .foregroundColor(DSColor.textMuted)
                            }
                        }
                    }
                    Spacer(minLength: 8)
                    if state.vkId != nil {
                        Button("Отвязать") { confirmVkUnlink(state) }
                            .font(DSFont.sans(14, .medium))
                            .foregroundColor(DSColor.danger)
                    }
                }
            } else if state.vkSdkSignedIn, state.vkId != nil {
                // Вход в VK выполнен, но имя из токена не досталось: показываем id
                // без призыва «Войдите в VK» и без повторной кнопки входа.
                HStack(alignment: .top, spacing: 12) {
                    VKAvatar(url: state.vkAvatarUrl, size: 52)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("VK ID").font(DSFont.sans(17, .semibold))
                        HStack(spacing: 6) {
                            DSPill(text: "ID", tone: .muted)
                            if let vkId = state.vkId {
                                Text(vkId)
                                    .font(DSFont.mono(13))
                                    .foregroundColor(DSColor.textMuted)
                            }
                        }
                    }
                    Spacer(minLength: 8)
                    Button("Отвязать") { confirmVkUnlink(state) }
                        .font(DSFont.sans(14, .medium))
                        .foregroundColor(DSColor.danger)
                }
            } else {
                VStack(alignment: .leading, spacing: 8) {
                    Text(state.vkId != nil ? "VK ID привязан" : "VK ID не привязан")
                        .font(DSFont.sans(15, .medium))
                    if state.vkId != nil {
                        Text("Войдите в VK, чтобы показать профиль")
                            .dsTextStyle(.captionMuted)
                        if let vkId = state.vkId {
                            Text(vkId)
                                .font(DSFont.mono(13))
                                .foregroundColor(DSColor.textMuted)
                        }
                    }
                    Button {
                        // One-tap VK ID (§18.1/§18.4): не привязан → attachVKID с
                        // access-токеном; привязан → только локальный признак.
                        vm.linkVKID()
                    } label: {
                        HStack(spacing: 8) {
                            Image(systemName: "person.crop.circle.badge.checkmark")
                            Text(state.vkId != nil ? "Войти в VK" : "Привязать VK ID")
                        }
                        .font(DSFont.sans(15, .medium))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity, minHeight: 46)
                        .background(Color(red: 0, green: 0.47, blue: 1))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    if state.vkId != nil {
                        Button("Отвязать VK ID") { confirmVkUnlink(state) }
                            .font(DSFont.sans(14, .medium))
                            .foregroundColor(DSColor.danger)
                    }
                }
            }
        }
        .padding(16)
    }

    @ViewBuilder
    private func emailBlock(_ state: ProfileIosState) -> some View {
        if state.emailBlockError {
            HStack(spacing: 10) {
                Image(systemName: "envelope").foregroundColor(DSColor.textMuted)
                Text("Ошибка загрузки").dsTextStyle(.captionMuted)
                Spacer()
                Button("Повторить") { vm.refresh() }
                    .font(DSFont.sans(14, .medium))
                    .foregroundColor(DSColor.accent)
            }
            .padding(16)
        } else {
            HStack(spacing: 12) {
                DSIconAvatar(icon: .document, size: .md, tone: .accent, shape: .square)
                VStack(alignment: .leading, spacing: 2) {
                    if let email = state.email {
                        HStack(spacing: 8) {
                            Text(email)
                                .font(DSFont.sans(15, .medium))
                                .lineLimit(1)
                            Button {
                                editEmailText = email
                                vm.resetUpdateEmailState()
                                activeSheet = .editEmail
                            } label: {
                                Image(systemName: "pencil")
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundColor(DSColor.accent)
                            }
                            .accessibilityLabel("Изменить почту")
                        }
                    } else {
                        Text("Почта не добавлена").font(DSFont.sans(15, .medium))
                        Text("Вход только через VK ID").dsTextStyle(.captionMuted)
                    }
                }
                Spacer(minLength: 8)
                if state.email == nil {
                    Button("Добавить") { openAddEmail() }
                        .font(DSFont.sans(14, .semibold))
                        .foregroundColor(DSColor.accent)
                }
            }
            .padding(16)
        }
    }

    // MARK: - Подписка (§18.1)

    private func subscriptionCard(_ state: ProfileIosState) -> some View {
        Button {
            // Гейт покупок (§3.2): isAuthorized — из общего хелпера авторизации (С4).
            router.showPurchases(isAuthorized: AuthSession.shared.isAuthorizedNow)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "crown.fill")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(state.hasActiveSubscription ? DSColor.accent : DSColor.accent)
                    .frame(width: 40, height: 40)
                    .background(
                        DSColor.accentSoft,
                        in: RoundedRectangle(cornerRadius: 12, style: .continuous)
                    )
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 8) {
                        Text("Машинист Pro").font(DSFont.sans(16, .semibold))
                        if state.hasActiveSubscription {
                            DSPill(text: "АКТИВНА", tone: .accent)
                        }
                    }
                    .foregroundColor(DSColor.text)
                    Text(state.hasActiveSubscription
                         ? state.subscriptionEndLabel
                         : "Оформить подписку — откроется больше возможностей")
                        .dsTextStyle(.captionMuted, color: DSColor.textMuted)
                }
                Spacer(minLength: 4)
                DSIcon.chevronRight.image
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(DSColor.textFaint)
            }
            .padding(16)
            .background(
                state.hasActiveSubscription ? Color.white : DSColor.surface,
                in: RoundedRectangle(cornerRadius: 16, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(state.hasActiveSubscription ? Color.clear : DSColor.border, lineWidth: 1)
            )
        }
        .buttonStyle(DSPressableStyle())
    }

    /// Строка «Пригласить друга» + одноразовая подсказка (§18.1, §6.9).
    /// Подсказка — тултип-пузырь над строкой, а не блок экрана.
    private func referralRow(_ state: ProfileIosState) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            if state.showReferralHint {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Приглашайте друзей: после первой оплаты друга вы оба получите бесплатные дни.")
                        .font(DSFont.sans(12))
                        .foregroundColor(DSColor.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                    HStack {
                        Spacer()
                        Button("Понятно") { vm.dismissReferralHint() }
                            .font(DSFont.sans(12, .semibold))
                            .foregroundColor(DSColor.accent)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(DSColor.bgSubtle, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(alignment: .bottomTrailing) {
                    // Хвостик тултипа, направленный на строку «Пригласить друга».
                    Triangle()
                        .fill(DSColor.bgSubtle)
                        .frame(width: 14, height: 7)
                        .rotationEffect(.degrees(180))
                        .offset(x: -18, y: 7)
                }
                .padding(.horizontal, 18)
                .padding(.bottom, 8)
            }
            DSCard {
                DSListRow(
                    title: "Пригласить друга",
                    subtitle: "Получите бесплатные дни обслуживания",
                    icon: .heart,
                    showsDivider: false
                ) {
                    router.show(.referral)
                }
            }
            .padding(.top, 10)
        }
    }

    // MARK: - Синхронизация (§18.1, §18.5)

    @ViewBuilder
    private func syncSection(_ state: ProfileIosState) -> some View {
        DSCard {
            VStack(alignment: .leading, spacing: 10) {
                if state.hasActiveSubscription {
                    Button {
                        vm.startSync()
                    } label: {
                        HStack(spacing: 10) {
                            if state.isSyncing {
                                ProgressView().controlSize(.small)
                            } else {
                                DSIcon.sync.image.font(.system(size: 16, weight: .semibold))
                            }
                            Text("Синхронизация").font(DSFont.sans(15, .medium))
                            Spacer()
                        }
                        .foregroundColor(DSColor.text)
                        .padding(.horizontal, 14)
                        .frame(maxWidth: .infinity, minHeight: 48)
                        .background(DSColor.surfaceAlt, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .buttonStyle(DSPressableStyle())
                    // BUG-53 (паритет с Android): при нулевой метке показывается текущее время.
                    Text(state.lastSyncLabel)
                        .dsTextStyle(.captionMuted)
                } else {
                    Text("Раздел синхронизации доступен после оплаты подписки.")
                        .dsTextStyle(.captionMuted)
                }
            }
            .padding(16)
        }
    }

    // MARK: - Данные (§18.1)

    private func dataSection(_ state: ProfileIosState) -> some View {
        DSCard {
            VStack(alignment: .leading, spacing: 0) {
                DSListRow(
                    title: "Мои треки",
                    subtitle: "История записанных поездок",
                    icon: .mapPin
                ) {
                    router.show(.trackHistory)
                }
                DSListRow(
                    title: "Данные и конфиденциальность",
                    subtitle: "Треки и история поездок",
                    icon: .info
                ) {
                    router.show(.dataPrivacy)
                }
                DSListRow(
                    title: "Корзина маршрутов",
                    subtitle: trashSubtitle(Int(state.trashCount)),
                    icon: .trash,
                    showsDivider: false
                ) {
                    router.show(.trash)
                }
            }
        }
    }

    private func trashSubtitle(_ count: Int) -> String {
        // Маршрут / маршрута / маршрутов (§18.1).
        let mod10 = count % 10
        let mod100 = count % 100
        let word: String
        if mod10 == 1 && mod100 != 11 {
            word = "маршрут"
        } else if mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14) {
            word = "маршрута"
        } else {
            word = "маршрутов"
        }
        return "\(count) \(word)"
    }

    private var logoutButton: some View {
        Button(role: .destructive) {
            // §18.1: выход без подтверждения.
            vm.logOut()
        } label: {
            Text("Выйти из аккаунта")
                .font(DSFont.sans(15, .medium))
                .foregroundColor(DSColor.danger)
                .frame(maxWidth: .infinity, minHeight: 50)
                .background(DSColor.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(DSColor.danger.opacity(0.35), lineWidth: 1)
                )
        }
        .buttonStyle(DSPressableStyle())
        .padding(.top, 22)
    }

    private func confirmVkUnlink(_ state: ProfileIosState) {
        // Пустая почта — пароля нет, отвязка означала бы потерю доступа (§18.4).
        if state.email == nil {
            showVkAddEmailAlert = true
            return
        }
        vkUnlinkRequest = DSDeleteRequest(
            title: "Отвязать VK ID?",
            message: "Вход через VK перестанет работать. Останется вход по почте \(state.email ?? "") и паролю, а привязать VK можно будет снова.",
            confirmTitle: "Отвязать"
        ) {
            vm.removeUsersVKID()
        }
    }
}

/// Хвостик тултипа-подсказки.
private struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: rect.midX, y: rect.minY))
        p.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
        p.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
        p.closeSubpath()
        return p
    }
}

/// Аватар VK: фото профиля, иначе стандартная иконка (§18.1).
private struct VKAvatar: View {
    let url: String?
    let size: CGFloat

    var body: some View {
        if let url = url, let remote = URL(string: url) {
            AsyncImage(url: remote) { phase in
                switch phase {
                case .success(let image):
                    image.resizable().scaledToFill()
                default:
                    DSIconAvatar(icon: .profile, size: .lg, tone: .accent, shape: .circle)
                }
            }
            .frame(width: size, height: size)
            .clipShape(Circle())
        } else {
            DSIconAvatar(icon: .profile, size: .lg, tone: .accent, shape: .circle)
        }
    }
}
