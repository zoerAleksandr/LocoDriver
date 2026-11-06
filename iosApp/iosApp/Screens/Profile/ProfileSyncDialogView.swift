import SwiftUI
import ComposeApp

// SCREEN_SPECS §18.5: `SyncProgressDialog` для единой двусторонней синхронизации.
// Заголовок «Синхронизация данных», текущий этап, один линейный progress bar и
// процент (шаг 25%); под процентом — первая ошибка этапа. Диалог не запускает
// sync сам — только отображает состояние VM. Тап вне — resetSyncState (§18.5).

struct SyncProgressDialogView: View {
    let dialog: ProfileIosSyncDialog
    let onCancel: () -> Void
    let onDismiss: () -> Void

    private var steps: [ProfileIosSyncStep] {
        dialog.steps
    }

    private var reportText: String { dialog.reportBody }
    private var reportSubject: String { dialog.reportSubject }

    var body: some View {
        ZStack {
            Color.black.opacity(0.45)
                .ignoresSafeArea()
                .onTapGesture { onDismiss() }
            VStack(spacing: 14) {
                Text("Синхронизация данных")
                    .font(DSFont.sans(18, .semibold))
                Text(dialog.currentLabel)
                    .dsTextStyle(.captionMuted)

                ProgressView(value: Double(dialog.percent), total: 100)
                    .tint(DSColor.accent)
                Text("\(Int(dialog.percent))%")
                    .font(DSFont.mono(14, .semibold))
                    .foregroundColor(DSColor.text)

                if let error = dialog.firstError {
                    Text(error)
                        .font(DSFont.sans(12))
                        .foregroundColor(DSColor.danger)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                stepRows

                HStack(spacing: 10) {
                    if dialog.isComplete {
                        if dialog.showReportButton {
                            ShareLink(item: reportText) {
                                Text("Отправить отчет об ошибке")
                                    .font(DSFont.sans(14, .medium))
                                    .foregroundColor(DSColor.accent)
                                    .frame(maxWidth: .infinity, minHeight: 46)
                                    .background(DSColor.bgSubtle, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            }
                        }
                        Button("Понятно") { onDismiss() }
                            .font(DSFont.sans(15, .semibold))
                            .foregroundColor(DSColor.ctaInk)
                            .frame(maxWidth: .infinity, minHeight: 46)
                            .background(DSColor.cta, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    } else {
                        // «Отмена» доступна сразу и прерывает синхронизацию (§18.S).
                        Button("Отмена", action: onCancel)
                            .font(DSFont.sans(15, .medium))
                            .foregroundColor(DSColor.textMuted)
                            .frame(maxWidth: .infinity, minHeight: 46)
                    }
                }
            }
            .padding(20)
            .background(DSColor.bg, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .padding(.horizontal, 28)
        }
    }

    /// Построчный список этапов (как в легаси-виде) — виден при ошибках.
    @ViewBuilder
    private var stepRows: some View {
        if steps.contains(where: { $0.status != "success" }) || dialog.isComplete {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(steps, id: \.key) { step in
                    HStack(alignment: .top, spacing: 8) {
                        statusIcon(step.status)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(step.label)
                                .font(DSFont.sans(13, .medium))
                                .foregroundColor(DSColor.text)
                            if !step.details.isEmpty {
                                Text(step.details)
                                    .font(DSFont.sans(12))
                                    .foregroundColor(step.status == "error" ? DSColor.danger : DSColor.textMuted)
                            }
                        }
                        Spacer(minLength: 0)
                    }
                }
            }
            .padding(.top, 4)
        }
    }

    @ViewBuilder
    private func statusIcon(_ status: String) -> some View {
        switch status {
        case "success":
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(DSColor.success)
                .font(.system(size: 15))
        case "error":
            Image(systemName: "exclamationmark.circle.fill")
                .foregroundColor(DSColor.danger)
                .font(.system(size: 15))
        default:
            ProgressView().controlSize(.small)
        }
    }
}

/// Итоговые виды диалога синхронизации: «Нет интернета» и «Сессия истекла» (§18.5).
struct SyncDialogResultView: View {
    let icon: String
    let title: String
    var message: String? = nil
    let buttonTitle: String
    let onButton: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.45)
                .ignoresSafeArea()
                .onTapGesture { onButton() }
            VStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 34, weight: .medium))
                    .foregroundColor(DSColor.textMuted)
                Text(title)
                    .font(DSFont.sans(18, .semibold))
                if let message = message {
                    Text(message)
                        .dsTextStyle(.captionMuted)
                        .multilineTextAlignment(.center)
                }
                Button(buttonTitle, action: onButton)
                    .font(DSFont.sans(15, .semibold))
                    .foregroundColor(DSColor.ctaInk)
                    .frame(maxWidth: .infinity, minHeight: 46)
                    .background(DSColor.cta, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .padding(.top, 6)
            }
            .padding(22)
            .background(DSColor.bg, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .padding(.horizontal, 28)
        }
    }
}
