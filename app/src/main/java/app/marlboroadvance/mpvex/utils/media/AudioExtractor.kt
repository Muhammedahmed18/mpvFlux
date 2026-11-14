package app.marlboroadvance.mpvex.utils.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.Locale
import net.mediaarea.mediainfo.lib.MediaInfo

/**
 * Copies one audio track out of a video without re-encoding (stream copy).
 *
 * - AAC  → .m4a (MediaMuxer MPEG_4)
 * - Opus → .ogg (MediaMuxer OGG, Android 10+)
 * - MP3  → .mp3 (MP3 frames written as-is)
 *
 * Any other codec is reported as unsupported.
 */
object AudioExtractor {

  enum class OutputFormat(val extension: String) {
    M4A("m4a"),
    OGG("ogg"),
    MP3("mp3"),
  }

  data class AudioTrack(
    /** Track index in the container (MediaExtractor index). */
    val index: Int,
    /** 1-based position among the audio tracks, for display. */
    val number: Int,
    val mime: String,
    val language: String?,
    val channels: Int,
    val sampleRate: Int,
    val durationUs: Long,
    val output: OutputFormat?,
    /** Set when [output] is null: why this track can't be extracted. */
    val unsupportedReason: UnsupportedReason?,
    /** Codec name from MediaInfo, for tracks Android's reader doesn't expose. */
    val codecName: String? = null,
  ) {
    val isSupported: Boolean get() = output != null
    val codecLabel: String get() = codecName ?: codecLabel(mime)
    val languageLabel: String?
      get() = language?.let { code ->
        runCatching { Locale.forLanguageTag(code).displayLanguage }.getOrNull()?.takeIf { it.isNotBlank() && it != code } ?: code
      }
    val channelsLabel: String
      get() = when (channels) {
        0 -> ""
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> "$channels ch"
      }
  }

  enum class UnsupportedReason { FORMAT, NEEDS_ANDROID_10 }

  /**
   * Reads the audio tracks of [uri]. Blocking; call off the main thread.
   *
   * Android's reader (MediaExtractor) silently skips codecs the phone can't handle, such as
   * E-AC3, DTS or TrueHD in MKV, so on its own a video could look like it has no audio. MediaInfo
   * sees every track, so tracks only it reports are added as "can't be extracted" with their
   * real codec name.
   */
  fun probe(context: Context, uri: Uri): List<AudioTrack> {
    val extractorResult = runCatching { probeWithExtractor(context, uri) }
    val extractorTracks = extractorResult.getOrDefault(emptyList())
    val mediaInfoTracks = runCatching { probeWithMediaInfo(context, uri) }.getOrDefault(emptyList())

    if (mediaInfoTracks.size <= extractorTracks.size) {
      // MediaInfo saw nothing extra (or failed): Android's list is complete.
      if (extractorTracks.isEmpty()) extractorResult.getOrThrow()
      return extractorTracks
    }

    // Merge in MediaInfo's order: use Android's track where one with the same codec is still
    // unused (it can be extracted), otherwise list the MediaInfo track as unsupported.
    val unused = extractorTracks.toMutableList()
    val merged = mutableListOf<AudioTrack>()
    mediaInfoTracks.forEach { info ->
      val match = unused.firstOrNull { it.codecLabel == info.codec }
      merged += if (match != null) {
        unused.remove(match)
        match.copy(number = merged.size + 1)
      } else {
        AudioTrack(
          index = -1,
          number = merged.size + 1,
          mime = "",
          language = info.language,
          channels = info.channels,
          sampleRate = 0,
          durationUs = 0L,
          output = null,
          unsupportedReason = UnsupportedReason.FORMAT,
          codecName = info.codec,
        )
      }
    }
    unused.forEach { merged += it.copy(number = merged.size + 1) }
    return merged
  }

  private data class MediaInfoAudio(val codec: String, val language: String?, val channels: Int)

