package app.marlboroadvance.mpvex.ui.player.controls

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.core.VisibilityThreshold
import app.marlboroadvance.mpvex.ui.player.Decoder
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.Sheets
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.AspectRatioSheet
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.AudioTracksSheet
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.ChaptersSheet
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.DecodersSheet
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.FrameNavigationSheet
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.MoreSheet
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.OnlineSubtitleSearchSheet
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.PlaybackSpeedSheet
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.PlayerOverlayHost
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.PlaylistSheet
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.SubtitlesSheet
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.VideoZoomSheet
import app.marlboroadvance.mpvex.utils.media.MediaInfoParser
import dev.vivvvek.seeker.Segment
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.marlboroadvance.mpvex.preferences.preference.collectAsState as collectPreferenceAsState

@Composable
fun PlayerSheets(
  sheetShown: Sheets,
  viewModel: app.marlboroadvance.mpvex.ui.player.PlayerViewModel,
  // subtitles sheet
  subtitles: ImmutableList<TrackNode>,
  onAddSubtitle: (Uri) -> Unit,
  onToggleSubtitle: (Int) -> Unit,
  isSubtitleSelected: (Int) -> Boolean,
  onAddSubtitleFromStorage: (String) -> Unit = {},
  onRemoveSubtitle: (Int) -> Unit,
  // audio sheet
  audioTracks: ImmutableList<TrackNode>,
  onAddAudio: (Uri) -> Unit,
  onSelectAudio: (TrackNode) -> Unit,
  // chapters sheet
  chapter: Segment?,
  chapters: ImmutableList<Segment>,
  onSeekToChapter: (Int) -> Unit,
  // Decoders sheet
  decoder: Decoder,
  onUpdateDecoder: (Decoder) -> Unit,
  // Speed sheet
  speed: Float,
  speedPresets: List<Float>,
  onSpeedChange: (Float) -> Unit,
  onAddSpeedPreset: (Float) -> Unit,
  onRemoveSpeedPreset: (Float) -> Unit,
  onResetSpeedPresets: () -> Unit,
  onMakeDefaultSpeed: (Float) -> Unit,
  onResetDefaultSpeed: () -> Unit,
  // More sheet
  sleepTimerTimeRemaining: Int,
  onStartSleepTimer: (Int) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  onShowSheet: (Sheets) -> Unit,
  onDismissRequest: () -> Unit,
  // Panels share the sheet host so switching between them keeps one overlay and scrim.
  panelShown: Panels,
) {
  val scope = rememberCoroutineScope()

  val sheetBody: @Composable (Sheets) -> Unit = sheetBody@{ shown ->
    when (shown) {
      Sheets.None -> {}
      Sheets.SubtitleTracks -> {
        val subtitlesPicker =
          rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
          ) {
            if (it == null) return@rememberLauncherForActivityResult
            onAddSubtitle(it)
          }

        val subtitlesPreferences = koinInject<app.marlboroadvance.mpvex.preferences.SubtitlesPreferences>()
        val savedPickerPath = subtitlesPreferences.pickerPath.get()

        val currentMediaTitle = viewModel.currentMediaTitle
        val matchToName = if (currentMediaTitle.isNotBlank()) {
            // Remove extension if present to improve matching
            currentMediaTitle.substringBeforeLast(".")
        } else null

        var showFilePicker by remember { mutableStateOf(false) }

        if (showFilePicker) {
            app.marlboroadvance.mpvex.ui.browser.dialogs.FilePickerDialog(
                isOpen = true,
                currentPath = savedPickerPath ?: android.os.Environment.getExternalStorageDirectory().absolutePath,
                onDismiss = { showFilePicker = false },
                onPathChanged = { path ->
                    if (path != null) {
                        subtitlesPreferences.pickerPath.set(path)
                    }
                },
                onFileSelected = { path ->
                    showFilePicker = false
                     onAddSubtitle(Uri.parse("file://$path"))
                },
                onSystemPickerRequest = {
                    showFilePicker = false
                    subtitlesPicker.launch(
                      arrayOf(
                        "text/plain",
                        "text/srt",
                        "text/vtt",
                        "application/x-subrip",
                        "application/x-subtitle",
                        "text/x-ssa",
                        "*/*",
                      ),
                    )
                },
                matchToName = matchToName
            )
        }

        SubtitlesSheet(
          tracks = subtitles.toImmutableList(),
          onToggleSubtitle = onToggleSubtitle,
          isSubtitleSelected = isSubtitleSelected,
          onAddSubtitle = { showFilePicker = true },
          onRemoveSubtitle = onRemoveSubtitle,
          onOpenSubtitleSettings = { onOpenPanel(Panels.SubtitleSettings) },
          onOpenSubtitleDelay = { onOpenPanel(Panels.SubtitleDelay) },
          onOpenOnlineSearch = { onShowSheet(Sheets.OnlineSubtitleSearch) },
        )
      }

      Sheets.OnlineSubtitleSearch -> {
        val isSearching by viewModel.isSearchingSub.collectAsState()
        val isDownloading by viewModel.isDownloadingSub.collectAsState()
        val results by viewModel.wyzieSearchResults.collectAsState()
        val isOnlineSectionExpanded by viewModel.isOnlineSectionExpanded.collectAsState()

        // Media Search / Autocomplete
        val mediaResults by viewModel.mediaSearchResults.collectAsState()
        val isSearchingMedia by viewModel.isSearchingMedia.collectAsState()
      
        // TV Show / Seasons / Episodes
        val selectedTvShow by viewModel.selectedTvShow.collectAsState()
        val isFetchingTvDetails by viewModel.isFetchingTvDetails.collectAsState()
        val selectedSeason by viewModel.selectedSeason.collectAsState()
        val seasonEpisodes by viewModel.seasonEpisodes.collectAsState()
        val isFetchingEpisodes by viewModel.isFetchingEpisodes.collectAsState()
        val selectedEpisode by viewModel.selectedEpisode.collectAsState()

        OnlineSubtitleSearchSheet(
          onDownloadOnline = { viewModel.downloadSubtitle(it) },
          isSearching = isSearching,
          isDownloading = isDownloading,
          searchResults = results.toImmutableList(),
          isOnlineSectionExpanded = isOnlineSectionExpanded,
          onToggleOnlineSection = { viewModel.toggleOnlineSection() },
          mediaTitle = viewModel.currentMediaTitle,
          // Autocomplete & Series Selection
          mediaSearchResults = mediaResults.toImmutableList(),
          isSearchingMedia = isSearchingMedia,
          onSearchMedia = { query ->
            // Use clean title from query for TMDB search (strip S01E05 noise)
            val queryInfo = MediaInfoParser.parse(query)
            val searchTitle = queryInfo.title.ifBlank { query }
            viewModel.searchMedia(searchTitle)
          },
          onSelectMedia = { viewModel.selectMedia(it) },
          selectedTvShow = selectedTvShow,
          isFetchingTvDetails = isFetchingTvDetails,
          selectedSeason = selectedSeason,
          onSelectSeason = { viewModel.selectSeason(it) },
          seasonEpisodes = seasonEpisodes.toImmutableList(),
          isFetchingEpisodes = isFetchingEpisodes,
          selectedEpisode = selectedEpisode,
          onSelectEpisode = { viewModel.selectEpisode(it) },
          onClearMediaSelection = { viewModel.clearMediaSelection() },
        )
      }

      Sheets.AudioTracks -> {
        val audioPicker =
          rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
          ) {
            if (it == null) return@rememberLauncherForActivityResult
            onAddAudio(it)
          }
        AudioTracksSheet(
          tracks = audioTracks,
          onSelect = onSelectAudio,
          onAddAudioTrack = { audioPicker.launch(arrayOf("*/*")) },
          onOpenDelayPanel = { onOpenPanel(Panels.AudioDelay) },
        )
      }

      Sheets.Chapters -> {
        if (chapter == null) return@sheetBody
        ChaptersSheet(
          chapters,
          currentChapter = chapter,
          onClick = { onSeekToChapter(chapters.indexOf(it)) },
        )
      }

      Sheets.Decoders -> {
        DecodersSheet(
          selectedDecoder = decoder,
          onSelect = onUpdateDecoder,
        )
      }

      Sheets.More -> {
        MoreSheet(
          remainingTime = sleepTimerTimeRemaining,
          onStartTimer = onStartSleepTimer,
          onDismissRequest = onDismissRequest,
          onEnterFiltersPanel = { onOpenPanel(Panels.VideoFilters) },
          onOpenAspectRatio = { onShowSheet(Sheets.AspectRatios) },
          onOpenFrameNavigation = { onShowSheet(Sheets.FrameNavigation) },
        )
      }

      Sheets.PlaybackSpeed -> {
        PlaybackSpeedSheet(
          speed,
          onSpeedChange = onSpeedChange,
          speedPresets = speedPresets,
          onAddSpeedPreset = onAddSpeedPreset,
          onRemoveSpeedPreset = onRemoveSpeedPreset,
          onResetPresets = onResetSpeedPresets,
          onMakeDefault = onMakeDefaultSpeed,
          onResetDefault = onResetDefaultSpeed,
          onDismissRequest = onDismissRequest,
        )
      }

      Sheets.VideoZoom -> {
        val videoZoom by viewModel.videoZoom.collectAsState()
        VideoZoomSheet(
          videoZoom = videoZoom,
          onSetVideoZoom = viewModel::setVideoZoom,
          onResetVideoPan = viewModel::resetVideoPan,
          onDismissRequest = onDismissRequest,
        )
      }

      Sheets.AspectRatios -> {
        val playerPreferences = koinInject<app.marlboroadvance.mpvex.preferences.PlayerPreferences>()
        val customRatiosSet by playerPreferences.customAspectRatios.collectPreferenceAsState()
        val currentRatio by viewModel.currentAspectRatio.collectAsState()
        val customRatios =
          customRatiosSet.mapNotNull { str ->
            val parts = str.split("|")
            if (parts.size == 2) {
              app.marlboroadvance.mpvex.ui.player.controls.components.sheets.AspectRatio(
                label = parts[0],
                ratio = parts[1].toDoubleOrNull() ?: return@mapNotNull null,
                isCustom = true,
              )
            } else {
              null
            }
          }

        AspectRatioSheet(
          currentRatio = currentRatio,
          customRatios = customRatios,
          onSelectRatio = { ratio ->
            if (ratio < 0) {
              // Default selected - apply Fit mode
              viewModel.changeVideoAspect(app.marlboroadvance.mpvex.ui.player.VideoAspect.Fit)
            } else {
              // Custom ratio selected
              viewModel.setCustomAspectRatio(ratio)
            }
          },
          onAddCustomRatio = { label, ratio ->
            playerPreferences.customAspectRatios.set(customRatiosSet + "$label|$ratio")
            viewModel.setCustomAspectRatio(ratio)
          },
          onDeleteCustomRatio = { ratio ->
            val toRemove = "${ratio.label}|${ratio.ratio}"
            playerPreferences.customAspectRatios.set(customRatiosSet - toRemove)
            // If the deleted ratio is currently active, reset to default (Fit)
            if (kotlin.math.abs(currentRatio - ratio.ratio) < 0.01) {
              viewModel.changeVideoAspect(app.marlboroadvance.mpvex.ui.player.VideoAspect.Fit)
            }
          },
          onDismissRequest = onDismissRequest,
        )
      }

      Sheets.FrameNavigation -> {
        val currentFrame by viewModel.currentFrame.collectAsState()
        val totalFrames by viewModel.totalFrames.collectAsState()
        FrameNavigationSheet(
          currentFrame = currentFrame,
          totalFrames = totalFrames,
          onUpdateFrameInfo = viewModel::updateFrameInfo,
          onPause = viewModel::pause,
          onUnpause = viewModel::unpause,
          onPauseUnpause = viewModel::pauseUnpause,
          onSeekTo = { position, _ -> viewModel.seekTo(position) },
          onDismissRequest = onDismissRequest,
        )
      }


      Sheets.Playlist -> {
        // Refresh playlist items when sheet is shown
        LaunchedEffect(Unit) {
          viewModel.refreshPlaylistItems()
        }

        // Observe playlist updates and loading state
        val playlist by viewModel.playlistItems.collectAsState()
        val loadingItemIndex by viewModel.loadingItemIndex.collectAsState()
        val playerPreferences = koinInject<app.marlboroadvance.mpvex.preferences.PlayerPreferences>()

        if (playlist.isNotEmpty()) {
          val playlistImmutable = playlist.toImmutableList()
          val isM3U = viewModel.isPlaylistM3U()
          PlaylistSheet(
            playlist = playlistImmutable,
            onDismissRequest = onDismissRequest,
            onItemClick = { item ->
              scope.launch {
                onDismissRequest()
                delay(200)
                viewModel.playPlaylistItem(item.index)
              }
            },
            isM3UPlaylist = isM3U,
            playerPreferences = playerPreferences,
            loadingItemIndex = loadingItemIndex,
          )
        }
      }
    }
  }

  // Track sheets and every panel share one PlayerOverlayHost. It stays mounted while
  // the content swaps, so tracks -> delay/settings never shows two overlays,
  // duplicate scrims or a jump. Other sheets keep their own PlayerSheet layout.
  val hosted = panelShown != Panels.None || sheetShown.usesOverlayHost()
  val panelReturnSheet by viewModel.panelReturnSheet.collectAsState()
  if (hosted) {
    val overlayKey: Any = if (panelShown != Panels.None) panelShown else sheetShown
    PlayerOverlayHost(onDismissRequest = onDismissRequest) { dismiss ->
      AnimatedContent(
        targetState = overlayKey,
        label = "player_overlay_content",
        transitionSpec = {
          // Sheet -> panel slides forward, panel -> sheet slides back (shared-axis).
          // Any other swap keeps the fade-through: old content leaves first, then the
          // new one arrives. Size settles on a non-bouncy spring (no overshoot, no
          // mid-transition clipping; the host surface already clips to its own shape).
          val direction =
            when {
              targetState is Panels && initialState !is Panels -> 1
              targetState !is Panels && initialState is Panels -> -1
              else -> 0
            }
          (
            if (direction == 0) {
              fadeIn(tween(durationMillis = 210, delayMillis = 90)) togetherWith
                fadeOut(tween(durationMillis = 90))
            } else {
              (
                slideInHorizontally(tween(durationMillis = 280, easing = FastOutSlowInEasing)) { width ->
                  width * direction
                } + fadeIn(tween(durationMillis = 200))
              ) togetherWith (
                slideOutHorizontally(tween(durationMillis = 280, easing = FastOutSlowInEasing)) { width ->
                  (-width * PANEL_SLIDE_FRACTION * direction).toInt()
                } + fadeOut(tween(durationMillis = 120))
              )
            }
          ).using(
            SizeTransform(clip = false) { _, _ ->
              spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntSize.VisibilityThreshold,
              )
            },
          )
        },
      ) { key ->
        if (key is Panels) {
          // Captured once per panel so the header keeps its back arrow while this
          // panel animates out after Back has already cleared the origin.
          val hasOrigin = remember { panelReturnSheet != Sheets.None }
          PlayerPanels(
            panelShown = key,
            onDismissRequest = dismiss,
            viewModel = viewModel,
            onBack = if (hasOrigin) viewModel::closePanelToSheet else null,
          )
        } else {
          sheetBody(key as Sheets)
        }
      }
    }
  } else {
    sheetBody(sheetShown)
  }
}

/** Share of the content width the outgoing content travels during a sheet <-> panel slide. */
private const val PANEL_SLIDE_FRACTION = 0.3f

private fun Sheets.usesOverlayHost(): Boolean =
  when (this) {
    Sheets.SubtitleTracks,
    Sheets.OnlineSubtitleSearch,
    Sheets.AudioTracks,
    Sheets.Chapters,
    Sheets.Decoders,
    -> true
    else -> false
  }
