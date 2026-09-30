import SwiftUI

struct PurchasesView: View {
    @StateObject private var profile = ProfileViewModelWrapper()
    @State private var referralCode = ""
    private let products = [
        ("1 месяц", "Полный доступ на 1 месяц"),
        ("3 месяца", "Полный доступ на 3 месяца"),
        ("1 год", "Полный доступ на 1 год"),
    ]

    var body: some View {
        List {
            if profile.canApplyReferralCode {
                Section {
                    Label("Есть код друга?", systemImage: "gift.fill")
                        .font(.headline)
                        .foregroundStyle(DSColor.accent)
                    Text("Введите его до первой оплаты. После оплаты каждый получит половину срока тарифа дополнительно.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    TextField("Код друга", text: $referralCode)
                        .font(.system(.body, design: .monospaced))
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                        .onChange(of: referralCode) { value in
                            referralCode = String(value.uppercased().filter { $0.isLetter || $0.isNumber }.prefix(16))
                        }
                    Button { profile.applyReferralCode(referralCode) } label: {
                        Text("Применить код").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                        .disabled(referralCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    if let error = profile.referralError {
                        Label(error, systemImage: "exclamationmark.triangle.fill")
                            .font(.footnote).foregroundStyle(DSColor.danger)
                    }
                } header: { Text("Реферальный код") }
            } else if let applied = profile.appliedReferralCode {
                Section("Реферальный код") {
                    if profile.appliedReferralStatus == "reversed" {
                        Label("Бонус по коду \(applied) отменён после возврата платежа", systemImage: "xmark.circle.fill").foregroundStyle(DSColor.danger)
                    } else if profile.appliedReferralStatus == "rewarded" {
                        Label("Бонус по коду \(applied) начислен", systemImage: "checkmark.circle.fill").foregroundStyle(DSColor.success)
                    } else {
                        Label("Код \(applied) применён", systemImage: "clock.fill").foregroundStyle(DSColor.accent)
                        Text("Бонус начислится после первой оплаты.").font(.footnote).foregroundStyle(.secondary)
                    }
                }
            }
            ForEach(products, id: \.0) { (title, desc) in
                Section {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(title)
                            .font(.headline)
                        Text(desc)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Button("Купить") {
                            // TODO: initPayment
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding(.vertical, 4)
                }
            }

            Section {
                Button("Восстановить покупки") {
                    // TODO: restore
                }
                .frame(maxWidth: .infinity)
            }
        }
        .navigationTitle("Подписка")
        .onAppear { profile.loadReferrals() }
    }
}
