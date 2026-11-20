package app.marlboroadvance.mpvex.preferences

import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore

/** Settings and bookkeeping for the GitHub release check. */
class UpdatePreferences(
  preferenceStore: PreferenceStore,
) {
  /** Check GitHub once per app launch when the app opens. */
  val autoCheck = preferenceStore.getBoolean("update_auto_check", true)

  /** Time of the last successful check, in milliseconds. */
  val lastCheckTime = preferenceStore.getLong("update_last_check_time", 0L)

  /** Newest version found by the last check that is newer than the installed one, or "". */
  val availableVersion = preferenceStore.getString("update_available_version", "")

  /** GitHub ETag and body of the last "latest release" reply (see ReleaseResponseCache). */
  val releaseEtag = preferenceStore.getString("update_latest_release_etag", "")
  val releaseBody = preferenceStore.getString("update_latest_release_body", "")
}
