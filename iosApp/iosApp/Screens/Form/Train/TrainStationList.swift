import SwiftUI
import ComposeApp

// Список станций следования (§6.2 п.5): строки станций, карточки перегонов, пилюли сводки,
// секундомер. Макет: design/src/ios-add-train.jsx → RouteStationRow.

struct TrainStationListView: View {
    let state: TrainScreenState
    let onStation: (Int) -> Void
    let onSegment: (Int) -> Void
    let onMove: (Int, Int) -> Void
    let onDelete: (Int) -> Void

    private var rows: [TrainRowUi] { (state.rows as? [TrainRowUi]) ?? [] }
    private var stations: [TrainStationUi] { (state.stations as? [TrainStationUi]) ?? [] }
    private var offset: Int { 3 * 3600 } // BUG-22 (паритет с Android): список всегда в GMT+3, шторка — в поясе пользователя.

    var body: some View {
        DSCard {
            columnHeader
            DSDivider(inset: 0)
            VStack(spacing: 0) {
                ForEach(Array(rows.enumerated()), id: \.element.key) { pos, row in
                    if row.isSegment {
                        if state.showSegments || row.travelText != nil {
                            segmentView(row, position: pos)
                        }
                    } else if stations.indices.contains(Int(row.stationIndex)) {
                        stationRow(stations[Int(row.stationIndex)], index: Int(row.stationIndex), position: pos)
                    }
                }
            }
            .padding(.vertical, 6)
        }
    }

    // MARK: Шапка колонок

