package app.marlboroadvance.mpvex.ui.player

import android.content.Context
import android.os.Environment
import android.util.AttributeSet

import android.view.KeyCharacterMap
import android.view.KeyEvent
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.ui.player.PlayerActivity.Companion.TAG
import app.marlboroadvance.mpvex.ui.player.controls.components.panels.toColorHexString
import `is`.xyz.mpv.BaseMPVView
import `is`.xyz.mpv.keyMapping
import `is`.xyz.mpv.MPVLib
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KProperty

class MPVView(
  context: Context,
  attributes: AttributeSet,
) : BaseMPVView(context, attributes),
  KoinComponent {
  private val audioPreferences: AudioPreferences by inject()
  private val playerPreferences: PlayerPreferences by inject()
  private val decoderPreferences: DecoderPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()

  var isExiting = false

  fun getVideoOutAspect(): Double? {
    // Try to get aspect from video-params/aspect first
    val rawAspect = MPVLib.getPropertyDouble("video-params/aspect")
    val rotate = MPVLib.getPropertyInt("video-params/rotate") ?: 0

    // If aspect is not available or 0, calculate from width and height
    val finalAspect = if (rawAspect == null || rawAspect < 0.001) {
      val width = runCatching {
        MPVLib.getPropertyInt("width") ?: MPVLib.getPropertyInt("video-params/w") ?: 0
      }.getOrDefault(0)

      val height = runCatching {
        MPVLib.getPropertyInt("height") ?: MPVLib.getPropertyInt("video-params/h") ?: 0
      }.getOrDefault(0)

      if (width > 0 && height > 0) {
        width.toDouble() / height.toDouble()
      } else {
        null
      }
    } else {
      rawAspect
    }

    return finalAspect?.let { aspect ->
      if (aspect <= 0.001) {
        return null
      }
      val isRotated = (rotate % 180 == 90)
      val correctedAspect = if (isRotated) 1.0 / aspect else aspect
      correctedAspect
    }
  }

  class TrackDelegate(
    private val name: String,
  ) {
    operator fun getValue(
      thisRef: Any?,
      property: KProperty<*>,
    ): Int {
      val v = MPVLib.getPropertyString(name)
      // we can get null here for "no" or other invalid value
      return v?.toIntOrNull() ?: -1
    }

    operator fun setValue(
      thisRef: Any?,
      property: KProperty<*>,
      value: Int,
    ) {
      if (value == -1) MPVLib.setPropertyString(name, "no") else MPVLib.setPropertyInt(name, value)
    }
  }

  var sid: Int by TrackDelegate("sid")
  var secondarySid: Int by TrackDelegate("secondary-sid")
  var aid: Int by TrackDelegate("aid")

  override fun initOptions() {
    val profile = decoderPreferences.profile.get()
    MPVLib.setOptionString("profile", profile)
    setVo(if (decoderPreferences.gpuNext.get()) "gpu-next" else "gpu")
    
    // Set GPU API context (Vulkan or OpenGL)
    if (decoderPreferences.useVulkan.get()) {
      MPVLib.setOptionString("gpu-context", "androidvk")
    }

    // Set hwdec with fallback order: HW+ (mediacodec) -> HW (mediacodec-copy) -> SW (no)
    MPVLib.setOptionString(
      "hwdec",
      if (decoderPreferences.tryHWDecoding.get()) "mediacodec,mediacodec-copy,no" else "no",
    )
    MPVLib.setOptionString("hwdec-codecs", "all")

    if (decoderPreferences.useYUV420P.get()) {
      MPVLib.setOptionString("vf", "format=yuv420p")
    }
    
    // Cap the demuxer cache for mobile. It only fills for network streams (local files don't
    // use the cache), where forward + back buffers together used to allow 128 MB of native
    // memory on every device (the old O_MR1 check was always true with minSdk 26). Low-RAM
    // devices get half, and the back buffer (only used for seeking backwards inside a
    // stream) is half of the forward one.
    val isLowRam = runCatching {
      (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).isLowRamDevice
    }.getOrDefault(false)
    val cacheMegs = if (isLowRam) 32 else 64
    MPVLib.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
    MPVLib.setOptionString("demuxer-max-back-bytes", "${cacheMegs / 2 * 1024 * 1024}")
    
    val logLevel = if (advancedPreferences.verboseLogging.get()) "v" else "warn"
    MPVLib.setOptionString("msg-level", "all=$logLevel")

    MPVLib.setPropertyBoolean("keep-open", true)
    MPVLib.setPropertyBoolean("input-default-bindings", true)

    MPVLib.setOptionString("tls-verify", "yes")
    MPVLib.setOptionString("tls-ca-file", "${context.filesDir.path}/cacert.pem")

    val screenshotDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    screenshotDir.mkdirs()
    MPVLib.setOptionString("screenshot-directory", screenshotDir.path)

    VideoFilters.entries.forEach {
      MPVLib.setOptionString(it.mpvProperty, it.preference(decoderPreferences).get().toString())
    }

    MPVLib.setOptionString("speed", playerPreferences.defaultSpeed.get().toString())
    // "auto" lets the GPU apply AV1 film grain when the renderer supports it. Forcing "cpu"
    // added per-frame CPU work on top of software AV1 decoding (no AV1 hardware decoder).
    MPVLib.setOptionString("vd-lavc-film-grain", "auto")

    val preciseSeek = playerPreferences.usePreciseSeeking.get()
    MPVLib.setOptionString("hr-seek", if (preciseSeek) "yes" else "no")
    MPVLib.setOptionString("hr-seek-framedrop", if (preciseSeek) "no" else "yes")

    setupSubtitlesOptions()
    setupAudioOptions()
  }

  override fun observeProperties() {
    for ((name, format) in observedProps) MPVLib.observeProperty(name, format)
  }

  override fun postInitOptions() {
    when (decoderPreferences.debanding.get()) {
      Debanding.None -> {}
      Debanding.CPU -> MPVLib.command("vf", "add", "@deband:gradfun=radius=12")
      Debanding.GPU -> MPVLib.setOptionString("deband", "yes")
    }

    advancedPreferences.enabledStatisticsPage.get().let {
      if (it != 0) {
        MPVLib.command("script-binding", "stats/display-stats-toggle")
        MPVLib.command("script-binding", "stats/display-page-$it")
      }
    }
  }

  @Suppress("ReturnCount", "DEPRECATION")
  fun onKey(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_MULTIPLE || KeyEvent.isModifierKey(event.keyCode)) {
      return false
    }

    var mapped = keyMapping[event.keyCode]
    if (mapped == null) {
      // Fallback to produced glyph
      if (!event.isPrintingKey) {
        return false
      }

      val ch = event.unicodeChar
      if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
        return false // dead key
      }
      mapped = ch.toChar().toString()
    }

    if (event.repeatCount > 0) {
      return true
    }

    val mod: MutableList<String> = mutableListOf()
    event.isShiftPressed && mod.add("shift")
    event.isCtrlPressed && mod.add("ctrl")
    event.isAltPressed && mod.add("alt")
    event.isMetaPressed && mod.add("meta")

    val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
    mod.add(mapped)
    MPVLib.command(action, mod.joinToString("+"))

    return true
  }

  // "pause" and "paused-for-cache" are deliberately NOT registered here. They are observed
  // through the shared MPVLib.propBoolean flows (PlayerViewModel, PlayerControls), which
  // register each property with mpv exactly once and re-register it for every new mpv
  // instance (BaseMPVView.reobserveAllProperties). libmpv sends one change event per
  // registration, and MPVLib forwards every event to every EventObserver, so a second
  // registration here doubled every pause event (extra progress saves and notification
  // rebuilds). The Activity and the service still receive these events as before.
  private val observedProps =
    mapOf(
      "video-params/aspect" to MPVLib.MpvFormat.MPV_FORMAT_DOUBLE,
      "video-params/w" to MPVLib.MpvFormat.MPV_FORMAT_INT64,
      "video-params/h" to MPVLib.MpvFormat.MPV_FORMAT_INT64,
      "eof-reached" to MPVLib.MpvFormat.MPV_FORMAT_FLAG,
      "user-data/mpvex/show_text" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/toggle_ui" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/show_panel" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/set_button_title" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/reset_button_title" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/toggle_button" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_by" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_to" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_by_with_text" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_to_with_text" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/software_keyboard" to MPVLib.MpvFormat.MPV_FORMAT_STRING,
    )

  private fun setupAudioOptions() {
    // Disable MPV's automatic audio selection
    // App will handle track selection manually via TrackSelector to respect user choices
    MPVLib.setOptionString("alang", "")
    MPVLib.setOptionString("audio-delay", (audioPreferences.defaultAudioDelay.get() / 1000.0).toString())
    MPVLib.setOptionString("audio-pitch-correction", audioPreferences.audioPitchCorrection.get().toString())
    MPVLib.setOptionString("volume-max", (audioPreferences.volumeBoostCap.get() + 100).toString())
    
    // Volume normalization using dynamic audio normalization filter
    if (audioPreferences.volumeNormalization.get()) {
      MPVLib.setOptionString("af", "dynaudnorm")
    }
  }

  // Setup
  private fun setupSubtitlesOptions() {
    // Disable MPV's automatic subtitle selection
    // App will handle track selection manually via TrackSelector to respect user choices
    MPVLib.setOptionString("slang", "")
    MPVLib.setOptionString("sub-auto", "no")
    MPVLib.setOptionString("sub-file-paths", "")
    MPVLib.setOptionString("subs-fallback", "no")

    // Delay and speed for both primary and secondary
    val subDelay = (subtitlesPreferences.defaultSubDelay.get() / 1000.0).toString()
    val subSpeed = subtitlesPreferences.defaultSubSpeed.get().toString()
    MPVLib.setOptionString("sub-delay", subDelay)
    MPVLib.setOptionString("sub-speed", subSpeed)
    MPVLib.setOptionString("secondary-sub-delay", subDelay)
    MPVLib.setOptionString("secondary-sub-speed", subSpeed)

    val fontsDirPath = "${context.filesDir.path}/fonts/"
    MPVLib.setOptionString("sub-fonts-dir", fontsDirPath)
    
    val font = subtitlesPreferences.font.get()
    MPVLib.setOptionString("sub-font", font)
    MPVLib.setOptionString("secondary-sub-font", font)

    if (subtitlesPreferences.overrideAssSubs.get()) {
      MPVLib.setOptionString("sub-ass-override", "force")
      MPVLib.setOptionString("sub-ass-justify", "yes")
      MPVLib.setOptionString("secondary-sub-ass-override", "force")
    } else {
      MPVLib.setOptionString("sub-ass-override", "no")
      MPVLib.setOptionString("secondary-sub-ass-override", "no")
    }

    // Typography and styling for both primary and secondary
    val fontSize = subtitlesPreferences.fontSize.get().toString()
    val bold = if (subtitlesPreferences.bold.get()) "yes" else "no"
    val italic = if (subtitlesPreferences.italic.get()) "yes" else "no"
    val justify = subtitlesPreferences.justification.get().value
    val textColor = subtitlesPreferences.textColor.get().toColorHexString()
    val backgroundColor = subtitlesPreferences.backgroundColor.get().toColorHexString()
    val borderColor = subtitlesPreferences.borderColor.get().toColorHexString()
    val borderSize = subtitlesPreferences.borderSize.get().toString()
    val borderStyle = subtitlesPreferences.borderStyle.get().value
    val shadowOffset = subtitlesPreferences.shadowOffset.get().toString()
    val subPos = subtitlesPreferences.subPos.get().toString()
    val subScale = subtitlesPreferences.subScale.get().toString()

    MPVLib.setOptionString("sub-font-size", fontSize)
    MPVLib.setOptionString("sub-bold", bold)
    MPVLib.setOptionString("sub-italic", italic)
    MPVLib.setOptionString("sub-justify", justify)
    MPVLib.setOptionString("sub-color", textColor)
    MPVLib.setOptionString("sub-back-color", backgroundColor)
    MPVLib.setOptionString("sub-border-color", borderColor)
    MPVLib.setOptionString("sub-border-size", borderSize)
    MPVLib.setOptionString("sub-border-style", borderStyle)
    MPVLib.setOptionString("sub-shadow-offset", shadowOffset)
    MPVLib.setOptionString("sub-scale", subScale)
    MPVLib.setOptionString("sub-pos", subPos)
    
    MPVLib.setOptionString("secondary-sub-font-size", fontSize)
    MPVLib.setOptionString("secondary-sub-bold", bold)
    MPVLib.setOptionString("secondary-sub-italic", italic)
    MPVLib.setOptionString("secondary-sub-justify", justify)
    MPVLib.setOptionString("secondary-sub-color", textColor)
    MPVLib.setOptionString("secondary-sub-back-color", backgroundColor)
    MPVLib.setOptionString("secondary-sub-border-color", borderColor)
    MPVLib.setOptionString("secondary-sub-border-size", borderSize)
    MPVLib.setOptionString("secondary-sub-border-style", borderStyle)
    MPVLib.setOptionString("secondary-sub-shadow-offset", shadowOffset)
    MPVLib.setOptionString("secondary-sub-scale", subScale)
    // Position secondary subtitle at top (10) instead of bottom to avoid overlap with primary
    MPVLib.setOptionString("secondary-sub-pos", "10")

    val scaleByWindow = if (subtitlesPreferences.scaleByWindow.get()) "yes" else "no"
    MPVLib.setOptionString("sub-scale-by-window", scaleByWindow)
    MPVLib.setOptionString("sub-use-margins", scaleByWindow)
    MPVLib.setOptionString("secondary-sub-scale-by-window", scaleByWindow)
    MPVLib.setOptionString("secondary-sub-use-margins", scaleByWindow)
  }
}
