package app.marlboroadvance.mpvex.ui.player

import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode

/**
 * Forwards mpv events to [PlayerActivity] on the main thread.
 *
 * isExiting is checked again inside each posted runnable: an event can be queued just before
 * the player starts tearing down mpv, and its handler would then call into a destroyed mpv
 * instance (which makes the native layer kill the process).
 */
class PlayerObserver(
  private val activity: PlayerActivity,
) : MPVLib.EventObserver {
  override fun eventProperty(property: String) {
    if (activity.player.isExiting) return
    activity.runOnUiThread { if (!activity.player.isExiting) activity.onObserverEvent(property) }
  }

  override fun eventProperty(
    property: String,
    value: Long,
  ) {
    if (activity.player.isExiting) return
    activity.runOnUiThread { if (!activity.player.isExiting) activity.onObserverEvent(property, value) }
  }

  override fun eventProperty(
    property: String,
    value: Boolean,
  ) {
    if (activity.player.isExiting) return
    activity.runOnUiThread { if (!activity.player.isExiting) activity.onObserverEvent(property, value) }
  }

  override fun eventProperty(
    property: String,
    value: String,
  ) {
    if (activity.player.isExiting) return
    activity.runOnUiThread { if (!activity.player.isExiting) activity.onObserverEvent(property, value) }
  }

  override fun eventProperty(
    property: String,
    value: Double,
  ) {
    if (activity.player.isExiting) return
    activity.runOnUiThread { if (!activity.player.isExiting) activity.onObserverEvent(property, value) }
  }

  @Suppress("EmptyFunctionBlock")
  override fun eventProperty(
    property: String,
    value: MPVNode,
  ) {
    if (activity.player.isExiting) return
    activity.runOnUiThread { if (!activity.player.isExiting) activity.onObserverEvent(property, value) }
  }

  override fun event(eventId: Int, data: MPVNode) {
    if (activity.player.isExiting) return
    activity.runOnUiThread { if (!activity.player.isExiting) activity.event(eventId) }
  }
}
