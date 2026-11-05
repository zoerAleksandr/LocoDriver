import SwiftUI
import ComposeApp

// Экран «Пассажиром» — SCREEN_SPECS §7.
// Макет: design/src/pass-screen.jsx → IOSScreenPass. Логика — Kotlin `PassengerScreenIosViewModel`.
// По спеке (макет противоречит): «Готово» слева, меню «⋮» нет, автосейв без «Сохранить».
// BUG-29 (паритет с Android): при выходе пассажир сохраняется даже с ошибкой валидации.

struct FormPassengerView: View {
    let routeId: String
    let passengerId: String?

    @StateObject private var vm = PassengerScreenViewModelWrapper()
    @State private var started = false
    @State private var picker: PassTimeTarget?
    @State private var clear: PassTimeTarget?
    @Environment(\.scenePhase) private var scenePhase

    private var offsetSeconds: Int { Int(vm.state?.displayOffsetSeconds ?? 10800) }
    private var format: RouteTimeFormat { RouteTimeFormat(offsetSeconds: offsetSeconds) }

    var body: some View {
        VStack(spacing: 0) {
            PassNavBar(title: "Пассажиром") { AppRouter.shared.back() }
            if let s = vm.state, !s.isLoading {
                content(s)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(DSColor.bg.ignoresSafeArea())
        .dsHideSystemNavBar()
        .navigationBarBackButtonHidden(true)
        .onAppear {
            if !started {
                started = true
                vm.start(basicId: routeId, passengerId: passengerId)
            }
        }
        .onChange(of: scenePhase) { phase in
            if phase != .active { vm.flush() }
        }
        .modifier(PassPickerHost(
            picker: $picker,
            clear: $clear,
            offsetSeconds: offsetSeconds,
            title: { $0 == .first ? "Отправление" : "Прибытие" },
            deleteTitle: { $0 == .first ? "Время отправления" : "Время прибытия" },
            current: { $0 == .first ? vm.state?.timeDeparture.ms : vm.state?.timeArrival.ms },
            onPick: { target, ms in
                if target == .first { vm.setTimeDeparture(ms) } else { vm.setTimeArrival(ms) }
            },
            onClear: { target in
                if target == .first { vm.setTimeDeparture(nil) } else { vm.setTimeArrival(nil) }
            }
        ))
    }

    // MARK: Содержимое

    private func content(_ s: PassengerScreenState) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if let error = s.errorMessage {
                    DSWarningBlock(title: error).padding(.top, 8)
                }
                if let text = warningText(s) {
                    DSWarningBlock(title: text).padding(.top, 8)
                }

                DSGroupHeader("ПОЕЗД", topSpacing: DSMetrics.groupHeadFirst)
                trainCard(s)

                DSGroupHeader("ОТКУДА")
                legCard(
                    placeholder: "От станции",
                    station: s.stationDeparture,
                    timeLabel: "Отправился",
                    time: s.timeDeparture.ms,
                    target: .first,
                    onStation: { vm.setStationDeparture($0) }
                )

                if let result = s.resultTime {
                    PassDurationBadge(text: "В пути · \(passDurationText(result.int64Value))")
                        .padding(.top, 10)
                }

                DSGroupHeader("КУДА")
                legCard(
                    placeholder: "До станции",
                    station: s.stationArrival,
                    timeLabel: "Прибыл",
                    time: s.timeArrival.ms,
                    target: .second,
                    onStation: { vm.setStationArrival($0) }
                )

                DSGroupHeader("УЧЁТ РАБОЧЕГО ВРЕМЕНИ")
                workStartCard(s)

                DSGroupHeader("ПРИМЕЧАНИЯ")
                DSCard {
                    PassNotesField(placeholder: "Например: номер приказа", external: s.notes) { vm.setNotes($0) }
                }
                Spacer(minLength: 40)
            }
            .padding(.horizontal, DSMetrics.screenPadX)
        }
        .passScrollDismissesKeyboard()
    }

    private func trainCard(_ s: PassengerScreenState) -> some View {
        DSCard {
            HStack(spacing: 12) {
                DSIconAvatar(icon: .train, size: .md, tone: .neutral, shape: .circle)
                Text("№").font(DSFont.mono(17, .semibold)).foregroundColor(DSColor.textMuted)
                PassTrainField(external: s.trainNumber) { vm.setTrainNumber($0) }
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 12)
        }
    }

    private func legCard(
        placeholder: String, station: String, timeLabel: String, time: Int64?,
        target: PassTimeTarget, onStation: @escaping (String) -> Void
    ) -> some View {
        DSCard {
            PassStationField(
                placeholder: placeholder,
                external: station,
                suggestions: { vm.stationSuggestions($0) },
                onChange: onStation,
                onRemoveSuggestion: { vm.removeStationName($0) }
            )
            DSDivider()
            PassDateTimeRow(
                label: timeLabel,
                valueText: time.map { format.dateTime($0) },
                onTap: { hideKeyboard(); picker = target },
                onLongPress: { clear = target }
            )
        }
    }

    // MARK: Предупреждение «не входит в оплату» (§7.3 п.3)

    private func warningText(_ s: PassengerScreenState) -> String? {
        guard let kind = s.outsideWarning else { return nil }
        // Время, затем дата: «HH:mm dd.MM.yy».
        func stamp(_ ms: Int64?) -> String {
            guard let ms = ms else { return "" }
            return "\(format.time(ms)) \(format.date(ms))"
        }
        switch kind {
        case "depBeforeStart": return "Отправление пассажиром раньше явки в \(stamp(s.routeWorkStart.ms)) — не входит в оплату."
        case "arrAfterEnd": return "Прибытие пассажиром позже сдачи в \(stamp(s.routeWorkEnd.ms)) — не входит в оплату."
        case "depAfterEnd": return "Отправление пассажиром позже сдачи в \(stamp(s.routeWorkEnd.ms)) — не входит в оплату."
        case "arrBeforeStart": return "Прибытие пассажиром раньше явки в \(stamp(s.routeWorkStart.ms)) — не входит в оплату."
        default: return nil
        }
    }

    // MARK: Явка по прибытию (§7.2, §7.3 п.7)

    private func workStartCard(_ s: PassengerScreenState) -> some View {
        let canEnable = s.timeArrival != nil
        return DSCard {
            HStack(spacing: 14) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Явка по прибытию").font(DSFont.sans(16, .semibold)).foregroundColor(DSColor.text)
                    Text(canEnable
                         ? "Явка на работу по прибытию пассажиром."
                         : "Укажите время прибытия («Куда»), чтобы включить.")
                        .font(DSFont.sans(13)).foregroundColor(DSColor.textMuted)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Spacer(minLength: 8)
                Toggle("", isOn: Binding(
                    get: { s.isWorkStartByArrival },
                    set: { vm.setWorkStartByArrival($0) }
                ))
                .labelsHidden()
                .tint(DSColor.accent)
                .disabled(!canEnable)
            }
            .padding(.horizontal, DSMetrics.rowPadX)
            .padding(.vertical, 14)

            if s.isWorkStartByArrival && canEnable {
                DSDivider()
                HStack(spacing: 12) {
                    DSIconAvatar(icon: .clock, size: .md, tone: .accent, shape: .circle)
                    VStack(alignment: .leading, spacing: 3) {
                        Text("НАЧАЛО РАБОТЫ").font(DSFont.mono(11, .bold)).tracking(1.2).foregroundColor(DSColor.accent)
                        workStartValue(s)
                    }
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, DSMetrics.rowPadX)
                .padding(.vertical, 13)
                DSDivider()
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Явка через").font(DSFont.sans(16, .semibold)).foregroundColor(DSColor.text)
                        Text("Время между прибытием и явкой.").font(DSFont.sans(13)).foregroundColor(DSColor.textMuted)
                    }
                    Spacer(minLength: 8)
                    PassOffsetField(minutes: Int(s.workStartOffsetMinutes)) { vm.setWorkStartOffsetMinutes($0) }
                }
                .padding(.horizontal, DSMetrics.rowPadX)
                .padding(.vertical, 10)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: s.isWorkStartByArrival)
    }

    /// «{станция прибытия} · {дата время}»; станция — Inter, время — Mono; без станции — только время.
    @ViewBuilder
    private func workStartValue(_ s: PassengerScreenState) -> some View {
        if let ms = s.workStartTime.ms {
            let time = format.dateTime(ms)
            if s.stationArrival.isEmpty {
                Text(time).font(DSFont.mono(16, .semibold)).foregroundColor(DSColor.text).lineLimit(1)
            } else {
                (Text(s.stationArrival + " · ").font(DSFont.sans(16, .semibold))
                 + Text(time).font(DSFont.mono(16, .semibold)))
                    .foregroundColor(DSColor.text).lineLimit(1)
            }
        } else {
            Text("—").font(DSFont.sans(16, .semibold)).foregroundColor(DSColor.text)
        }
    }
}

