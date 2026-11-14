package app.marlboroadvance.mpvex.update

import android.os.Build
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
 * Reads releases from GitHub's public API (no account or token needed; GitHub allows
 * 60 requests an hour per network). Draft releases are never visible here, so users only
 * see a release after it is published.
 */
/**
 * Remembers GitHub's ETag and the last response body, so an unchanged release list costs a
 * tiny "304 Not modified" reply that GitHub doesn't count against its rate limit.
 */
interface ReleaseResponseCache {
  fun get(url: String): Pair<String, String>?

  fun put(url: String, etag: String, body: String)
}

class GitHubReleases(
  private val client: OkHttpClient,
  private val cache: ReleaseResponseCache? = null,
  private val owner: String = OWNER,
  private val repo: String = REPO,
) {

  private val json = Json { ignoreUnknownKeys = true }

  /**
   * Newest release with an APK for this phone, or null if there is none.
   * [includePreReleases] also considers pre-releases (tags like v1.5.0-beta1).
   */
  @Throws(UpdateCheckException::class)
  fun fetchLatest(includePreReleases: Boolean): ReleaseInfo? {
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
      .header("User-Agent", "MpvFlux-Updater")
      .apply { cached?.let { header("If-None-Match", it.first) } }
      .build()
    try {
      client.newCall(request).execute().use { response ->
        // Nothing changed since the last check: reuse the saved response.
        if (response.code == 304 && cached != null) return cached.second
        when {
          response.code == 403 || response.code == 429 -> throw UpdateCheckException(UpdateError.RateLimited)
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
    val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
    // Prefer the phone's own ABI (in the phone's order of preference), then universal.
    val candidates = Build.SUPPORTED_ABIS.toList() + "universal"
    for (abi in candidates) {
      val asset = apks.firstOrNull { it.name.contains("-$abi-", ignoreCase = true) } ?: continue
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
  }
}
