package app.marlboroadvance.mpvex.utils.media

import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.os.Build
import androidx.compose.runtime.Immutable

/**
 * Which video formats this phone can decode in hardware, for Settings → Decoder.
 *
 * Asks Android's codec list once. Formats without a hardware decoder still play through
 * mpv's software decoder, just with more battery use, so nothing here is "unsupported".
 */
object DeviceCodecReport {

  @Immutable
  data class Entry(
    val label: String,
    val hardware: Boolean,
    /** e.g. "4K" or "1080p"; null when not decoded in hardware. */
    val maxResolution: String?,
    /** Highest frame rate at [maxResolution], or null. */
    val maxFps: Int?,
    val hdr: Boolean,
    /** Android decoder names, hardware first, e.g. "c2.qti.hevc.decoder". */
    val decoders: List<String>,
  )

  private class Format(
    val label: String,
    val mime: String,
    val profile: Int? = null,
    val hdrProfiles: List<Int> = emptyList(),
  )

  // Profile constants are plain ints, so using the newer ones on older Android is safe:
  // they simply never match there.
  private val FORMATS = listOf(
    Format("H.264 (AVC)", "video/avc"),
    Format("H.264 10-bit (Hi10P)", "video/avc", CodecProfileLevel.AVCProfileHigh10),
    Format(
      "HEVC (H.265)",
      "video/hevc",
      hdrProfiles = listOf(CodecProfileLevel.HEVCProfileMain10HDR10, CodecProfileLevel.HEVCProfileMain10HDR10Plus),
    ),
    Format("HEVC 10-bit (Main 10)", "video/hevc", CodecProfileLevel.HEVCProfileMain10),
    Format(
      "AV1",
      "video/av01",
      hdrProfiles = listOf(CodecProfileLevel.AV1ProfileMain10HDR10, CodecProfileLevel.AV1ProfileMain10HDR10Plus),
    ),
    Format("AV1 10-bit", "video/av01", CodecProfileLevel.AV1ProfileMain10),
    Format(
      "VP9",
      "video/x-vnd.on2.vp9",
      hdrProfiles = listOf(CodecProfileLevel.VP9Profile2HDR, CodecProfileLevel.VP9Profile2HDR10Plus),
    ),
    Format("VP9 10-bit (Profile 2)", "video/x-vnd.on2.vp9", CodecProfileLevel.VP9Profile2),
    Format("Dolby Vision", "video/dolby-vision"),
    Format("VP8", "video/x-vnd.on2.vp8"),
    Format("MPEG-2", "video/mpeg2"),
    Format("MPEG-4 Part 2", "video/mp4v-es"),
  )

  @Volatile private var cached: List<Entry>? = null

  /** Builds the report (cached after the first call). Call off the main thread. */
  fun build(): List<Entry> {
    cached?.let { return it }
    val decoders = runCatching {
      MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { !it.isEncoder }
    }.getOrDefault(emptyList())

    val result = FORMATS.map { format ->
      val forType = decoders.filter { info -> info.supportedTypes.any { it.equals(format.mime, ignoreCase = true) } }
      val hardware = forType.filter { isHardware(it) && supportsProfile(it, format.mime, format.profile) }
      val best = hardware.maxByOrNull { maxPixels(it, format.mime) }
      val caps = best?.let { runCatching { it.getCapabilitiesForType(format.mime).videoCapabilities }.getOrNull() }

      val (resolution, fps) = if (caps != null) resolutionLabel(caps) else null to null
      val hdr = format.hdrProfiles.isNotEmpty() && hardware.any { info ->
        format.hdrProfiles.any { supportsProfile(info, format.mime, it) }
      }
      Entry(
        label = format.label,
        hardware = hardware.isNotEmpty(),
        maxResolution = resolution,
        maxFps = fps,
        hdr = hdr,
        decoders = (hardware + forType.filterNot { it in hardware }).map { it.name }.distinct(),
      )
    }
    cached = result
    return result
  }

  /** Plain-text version for "Copy report", handy for bug reports. */
  fun asText(entries: List<Entry>): String =
    buildString {
      appendLine("MpvFlux device codec support")
      appendLine("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
      appendLine()
      entries.forEach { e ->
        val details = listOfNotNull(
          if (e.hardware) "HW" else "SW",
          e.maxResolution?.let { r -> e.maxFps?.let { "$r@$it" } ?: r },
          if (e.hdr) "HDR" else null,
        ).joinToString(" · ")
        appendLine("${e.label}: $details")
        if (e.decoders.isNotEmpty()) appendLine("  ${e.decoders.joinToString(", ")}")
      }
    }.trimEnd()

  private fun isHardware(info: MediaCodecInfo): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      return info.isHardwareAccelerated && !info.isSoftwareOnly
    }
    val name = info.name.lowercase()
    return !name.startsWith("omx.google.") &&
      !name.startsWith("c2.android.") &&
      !name.contains(".sw.") &&
      !name.endsWith(".sw")
  }

  private fun supportsProfile(info: MediaCodecInfo, mime: String, profile: Int?): Boolean {
    if (profile == null) return true
    return runCatching {
      info.getCapabilitiesForType(mime).profileLevels.any { it.profile == profile }
    }.getOrDefault(false)
  }

  private fun maxPixels(info: MediaCodecInfo, mime: String): Long =
    runCatching {
      val v = info.getCapabilitiesForType(mime).videoCapabilities
      v.supportedWidths.upper.toLong() * v.supportedHeights.upper.toLong()
    }.getOrDefault(0L)

  /** Largest standard size the decoder supports, and its top frame rate there. */
  private fun resolutionLabel(caps: MediaCodecInfo.VideoCapabilities): Pair<String?, Int?> {
    val sizes = listOf(
      "8K" to (7680 to 4320),
      "4K" to (3840 to 2160),
      "1440p" to (2560 to 1440),
      "1080p" to (1920 to 1080),
      "720p" to (1280 to 720),
      "480p" to (854 to 480),
    )
    for ((label, size) in sizes) {
      val (w, h) = size
      if (runCatching { caps.isSizeSupported(w, h) }.getOrDefault(false)) {
        val fps = runCatching { caps.getSupportedFrameRatesFor(w, h).upper.toInt() }.getOrNull()
        return label to fps?.coerceAtMost(240)
      }
    }
    return null to null
  }
}
