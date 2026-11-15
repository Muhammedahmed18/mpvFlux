package app.marlboroadvance.mpvex.ui.player

import android.util.Log
import android.view.KeyEvent
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import `is`.xyz.mpv.MPVLib

/**
 * Single place that decides what a hardware media button does (wired headset, Bluetooth
 * earbuds, car kits, keyboards). Used by both [MediaPlaybackService] (buttons routed through
 * the MediaSession) and [PlayerActivity] (key events delivered to the focused player window),
 * so a button behaves the same whether the player is on screen or playing in the background.
 *
 * On-screen controls (the notification buttons, lock screen and system media controls) are
 * not hardware buttons and are not affected by these settings.
 */
object MediaButtonHandler {
  private const val TAG = "MediaButtonHandler"

  /** How the caller performs playback actions (the player screen also manages audio focus). */
  interface Actions {
    fun togglePause()

    fun setPaused(paused: Boolean)

    fun seekBy(seconds: Int)

    fun stop()
  }

  private enum class Button { Previous, Play, Next }

  /**
   * Whether [keyCode] is a media key this handler is responsible for. The player screen
   * consumes these (down and up) instead of passing them to mpv's own key bindings.
   */
  fun isMediaKey(keyCode: Int): Boolean =
    when (keyCode) {
      KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
      KeyEvent.KEYCODE_MEDIA_PLAY,
      KeyEvent.KEYCODE_MEDIA_PAUSE,
      KeyEvent.KEYCODE_HEADSETHOOK,
      KeyEvent.KEYCODE_MEDIA_NEXT,
      KeyEvent.KEYCODE_MEDIA_PREVIOUS,
      KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
      KeyEvent.KEYCODE_MEDIA_REWIND,
      KeyEvent.KEYCODE_MEDIA_STOP,
      -> true
      else -> false
    }

  /**
   * Handles one hardware media key event. Returns true when the event was consumed, which is
   * always the case for media keys: when hardware buttons are turned off they are swallowed,
   * so they can neither control playback nor fall through to mpv's default bindings.
   */
  fun handle(
    event: KeyEvent,
    gesturePreferences: GesturePreferences,
    actions: Actions,
  ): Boolean {
    if (!isMediaKey(event.keyCode)) return false

    if (!gesturePreferences.mediaButtonsEnabled.get()) {
      if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
        Log.d(TAG, "Ignored ${KeyEvent.keyCodeToString(event.keyCode)}: hardware media buttons are off")
      }
      return true
    }

    // Act once per press. Key-up and auto-repeat (button held down) are consumed silently.
    if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount > 0) return true
    if (!MpvSession.isAlive) return true

    val seekStep = gesturePreferences.doubleTapToSeekDuration.get()

    when (event.keyCode) {
      KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
      KeyEvent.KEYCODE_HEADSETHOOK,
      -> runAction(Button.Play, gesturePreferences.mediaPlayGesture.get(), seekStep, actions)

      // Explicit play / pause keys (sent by some car kits and keyboards): honour the
      // requested state instead of toggling, unless the Play/Pause button was remapped.
      KeyEvent.KEYCODE_MEDIA_PLAY,
      KeyEvent.KEYCODE_MEDIA_PAUSE,
      -> {
        val gesture = gesturePreferences.mediaPlayGesture.get()
        if (gesture == SingleActionGesture.PlayPause) {
          actions.setPaused(event.keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE)
        } else {
          runAction(Button.Play, gesture, seekStep, actions)
        }
      }

      KeyEvent.KEYCODE_MEDIA_NEXT ->
        runAction(Button.Next, gesturePreferences.mediaNextGesture.get(), seekStep, actions)

      KeyEvent.KEYCODE_MEDIA_PREVIOUS ->
        runAction(Button.Previous, gesturePreferences.mediaPreviousGesture.get(), seekStep, actions)

      KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> actions.seekBy(seekStep)

      KeyEvent.KEYCODE_MEDIA_REWIND -> actions.seekBy(-seekStep)

      KeyEvent.KEYCODE_MEDIA_STOP -> actions.stop()
    }
    return true
  }

  private fun runAction(
    button: Button,
    gesture: SingleActionGesture,
    seekStep: Int,
    actions: Actions,
  ) {
    when (gesture) {
      SingleActionGesture.None -> Unit

      SingleActionGesture.PlayPause -> actions.togglePause()

      SingleActionGesture.Seek ->
        when (button) {
          Button.Previous -> actions.seekBy(-seekStep)
          Button.Next -> actions.seekBy(seekStep)
          // "Seek" has no direction on the play button; treat it as play/pause so the
          // button never silently stops working.
          Button.Play -> actions.togglePause()
        }

      SingleActionGesture.Custom -> {
        val key =
          when (button) {
            Button.Previous -> CustomKeyCodes.MediaPrevious
            Button.Play -> CustomKeyCodes.MediaPlay
            Button.Next -> CustomKeyCodes.MediaNext
          }
        // Fires the input.conf binding for PREV / PLAYPAUSE / NEXT.
        MPVLib.command("keypress", key.keyCode)
      }
    }
  }

  // ==================== Direct mpv helpers (used by the background service) ====================

  fun togglePause() {
    if (!MpvSession.isAlive) return
    val paused = MPVLib.getPropertyBoolean("pause") == true
    MPVLib.setPropertyBoolean("pause", !paused)
  }

  fun setPaused(paused: Boolean) {
    if (!MpvSession.isAlive) return
    MPVLib.setPropertyBoolean("pause", paused)
  }

  /** Relative seek; precise for clips under 2 minutes or when precise seeking is enabled. */
  fun seek(
    seconds: Int,
    playerPreferences: PlayerPreferences,
  ) {
    if (!MpvSession.isAlive || seconds == 0) return
    val duration = MPVLib.getPropertyInt("duration") ?: 0
    val precise = playerPreferences.usePreciseSeeking.get() || duration < 120
    val mode = if (precise) "relative+exact" else "relative+keyframes"
    MPVLib.command("seek", seconds.toString(), mode)
  }
}