    private var columnHeader: some View {
        HStack(spacing: 8) {
            Text("СТАНЦИЯ").frame(maxWidth: .infinity, alignment: .leading)
            Text("ПРИБ").frame(width: 48, alignment: .trailing)
            Spacer().frame(width: 40)
            Text("ОТПР").frame(width: 48, alignment: .trailing)
        }
        .dsTextStyle(.group)
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 8)
    }

    // MARK: Станция

    private func timeline(position: Int, isDot: Bool, filled: Bool) -> some View {
        let isTop = position == 0
        let isBottom = position == rows.count - 1
        return VStack(spacing: 0) {
            Rectangle().fill(isTop ? Color.clear : DSColor.borderStrong).frame(width: 2)
            Rectangle().fill(isBottom ? Color.clear : DSColor.borderStrong).frame(width: 2)
        }
        .overlay {
            if isDot {
                Circle()
                    .fill(filled ? DSColor.accent : DSColor.surface)
                    .frame(width: 11, height: 11)
                    .overlay(Circle().stroke(DSColor.accent, lineWidth: 2))
            }
        }
        .frame(width: 14)
    }

    private func stationRow(_ s: TrainStationUi, index: Int, position: Int) -> some View {
        let isEnd = index == 0 || index == stations.count - 1
        return Button { onStation(index) } label: {
            HStack(spacing: 10) {
                timeline(position: position, isDot: true, filled: isEnd)
                VStack(alignment: .leading, spacing: 2) {
                    Text(s.name.isEmpty ? "—" : s.name)
                        .font(DSFont.sans(16))
                        .foregroundColor(s.isPassing ? DSColor.textMuted : DSColor.text)
                        .lineLimit(2)
                    if let sub = subtitle(s) {
                        Text(sub).font(DSFont.sans(12)).foregroundColor(DSColor.textMuted)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if s.isPassing {
                    HStack(spacing: 6) {
                        Text("просл.").font(DSFont.sans(12)).foregroundColor(DSColor.textFaint)
                        Text(TrainTime.hm(s.arrival?.int64Value, offset: offset)).font(DSFont.mono(15, .medium)).foregroundColor(DSColor.text)
                    }
                    .frame(width: 136, alignment: .trailing)
                } else {
                    Text(TrainTime.hm(s.arrival?.int64Value, offset: offset))
                        .font(DSFont.mono(15, .medium)).foregroundColor(DSColor.text)
                        .frame(width: 48, alignment: .trailing)
                    dwell(s).frame(width: 40)
                    Text(TrainTime.hm(s.departure?.int64Value, offset: offset))
                        .font(DSFont.mono(15, .medium)).foregroundColor(DSColor.text)
                        .frame(width: 48, alignment: .trailing)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .fixedSize(horizontal: false, vertical: true)
            .contentShape(Rectangle())
        }
        .buttonStyle(DSRowPressStyle())
        // Вместо перетаскивания и свайпа (в ScrollView их нет без List): меню по долгому нажатию.
        .contextMenu {
            if position > 0 || stations.count > 1 {
                Button { moveUp(index) } label: { Label("Выше", systemImage: "arrow.up") }
                Button { moveDown(index) } label: { Label("Ниже", systemImage: "arrow.down") }
            }
            Button(role: .destructive) { onDelete(index) } label: { Label("Удалить станцию", systemImage: "trash") }
        }
    }

    private func moveUp(_ index: Int) { onMove(index, state.isReversed ? index + 1 : index - 1) }
    private func moveDown(_ index: Int) { onMove(index, state.isReversed ? index - 1 : index + 1) }

    private func subtitle(_ s: TrainStationUi) -> String? {
        let hasTrack = !s.trackNumber.isEmpty
        switch (s.isPassing, hasTrack) {
        case (true, true): return "проходная · путь \(s.trackNumber)"
        case (true, false): return "проходная"
        case (false, true): return "путь \(s.trackNumber)"
        default: return nil
        }
    }

    @ViewBuilder
    private func dwell(_ s: TrainStationUi) -> some View {
        if let text = s.dwellText {
            let level = Int(s.dwellLevel)
            Text(text)
                .font(DSFont.mono(11, .bold))
                .foregroundColor(level == 0 ? DSColor.textMuted : (level == 2 ? Color.white : DSColor.ink))
                .padding(.horizontal, 5).padding(.vertical, 2)
                .background(level == 2 ? DSColor.danger : (level == 1 ? DSColor.warning : Color.clear))
                .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
        } else {
            Color.clear
        }
    }

    // MARK: Перегон

    @ViewBuilder
    private func segmentView(_ row: TrainRowUi, position: Int) -> some View {
        let after = Int(row.stationIndex)
        HStack(spacing: 10) {
            timeline(position: position, isDot: false, filled: false)
            Group {
                if state.showSegments {
                    segmentCard(row)
                } else {
                    // Перегоны скрыты: время в пути остаётся компактной пилюлей на линии таймлайна.
                    HStack {
                        Text(row.travelText ?? "").font(DSFont.mono(12, .medium)).foregroundColor(DSColor.textMuted)
                            .padding(.horizontal, 10).padding(.vertical, 3)
                            .background(DSColor.bgSubtle).clipShape(Capsule())
                        Spacer()
                    }
                }
            }
            .onTapGesture { onSegment(after) }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .fixedSize(horizontal: false, vertical: true)
    }

    private func segmentCard(_ row: TrainRowUi) -> some View {
        let hasTrack = !row.segmentTrack.isEmpty
        let hasNotes = !row.segmentNotes.isEmpty
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)
        return VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 8) {
                (row.isFuture ? DSIcon.locomotive : DSIcon.sort).image
                    .font(.system(size: 12, weight: .semibold)).foregroundColor(DSColor.textMuted)
                    .opacity(row.isFuture ? 1 : 0)
                    .frame(width: row.isFuture ? 14 : 0)
                Text("ПЕРЕГОН").dsTextStyle(.group)
                Spacer()
                if let t = row.travelText {
                    Text(t).font(DSFont.mono(13, .medium)).foregroundColor(DSColor.text)
                }
                if !row.isFuture {
                    Image(systemName: "pencil").font(.system(size: 11)).foregroundColor(DSColor.textFaint)
                    DSIcon.chevronRight.image.font(.system(size: 11, weight: .semibold)).foregroundColor(DSColor.textFaint)
                }
            }
            if hasTrack {
                Text("\(row.segmentTrack) путь")
                    .font(DSFont.mono(12, .semibold)).foregroundColor(DSColor.accent)
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(DSColor.accentSoft).clipShape(Capsule())
            }
            if hasNotes {
                Text(row.segmentNotes).font(DSFont.sans(14)).foregroundColor(DSColor.text)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.horizontal, 12).padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(row.isFuture ? Color.clear : DSColor.bg)
        .clipShape(shape)
        .overlay(
            shape.strokeBorder(DSColor.borderStrong, style: StrokeStyle(lineWidth: 1, dash: row.isFuture ? [5, 4] : []))
                .opacity(row.isFuture ? 1 : 0)
        )
    }
}

// MARK: - Сводка (пилюли)

struct TrainSummaryPills: View {
    let state: TrainScreenState

    var body: some View {
        if let total = state.summaryTotalTime {
            HStack(spacing: 8) {
                pill("Время в пути:", total, mono: false)
                if let v = state.summarySectionSpeed { pill("Уч", "\(v) км/ч", mono: true) }
                if let v = state.summaryTechSpeed { pill("Техн", "\(v) км/ч", mono: true) }
                Spacer(minLength: 0)
            }
            .padding(.top, 12)
        }
    }

    private func pill(_ label: String, _ value: String, mono: Bool) -> some View {
        HStack(spacing: 4) {
            Text(label).foregroundColor(DSColor.textMuted)
            Text(value).foregroundColor(DSColor.text)
        }
        .font(mono ? DSFont.mono(12, .semibold) : DSFont.sans(12, .semibold))
        .padding(.horizontal, 10).padding(.vertical, 5)
        .background(DSColor.bgSubtle)
        .clipShape(Capsule())
    }
}

// MARK: - Секундомер и GO

struct TrainStopwatchChip: View {
    @ObservedObject var vm: TrainScreenViewModelWrapper

    var body: some View {
        TimelineView(.periodic(from: Date(), by: 1)) { context in
            let sw = vm.stopwatch(now: context.date)
            if sw.kind == 0 {
                Color.clear.frame(height: 40)
            } else {
                let level = Int(sw.level)
                VStack(spacing: 2) {
                    Text(sw.text)
                        .font(DSFont.mono(15, .bold))
                        .foregroundColor(level == 0 ? DSColor.text : (level == 2 ? Color.white : DSColor.ink))
                        .padding(.horizontal, 14).frame(height: 40)
                        .background(level == 2 ? DSColor.danger : (level == 1 ? DSColor.warning : DSColor.bgSubtle))
                        .clipShape(Capsule())
                    Text(sw.kind == 1 ? "стоянка" : "в пути").font(DSFont.sans(11)).foregroundColor(DSColor.textMuted)
                }
            }
        }
    }
}

struct TrainGoButton: View {
    let isDeparture: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: isDeparture ? "play.fill" : "pause.fill").font(.system(size: 13, weight: .bold))
                Text("GO").font(DSFont.sans(15, .bold))
            }
            .foregroundColor(DSColor.accent)
            .padding(.horizontal, 16).frame(height: 40)
            .overlay(Capsule().stroke(DSColor.accent, lineWidth: 1.5))
        }
        .buttonStyle(DSPressableStyle())
        .accessibilityLabel(Text(isDeparture ? "Записать отправление" : "Записать прибытие"))
    }
}
