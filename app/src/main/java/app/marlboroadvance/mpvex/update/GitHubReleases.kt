package app.marlboroadvance.mpvex.update

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** The newest stable GitHub release. Only information to show; nothing here is downloaded. */
data class ReleaseInfo(
  val version: AppVersion,
  val tag: String,
  /** Release notes in Markdown; empty when the GitHub API wasn't available. */
  val notes: String,
  /** Page of the release on github.com, opened in the user's browser. */
  val htmlUrl: String,
)

/** Why a check failed. */
class UpdateCheckException(val reason: UpdateError, cause: Throwable? = null) : Exception(reason.name, cause)

/**
 * Remembers GitHub's ETag and the last response body. GitHub answers a request that sends the
 * ETag back with a tiny "304 Not modified" when nothing changed, and such replies don't count
 * toward the API's hourly limit.
 */
interface ReleaseResponseCache {
  /** The saved (ETag, body) pair, or null if nothing is saved. */
  fun get(): Pair<String, String>?

  fun put(etag: String, body: String)
}

/**
 * Reads the latest stable release from GitHub (no account or token needed).
 *
 * First tries GitHub's public API (`/releases/latest`, which never returns drafts or
 * pre-releases), which gives the release notes but only allows 60 requests an hour per public
 * IP address. Many internet providers put thousands of customers behind one address, so that
 * limit can be used up by other people. Checks that find nothing new use [cache] and cost
 * nothing against the limit. When the API still refuses with 403/429, [fetchLatest] falls back
 * to the normal github.com `/releases/latest` page, which doesn't have that limit but only
 * tells the tag (no notes).
 */
class GitHubReleases(
  private val client: OkHttpClient,
  private val cache: ReleaseResponseCache? = null,
  private val owner: String = OWNER,
  private val repo: String = REPO,
) {

  private val json = Json { ignoreUnknownKeys = true }

  /** Newest stable release, or null if there is none or its tag isn't a version number. */
  @Throws(UpdateCheckException::class)
  fun fetchLatest(): ReleaseInfo? =
    try {
      fetchFromApi()
    } catch (e: UpdateCheckException) {
      if (e.reason != UpdateError.RateLimited) throw e
      Log.i(TAG, "GitHub API refused the request, using the release page instead")
      fetchFromWebsite()
    }

  // ---------------------------------------------------------------------------
  // GitHub API (preferred)
  // ---------------------------------------------------------------------------

  private fun fetchFromApi(): ReleaseInfo? {
    val body = get("https://api.github.com/repos/$owner/$repo/releases/latest")
    return try {
      json.decodeFromString<ReleaseDto>(body).toReleaseInfo()
    } catch (e: kotlinx.serialization.SerializationException) {
      throw UpdateCheckException(UpdateError.Unknown, e)
    }
  }

  private fun get(url: String): String {
    val cached = cache?.get()
    val request = Request.Builder()
      .url(url)
      .header("Accept", "application/vnd.github+json")
      .header("X-GitHub-Api-Version", "2022-11-28")
      .header("User-Agent", USER_AGENT)
      .apply { cached?.let { header("If-None-Match", it.first) } }
      .build()
    try {
      client.newCall(request).execute().use { response ->
        // Nothing changed since the last check: reuse the saved response.
        if (response.code == 304 && cached != null) return cached.second
        when {
          response.code == 403 || response.code == 429 -> {
            Log.w(
              TAG,
              "GitHub API answered ${response.code}, " +
                "remaining=${response.header("x-ratelimit-remaining")}, " +
                "reset=${response.header("x-ratelimit-reset")}",
            )
            throw UpdateCheckException(UpdateError.RateLimited)
          }
          response.code == 404 -> throw UpdateCheckException(UpdateError.NoRelease)
          !response.isSuccessful -> throw UpdateCheckException(UpdateError.Network)
        }
        val body = response.body.string()
        response.header("ETag")?.let { etag -> cache?.put(etag, body) }
        return body
      }
    } catch (e: UpdateCheckException) {
      throw e
    } catch (e: IOException) {
      throw UpdateCheckException(UpdateError.Network, e)
    } catch (e: Exception) {
      throw UpdateCheckException(UpdateError.Unknown, e)
    }
  }

  private fun ReleaseDto.toReleaseInfo(): ReleaseInfo? {
    val version = AppVersion.parse(tagName) ?: return null
    return ReleaseInfo(
      version = version,
      tag = tagName,
      notes = body.orEmpty(),
      htmlUrl = htmlUrl,
    )
  }

  // ---------------------------------------------------------------------------
  // github.com release page (fallback when the API says "too many requests")
  // ---------------------------------------------------------------------------

  /**
   * Finds the newest stable release without the API: github.com/<owner>/<repo>/releases/latest
   * redirects to .../releases/tag/<tag>, so the tag can be read from where the request ends up.
   * Release notes aren't available this way.
   */
  private fun fetchFromWebsite(): ReleaseInfo? {
    val request = Request.Builder()
      .url("https://github.com/$owner/$repo/releases/latest")
      .header("Accept", "text/html")
      .header("User-Agent", USER_AGENT)
      .build()
    try {
      client.newCall(request).execute().use { response ->
        when {
          response.code == 429 -> throw UpdateCheckException(UpdateError.RateLimited)
          response.code == 404 -> throw UpdateCheckException(UpdateError.NoRelease)
          !response.isSuccessful -> throw UpdateCheckException(UpdateError.Network)
        }
        // After the redirect: /<owner>/<repo>/releases/tag/<tag>. Without any release GitHub
        // redirects to /releases instead, which has no "tag" part.
        val segments = response.request.url.pathSegments
        val tagIndex = segments.indexOf("tag")
        if (tagIndex < 0 || tagIndex == segments.lastIndex) return null
        val tag = segments.drop(tagIndex + 1).joinToString("/")
        val version = AppVersion.parse(tag) ?: return null
        return ReleaseInfo(
          version = version,
          tag = tag,
          notes = "",
          htmlUrl = response.request.url.toString(),
        )
      }
    } catch (e: UpdateCheckException) {
      throw e
    } catch (e: IOException) {
      throw UpdateCheckException(UpdateError.Network, e)
    } catch (e: Exception) {
      throw UpdateCheckException(UpdateError.Unknown, e)
    }
  }

  @Serializable
  private data class ReleaseDto(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String = "",
    val body: String? = null,
  )

  companion object {
    const val OWNER = "Muhammedahmed18"
    const val REPO = "mpvFlux"

    private const val TAG = "GitHubReleases"
    private const val USER_AGENT = "MpvFlux-Updater"
  }
}
