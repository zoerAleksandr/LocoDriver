import UIKit
import ComposeApp

// Рендер PDF по модели вёрстки Kotlin (SCREEN_SPECS §21).
//
// Android рисует Canvas/PdfDocument (features/route/.../util/PdfGenerator.kt) —
// на iOS недоступно. Здесь: UIGraphicsPDFRenderer, A4 595×842 (§21 «Страница»),
// поля слева/справа 36. Содержимое (тексты, числа, порядок блоков, оценки высот
// и политика разрывов) задаёт `PdfLayoutDocument` из Kotlin; рендерер только
// расставляет пиксели и нарезает страницы по тем же правилам («рендер нативным
// UIKit, вёрстка по модели §21»).

enum PdfRenderer {

    /// Полный PDF-документ.
    static func pdfData(document: PdfLayoutDocument) -> Data? {
        let m = document.metrics
        let bounds = CGRect(x: 0, y: 0, width: CGFloat(m.pageWidth), height: CGFloat(m.pageHeight))
        let renderer = UIGraphicsPDFRenderer(bounds: bounds)
        return renderer.pdfData { ctx in
            let state = RenderState(context: ctx, metrics: m)
            state.draw(blocks: document.blocks)
        }
    }

    /// Запись в кэш приложения: `Машинист_{Месяц}_{Год}.pdf`; старые `Машинист_*.pdf`
    /// удаляются перед записью (§21 «Файл»).
    static func writeToFile(document: PdfLayoutDocument) -> URL? {
        guard let data = pdfData(document: document) else { return nil }
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
        if let existing = try? FileManager.default.contentsOfDirectory(at: caches, includingPropertiesForKeys: nil) {
            for file in existing
            where file.lastPathComponent.hasPrefix("Машинист_") && file.pathExtension == "pdf" {
                try? FileManager.default.removeItem(at: file)
            }
        }
        let url = caches.appendingPathComponent(document.fileName)
        do {
            try data.write(to: url)
            return url
        } catch {
            return nil
        }
    }
}

// MARK: - Внутренний рендерер

private final class RenderState {
    private let ctx: UIGraphicsPDFRendererContext
    private let m: PdfMetrics
    private var y: CGFloat = 0
    private var pageStarted = false

    init(context: UIGraphicsPDFRendererContext, metrics: PdfMetrics) {
        self.ctx = context
        self.m = metrics
        y = CGFloat(metrics.marginTop)
    }

    private var pageWidth: CGFloat { CGFloat(m.pageWidth) }
    private var pageHeight: CGFloat { CGFloat(m.pageHeight) }
    private var marginLeft: CGFloat { CGFloat(m.marginSide) }
    private var contentWidth: CGFloat { CGFloat(m.contentWidth) }
    private var marginBottom: CGFloat { CGFloat(m.marginBottom) }

    // ── Шрифты (§21: заголовок 13 жирный, подзаголовок 10 жирный, текст 9, мелкий 7–7.5) ──

    private func font(_ style: PdfTextStyle) -> UIFont {
        switch style {
        case .title: return .systemFont(ofSize: CGFloat(m.titleFontSize), weight: .bold)
        case .section: return .systemFont(ofSize: CGFloat(m.sectionFontSize), weight: .bold)
        case .body: return .systemFont(ofSize: CGFloat(m.bodyFontSize))
        case .bodyBold: return .systemFont(ofSize: CGFloat(m.bodyFontSize), weight: .bold)
        case .small: return .systemFont(ofSize: CGFloat(m.smallFontSize))
        case .tiny: return .systemFont(ofSize: CGFloat(m.tinyFontSize))
        default: return .systemFont(ofSize: CGFloat(m.bodyFontSize))
        }
    }

    private func font(_ style: PdfTextStyle?) -> UIFont {
        font(style ?? .body)
    }

    private static let textColor = UIColor.black
    private static let mutedColor = UIColor(white: 0.33, alpha: 1) // DKGRAY
    private static let lineColor = UIColor(white: 0.75, alpha: 1)  // LTGRAY
    private static let borderColor = UIColor(white: 0.55, alpha: 1) // GRAY
    private static let sectionFill = UIColor(white: 0, alpha: 25.0 / 255.0)

