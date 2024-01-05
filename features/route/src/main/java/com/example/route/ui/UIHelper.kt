package com.example.route.ui

fun changeDpWithScroll(offset: Float, max: Int, min: Int): Float {
    val offsetMax = 1500
    val offsetMin = 250

    val offsetRange = offsetMax - offsetMin
    val dpRange = max - min

    var result = (((offset - offsetMin) * dpRange) / offsetRange) + min
    if (result > max) result = max.toFloat()
    if (result < min) result = min.toFloat()

    return result
}

fun changeAlphaWithOffset(offset: Float): Float {
    val offsetMax = 1500
    val offsetMin = 250

    val alphaMax = 0.2f
    val alphaMin = 1.0f

    val offsetRange = offsetMax - offsetMin
    val alphaRange = alphaMax - alphaMin

    var alpha = (((offset - offsetMin) * alphaRange) / offsetRange) + alphaMin

    if (alpha > 1F) alpha = 1F
    if (alpha < 0F) alpha = 0F

    return alpha
}