// MARK: - Поля

/// Номер поезда: десятичная клавиатура, пустое → nil (на стороне Kotlin).
private struct PassTrainField: View {
    let external: String
    let onChange: (String) -> Void
    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        TextField("поезда", text: $text)
            .keyboardType(.decimalPad)
            .focused($focused)
            .font(DSFont.mono(17, .semibold))
            .foregroundColor(DSColor.text)
            .onAppear { text = external }
            .onChange(of: text) { value in if value != external { onChange(value) } }
            .onChange(of: external) { value in if !focused { text = value } }
    }
}

/// «Явка через» в формате ЧЧ:ММ: цифры вводятся справа налево, как на калькуляторе (0:00–23:59).
private struct PassOffsetField: View {
    let minutes: Int
    let onChange: (Int) -> Void
    @State private var text = ""

    private static func format(_ minutes: Int) -> String {
        String(format: "%02d:%02d", minutes / 60, minutes % 60)
    }

    private static func minutes(from digits: String) -> Int {
        let value = Int(digits) ?? 0
        let m = (value / 100) * 60 + min(value % 100, 59)
        return min(max(m, 0), 23 * 60 + 59)
    }

    var body: some View {
        TextField("00:00", text: $text)
            .keyboardType(.numberPad)
            .multilineTextAlignment(.center)
            .font(DSFont.mono(17, .semibold))
            .foregroundColor(DSColor.text)
            .frame(width: 96, height: 40)
            .background(DSColor.bgSubtle)
            .clipShape(RoundedRectangle(cornerRadius: DSRadius.input, style: .continuous))
            .onAppear { text = Self.format(minutes) }
            .onChange(of: text) { value in
                let digits = String(value.filter { $0.isNumber }.suffix(4))
                let m = Self.minutes(from: digits)
                let formatted = Self.format(m)
                if formatted != value { text = formatted }
                if m != minutes { onChange(m) }
            }
            .onChange(of: minutes) { value in
                let formatted = Self.format(value)
                if formatted != text { text = formatted }
            }
    }
}