    // ── Страницы ──

    private func needNewPage(h: CGFloat) -> Bool {
        y + h > pageHeight - marginBottom
    }

    private func beginPage(drawAppHeader: Bool) {
        ctx.beginPage()
        pageStarted = true
        y = CGFloat(m.marginTop)
        if drawAppHeader {
            // Колонтитул §21: мелкий курсив + линия (на страницах Поездков и Графика).
            let header = "Файл сформирован в приложении «Машинист»"
            let italic = UIFont.systemFont(ofSize: CGFloat(m.appHeaderFontSize)).withTraits(.traitItalic)
            header.draw(
                at: CGPoint(x: marginLeft, y: 15),
                withAttributes: [.font: italic, .foregroundColor: RenderState.mutedColor]
            )
            let line = UIBezierPath()
            line.move(to: CGPoint(x: marginLeft, y: 26))
            line.addLine(to: CGPoint(x: pageWidth - marginLeft, y: 26))
            RenderState.lineColor.setStroke()
            line.lineWidth = 0.5
            line.stroke()
        }
    }

    private func ensurePage(drawAppHeader: Bool) {
        if !pageStarted {
            beginPage(drawAppHeader: drawAppHeader)
        }
    }

    private func checkNewPage(h: CGFloat, drawAppHeader: Bool = true) {
        ensurePage(drawAppHeader: drawAppHeader)
        if needNewPage(h: h) {
            beginPage(drawAppHeader: drawAppHeader)
        }
    }

    // ── Текст ──

    private func textWidth(_ text: String, _ f: UIFont) -> CGFloat {
        (text as NSString).size(withAttributes: [.font: f]).width
    }

    /** Обрезка посимвольно, пока не влезет (§21 «Страница»). */
    private func truncated(_ text: String, to width: CGFloat, font f: UIFont) -> String {
        guard width > 6 else { return "" }
        var t = text
        while t.count > 3 && textWidth(t, f) > width {
            t = String(t.dropLast())
        }
        return t
    }

    /** Перенос слов (порт `wrapText` Android). */
    private func wrap(_ text: String, font f: UIFont, maxWidth: CGFloat) -> [String] {
        if text.trimmingCharacters(in: .whitespaces).isEmpty { return [""] }
        var result: [String] = []
        var rest = text
        while !rest.isEmpty {
            var count = rest.count
            while count > 1 && textWidth(String(rest.prefix(count)), f) > maxWidth {
                count -= 1
            }
            if count < rest.count {
                let slice = String(rest.prefix(count + 1))
                let breakAt = slice.lastIndex(of: " ").map { slice.distance(from: slice.startIndex, to: $0) } ?? count
                count = breakAt > 0 ? breakAt : count
            }
            let line = String(rest.prefix(count)).trimmingCharacters(in: .whitespaces)
            result.append(line)
            rest = String(rest.dropFirst(count)).trimmingCharacters(in: .whitespaces)
        }
        return result
    }

    private func drawText(
        _ text: String, _ f: UIFont, _ color: UIColor,
        x: CGFloat, top: CGFloat, width: CGFloat
    ) {
        (text as NSString).draw(
            in: CGRect(x: x, y: top, width: width, height: f.lineHeight + 2),
            withAttributes: [.font: f, .foregroundColor: color]
        )
    }

    private func drawCentered(_ text: String, _ f: UIFont, _ color: UIColor, centerX: CGFloat, top: CGFloat) {
        let w = textWidth(text, f)
        drawText(text, f, color, x: centerX - w / 2, top: top, width: w + 2)
    }

    private func drawRight(_ text: String, _ f: UIFont, _ color: UIColor, right: CGFloat, top: CGFloat) {
        let w = textWidth(text, f)
        drawText(text, f, color, x: right - w, top: top, width: w + 2)
    }

    // ── Блоки ──

