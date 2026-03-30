import SwiftUI

struct SearchView: View {
    @State private var query = ""

    var body: some View {
        List {
            if query.isEmpty {
                Section("История поиска") {
                    Text("Нет истории поиска")
                        .foregroundColor(.secondary)
                }
            } else {
                Section("Результаты") {
                    Text("Поиск: «\(query)»...")
                        .foregroundColor(.secondary)
                }
            }
        }
        .searchable(text: $query, prompt: "Поиск маршрутов")
        .navigationTitle("Поиск")
    }
}
