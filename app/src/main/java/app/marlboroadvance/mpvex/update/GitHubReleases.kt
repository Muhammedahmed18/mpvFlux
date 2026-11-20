package app.marlboroadvance.mpvex.update

import android.os.Build
import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** A GitHub release that has an APK for this phone. */
data class ReleaseInfo(
  val version: AppVersion,
  val tag: String,
  val notes: String,
  val htmlUrl: String,
  val isPreRelease: Boolean,
  val apkName: String,
  val apkUrl: String,
  val apkSize: Long,
  /** Lower-case hex SHA-256 of the APK from GitHub, or null if GitHub didn't provide one. */
  val apkSha256: String?,
  /** ABI of the chosen APK ("arm64-v8a", … or "universal"). */
  val abi: String,
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
 * Reads releases from GitHub (no account or token needed).
 *
 * First tries GitHub's public API, which gives full details (notes, size, SHA-256) but only
 * allows 60 requests an hour per public IP address. Many internet providers put thousands of
 * customers behind one address, so that limit can be used up by other people. When the API
 * refuses with 403/429, [fetchLatest] falls back to the normal github.com release pages,
 * which don't have that limit. Draft releases are never visible either way.
 */
class GitHubReleases(
  private val client: OkHttpClient,
  private val cache: ReleaseResponseCache? = null,
  private val owner: String = OWNER,
  private val repo: String = REPO,
) {

  private val json = Json { ignoreUnknownKeys = true }

  /**
   * Newest release with an APK for this phone, or null if there is none.
   * [includePreReleases] also considers pre-releases (tags like v1.5.0-beta1); if the API is
   * unavailable, the fallback only knows the newest stable release.
   */
  @Throws(UpdateCheckException::class)
  fun fetchLatest(includePreReleases: Boolean): ReleaseInfo? =
    try {
      fetchFromApi(includePreReleases)
    } catch (e: UpdateCheckException) {
      if (e.reason != UpdateError.RateLimited) throw e
      Log.i(TAG, "GitHub API refused the request, using the release page instead")
      fetchFromWebsite()
    }

  // ---------------------------------------------------------------------------
  // GitHub API (preferred)
  // ---------------------------------------------------------------------------

  private fun fetchFromApi(includePreReleases: Boolean): ReleaseInfo? {
    val releases = if (includePreReleases) {
      get("https://api.github.com/repos/$owner/$repo/releases?per_page=15")
        .let { json.decodeFromString<List<ReleaseDto>>(it) }
    } else {
      // /latest skips drafts and pre-releases on GitHub's side.
      get("https://api.github.com/repos/$owner/$repo/releases/latest")
        .let { listOf(json.decodeFromString<ReleaseDto>(it)) }
    }

    return releases
      .asSequence()
      .filter { !it.draft && (includePreReleases || !it.prerelease) }
      .mapNotNull { it.toReleaseInfo() }
      .maxByOrNull { it.version }
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
    val (abi, asset) = pickForDevice(assets) { it.name } ?: return null
    return ReleaseInfo(
      version = version,
      tag = tagName,
      notes = body.orEmpty(),
      htmlUrl = htmlUrl,
      isPreRelease = prerelease,
      apkName = asset.name,
      apkUrl = asset.downloadUrl,
      apkSize = asset.size,
      apkSha256 = asset.digest
        ?.takeIf { it.startsWith("sha256:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.lowercase(),
      abi = abi,
    )
  }

  // ---------------------------------------------------------------------------
  // github.com release pages (fallback when the API says "too many requests")
  // ---------------------------------------------------------------------------

  /**
   * Finds the newest stable release without the API:
   * 1. github.com/<owner>/<repo>/releases/latest redirects to .../releases/tag/<tag>.
   * 2. .../releases/expanded_assets/<tag> is the small piece of HTML with the download links.
   *
   * Notes, size and SHA-256 aren't available this way; the download is still checked
   * against the installed app's signing key before it can be installed.
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
    }
    val version = AppVersion.parse(tag) ?: return null

    val assetsUrl = "$base/expanded_assets".toHttpUrl().newBuilder()
      .addPathSegments(tag)
      .build()
      .toString()
    val links = getPage(assetsUrl) { _, html ->
      APK_LINK.findAll(html).map { it.groupValues[1].replace("&amp;", "&") }.distinct().toList()
    }

    val (abi, link) = pickForDevice(links) { it.substringAfterLast('/') } ?: return null
    return ReleaseInfo(
      version = version,
      tag = tag,
      notes = "",
      htmlUrl = releaseUrl,
      isPreRelease = false,
      apkName = link.substringAfterLast('/'),
      apkUrl = if (link.startsWith("http")) link else "https://github.com$link",
      apkSize = 0,
      apkSha256 = null,
      abi = abi,
    )
  }

  private inline fun <T> getPage(url: String, read: (okhttp3.Response, String) -> T): T {
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

  // ---------------------------------------------------------------------------
  // Shared
  // ---------------------------------------------------------------------------

  /**
   * Picks the APK for this phone: its own ABI first (in the phone's order of preference),
   * then universal. Returns the ABI and the chosen item, or null if nothing fits.
   */
  private fun <T> pickForDevice(items: List<T>, name: (T) -> String): Pair<String, T>? {
    val apks = items.filter { name(it).endsWith(".apk", ignoreCase = true) }
    val candidates = Build.SUPPORTED_ABIS.toList() + "universal"
    for (abi in candidates) {
      val match = apks.firstOrNull { name(it).contains("-$abi-", ignoreCase = true) } ?: continue
      return abi to match
    }
    return null
  }

  @Serializable
  private data class ReleaseDto(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String = "",
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<AssetDto> = emptyList(),
  )

  @Serializable
  private data class AssetDto(
    val name: String,
    val size: Long = 0,
    @SerialName("browser_download_url") val downloadUrl: String,
    /** "sha256:<hex>", added by GitHub for release assets. */
    val digest: String? = null,
  )

  companion object {
    const val OWNER = "Muhammedahmed18"
    const val REPO = "mpvFlux"

    private const val TAG = "GitHubReleases"
    private const val USER_AGENT = "MpvFlux-Updater"

    /** Download links of APKs in GitHub's assets HTML. */
    private val APK_LINK = Regex(
      """href="([^"]*/releases/download/[^"]+\.apk)"""",
      RegexOption.IGNORE_CASE,
    )
  }
}
