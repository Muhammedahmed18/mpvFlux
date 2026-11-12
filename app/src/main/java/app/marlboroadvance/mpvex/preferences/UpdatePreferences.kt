package app.marlboroadvance.mpvex.preferences

import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore

/** Settings and bookkeeping for the in-app updater (GitHub releases). */
class UpdatePreferences(
  preferenceStore: PreferenceStore,
) {
  /** Check GitHub at most once a day when the app opens. */
  val autoCheck = preferenceStore.getBoolean("update_auto_check", true)

  /** Also offer pre-releases (tags like v1.5.0-beta1). */
  val includePrereleases = preferenceStore.getBoolean("update_include_prereleases", false)

  /** Time of the last successful check, in milliseconds. */
  val lastCheckTime = preferenceStore.getLong("update_last_check_time", 0L)

  /** Newest version found by the last check that is newer than the installed one, or "". */
  val availableVersion = preferenceStore.getString("update_available_version", "")

  /** Version the user chose to skip, e.g. "1.4.5". */
  val skippedVersion = preferenceStore.getString("update_skipped_version", "")

  /** Version whose "update available" popup was already shown once. */
  val notifiedVersion = preferenceStore.getString("update_notified_version", "")

  /** App version that last ran; used to show "What's new" once after an update. */
  val lastRunVersion = preferenceStore.getString("update_last_run_version", "")

  /** Release notes of the version being installed, shown as "What's new" after the update. */
  val pendingNotesVersion = preferenceStore.getString("update_pending_notes_version", "")
  val pendingNotes = preferenceStore.getString("update_pending_notes", "")
}
