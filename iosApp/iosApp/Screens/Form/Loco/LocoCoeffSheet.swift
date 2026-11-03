import SwiftUI
import ComposeApp

/// Шторка коэффициента (`CoeffSheet`, SCREEN_SPECS §1.3 п.4): степпер, поле, «ИЗ ИСТОРИИ»,
/// для экипировки — чекбокс «Применить ко всем значениям этой секции».
struct LocoCoeffSheet: View {
    let title: String
    let value: String
    let recent: [String]
    var applyLabel: String? = nil
    @Binding var applyToSection: Bool
    let step: (String, Int) -> String
    let onChange: (String) -> Void
    let onApplyToggled: (Bool) -> Void
    let onDone: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text(title).dsTextStyle(.sectionH2)
                Spacer()
                Button("Готово", action: onDone)
                    .font(DSTextStyle.navActionAccent.font)
                    .foregroundColor(DSColor.accent)
            }
            HStack(spacing: 24) {
                stepButton("−") { onChange(step(value, -1)) }
                TextField("1.0", text: Binding(get: { value }, set: { onChange($0) }))
                    .keyboardType(.decimalPad)
                    .multilineTextAlignment(.center)
                    .font(DSFont.mono(28, .bold))
                    .foregroundColor(DSColor.text)
                    .frame(minWidth: 100)
                stepButton("+") { onChange(step(value, 1)) }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(DSColor.bgSubtle)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.md, style: .continuous))

            if let applyLabel = applyLabel {
                Button {
                    applyToSection.toggle()
                    onApplyToggled(applyToSection)
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: applyToSection ? "checkmark.square.fill" : "square")
                            .font(.system(size: 20))
                            .foregroundColor(applyToSection ? DSColor.accent : DSColor.textFaint)
                        Text(applyLabel).font(DSFont.sans(14)).foregroundColor(DSColor.text)
                        Spacer()
                    }
                }
                .buttonStyle(.plain)
            }

            if !recent.isEmpty {
                Text("ИЗ ИСТОРИИ").dsTextStyle(.group)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(recent, id: \.self) { item in
                            DSChip(title: item, style: .choice(isSelected: item == value)) { onChange(item) }
                        }
                    }
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .padding(.top, 20)
        .background(DSColor.bg.ignoresSafeArea())
    }

    private func stepButton(_ symbol: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(symbol)
                .font(DSFont.sans(26, .semibold))
                .foregroundColor(DSColor.text)
                .frame(width: 48, height: 48)
                .background(DSColor.surface)
                .clipShape(Circle())
                .dsShadow(.sm)
        }
        .buttonStyle(.plain)
    }
}
