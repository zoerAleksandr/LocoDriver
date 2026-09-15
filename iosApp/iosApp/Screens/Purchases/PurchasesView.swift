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
                Section("Реферальный код") {
                    Text("Введите код друга до первой оплаты. После оплаты каждый получит половину срока тарифа дополнительно.")
                    TextField("Код друга", text: $referralCode)
                        .textInputAutocapitalization(.characters)
                        .autocorrectionDisabled()
                    Button("Применить код") { profile.applyReferralCode(referralCode) }
                        .disabled(referralCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                    if let error = profile.referralError { Text(error).foregroundColor(.red) }
                }
            } else if let applied = profile.appliedReferralCode {
                Section("Реферальный код") {
                    if profile.appliedReferralStatus == "reversed" {
                        Text("Бонус по коду \(applied) отменён после возврата платежа.")
                    } else if profile.appliedReferralStatus == "rewarded" {
                        Text("Бонус по коду \(applied) начислен.")
                    } else {
                        Text("Код \(applied) применён. Бонус начислится после первой оплаты.")
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
