import Foundation
import VKID

/// Обёртка VK ID SDK (§18.2/§18.4): one-tap авторизация → access token →
/// сетевые методы `ProfileIosViewModel`. Токен нужен ровно на один запрос и
/// нигде не сохраняется (VKID_AUTH_3.0.4_TASK.md) — в логи не попадает.
enum VKIDService {
    /// client_id веб-PWA «VK Машинист Web» (54740437). То же значение уходит на
    /// сервер в `vkClientId` (`IosRepositoryModule.kt`); в allowlist сервера
    /// `VK_APP_IDS=54740437,54410856` (VKID_AUTH_MIGRATION.md).
    static let clientId = "54740437"

    /// Результат one-tap авторизации: легаси-id (`accessToken.userId`, как
    /// `accessToken.userID.toString()` на Android), сам access token и имя
    /// профиля из ID-токена (JWT) — для карточки «VK ID» в профиле (§18.1).
    /// Email из VK по умолчанию не запрашивается — в регистрации уходит пустая
    /// строка, почту пользователь добавляет позже в профиле (контракт 3.0.4).
    struct Auth {
        let vkId: String
        let accessToken: String
        let fullName: String?
    }

    enum Failure: Error {
        /// Пользователь закрыл шторку VK ID.
        case cancelled
        /// Предыдущий флоу авторизации ещё не завершён.
        case alreadyInProgress
        /// Любая другая ошибка SDK (`.unknown` и т.п.).
        case failed
    }

    private static var isConfigured = false

    /// Инициализация SDK. `VKID.shared.set` проверяет URL-схемы в Info.plist
    /// (`vk54740437` в `CFBundleURLTypes`, `vkauthorize-silent` в
    /// `LSApplicationQueriesSchemes`) и падает без них. Логи SDK выключены,
    /// чтобы токены не попали в консоль.
    @discardableResult
    static func configure() -> Bool {
        guard !isConfigured else { return true }
        do {
            try VKID.shared.set(config: Configuration(
                appCredentials: AppCredentials(clientId: clientId, clientSecret: ""),
                loggingEnabled: false
            ))
            isConfigured = true
            print("AuthFlow: VKID SDK configured")
            return true
        } catch {
            print("AuthFlow: VKID configure failed — \(error)")
            return false
        }
    }

    /// Запуск one-tap авторизации (шторка VK ID). `completion` вызывается в
    /// главной очереди; токен в нём живёт до конца колбэка и не логируется.
    static func authorize(completion: @escaping (Result<Auth, Failure>) -> Void) {
        DispatchQueue.main.async {
            guard configure() else {
                completion(.failure(.failed))
                return
            }
            print("AuthFlow: VKID auth start")
            VKID.shared.authorize(
                with: AuthConfiguration(),
                using: .newUIWindow
            ) { result in
                // SDK вызывает completion в главной очереди.
                switch result {
                case .success(let session):
                    print("AuthFlow: VKID auth → success")
                    completion(.success(Auth(
                        vkId: String(session.accessToken.userId.value),
                        accessToken: session.accessToken.value,
                        fullName: name(from: session.idToken.value)
                    )))
                case .failure(let error):
                    let failure: Failure
                    switch error {
                    case .cancelled:
                        failure = .cancelled
                    case .authAlreadyInProgress:
                        failure = .alreadyInProgress
                    default:
                        failure = .failed
                    }
                    print("AuthFlow: VKID auth → \(failure)")
                    completion(.failure(failure))
                }
            }
        }
    }

    /// Имя пользователя из ID-токена (JWT). Поддерживает варианты claims:
    /// `name`, `first_name`+`last_name`, `given_name`+`family_name`.
    /// Сеть не нужна — профиль берём из того, что SDK уже вернул.
    private static func name(from idToken: String) -> String? {
        let parts = idToken.split(separator: ".")
        guard parts.count >= 2 else { return nil }
        var base64 = String(parts[1])
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        while base64.count % 4 != 0 { base64.append("=") }
        guard let data = Data(base64Encoded: base64),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return nil }
        // Диагностика: какие claims есть в токене (без значений).
        print("AuthFlow: VKID idToken claims — \(json.keys.sorted().joined(separator: ","))")
        if let name = json["name"] as? String, !name.isEmpty { return name }
        for (firstKey, lastKey) in [("first_name", "last_name"), ("given_name", "family_name")] {
            let first = json[firstKey] as? String ?? ""
            let last = json[lastKey] as? String ?? ""
            let joined = (first + " " + last).trimmingCharacters(in: .whitespaces)
            if !joined.isEmpty { return joined }
        }
        return nil
    }

    /// Завершение локальной сессии VK ID SDK при отвязке (Android делает то же
    /// через `VKID.instance.logout`). На авторизацию «Машиниста» не влияет.
    static func logout() {
        DispatchQueue.main.async {
            guard isConfigured else { return }
            VKID.shared.currentAuthorizedSession?.logout { _ in
                print("AuthFlow: VKID session logout done")
            }
        }
    }
}
