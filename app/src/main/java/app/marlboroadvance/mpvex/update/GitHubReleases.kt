package app.marlboroadvance.mpvex.update

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** A GitHub stable release. */
data class ReleaseInfo(
  val version: AppVersion,
  val tag: String,
  val notes: String,
  val htmlUrl: String,
)

/** Why a check failed. */
class UpdateCheckException(val reason: UpdateError, cause: Throwable? = null) : Exception(reason.name, cause)

/**
 * Remembers GitHub's ETag and the last response body, so an unchanged release list can be
 * answered with a tiny "304 Not modified" reply.
 */
interface ReleaseResponseCache {
  fun get(url: String): Pair<String, String>?

  fun put(url: String, etag: String, body: String)
}

/**
 * Reads the latest stable release from GitHub (no account or token needed).
 *
 * First tries GitHub's public API, which gives full details (notes) but only
 * allows 60 requests an hour per public IP address. Many internet providers put thousands of
 * customers behind one address, so that limit can be used up by other people. When the API
 * refuses with 403/429, [fetchLatest] falls back to the normal github.com release pages,
 * which don't have that limit. Draft and pre-releases are never returned.
 */
class GitHubReleases(
  private val client: OkHttpClient,
  private val cache: ReleaseResponseCache? = null,
  private val owner: String = OWNER,
  private val repo: String = REPO,
) {

  private val json = Json { ignoreUnknownKeys = true }

  /**
   * Newest stable release, or null if there is none.
   */
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
    // /latest skips drafts and pre-releases on GitHub's side.
    val release = get("https://api.github.com/repos/$owner/$repo/releases/latest")
      .let { json.decodeFromString<ReleaseDto>(it) }

    return if (!release.draft && !release.prerelease) release.toReleaseInfo() else null
  }

  private fun get(url: String): String {
    val cached = cache?.get(url)
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
        response.header("ETag")?.let { etag -> cache?.put(url, etag, body) }
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
  // github.com release pages (fallback when the API says "too many requests")
  // ---------------------------------------------------------------------------

  /**
   * Finds the newest stable release without the API:
   * 1. github.com/<owner>/<repo>/releases/latest redirects to .../releases/tag/<tag>.
   */
  private fun fetchFromWebsite(): ReleaseInfo? {
    val base = "https://github.com/$owner/$repo/releases"

    val (tag, releaseUrl) = getPage("$base/latest") { response, _ ->
      // After the redirect: /<owner>/<repo>/releases/tag/<tag>. Without any release GitHub
      // redirects to /releases instead, which has no "tag" part.
      val segments = response.request.url.pathSegments
      val tagIndex = segments.indexOf("tag")
      if (tagIndex < 0 || tagIndex == segments.lastIndex) return null
      segments.drop(tagIndex + 1).joinToString("/") to response.request.url.toString()
    } ?: return null
    val version = AppVersion.parse(tag) ?: return null

    // Notes are not reliably available from the fallback HTML without heavier parsing;
    // leave them empty. The HTML URL is still useful.
    return ReleaseInfo(
      version = version,
      tag = tag,
      notes = "",
      htmlUrl = releaseUrl,
    )
  }

  private inline fun <T> getPage(url: String, read: (okhttp3.Response, String) -> T?): T? {
    val request = Request.Builder()
      .url(url)
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
        return read(response, response.body.string())
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
    val draft: Boolean = false,
    val prerelease: Boolean = false,
  )

  companion object {
    const val OWNER = "Muhammedahmed18"
    const val REPO = "mpvFlux"

    private const val TAG = "GitHubReleases"
    private const val USER_AGENT = "MpvFlux-Updater"
  }
}
