package com.kyant.capsule

@Suppress("NOTHING_TO_INLINE")
internal inline fun lerp(start: Double, stop: Double, fraction: Double): Double {
    return start + (stop - start) * fraction
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Double.fastCoerceIn(minimumValue: Double, maximumValue: Double): Double {
    return if (this < minimumValue) minimumValue else if (this > maximumValue) maximumValue else this
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Float.fastCoerceIn(minimumValue: Float, maximumValue: Float): Float {
    return if (this < minimumValue) minimumValue else if (this > maximumValue) maximumValue else this
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Double.fastCoerceAtLeast(minimumValue: Double): Double {
    return if (this < minimumValue) minimumValue else this
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Float.fastCoerceAtLeast(minimumValue: Float): Float {
    return if (this < minimumValue) minimumValue else this
}
