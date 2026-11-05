import SwiftUI
import UIKit
import WebKit
import ComposeApp

// SCREEN_SPECS §18.R: «Пригласить друга» (ReferralScreen). Не nav-destination на
// Android, но в iOS — push-маршрут `AppRoute.referral` (стрелка «назад» возвращает
// в Профиль). Запрос `GET /v1/referrals/me` — при каждом открытии экрана.

struct ReferralView: View {
    @StateObject private var vm = ReferralViewModelWrapper()
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                if Bundle.main.url(forResource: "referral_v3_animated", withExtension: "svg") != nil {
                    ReferralHeroView(dark: colorScheme == .dark)
                        .frame(maxWidth: .infinity)
                        .aspectRatio(360.0 / 334.0, contentMode: .fit)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                } else {
                    // SVG не попал в бандл — нативная заглушка (§18.R допускает
                    // любой нативный способ показа иллюстрации).
                    ReferralHeroFallback()
                        .frame(maxWidth: .infinity)
                        .aspectRatio(360.0 / 334.0, contentMode: .fit)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }

                statusBlock
                metricsRow
                howItWorks
                rulesButton
                    .padding(.bottom, 28)
            }
            .padding(.horizontal, DSMetrics.screenPadX)
            .padding(.top, 8)
        }
        .background(DSColor.bg.ignoresSafeArea())
        .navigationTitle("Пригласить друга")
        .navigationBarTitleDisplayMode(.inline)
        .dsSnackbarQueue(vm.messages) { vm.popMessage() }
        .onAppear { vm.refresh() }
    }

    // MARK: - Блок статуса (п. 3–5)

    @ViewBuilder
    private var statusBlock: some View {
        if vm.status == nil {
            if vm.loading {
                DSCard {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                    .frame(height: 120)
                }
            } else {
                DSCard {
                    VStack(alignment: .leading, spacing: 10) {
                        Text("Не удалось загрузить код")
                            .font(DSFont.sans(15, .medium))
                            .foregroundColor(DSColor.danger)
                        Button {
                            vm.refresh()
                        } label: {
                            HStack(spacing: 8) {
                                DSIcon.sync.image.font(.system(size: 15, weight: .semibold))
                                Text("Повторить").font(DSFont.sans(14, .medium))
                            }
                            .foregroundColor(DSColor.accent)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, 6)
                }
            }
        } else if let status = vm.status {
            if !vm.canInvite {
                inviteLockedPlate
            } else {
                codeCard(status)
            }
        }
    }

    /// Жёлтая плашка «Приглашения после первой оплаты» (п. 4) — успешный ответ, не ошибка.
    private var inviteLockedPlate: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "crown.fill")
                .font(.system(size: 20))
                .foregroundColor(Color(red: 0.5, green: 0.35, blue: 0))
            VStack(alignment: .leading, spacing: 5) {
                Text("Приглашения после первой оплаты")
                    .font(DSFont.sans(15, .semibold))
                    .foregroundColor(Color(red: 0.35, green: 0.24, blue: 0))
                Text("Реферальная программа доступна для приглашающих, которые уже хотя бы раз оплачивали Про. После вашей первой оплаты здесь появится код.")
                    .font(DSFont.sans(13))
                    .foregroundColor(Color(red: 0.35, green: 0.24, blue: 0))
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(red: 1, green: 0.94, blue: 0.74), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    /// Карточка кода (п. 5): тап по строке кода копирует его (без тоста).
    private func codeCard(_ status: ReferralStatusResponse) -> some View {
        DSCard {
            VStack(alignment: .leading, spacing: 12) {
                Text("ВАШ КОД")
                    .dsTextStyle(.labelMuted)
                Button {
                    UIPasteboard.general.string = status.code
                } label: {
                    HStack {
                        Text(status.code)
                            .font(DSFont.mono(26, .bold))
                            .foregroundColor(DSColor.text)
                            .tracking(1.5)
                        Spacer()
                        Image(systemName: "doc.on.doc")
                            .font(.system(size: 18, weight: .medium))
                            .foregroundColor(DSColor.accent)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(DSPressableStyle())
                ShareLink(item: shareText(status.code)) {
                    HStack(spacing: 8) {
                        DSIcon.share.image.font(.system(size: 16, weight: .semibold))
                        Text("Поделиться кодом").font(DSFont.sans(15, .semibold))
                    }
                    .foregroundColor(DSColor.ctaInk)
                    .frame(maxWidth: .infinity, minHeight: 50)
                    .background(DSColor.cta, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
            }
        }
    }

    private func shareText(_ code: String) -> String {
        "Попробуй «Машинист Про». Введи мой код \(code) перед первой оплатой — после оплаты каждому добавят половину оплаченного периода."
    }

    // MARK: - Метрики (п. 6)

    private var metricsRow: some View {
        HStack(spacing: 10) {
            metricCard(title: "Приглашено", value: "\(vm.invitedRewardedCount)")
            metricCard(title: "Начислено", value: "\(vm.awardedDays) дн.")
        }
    }

    private func metricCard(title: String, value: String) -> some View {
        DSCard {
            VStack(alignment: .leading, spacing: 6) {
                Text(title).dsTextStyle(.captionMuted)
                Text(value)
                    .font(DSFont.sans(22, .bold))
                    .foregroundColor(DSColor.text)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.vertical, 4)
        }
    }

    // MARK: - «Как это работает» (п. 7)

    private var howItWorks: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Как это работает")
                .font(DSFont.sans(18, .semibold))
            Text("Приглашать может только тот, кто уже хотя бы раз оплачивал Про. Друг может пользоваться Android или iOS — код работает одинаково на обеих платформах.")
                .dsTextStyle(.captionMuted)
            DSCard {
                VStack(alignment: .leading, spacing: 14) {
                    stepRow(number: 1, title: "Отправьте код другу", subtitle: "Количество приглашений не ограничено.")
                    stepRow(number: 2, title: "Друг вводит код", subtitle: "Применить код можно до его первой оплаты Про.")
                    stepRow(number: 3, title: "Получите бонус вместе", subtitle: "После оплаты каждому добавится половина оплаченного периода.", showsDivider: false)
                }
            }
            Text("При возврате платежа неиспользованный бонус отменяется у обоих участников.")
                .dsTextStyle(.hint)
        }
    }

    private func stepRow(number: Int, title: String, subtitle: String, showsDivider: Bool = true) -> some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 12) {
                Text("\(number)")
                    .font(DSFont.sans(13, .bold))
                    .foregroundColor(DSColor.accentInk)
                    .frame(width: 26, height: 26)
                    .background(DSColor.accent, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(DSFont.sans(15, .medium))
                    Text(subtitle).dsTextStyle(.captionMuted)
                }
                Spacer(minLength: 0)
            }
            if showsDivider {
                DSDivider(inset: 38).padding(.vertical, 12)
            }
        }
    }

    // MARK: - Правила (п. 8)

    private var rulesButton: some View {
        Button {
            if let url = URL(string: "https://locodriver.ru/referral.html") {
                openURL(url)
            }
        } label: {
            Text("Подробные правила программы ›")
                .font(DSFont.sans(15, .medium))
                .foregroundColor(DSColor.accent)
        }
    }
}