    func draw(blocks: [PdfBlock]) {
        for block in blocks {
            draw(block: block)
        }
    }

    private func draw(block: PdfBlock) {
        if let newPage = block as? PdfNewPageBlock {
            beginPage(drawAppHeader: newPage.drawAppHeader)
        } else if let group = block as? PdfBlockGroup {
            drawGroup(group)
        } else if let text = block as? PdfTextBlock {
            drawTextBlock(text)
        } else if let row = block as? PdfRowBlock {
            drawRowBlock(row)
        } else if let title = block as? PdfSectionTitleBlock {
            drawSectionTitle(title)
        } else if block is PdfSeparatorBlock {
            y += 3
            let path = UIBezierPath()
            path.move(to: CGPoint(x: marginLeft, y: y))
            path.addLine(to: CGPoint(x: marginLeft + contentWidth, y: y))
            RenderState.lineColor.setStroke()
            path.lineWidth = 0.5
            path.stroke()
            y += 5
        } else if let grid = block as? PdfScheduleGridBlock {
            drawScheduleGrid(grid)
        } else if let statement = block as? PdfSalaryStatementBlock {
            drawSalaryStatement(statement)
        }
    }

    /** Маршрут целиком: не разрывать между страницами, если оценка высоты помещается (§21). */
    private func drawGroup(_ group: PdfBlockGroup) {
        let estimate = CGFloat(group.estimatedHeight)
        let cap = (pageHeight - CGFloat(m.marginTop) - marginBottom) * 0.85
        let needed = min(estimate, cap)
        if needNewPage(h: needed) {
            beginPage(drawAppHeader: true)
        }
        draw(blocks: group.children)
    }

    private func drawTextBlock(_ block: PdfTextBlock) {
        y += CGFloat(block.gapBefore)
        guard !block.text.isEmpty else { return }
        let f = font(block.style)
        let lineHeight = f.pointSize + 3
        let indent = CGFloat(block.indent)
        for line in wrap(block.text, font: f, maxWidth: contentWidth - indent) {
            checkNewPage(h: lineHeight)
            drawText(line, f, RenderState.textColor, x: marginLeft + indent, top: y, width: contentWidth - indent)
            y += lineHeight
        }
    }

    private func drawSectionTitle(_ block: PdfSectionTitleBlock) {
        checkNewPage(h: 26)
        y += CGFloat(block.gapBefore)
        let f = font(.section)
        let top = y
        let rect = CGRect(x: marginLeft, y: top, width: contentWidth, height: 14)
        RenderState.sectionFill.setFill()
        ctx.cgContext.fill(rect)
        drawText(block.text, f, RenderState.textColor, x: marginLeft + 4, top: top + 2, width: contentWidth - 8)
        y += 14
    }

    private func drawRowBlock(_ block: PdfRowBlock) {
        y += CGFloat(block.gapBefore)
        let h = CGFloat(block.rowHeight)
        checkNewPage(h: h + 2)
        let top = y
        let bottom = y + h
        if block.fillRow {
            let rect = CGRect(x: marginLeft, y: top, width: contentWidth, height: h)
            RenderState.sectionFill.setFill()
            ctx.cgContext.fill(rect)
        }
        var x = marginLeft
        let cells = block.cells
        let weights = block.weights
        for i in 0..<cells.count {
            let w = CGFloat(weights[i].doubleValue) * contentWidth
            let cell = cells[i]
            let border = UIBezierPath(rect: CGRect(x: x, y: top, width: w, height: h))
            RenderState.borderColor.setStroke()
            border.lineWidth = 0.5
            border.stroke()
            let f = font(cell.style)
            let avail = w - 6
            let text = truncated(cell.text, to: avail, font: f)
            let textTop = top + (h - f.lineHeight) / 2
            if cell.alignRight {
                drawRight(text, f, RenderState.textColor, right: x + w - 3, top: textTop)
            } else {
                drawText(text, f, RenderState.textColor, x: x + 3, top: textTop, width: avail)
            }
            x += w
        }
        y += h
    }