  /** Audio tracks as MediaInfo reports them (codec, language, channels). */
  private fun probeWithMediaInfo(context: Context, uri: Uri): List<MediaInfoAudio> {
    val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
    // Same pattern as MediaInfoOps: MediaInfo takes the detached fd and closes it.
    val fd = pfd.detachFd()
    val mi = MediaInfo()
    try {
      mi.Open(fd, uri.lastPathSegment ?: "video")
      val count = mi.Count_Get(MediaInfo.Stream.Audio)
      return (0 until count).map { i ->
        MediaInfoAudio(
          codec = mediaInfoCodecLabel(mi.Get(MediaInfo.Stream.Audio, i, "Format")),
          language = mi.Get(MediaInfo.Stream.Audio, i, "Language").takeIf { it.isNotBlank() && it != "und" },
          channels = mi.Get(MediaInfo.Stream.Audio, i, "Channel(s)").trim().toIntOrNull() ?: 0,
        )
      }
    } finally {
      mi.Close()
      pfd.close()
    }
  }

  /** MediaInfo format names → the labels used in the sheet (same as [codecLabel]). */
  private fun mediaInfoCodecLabel(format: String): String = when (format.trim()) {
    "AAC" -> "AAC"
    "MPEG Audio" -> "MP3"
    "Opus" -> "Opus"
    "Vorbis" -> "Vorbis"
    "FLAC" -> "FLAC"
    "AC-3" -> "AC3"
    "E-AC-3" -> "E-AC3"
    "DTS" -> "DTS"
    "MLP FBA" -> "TrueHD"
    "PCM" -> "PCM"
    "" -> "Unknown"
    else -> format.trim()
  }

