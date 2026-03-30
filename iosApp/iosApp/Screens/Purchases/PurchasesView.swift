import SwiftUI

struct PurchasesView: View {
    private let products = [
        ("1 месяц", "Полный доступ на 1 месяц"),
        ("3 месяца", "Полный доступ на 3 месяца"),
        ("1 год", "Полный доступ на 1 год"),
    ]

    var body: some View {
        List {
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
    }
}