    // ── График (§21 «Раздел «График»») ──

    private func drawScheduleGrid(_ grid: PdfScheduleGridBlock) {
        let cellW = contentWidth / 7
        let cellH = CGFloat(m.scheduleCellHeight)
        let headerH = CGFloat(m.scheduleHeaderHeight)
        let cells = grid.cells
        let daysInMonth = cells.count
        let firstCol = Int(grid.firstColumnIndex)
        let rows = (firstCol + daysInMonth + 6) / 7
        let gridHeight = headerH + CGFloat(rows) * cellH + 4
        checkNewPage(h: gridHeight + 60)
        let gridTop = y

        // Шапка Пн…Вс
        let labels = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"]
        for (col, label) in labels.enumerated() {
            let x = marginLeft + CGFloat(col) * cellW
            let rect = CGRect(x: x, y: gridTop, width: cellW, height: headerH)
            let border = UIBezierPath(rect: rect)
            RenderState.borderColor.setStroke()
            border.lineWidth = 0.5
            border.stroke()
            RenderState.sectionFill.setFill()
            ctx.cgContext.fill(rect)
            let f = font(.bodyBold)
            drawCentered(label, f, RenderState.textColor, centerX: x + cellW / 2, top: gridTop + (headerH - f.lineHeight) / 2)
        }

        var col = firstCol
        var row = 0
        for cell in cells {
            let x = marginLeft + CGFloat(col) * cellW
            let cellY = gridTop + headerH + CGFloat(row) * cellH
            let border = UIBezierPath(rect: CGRect(x: x, y: cellY, width: cellW, height: cellH))
            RenderState.borderColor.setStroke()
            border.lineWidth = 0.5
            border.stroke()

            for fill in cell.fills {
                let left = x + CGFloat(fill.startFraction) * cellW
                let right = x + CGFloat(fill.endFraction) * cellW
                let rect = CGRect(
                    x: max(left, x + 1),
                    y: cellY + 1,
                    width: max(0, min(right, x + cellW - 1) - max(left, x + 1)),
                    height: cellH - 2
                )
                color(fromArgb: fill.argb).setFill()
                ctx.cgContext.fill(rect)
            }

            let dayFont = font(.body)
            drawText("\(cell.dayOfMonth)", dayFont, RenderState.textColor, x: x + 3, top: cellY + 2, width: cellW - 6)

            let entries = cell.entries
            if entries.count == 1 {
                let f = UIFont.systemFont(ofSize: 11, weight: .bold)
                drawCentered(entries[0], f, RenderState.textColor, centerX: x + cellW / 2, top: cellY + cellH / 2 - f.lineHeight / 2)
            } else if entries.count >= 2 {
                let f = UIFont.systemFont(ofSize: 11, weight: .bold)
                let spacing = f.pointSize + 3
                let totalH = CGFloat(entries.count) * spacing
                var textY = cellY + (cellH - totalH) / 2
                for e in entries {
                    drawCentered(e, f, RenderState.textColor, centerX: x + cellW / 2, top: textY)
                    textY += spacing
                }
            } else if let label = cell.releaseLabel {
                let f = font(.tiny)
                drawCentered(label, f, RenderState.mutedColor, centerX: x + cellW / 2, top: cellY + cellH / 2 - f.lineHeight / 2)
            } else if let dur = cell.passengerDuration {
                let f = font(.tiny)
                drawCentered(dur, f, RenderState.mutedColor, centerX: x + cellW / 2, top: cellY + cellH / 2 - f.lineHeight / 2)
            }

            col += 1
            if col == 7 {
                col = 0
                row += 1
            }
        }
        y = gridTop + headerH + CGFloat(rows) * cellH + 8
        y += 8
    }

    private func color(fromArgb argb: Int64) -> UIColor {
        let v = UInt32(truncatingIfNeeded: argb)
        let a = CGFloat((v >> 24) & 0xFF) / 255
        let r = CGFloat((v >> 16) & 0xFF) / 255
        let g = CGFloat((v >> 8) & 0xFF) / 255
        let b = CGFloat(v & 0xFF) / 255
        return UIColor(red: r, green: g, blue: b, alpha: a)
    }