/// Hero-иллюстрация (п. 2): SVG `referral_v3_animated.svg` в WKWebView с
/// CSS-переменными текущей темы (копия токенов PWA, как на Android).
/// Если ресурс не попал в бандл — нативная заглушка (§18.R допускает любой
/// нативный способ показа).
private struct ReferralHeroView: UIViewRepresentable {
    let dark: Bool

    private static let varsLight = """
    --surface:#ffffff;--surface-alt:#f7f8fa;--accent:#00a0f5;--accent-soft:rgba(0,160,245,0.1);
    --success:#00b341;--text:#0a0e14;--text-muted:rgba(10,14,20,0.55);--text-faint:rgba(10,14,20,0.38);
    --border-strong:rgba(10,14,20,0.14);--shadow-color:rgba(10,14,20,0.1);
    """
    private static let varsDark = """
    --surface:#2a2b2d;--surface-alt:#34363a;--accent:#33bfff;--accent-soft:rgba(51,191,255,0.16);
    --success:#4ade80;--text:#f5f5f5;--text-muted:rgba(245,245,245,0.6);--text-faint:rgba(245,245,245,0.38);
    --border-strong:rgba(255,255,255,0.16);--shadow-color:rgba(0,0,0,0.4);
    """

    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView(frame: .zero)
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.isScrollEnabled = false
        webView.scrollView.bounces = false
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        guard let url = Bundle.main.url(forResource: "referral_v3_animated", withExtension: "svg"),
              let svg = try? String(contentsOf: url, encoding: .utf8) else {
            return
        }
        let vars = dark ? Self.varsDark : Self.varsLight
        let html = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">\
        <style>:root{\(vars)}html,body{margin:0;padding:0;width:100%;overflow:hidden;background:transparent}\
        svg{display:block;width:100%;height:auto}</style></head><body>\(svg)</body></html>
        """
        webView.loadHTMLString(html, baseURL: nil)
    }
}

/// Нативная заглушка hero, если SVG недоступен в бандле.
struct ReferralHeroFallback: View {
    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: "gift.fill")
                .font(.system(size: 40, weight: .semibold))
                .foregroundColor(DSColor.accent)
            Text("Друг впервые оплачивает год Про")
                .font(DSFont.sans(15, .semibold))
            Text("Каждому добавится половина оплаченного периода — примерно 6 месяцев")
                .dsTextStyle(.captionMuted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DSColor.accentSoft)
    }
}
