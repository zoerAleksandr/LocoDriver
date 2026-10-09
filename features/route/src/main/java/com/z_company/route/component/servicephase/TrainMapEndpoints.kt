package com.z_company.route.component.servicephase

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Значки начала и окончания пройденного пути на карте поезда (SCREEN_SPECS §6.9). Круг 28 dp с белым кольцом и тёмной
 * обводкой (читается на светлой и тёмной подложке): «старт» - зелёный круг с белым треугольником, «финиш» - красный круг
 * с клетчатым флажком. Размер - в пикселях экрана.
 */
internal fun renderTrackEndIcon(density: Float, isStart: Boolean): Bitmap {
    val size = (32 * density).toInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = size / 2f
    val r = 14 * density
    fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    val ink = android.graphics.Color.parseColor("#0A0E14")
    c.drawCircle(cx, cx, r, fillPaint(ink))
    c.drawCircle(cx, cx, r - 1.5f * density, fillPaint(android.graphics.Color.WHITE))
    c.drawCircle(cx, cx, r - 4 * density, fillPaint(android.graphics.Color.parseColor(if (isStart) "#2E7D32" else "#D32F2F")))
    if (isStart) {
        val tri = Path().apply {
            moveTo(cx - 3 * density, cx - 5 * density)
            lineTo(cx + 5.5f * density, cx)
            lineTo(cx - 3 * density, cx + 5 * density)
            close()
        }
        c.drawPath(tri, fillPaint(android.graphics.Color.WHITE))
    } else {
        // Клетчатый флажок 3x2 клетки: белые и тёмные клетки на красном круге.
        val cell = 3.2f * density
        val left = cx - 4.8f * density
        val top = cx - 3.2f * density
        val white = fillPaint(android.graphics.Color.WHITE)
        val dark = fillPaint(ink)
        for (row in 0 until 2) for (col in 0 until 3) {
            val rect = RectF(left + col * cell, top + row * cell, left + (col + 1) * cell, top + (row + 1) * cell)
            c.drawRect(rect, if ((row + col) % 2 == 0) white else dark)
        }
    }
    return bmp
}