    // ── Расчётный листок (§21 «Раздел «Расчётный листок»») ──

    private struct StatementColumns {
        let start: CGFloat
        let monthEnd: CGFloat
        let nameEnd: CGFloat
        let codeEnd: CGFloat
        let hoursEnd: CGFloat
        let percentEnd: CGFloat
        let end: CGFloat
    }

    private func statementColumns(start: CGFloat, width: CGFloat, accrual: Bool) -> StatementColumns {
        let monthEnd = start + 48
        let codeWidth: CGFloat = 32
        let hoursWidth: CGFloat = accrual ? 38 : 0
        let percentWidth: CGFloat = accrual ? 32 : 0
        let amountWidth: CGFloat = 58
        let nameEnd = start + width - codeWidth - hoursWidth - percentWidth - amountWidth
        let codeEnd = nameEnd + codeWidth
        let hoursEnd = codeEnd + hoursWidth
        let percentEnd = hoursEnd + percentWidth
        return StatementColumns(
            start: start, monthEnd: monthEnd, nameEnd: nameEnd,
            codeEnd: codeEnd, hoursEnd: hoursEnd, percentEnd: percentEnd, end: start + width
        )
    }

    private func drawSalaryStatement(_ s: PdfSalaryStatementBlock) {
        var continuation = false
        drawSalaryHeader(s, continuation: false)
        let pairs = max(s.accruals.count, s.deductions.count)
        var index = 0
        while index < pairs {
            let left = index < s.accruals.count ? s.accruals[index] : nil
            let right = index < s.deductions.count ? s.deductions[index] : nil
            let rowHeight = max(statementRowHeight(left, accrual: true), statementRowHeight(right, accrual: false))
            if y + rowHeight > pageHeight - 92 {
                beginPage(drawAppHeader: false)
                drawSalaryHeader(s, continuation: true)
                continuation = true
            }
            drawStatementPair(month: s.monthCell, left: left, right: right, height: rowHeight)
            index += 1
        }
        _ = continuation
        drawStatementTotals(s)
    }

    private func drawSalaryHeader(_ s: PdfSalaryStatementBlock, continuation: Bool) {
        y = 34
        let title = continuation ? s.title + " (продолжение)" : s.title
        let titleFont = font(.title)
        drawCentered(title, titleFont, RenderState.textColor, centerX: pageWidth / 2, top: y)
        y += 18
        if !continuation {
            let leftX = marginLeft + 4
            let rightX = marginLeft + contentWidth * 0.57
            let box = UIBezierPath(rect: CGRect(x: marginLeft, y: y - 11, width: contentWidth, height: 54))
            RenderState.borderColor.setStroke()
            box.lineWidth = 0.5
            box.stroke()
            let f = font(.body)
            drawText(s.tariffText, f, RenderState.textColor, x: leftX, top: y - 6, width: contentWidth * 0.55)
            drawText(s.normaText, f, RenderState.textColor, x: rightX, top: y - 6, width: contentWidth * 0.42)
            y += 16
            drawText(s.totalWorkText, font(.bodyBold), RenderState.textColor, x: leftX, top: y - 6, width: contentWidth * 0.55)
            drawText(s.currencyText, f, RenderState.textColor, x: rightX, top: y - 6, width: contentWidth * 0.42)
            y += 28
        }
        drawStatementColumnsHeader()
    }

