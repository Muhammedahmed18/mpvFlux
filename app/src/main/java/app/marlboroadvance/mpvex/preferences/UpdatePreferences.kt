package app.marlboroadvance.mpvex.preferences

import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore

/** Settings and bookkeeping for the in-app update checker (GitHub stable releases). */
class UpdatePreferences(
  preferenceStore: PreferenceStore,
) {
  /** Check GitHub at most once a day when the app opens. */
  val autoCheck = preferenceStore.getBoolean("update_auto_check", true)

  /** Time of the last successful check, in milliseconds. */
  val lastCheckTime = preferenceStore.getLong("update_last_check_time", 0L)

  /** Newest version found by the last check that is newer than the installed one, or "". */
  val availableVersion = preferenceStore.getString("update_available_version", "")

  /** GitHub ETag, request URL and body of the last release check (see ReleaseResponseCache). */
  val releasesEtag = preferenceStore.getString("update_releases_etag", "")
  val releasesUrl = preferenceStore.getString("update_releases_url", "")
  val releasesBody = preferenceStore.getString("update_releases_body", "")
}