  private fun probeWithExtractor(context: Context, uri: Uri): List<AudioTrack> {
    val extractor = MediaExtractor()
    try {
      extractor.setDataSource(context, uri, null)
      val tracks = mutableListOf<AudioTrack>()
      for (i in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
        if (!mime.startsWith("audio/")) continue
        val (output, reason) = outputFor(mime)
        tracks += AudioTrack(
          index = i,
          number = tracks.size + 1,
          mime = mime,
          language = format.stringOrNull(MediaFormat.KEY_LANGUAGE)?.takeIf { it.isNotBlank() && it != "und" },
          channels = format.intOrZero(MediaFormat.KEY_CHANNEL_COUNT),
          sampleRate = format.intOrZero(MediaFormat.KEY_SAMPLE_RATE),
          durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) else 0L,
          output = output,
          unsupportedReason = reason,
        )
      }
      return tracks
    } finally {
      extractor.release()
    }
  }

  /**
   * Copies [track] from [source] into the already created document [output].
   * [onProgress] gets 0..1, or -1 when the duration is unknown. Cancellable.
   */
  suspend fun extract(
    context: Context,
    source: Uri,
    track: AudioTrack,
    output: Uri,
    onProgress: (Float) -> Unit,
  ) = withContext(Dispatchers.IO) {
    val format = track.output ?: error("Unsupported audio format: ${track.mime}")
    val extractor = MediaExtractor()
    try {
      extractor.setDataSource(context, source, null)
      val trackFormat = extractor.getTrackFormat(track.index)
      extractor.selectTrack(track.index)

      val bufferSize = trackFormat.intOrZero(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(1 shl 20)
      val buffer = ByteBuffer.allocateDirect(bufferSize)
      val durationUs = track.durationUs

      // Length of the audio written so far, from the frames themselves.
      var writtenUs = 0L

      fun report(timeUs: Long) {
        onProgress(if (durationUs > 0) (timeUs.toFloat() / durationUs).coerceIn(0f, 1f) else -1f)
      }

      when (format) {
        OutputFormat.MP3 -> {
          val out = context.contentResolver.openOutputStream(output, "w")
            ?: error("Can't open output file")
          out.use { stream ->
            val bytes = ByteArray(bufferSize)
            while (true) {
              ensureActive()
              buffer.clear()
              val size = extractor.readSampleData(buffer, 0)
              if (size < 0) break
              buffer.position(0)
              buffer.limit(size)
              buffer.get(bytes, 0, size)
              stream.write(bytes, 0, size)
              writtenUs += mp3FrameDurationUs(bytes, size)
              report(writtenUs)
              extractor.advance()
            }
          }
        }

        OutputFormat.M4A, OutputFormat.OGG -> {
          val pfd = context.contentResolver.openFileDescriptor(output, "rw")
            ?: error("Can't open output file")
          pfd.use {
            val muxerFormat = if (format == OutputFormat.OGG) {
              // Only reached on Android 10+ (see outputFor).
              MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG
            } else {
              MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            }
            val muxer = MediaMuxer(it.fileDescriptor, muxerFormat)
            var started = false
            try {
              val dstTrack = muxer.addTrack(trackFormat)
              muxer.start()
              started = true
              val info = MediaCodec.BufferInfo()
              // Timestamps are calculated from the audio itself, not taken from the
              // container: MediaExtractor reports wrong times for laced MKV blocks (several
              // AAC frames per block), which made a 23 min episode come out as ~3 h.
              val aacFrameUs = if (format == OutputFormat.M4A) aacFrameDurationUs(trackFormat) else 0.0
              var frameCount = 0L
              var opusUs = 0L
              while (true) {
                ensureActive()
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val ptsUs: Long
                if (format == OutputFormat.M4A) {
                  // AAC: every frame is 1024 samples at the core sample rate.
                  ptsUs = (frameCount * aacFrameUs).toLong()
                  frameCount++
                  writtenUs = (frameCount * aacFrameUs).toLong()
                } else {
                  // Opus: each packet states its own length in its first byte(s).
                  ptsUs = opusUs
                  opusUs += opusPacketDurationUs(buffer, size)
                  writtenUs = opusUs
                }
                info.set(0, size, ptsUs, MediaCodec.BUFFER_FLAG_KEY_FRAME)
                muxer.writeSampleData(dstTrack, buffer, info)
                report(writtenUs)
                extractor.advance()
              }
            } finally {
              // stop() throws if nothing was written; the caller deletes the file on error.
              runCatching { if (started) muxer.stop() }
              runCatching { muxer.release() }
            }
          }
        }
      }

      // Safety net: never keep a file whose length doesn't match the video's audio track.
      if (durationUs > 0) {
        val diff = kotlin.math.abs(writtenUs - durationUs).toDouble() / durationUs
        if (diff > MAX_DURATION_MISMATCH) {
          throw DurationMismatchException(expectedUs = durationUs, actualUs = writtenUs)
        }
      }
    } finally {
      extractor.release()
    }
  }

  /** Thrown when the extracted audio is more than 2% longer or shorter than the source track. */
  class DurationMismatchException(expectedUs: Long, actualUs: Long) :
    IllegalStateException("Extracted length ${actualUs / 1000} ms, expected ${expectedUs / 1000} ms")

  private const val MAX_DURATION_MISMATCH = 0.02

  private val AAC_SAMPLE_RATES = intArrayOf(
    96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350,
  )

  /**
   * Duration of one AAC frame (1024 samples) in microseconds. The sample rate is read from the
   * AudioSpecificConfig (csd-0): for HE-AAC it is the core rate, while KEY_SAMPLE_RATE can report
   * the doubled SBR output rate, which would halve every timestamp.
   */
  private fun aacFrameDurationUs(format: MediaFormat): Double {
    val rate = aacCoreSampleRate(format) ?: format.intOrZero(MediaFormat.KEY_SAMPLE_RATE)
    require(rate > 0) { "Unknown AAC sample rate" }
    return 1024.0 * 1_000_000.0 / rate
  }

  private fun aacCoreSampleRate(format: MediaFormat): Int? = runCatching {
    val csd = format.getByteBuffer("csd-0") ?: return null
    val bytes = ByteArray(csd.remaining())
    csd.duplicate().get(bytes)
    val bits = BitReader(bytes)
    var objectType = bits.read(5)
    if (objectType == 31) objectType = 32 + bits.read(6)
    val index = bits.read(4)
    if (index == 15) bits.read(24) else AAC_SAMPLE_RATES.getOrNull(index)
  }.getOrNull()?.takeIf { it > 0 }

  /** Opus packet length from its TOC byte (RFC 6716 §3.1). */
  private fun opusPacketDurationUs(buffer: ByteBuffer, size: Int): Long {
    if (size < 1) return 0L
    val toc = buffer.get(0).toInt() and 0xFF
    val config = toc shr 3
    val frameUs = when {
      config < 12 -> longArrayOf(10_000, 20_000, 40_000, 60_000)[config and 3] // SILK
      config < 16 -> longArrayOf(10_000, 20_000)[config and 1] // Hybrid
      else -> longArrayOf(2_500, 5_000, 10_000, 20_000)[config and 3] // CELT
    }
    val frames = when (toc and 3) {
      0 -> 1
      1, 2 -> 2
      else -> if (size >= 2) buffer.get(1).toInt() and 0x3F else 1
    }
    return frameUs * frames
  }

  /** MP3 frame length from its header; 0 if the bytes don't start with a frame header. */
  private fun mp3FrameDurationUs(bytes: ByteArray, size: Int): Long {
    if (size < 4) return 0L
    val b1 = bytes[1].toInt() and 0xFF
    val b2 = bytes[2].toInt() and 0xFF
    if ((bytes[0].toInt() and 0xFF) != 0xFF || (b1 and 0xE0) != 0xE0) return 0L
    val version = (b1 shr 3) and 3 // 3 = MPEG-1, 2 = MPEG-2, 0 = MPEG-2.5
    val layer = (b1 shr 1) and 3 // 1 = Layer III, 2 = Layer II, 3 = Layer I
    val rateIndex = (b2 shr 2) and 3
    if (version == 1 || layer == 0 || rateIndex == 3) return 0L
    val baseRate = intArrayOf(44100, 48000, 32000)[rateIndex]
    val rate = when (version) {
      3 -> baseRate
      2 -> baseRate / 2
      else -> baseRate / 4
    }
    val samples = when (layer) {
      3 -> 384
      2 -> 1152
      else -> if (version == 3) 1152 else 576
    }
    return samples * 1_000_000L / rate
  }

  private class BitReader(private val data: ByteArray) {
    private var pos = 0
    fun read(count: Int): Int {
      var value = 0
      repeat(count) {
        val byte = data[pos shr 3].toInt() and 0xFF
        val bit = (byte shr (7 - (pos and 7))) and 1
        value = (value shl 1) or bit
        pos++
      }
      return value
    }
  }

  private fun outputFor(mime: String): Pair<OutputFormat?, UnsupportedReason?> = when (mime) {
    MediaFormat.MIMETYPE_AUDIO_AAC -> OutputFormat.M4A to null
    MediaFormat.MIMETYPE_AUDIO_MPEG -> OutputFormat.MP3 to null
    MediaFormat.MIMETYPE_AUDIO_OPUS ->
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) OutputFormat.OGG to null
      else null to UnsupportedReason.NEEDS_ANDROID_10
    else -> null to UnsupportedReason.FORMAT
  }

  fun codecLabel(mime: String): String = when (mime) {
    MediaFormat.MIMETYPE_AUDIO_AAC -> "AAC"
    MediaFormat.MIMETYPE_AUDIO_MPEG -> "MP3"
    MediaFormat.MIMETYPE_AUDIO_OPUS -> "Opus"
    MediaFormat.MIMETYPE_AUDIO_VORBIS -> "Vorbis"
    MediaFormat.MIMETYPE_AUDIO_FLAC -> "FLAC"
    MediaFormat.MIMETYPE_AUDIO_AC3 -> "AC3"
    MediaFormat.MIMETYPE_AUDIO_EAC3 -> "E-AC3"
    "audio/vnd.dts", "audio/vnd.dts.hd" -> "DTS"
    "audio/true-hd" -> "TrueHD"
    MediaFormat.MIMETYPE_AUDIO_RAW -> "PCM"
    else -> mime.removePrefix("audio/").uppercase()
  }

  private fun MediaFormat.stringOrNull(key: String): String? =
    if (containsKey(key)) runCatching { getString(key) }.getOrNull() else null

  private fun MediaFormat.intOrZero(key: String): Int =
    if (containsKey(key)) runCatching { getInteger(key) }.getOrDefault(0) else 0
}