    private func drawStatementColumnsHeader() {
        let split = marginLeft + contentWidth * 0.58
        let top = y
        let bottom = y + 35
        let labelsTop = y + 15
        let left = statementColumns(start: marginLeft, width: split - marginLeft, accrual: true)
        let right = statementColumns(start: split, width: marginLeft + contentWidth - split, accrual: false)

        let box = UIBezierPath(rect: CGRect(x: marginLeft, y: top, width: contentWidth, height: 35))
        RenderState.borderColor.setStroke()
        box.lineWidth = 0.5
        box.stroke()
        drawVerticalLine(x: split, top: top, bottom: bottom)

        drawCentered("Начислено", font(.bodyBold), RenderState.textColor, centerX: (marginLeft + split) / 2, top: top + 2)
        drawCentered(
            "Удержано и перечислено", font(.bodyBold), RenderState.textColor,
            centerX: (split + marginLeft + contentWidth) / 2, top: top + 2
        )
        drawColumnLines(left, top: labelsTop, bottom: bottom, accrual: true)
        drawColumnLines(right, top: labelsTop, bottom: bottom, accrual: false)
        drawColumnHeader(left, top: top + 20, name: "Вид выплаты", accrual: true)
        drawColumnHeader(right, top: top + 20, name: "Вид удержания", accrual: false)
        y = bottom
    }

    private func drawColumnHeader(_ c: StatementColumns, top: CGFloat, name: String, accrual: Bool) {
        let f = font(.small)
        drawCentered("Месяц", f, RenderState.mutedColor, centerX: (c.start + c.monthEnd) / 2, top: top)
        drawCentered(name, f, RenderState.mutedColor, centerX: (c.monthEnd + c.nameEnd) / 2, top: top)
        drawCentered("Код", f, RenderState.mutedColor, centerX: (c.nameEnd + c.codeEnd) / 2, top: top)
        if accrual {
            drawCentered("Часы", f, RenderState.mutedColor, centerX: (c.codeEnd + c.hoursEnd) / 2, top: top)
            drawCentered("%", f, RenderState.mutedColor, centerX: (c.hoursEnd + c.percentEnd) / 2, top: top)
        }
        drawCentered("Сумма", f, RenderState.mutedColor, centerX: (c.percentEnd + c.end) / 2, top: top)
    }

    private func drawColumnLines(_ c: StatementColumns, top: CGFloat, bottom: CGFloat, accrual: Bool) {
        var boundaries = [c.monthEnd, c.nameEnd, c.codeEnd]
        if accrual {
            boundaries.append(contentsOf: [c.hoursEnd, c.percentEnd])
        }
        for x in boundaries {
            drawVerticalLine(x: x, top: top, bottom: bottom)
        }
    }

    private func drawVerticalLine(x: CGFloat, top: CGFloat, bottom: CGFloat) {
        let path = UIBezierPath()
        path.move(to: CGPoint(x: x, y: top))
        path.addLine(to: CGPoint(x: x, y: bottom))
        RenderState.borderColor.setStroke()
        path.lineWidth = 0.5
        path.stroke()
    }

    private func drawHorizontalLine(y lineY: CGFloat, left: CGFloat, right: CGFloat) {
        let path = UIBezierPath()
        path.move(to: CGPoint(x: left, y: lineY))
        path.addLine(to: CGPoint(x: right, y: lineY))
        RenderState.borderColor.setStroke()
        path.lineWidth = 0.5
        path.stroke()
    }

    private func statementRowHeight(_ row: PdfStatementRowData?, accrual: Bool) -> CGFloat {
        guard let row else { return CGFloat(m.statementRowMinHeight) }
        let sideWidth = contentWidth * (accrual ? 0.58 : 0.42)
        let columns = statementColumns(start: 0, width: sideWidth, accrual: accrual)
        let nameWidth = columns.nameEnd - columns.monthEnd - 6
        let lines = wrap(row.name, font: font(.small), maxWidth: max(nameWidth, 30)).count
        return max(CGFloat(m.statementRowMinHeight), CGFloat(lines) * CGFloat(m.statementLineHeight) + 6)
    }

