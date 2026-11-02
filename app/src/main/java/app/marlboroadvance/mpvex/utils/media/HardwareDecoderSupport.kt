package app.marlboroadvance.mpvex.utils.media

import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecProfileLevel
import android.media.MediaCodecList
import android.os.Build
import java.util.concurrent.ConcurrentHashMap

/**
 * Answers "can this phone decode the current video in hardware?" by asking Android's
 * codec list (MediaCodecList), so the decoder sheet can mark HW / HW+ as unavailable
 * instead of letting the user pick an option that silently falls back to software.
 *
 * Checks the codec (e.g. AV1, HEVC), the profile where it matters for anime releases
 * (H.264 High 10 "Hi10P", HEVC Main 10, VP9 Profile 2) and the resolution.
 */
object HardwareDecoderSupport {

  sealed interface Result {
    /** A hardware decoder exists for this codec, profile and size. */
    data object Available : Result

    /** No hardware decoder can play this video; [reason] says why. */
    data class Unavailable(val reason: Reason) : Result

    /** Codec we can't map to an Android type, so we don't guess. */
    data object Unknown : Result
  }

  enum class Reason { NoHardwareDecoder, ProfileNotSupported, ResolutionTooHigh }

  private val cache = ConcurrentHashMap<String, Result>()

  /**
   * @param codec mpv codec name (`current-tracks/video/codec`), e.g. "av1", "hevc", "h264"
   * @param profile ffmpeg profile name (`current-tracks/video/codec-profile`), e.g. "High 10"
   */
  fun check(codec: String?, profile: String?, width: Int, height: Int): Result {
    val codecName = codec?.lowercase() ?: return Result.Unknown
    val mime = MIME_BY_MPV_CODEC[codecName] ?: return Result.Unknown
    val key = "$mime|${profile.orEmpty()}|${width}x$height"
    return cache.getOrPut(key) { query(mime, codecName, profile, width, height) }
  }

  private fun query(mime: String, codec: String, profile: String?, width: Int, height: Int): Result {
    val hardware = runCatching {
      MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->
        !info.isEncoder && isHardware(info) && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
      }
    }.getOrDefault(emptyList())

    if (hardware.isEmpty()) return Result.Unavailable(Reason.NoHardwareDecoder)

    val requiredProfile = requiredProfile(codec, profile)
    val withProfile = if (requiredProfile == null) {
      hardware
    } else {
      hardware.filter { info ->
        runCatching {
          info.getCapabilitiesForType(mime).profileLevels.any { it.profile == requiredProfile }
        }.getOrDefault(false)
      }
    }
    if (withProfile.isEmpty()) return Result.Unavailable(Reason.ProfileNotSupported)

    if (width > 0 && height > 0) {
      val fitsSize = withProfile.any { info ->
        runCatching {
          val video = info.getCapabilitiesForType(mime).videoCapabilities
          video == null || video.isSizeSupported(width, height) || video.isSizeSupported(height, width)
        }.getOrDefault(true)
      }
      if (!fitsSize) return Result.Unavailable(Reason.ResolutionTooHigh)
    }
    return Result.Available
  }

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

  /** Profiles that commonly lack hardware support even when the codec itself is supported. */
  private fun requiredProfile(codec: String, profile: String?): Int? {
    val p = profile?.lowercase() ?: return null
    return when (codec) {
      "h264" -> when {
        "4:4:4" in p -> CodecProfileLevel.AVCProfileHigh444
        "4:2:2" in p -> CodecProfileLevel.AVCProfileHigh422
        "10" in p -> CodecProfileLevel.AVCProfileHigh10
        else -> null
      }
      "hevc" -> if ("10" in p) CodecProfileLevel.HEVCProfileMain10 else null
      "vp9" -> if ("2" in p) CodecProfileLevel.VP9Profile2 else null
      else -> null
    }
  }

  private val MIME_BY_MPV_CODEC = mapOf(
    "h264" to "video/avc",
    "hevc" to "video/hevc",
    "av1" to "video/av01",
    "vp9" to "video/x-vnd.on2.vp9",
    "vp8" to "video/x-vnd.on2.vp8",
    "mpeg2video" to "video/mpeg2",
    "mpeg4" to "video/mp4v-es",
    "h263" to "video/3gpp",
  )
}
