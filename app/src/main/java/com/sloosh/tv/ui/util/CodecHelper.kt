package com.sloosh.tv.ui.util

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import java.util.Locale

object CodecHelper {
    private var isAv1HardwareSupported: Boolean? = null

    /**
     * Checks if the device has a dedicated hardware decoder for AV1 (video/av01).
     * Software decoding (e.g. c2.android.av1-dav1d.decoder) cannot decode 4K or 1080p in real time on TV hardware,
     * resulting in ~1 FPS slide-shows and 100% CPU usage.
     */
    fun isHardwareAv1Supported(): Boolean {
        isAv1HardwareSupported?.let { return it }
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val supported = codecList.codecInfos.any { info ->
                !info.isEncoder &&
                info.supportedTypes.any { it.equals("video/av01", ignoreCase = true) } &&
                isHardwareCodec(info)
            }
            isAv1HardwareSupported = supported
            supported
        } catch (e: Exception) {
            isAv1HardwareSupported = false
            false
        }
    }

    private fun isHardwareCodec(info: MediaCodecInfo): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return info.isHardwareAccelerated
        }
        val name = info.name.lowercase(Locale.ROOT)
        return !name.startsWith("omx.google.") &&
               !name.startsWith("c2.android.") &&
               !name.contains("sw_codec") &&
               !name.contains("software") &&
               !name.contains("dav1d")
    }
}