    private func drawStatementPair(
        month: String, left: PdfStatementRowData?, right: PdfStatementRowData?, height: CGFloat
    ) {
        let top = y
        let bottom = top + height
        let split = marginLeft + contentWidth * 0.58
        let leftCols = statementColumns(start: marginLeft, width: split - marginLeft, accrual: true)
        let rightCols = statementColumns(start: split, width: marginLeft + contentWidth - split, accrual: false)
        drawVerticalLine(x: marginLeft, top: top, bottom: bottom)
        drawVerticalLine(x: split, top: top, bottom: bottom)
        drawVerticalLine(x: marginLeft + contentWidth, top: top, bottom: bottom)
        drawColumnLines(leftCols, top: top, bottom: bottom, accrual: true)
        drawColumnLines(rightCols, top: top, bottom: bottom, accrual: false)
        if let left { drawStatementRow(month: month, row: left, columns: leftCols, top: top, accrual: true) }
        if let right { drawStatementRow(month: month, row: right, columns: rightCols, top: top, accrual: false) }
        drawHorizontalLine(y: bottom, left: marginLeft, right: marginLeft + contentWidth)
        y = bottom
    }

    private func drawStatementRow(
        month: String, row: PdfStatementRowData, columns c: StatementColumns, top: CGFloat, accrual: Bool
    ) {
        let f = font(.small)
        let baseline = top + 4
        drawCentered(month, f, RenderState.mutedColor, centerX: (c.start + c.monthEnd) / 2, top: baseline)
        let nameWidth = c.nameEnd - c.monthEnd - 6
        for (i, line) in wrap(row.name, font: f, maxWidth: max(nameWidth, 30)).enumerated() {
            drawText(line, f, RenderState.mutedColor, x: c.monthEnd + 3, top: baseline + CGFloat(i) * CGFloat(m.statementLineHeight), width: nameWidth)
        }
        drawCentered(row.code, f, RenderState.mutedColor, centerX: (c.nameEnd + c.codeEnd) / 2, top: baseline)
        if accrual {
            drawRight(row.hours, f, RenderState.mutedColor, right: c.hoursEnd - 3, top: baseline)
            drawRight(row.percent, f, RenderState.mutedColor, right: c.percentEnd - 3, top: baseline)
        }
        drawRight(row.amount, f, RenderState.mutedColor, right: c.end - 3, top: baseline)
    }

    private func drawStatementTotals(_ s: PdfSalaryStatementBlock) {
        if y + 58 > pageHeight - marginBottom {
            beginPage(drawAppHeader: false)
            y = 40
        }
        let split = marginLeft + contentWidth * 0.58
        let top = y
        let box = UIBezierPath(rect: CGRect(x: marginLeft, y: top, width: contentWidth, height: 56))
        RenderState.borderColor.setStroke()
        box.lineWidth = 0.5
        box.stroke()
        drawVerticalLine(x: split, top: top, bottom: top + 56)
        drawText("Всего начислено", font(.bodyBold), RenderState.textColor, x: marginLeft + 4, top: top + 6, width: 200)
        drawRight(s.totalChargedText, font(.bodyBold), RenderState.textColor, right: split - 4, top: top + 6)
        drawText("Всего удержано", font(.bodyBold), RenderState.textColor, x: split + 4, top: top + 6, width: 200)
        drawRight(s.totalRetentionText, font(.bodyBold), RenderState.textColor, right: marginLeft + contentWidth - 4, top: top + 6)
        drawText("Всего отработано часов за месяц", font(.bodyBold), RenderState.textColor, x: marginLeft + 4, top: top + 23, width: 250)
        drawRight(s.totalHoursText, font(.body), RenderState.textColor, right: split - 4, top: top + 23)
        drawText("К перечислению", font(.bodyBold), RenderState.textColor, x: split + 4, top: top + 23, width: 200)
        drawRight(s.toBeCreditedText, font(.bodyBold), RenderState.textColor, right: marginLeft + contentWidth - 4, top: top + 23)
        drawText(s.footerText, font(.tiny), RenderState.mutedColor, x: marginLeft + 4, top: top + 41, width: contentWidth - 8)
        y = top + 56
    }
}

private extension UIFont {
    func withTraits(_ traits: UIFontDescriptor.SymbolicTraits) -> UIFont {
        guard let descriptor = fontDescriptor.withSymbolicTraits(traits) else { return self }
        return UIFont(descriptor: descriptor, size: pointSize)
    }
}
