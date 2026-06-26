package app.marlboroadvance.mpvex.ui.player

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity
import app.marlboroadvance.mpvex.databinding.PlayerLayoutBinding
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.ui.player.controls.PlayerControls
import app.marlboroadvance.mpvex.ui.theme.MpvexTheme
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import app.marlboroadvance.mpvex.utils.media.HttpUtils
import app.marlboroadvance.mpvex.utils.media.SubtitleOps
import app.marlboroadvance.mpvex.utils.storage.FileTypeUtils
import app.marlboroadvance.mpvex.utils.storage.FileFilterUtils
import com.github.k1rakishou.fsaf.FileManager
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

/**
 * Main player activity that handles video playback using the MPV library.
 */
@Suppress("TooManyFunctions", "LargeClass")
class PlayerActivity :
  AppCompatActivity(),
  PlayerHost {
  // ==================== ViewModels and Bindings ====================

  /**
   * View model for managing player UI state.
   */
  private val viewModel: PlayerViewModel by viewModels<PlayerViewModel> {
    PlayerViewModelProviderFactory(this)
  }
  
  // Initialize ViewModel callback for progress saving on pause
  private fun setupViewModelCallbacks() {
    viewModel.onPauseCallback = {
      saveVideoProgress(isImmediate = true)
    }
  }

  /**
   * Binding for the player layout.
   */
  private val binding by lazy { PlayerLayoutBinding.inflate(layoutInflater) }

  /**
   * Observer for MPV events.
   */
  private val playerObserver by lazy { PlayerObserver(this) }

  // ==================== Dependency Injection ====================

  /**
   * Repository for managing playback state.
   */
  private val playbackStateRepository: PlaybackStateRepository by inject()

  /**
   * Repository for managing playlists.
   */
  private val playlistRepository: app.marlboroadvance.mpvex.database.repository.PlaylistRepository by inject()

  /**
   * Preferences for player settings.
   */
  private val playerPreferences: PlayerPreferences by inject()

  /**
   * Preferences for audio settings.
   */
  private val audioPreferences: AudioPreferences by inject()

  /**
   * Preferences for subtitle settings.
   */
  private val subtitlesPreferences: SubtitlesPreferences by inject()

  /**
   * Preferences for advanced settings.
   */
  private val advancedPreferences: AdvancedPreferences by inject()

  /**
   * Preferences for browser settings.
   */
  private val browserPreferences: BrowserPreferences by inject()

  /**
   * Manager for file operations.
   */
  private val fileManager: FileManager by inject()

  /**
   * Track selector for automatic audio/subtitle selection
   */
  private val trackSelector: TrackSelector by lazy {
    TrackSelector(audioPreferences, subtitlesPreferences)
  }

  // ==================== Views ====================

  /**
   * The MPV player view.
   */
  val player by lazy { binding.player }

  // ==================== State Management ====================

  /**
   * Current video file name being played.
   */
  private var fileName = ""

  /**
   * Unique identifier for the current media, used for saving/loading playback state.
   */
  private var mediaIdentifier = ""

  /**
   * Playlist of URIs for sequential playback
   */
  internal var playlist: List<Uri> = emptyList()

  /**
   * Current index in the playlist
   */
  internal var playlistIndex: Int = 0

  /**
   * Shuffled order of playlist indices (when shuffle is enabled)
   */
  private var shuffledIndices: List<Int> = emptyList()

  /**
   * Current position in shuffled playlist (when shuffle is enabled)
   */
  private var shuffledPosition: Int = 0

  /**
   * Playlist ID for tracking play history
   */
  private var playlistId: Int? = null

  /**
   * Tracks the starting offset of the loaded playlist window in the full playlist.
   */
  private var playlistWindowOffset: Int = 0

  /**
   * Total count of items in the full playlist.
   */
  var playlistTotalCount: Int = -1
    private set

  /**
   * Indicates whether the current playlist is an M3U playlist sourced from database.
   */
  private var isM3uPlaylist: Boolean = false

  /**
   * Helper for managing Picture-in-Picture mode.
   */
  private lateinit var pipHelper: MPVPipHelper

  private var isReady = false
  private var isUserFinishing = false
  private var isManualBackgroundPlayback = false
  private var noisyReceiverRegistered = false
  private var mpvInitialized = false
  private var wasPlayingBeforePause = false

  // ==================== Progress Save Management ====================

  /**
   * Centralized manager for video progress saving operations.
   */
  private val progressSaveManager = ProgressSaveManager()

  // ==================== Background Playback ====================

  /**
   * Reference to the background playback service.
   */
  private var mediaPlaybackService: MediaPlaybackService? = null

  /**
   * Tracks whether we're currently bound to the background playback service.
   */
  private var serviceBound = false

  // System media controls (lock screen, Bluetooth, headset) are owned solely by
  // MediaPlaybackService's MediaSessionCompat. The Activity previously kept a second,
  // parallel android.media.session.MediaSession which produced two competing active
  // sessions; it has been removed so the Service is the single source of truth.

  // ==================== Audio Focus ====================

  /**
   * Audio focus request for API 26+.
   */
  private var audioFocusRequest: AudioFocusRequest? = null

  /**
   * Callback to restore audio focus after it's been lost and regained.
   */
  private var restoreAudioFocus: () -> Unit = {}

  // ==================== Broadcast Receivers ====================

  private val noisyReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(
        context: Context?,
        intent: Intent?,
      ) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
          viewModel.pause()
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
      }
    }

  private val audioFocusChangeListener =
    AudioManager.OnAudioFocusChangeListener { focusChange ->
      when (focusChange) {
        AudioManager.AUDIOFOCUS_LOSS,
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
          -> {
          val oldRestore = restoreAudioFocus
          val wasPlayerPaused = viewModel.paused ?: false
          viewModel.pause()
          restoreAudioFocus = {
            oldRestore()
            if (!wasPlayerPaused) viewModel.unpause()
          }
        }

        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          MPVLib.command("multiply", "volume", "0.5")
          restoreAudioFocus = {
            MPVLib.command("multiply", "volume", "2")
          }
        }

        AudioManager.AUDIOFOCUS_GAIN -> {
          restoreAudioFocus()
          restoreAudioFocus = {}
        }

        AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
          Log.d(TAG, "Audio focus request failed")
        }
      }
    }

  @RequiresApi(Build.VERSION_CODES.P)
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContentView(binding.root)

    volumeControlStream = AudioManager.STREAM_MUSIC

    setupMPV()
    MediaPlaybackService.createNotificationChannel(this)
    setupAudio()
    setupBackPressHandler()
    setupPlayerControls()
    setupPipHelper()
    setupViewModelCallbacks()

    lifecycleScope.launch {
      playerPreferences.showFileExtension.changes().collect {
        updateDisplayTitle()
        viewModel.refreshPlaylistItems()
      }
    }

    playlistId = intent.getIntExtra("playlist_id", -1).takeIf { it != -1 }
    playlistIndex = intent.getIntExtra("playlist_index", 0)

    playlist = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableArrayListExtra("playlist", Uri::class.java) ?: emptyList()
    } else {
      @Suppress("DEPRECATION")
      intent.getParcelableArrayListExtra("playlist") ?: emptyList()
    }

    if (playlist.isEmpty() && playlistId != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        val pid = playlistId ?: return@launch
        try {
          val playlistEntity = playlistRepository.getPlaylistById(pid)
          isM3uPlaylist = playlistEntity?.isM3uPlaylist ?: false
          val items = playlistRepository.getPlaylistItemsAsUris(pid)
          val totalCount = items.size

          withContext(Dispatchers.Main) {
            playlist = items
            playlistWindowOffset = 0
            playlistTotalCount = totalCount
            Log.d(TAG, "Loaded all $totalCount items from playlist $pid (isM3U: $isM3uPlaylist)")
            if (viewModel.shuffleEnabled.value) {
              onShuffleToggled(true)
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to load playlist from database", e)
        }
      }
    }

    if (playlist.isEmpty() && playlistId == null && playerPreferences.playlistMode.get()) {
      val path = parsePathFromIntent(intent)
      if (path != null) {
        generatePlaylistFromFolder(path)
      }
    }

    fileName = getFileName(intent)
    if (fileName.isBlank()) {
      fileName = intent.data?.lastPathSegment ?: "Unknown Video"
    }
    mediaIdentifier = getMediaIdentifier(intent, fileName)

    setHttpHeadersFromExtras(intent.extras)

    getPlayableUri(intent)?.let(player::playFile)

    if (playerPreferences.orientation.get() != PlayerOrientation.Video) {
      setOrientation()
    }

    viewModel.applyPersistedShuffleState()

    window.attributes.layoutInDisplayCutoutMode =
      WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
  }

  override fun attachBaseContext(newBase: Context?) {
    if (newBase == null) {
      super.attachBaseContext(null)
      return
    }

    val originalConfiguration = newBase.resources.configuration
    val contextToUse =
      if (originalConfiguration.fontScale == 1f) {
        newBase
      } else {
        val updatedConfiguration = Configuration(originalConfiguration).apply { fontScale = 1f }
        val configurationContext = newBase.createConfigurationContext(updatedConfiguration)
        val configurationDisplayMetrics = configurationContext.resources.displayMetrics
        @Suppress("DEPRECATION")
        configurationDisplayMetrics.scaledDensity = updatedConfiguration.fontScale * configurationDisplayMetrics.density
        configurationContext
      }

    super.attachBaseContext(contextToUse)
  }

  private fun setupBackPressHandler() {
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun handleOnBackPressed() {
          handleBackPress()
        }
      },
    )
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun handleBackPress() {
    if (viewModel.sheetShown.value != Sheets.None) {
      viewModel.setSheetShown(Sheets.None)
      viewModel.showControls()
      return
    }

    if (viewModel.panelShown.value != Panels.None) {
      viewModel.panelShown.update { Panels.None }
      viewModel.showControls()
      return
    }

    if (playerPreferences.autoPiPOnNavigation.get() && isReady) {
      pipHelper.enterPipMode()
      return
    }

    saveVideoProgress(isImmediate = true)

    isUserFinishing = true
    finish()
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun setupPlayerControls() {
    binding.controls.setContent {
      MpvexTheme {
        PlayerControls(
          viewModel = viewModel,
          onBackPress = {
            saveVideoProgress(isImmediate = true)
            isUserFinishing = true
            finish()
          },
          modifier = Modifier,
        )
      }
    }
  }

  private fun setupPipHelper() {
    pipHelper = MPVPipHelper(activity = this, mpvView = player)
  }

  private fun setupAudio() {
    audioPreferences.audioChannels.get().let {
      runCatching {
        MPVLib.setPropertyString(it.property, it.value)
      }.onFailure { e ->
        Log.e(TAG, "Error setting audio channels: ${it.property}=${it.value}", e)
      }
    }

    if (!serviceBound) {
      audioFocusRequest =
        AudioFocusRequest
          .Builder(AudioManager.AUDIOFOCUS_GAIN)
          .setAudioAttributes(
            AudioAttributes
              .Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
              .build(),
          ).setOnAudioFocusChangeListener(audioFocusChangeListener)
          .setAcceptsDelayedFocusGain(true)
          .setWillPauseWhenDucked(true)
          .build()
      requestAudioFocus()
    }
  }

  override fun requestAudioFocus(): Boolean {
    val req = audioFocusRequest ?: return false
    val result = audioManager.requestAudioFocus(req)
    return when (result) {
      AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
        restoreAudioFocus = {}
        true
      }

      AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
        restoreAudioFocus = { requestAudioFocus() }
        false
      }

      else -> {
        restoreAudioFocus = {}
        false
      }
    }
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    if (playerPreferences.autoPiPOnNavigation.get() && isReady && !isFinishing) {
      pipHelper.enterPipMode()
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  override fun onDestroy() {
    Log.d(TAG, "PlayerActivity onDestroy")

    runCatching {
      isReady = false

      if ((isUserFinishing || isFinishing) && !isManualBackgroundPlayback) {
        if (serviceBound) {
          runCatching { unbindService(serviceConnection) }
          serviceBound = false
        }
        stopService(Intent(this, MediaPlaybackService::class.java))
        mediaPlaybackService = null
      }

      // We no longer cancel all pending saves here because "Immediate" saves
      // use NonCancellable and separate jobs to ensure they finish during destruction.
      // progressSaveManager.cancelPendingSave() // REMOVED to allow final save to finish

      cleanupMPV()
      cleanupAudio()
      cleanupReceivers()
    }.onFailure { e ->
      Log.e(TAG, "Error during onDestroy", e)
    }

    super.onDestroy()
  }

  private fun cleanupMPV() {
    if (!mpvInitialized) return

    player.isExiting = true

    endBackgroundPlayback()

    if (!isFinishing || isManualBackgroundPlayback) return

    runCatching {
      MPVLib.removeObserver(playerObserver)

      if (isReady) {
        MPVLib.setPropertyBoolean("pause", true)
        MPVLib.command("quit")
        Thread.sleep(100)
      }

      MPVLib.destroy()
      mpvInitialized = false
    }.onFailure { e ->
      Log.e(TAG, "Error cleaning up MPV", e)
    }
  }

  override fun abandonAudioFocus() {
    if (restoreAudioFocus != {}) {
      audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
      restoreAudioFocus = {}
    }
  }

  private fun cleanupAudio() {
    abandonAudioFocus()
  }

  private fun cleanupReceivers() {
    if (noisyReceiverRegistered) {
      runCatching {
        unregisterReceiver(noisyReceiver)
        noisyReceiverRegistered = false
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  override fun onPause() {
    runCatching {
      val isInPip = isInPictureInPictureMode
      val shouldPause = (!audioPreferences.automaticBackgroundPlayback.get() && !isManualBackgroundPlayback) ||
                        (isUserFinishing && !isManualBackgroundPlayback)

      if (isFinishing && !isManualBackgroundPlayback) {
        viewModel.pause()
        MPVLib.command("stop")
      } else if (!isInPip && shouldPause) {
        wasPlayingBeforePause = !(viewModel.paused ?: true)
        viewModel.pause()
      }

      if (isUserFinishing && !isInPip && !isManualBackgroundPlayback) {
        restoreSystemUI()
      }
    }.onFailure { e ->
      Log.e(TAG, "Error during onPause", e)
    }

    super.onPause()
  }

  @RequiresApi(Build.VERSION_CODES.P)
  override fun finish() {
    runCatching {
      saveVideoProgress(isImmediate = true)
      isReady = false
      if (serviceBound || mediaPlaybackService != null) {
        endBackgroundPlayback()
      }
      setReturnIntent()
    }.onFailure { e ->
      Log.e(TAG, "Error during finish", e)
    }

    super.finish()
  }

  override fun finishAndRemoveTask() {
    runCatching {
      saveVideoProgress(isImmediate = true)
      isReady = false
      isUserFinishing = true
      if (serviceBound || mediaPlaybackService != null) {
        endBackgroundPlayback()
      }
      setReturnIntent()
    }.onFailure { e ->
      Log.e(TAG, "Error during finishAndRemoveTask", e)
    }

    super.finishAndRemoveTask()
  }

  override fun onStop() {
    runCatching {
      pipHelper.onStop()

      if (noisyReceiverRegistered) {
        unregisterReceiver(noisyReceiver)
        noisyReceiverRegistered = false
      }

      val shouldAllowBackgroundPlayback = isManualBackgroundPlayback ||
                                          audioPreferences.automaticBackgroundPlayback.get()

      if (!shouldAllowBackgroundPlayback && (isUserFinishing || isFinishing)) {
        viewModel.pause()
      }
    }.onFailure { e ->
      Log.e(TAG, "Error during onStop", e)
    }

    super.onStop()
  }

  @RequiresApi(Build.VERSION_CODES.P)
  override fun onStart() {
    super.onStart()

    runCatching {
      setupWindowFlags()
      setupSystemUI()

      if (!noisyReceiverRegistered) {
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)
        noisyReceiverRegistered = true
      }

      if (playerPreferences.rememberBrightness.get()) {
        val brightness = playerPreferences.defaultBrightness.get()
        if (brightness != BRIGHTNESS_NOT_SET) {
          viewModel.changeBrightnessTo(brightness)
        }
      }

      isManualBackgroundPlayback = false
    }.onFailure { e ->
      Log.e(TAG, "Error during onStart", e)
    }
  }

  private fun setupWindowFlags() {
    pipHelper.updatePictureInPictureParams()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setFlags(
      WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
    )
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun setupSystemUI() {
    window.attributes.layoutInDisplayCutoutMode =
      WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

    if (playerPreferences.showSystemStatusBar.get()) {
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
      window.statusBarColor = android.graphics.Color.parseColor("#80000000")
    }

    try {
      @Suppress("DEPRECATION")
      WindowCompat.getInsetsController(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.statusBars())
        hide(WindowInsetsCompat.Type.navigationBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to setup system UI insets", e)
    }

    @Suppress("DEPRECATION")
    binding.root.systemUiVisibility =
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        if (playerPreferences.showSystemStatusBar.get()) 0 else View.SYSTEM_UI_FLAG_LOW_PROFILE
  }

  private fun restoreSystemUI() {
    window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
    }

    WindowCompat.setDecorFitsSystemWindows(window, true)

    try {
      WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        show(WindowInsetsCompat.Type.systemBars())
        show(WindowInsetsCompat.Type.navigationBars())
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to restore system UI insets", e)
    }
  }

  private fun setupMPV() {
    runCatching {
      Utils.copyAssets(this@PlayerActivity)
      syncFromUserMpvDirectory()
      Log.d(TAG, "MPV config and scripts prepared successfully")
    }.onFailure { e ->
      Log.e(TAG, "Error copying MPV config and scripts", e)
    }

    player.initialize(filesDir.path, cacheDir.path)
    mpvInitialized = true
    Log.d(TAG, "MPV initialized")

    MPVLib.addObserver(playerObserver)
  }

  private fun syncFromUserMpvDirectory() {
    val mpvConfStorageUri = advancedPreferences.mpvConfStorageUri.get()

    val tree = if (mpvConfStorageUri.isNotBlank()) {
      runCatching {
        DocumentFile.fromTreeUri(this, mpvConfStorageUri.toUri())
      }.getOrNull()?.takeIf { it.exists() && it.canRead() }
    } else null

    if (tree != null) {
      Log.d(TAG, "Syncing from user MPV directory: ${tree.uri}")
      syncConfigFiles(tree)
      syncFonts(tree)
      Log.d(TAG, "Full MPV directory sync completed")
    } else {
      Log.d(TAG, "No MPV directory configured, using preferences fallback")
      copyMPVConfigFromPreferences()
    }
  }

  private fun syncConfigFiles(tree: DocumentFile) {
    for (configName in listOf("mpv.conf", "input.conf")) {
      runCatching {
        val configFile = findFileCaseInsensitive(tree, configName)
        if (configFile != null && configFile.exists() && configFile.canRead()) {
          contentResolver.openInputStream(configFile.uri)?.use { input ->
            val content = input.bufferedReader().readText()
            File(filesDir, configName).writeText(content)
            when (configName) {
              "mpv.conf" -> advancedPreferences.mpvConf.set(content)
              "input.conf" -> advancedPreferences.inputConf.set(content)
            }
            Log.d(TAG, "Synced config: $configName (${content.length} chars)")
          }
        } else {
          val prefContent = when (configName) {
            "mpv.conf" -> advancedPreferences.mpvConf.get()
            "input.conf" -> advancedPreferences.inputConf.get()
            else -> ""
          }
          File(filesDir, configName).apply {
            if (!exists()) createNewFile()
            if (prefContent.isNotBlank()) writeText(prefContent)
          }
          Log.d(TAG, "Config not found in directory, used preferences: $configName")
        }
      }.onFailure { e ->
        Log.e(TAG, "Error syncing config: $configName", e)
      }
    }
  }

  private fun syncFonts(tree: DocumentFile) {
    val internalFontsDir = File(filesDir, "fonts")
    internalFontsDir.mkdirs()

    val fontsSubdir = findSubdirCaseInsensitive(tree, "fonts")
    val sourceDir = fontsSubdir ?: tree
    val fontExtensions = setOf("ttf", "otf", "ttc", "woff", "woff2")
    var count = 0

    sourceDir.listFiles().forEach { file ->
      if (!file.isFile) return@forEach
      val name = file.name ?: return@forEach
      val ext = name.substringAfterLast('.', "").lowercase()
      if (ext !in fontExtensions) return@forEach

      val target = File(internalFontsDir, name)
      if (target.exists()) return@forEach

      runCatching {
        contentResolver.openInputStream(file.uri)?.use { input ->
          target.outputStream().use { output ->
            input.copyTo(output)
          }
          count++
          Log.d(TAG, "Synced font: $name")
        }
      }.onFailure { e ->
        Log.e(TAG, "Error syncing font: $name", e)
      }
    }

    runCatching {
      val fontsFolderUri = subtitlesPreferences.fontsFolder.get()
      if (fontsFolderUri.isNotBlank()) {
        val destDir = fileManager.fromPath("${filesDir.path}/fonts")
        if (!fileManager.exists(destDir)) {
          fileManager.createDir(fileManager.fromPath(filesDir.path), "fonts")
        }
        val fontsDir = fileManager.fromUri(fontsFolderUri.toUri())
        if (fontsDir != null && fileManager.exists(fontsDir)) {
          fileManager.copyDirectoryWithContent(fontsDir, destDir, false)
        }
      }
    }.onFailure { e ->
      Log.e(TAG, "Error syncing subtitle fonts: ${e.message}")
    }

    Log.d(TAG, "Fonts sync: $count file(s) from MPV directory")
  }

  private fun copyMPVConfigFromPreferences() {
    runCatching {
      File(filesDir, "mpv.conf").apply {
        if (!exists()) createNewFile()
        val content = advancedPreferences.mpvConf.get()
        if (content.isNotBlank()) writeText(content)
      }
      File(filesDir, "input.conf").apply {
        if (!exists()) createNewFile()
        val content = advancedPreferences.inputConf.get()
        if (content.isNotBlank()) writeText(content)
      }
      File(filesDir, "fonts").mkdirs()
    }.onFailure { e ->
      Log.e(TAG, "Error creating fallback config files", e)
    }
  }

  private fun findSubdirCaseInsensitive(parent: DocumentFile, name: String): DocumentFile? =
    parent.listFiles().firstOrNull {
      it.isDirectory && it.name?.equals(name, ignoreCase = true) == true
    }

  private fun findFileCaseInsensitive(parent: DocumentFile, name: String): DocumentFile? =
    parent.listFiles().firstOrNull {
      it.isFile && it.name?.equals(name, ignoreCase = true) == true
    }

  override fun onResume() {
    super.onResume()
    updateVolume()
  }

  private fun updateVolume() {
    viewModel.currentVolume.update {
      audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).also { volume ->
        if (volume < viewModel.maxVolume) {
          viewModel.changeMPVVolumeTo(MAX_MPV_VOLUME)
        }
      }
    }
  }

  private fun setIntentExtras(extras: Bundle?) {
    if (extras == null) return

    extras.getInt("position", POSITION_NOT_SET).takeIf { it != POSITION_NOT_SET }?.let {
      MPVLib.setPropertyInt("time-pos", it / MILLISECONDS_TO_SECONDS)
    }

    addSubtitlesFromExtras(extras)
    setHttpHeadersFromExtras(extras)
  }

  private fun addSubtitlesFromExtras(extras: Bundle) {
    if (!extras.containsKey("subs")) return

    val subList = Utils.getParcelableArray<Uri>(extras, "subs")
    val subsToEnable = Utils.getParcelableArray<Uri>(extras, "subs.enable")

    lifecycleScope.launch(Dispatchers.Default) {
      for (suburi in subList) {
        val subfile = suburi.resolveUri(this@PlayerActivity) ?: continue
        val flag = if (subsToEnable.any { it == suburi }) "select" else "auto"

        Log.v(TAG, "Adding subtitles from intent extras: $subfile")
        MPVLib.command("sub-add", subfile, flag)
      }
    }
  }

  private fun setHttpHeadersFromExtras(extras: Bundle?) {
    val headerMap = mutableMapOf<String, String>()

    val uri = extractUriFromIntent(intent)
    if (uri != null && HttpUtils.isNetworkStream(uri)) {
      HttpUtils.extractRefererDomain(uri)?.let { referer ->
        headerMap["Referer"] = referer
        Log.d(TAG, "Auto-detected Referer: $referer")
      }
    }

    extras?.getStringArray("headers")?.let { headers ->
      if (headers.isEmpty()) return@let

      if (headers[0].startsWith("User-Agent", ignoreCase = true)) {
        MPVLib.setPropertyString("user-agent", headers[1])
      }

      if (headers.size > 2) {
        headers
          .asSequence()
          .drop(2)
          .chunked(2)
          .filter { it.size == 2 }
          .forEach { (key, value) ->
            headerMap[key] = value
          }
      }
    }

    if (headerMap.isNotEmpty()) {
      val headersString = headerMap
        .map { "${it.key}: ${it.value.replace(",", "\\,")}" }
        .joinToString(",")

      MPVLib.setPropertyString("http-header-fields", headersString)
      Log.d(TAG, "Set HTTP headers: $headersString")
    }
  }

  private fun setHttpHeadersForUri(uri: Uri) {
    if (!HttpUtils.isNetworkStream(uri)) return

    val headerMap = mutableMapOf<String, String>()

    HttpUtils.extractRefererDomain(uri)?.let { referer ->
      headerMap["Referer"] = referer
      Log.d(TAG, "Auto-detected Referer for playlist item: $referer")
    }

    if (headerMap.isNotEmpty()) {
      val headersString = headerMap
        .map { "${it.key}: ${it.value.replace(",", "\\,")}" }
        .joinToString(",")

      MPVLib.setPropertyString("http-header-fields", headersString)
      Log.d(TAG, "Set HTTP headers for playlist item: $headersString")
    }
  }

  private fun parsePathFromIntent(intent: Intent): String? =
    when (intent.action) {
      Intent.ACTION_VIEW -> intent.data?.resolveUri(this)
      Intent.ACTION_SEND -> parsePathFromSendIntent(intent)
      else -> intent.getStringExtra("uri")
    }

  private fun parsePathFromSendIntent(intent: Intent): String? =
    if (intent.hasExtra(Intent.EXTRA_STREAM)) {
      val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
      }
      uri?.resolveUri(this@PlayerActivity)
    } else {
      intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
        val uri = text.trim().toUri()
        if (uri.isHierarchical && !uri.isRelative) {
          uri.resolveUri(this)
        } else {
          null
        }
      }
    }

  private fun getFileName(intent: Intent): String {
    intent.getStringExtra("title")?.let { return it }
    intent.getStringExtra("filename")?.let { return it }

    val uri = extractUriFromIntent(intent) ?: return ""

    getDisplayNameFromUri(uri)?.let { return it }

    return extractFileNameFromUri(uri)
  }

  private fun extractFileNameFromUri(uri: Uri): String {
    if (HttpUtils.isNetworkStream(uri)) {
      val path = uri.path ?: return uri.host ?: "Network Stream"
      val lastSegment = path.substringAfterLast("/")

      if (lastSegment.isNotBlank()) {
        return try {
          java.net.URLDecoder.decode(lastSegment, "UTF-8")
            .substringBefore("?")
            .substringBefore("#")
            .takeIf { it.isNotBlank() } ?: uri.host ?: "Network Stream"
        } catch (e: Exception) {
          lastSegment
            .substringBefore("?")
            .substringBefore("#")
        }
      }

      return uri.host ?: "Network Stream"
    }

    val lastSegment = uri.lastPathSegment?.substringAfterLast("/") ?: uri.path ?: "Unknown Video"

    return try {
      java.net.URLDecoder.decode(lastSegment, "UTF-8")
    } catch (e: Exception) {
      lastSegment
    }
  }

  internal fun getPlaylistItemTitle(uri: Uri): String {
    val rawTitle = getDisplayNameFromUri(uri) ?: extractFileNameFromUri(uri)
    return formatTitle(rawTitle, isUriM3U(uri))
  }

  internal fun playPlaylistItem(index: Int) {
    if (index in playlist.indices) {
      loadPlaylistItem(index)
    }
  }

  private fun extractUriFromIntent(intent: Intent): Uri? =
    if (intent.type == "text/plain") {
      intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
    } else {
      intent.data ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_STREAM)
      }
    }

  private fun getDisplayNameFromUri(uri: Uri): String? =
    runCatching {
      contentResolver
        .query(
          uri,
          arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
          null,
          null,
          null,
        )?.use { cursor ->
          if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.onFailure { e ->
      Log.e(TAG, "Error getting display name from URI", e)
    }.getOrNull()

  private fun getPlayableUri(intent: Intent): String? {
    val uri = parsePathFromIntent(intent) ?: return null
    return if (uri.startsWith("content://")) {
      uri.toUri().openContentFd(this)
    } else {
      uri
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    if (isReady) {
      handleConfigurationChange()
    }
  }

  private fun handleConfigurationChange() {
    if (!isInPictureInPictureMode) {
      // Configuration changes don't affect aspect ratio
    } else {
      viewModel.hideControls()
    }
  }

  internal fun onObserverEvent(
    property: String,
    _value: Long,
  ) {
    when (property) {
      "video-params/w",
      "video-params/h" -> {
        if (!mpvInitialized || player.isExiting || isFinishing) return

        val aspect = player.getVideoOutAspect()
        Log.d(TAG, "Video dimension changed: $property, aspect: $aspect")
        pipHelper.updatePictureInPictureParams()
        if (playerPreferences.orientation.get() == PlayerOrientation.Video) {
          setOrientation()
        }
      }
    }
  }

  internal fun onObserverEvent(
    property: String,
    value: Boolean,
  ) {
    when (property) {
      "pause" -> {
        handlePauseStateChange(value)
        if (!value && !isReady) {
          isReady = true
        }
      }
      "eof-reached" -> handleEndOfFile(value)
    }
  }

  private fun handlePauseStateChange(isPaused: Boolean) {
    if (isPaused) {
      saveVideoProgress(isImmediate = true)

      if (!playerPreferences.keepScreenOnWhenPaused.get()) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    } else {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    // MediaPlaybackService observes the "pause" property directly and updates the
    // MediaSession/notification itself, so no Activity-side push is needed here.
    runCatching {
      if (isInPictureInPictureMode) {
        pipHelper.updatePictureInPictureParams()
      }
    }.onFailure { /* Silently ignore PiP update failures */ }
  }

  private fun handleEndOfFile(isEof: Boolean) {
    if (isEof) {
      saveVideoProgress(isImmediate = true)

      if (viewModel.shouldRepeatCurrentFile()) {
        MPVLib.command("seek", "0", "absolute")
        viewModel.unpause()
        return
      }

      if (playlist.isNotEmpty()) {
        val hasNextItem = if (viewModel.shuffleEnabled.value) {
          shuffledPosition < shuffledIndices.size - 1
        } else {
          playlistIndex < playlist.size - 1
        }

        val autoplayEnabled = playerPreferences.autoplayNextVideo.get()

        if (hasNextItem && (autoplayEnabled || viewModel.shouldRepeatPlaylist())) {
          playNext()
        } else if (viewModel.shouldRepeatPlaylist()) {
          if (viewModel.shuffleEnabled.value) {
            generateShuffledIndices()
            shuffledPosition = 0
            playlistIndex = shuffledIndices[0]
            loadPlaylistItem(playlistIndex)
          } else {
            playlistIndex = 0
            loadPlaylistItem(0)
          }
        } else if (playerPreferences.closeAfterReachingEndOfVideo.get()) {
          finishAndRemoveTask()
        }
      } else {
        if (playerPreferences.closeAfterReachingEndOfVideo.get()) {
          finishAndRemoveTask()
        }
      }
    }
  }

  internal fun onObserverEvent(
    _property: String,
    _value: MPVNode,
  ) {}

  internal fun onObserverEvent(
    property: String,
    _value: Double,
  ) {
    when (property) {
      "video-params/aspect" -> {
        if (!mpvInitialized || player.isExiting || isFinishing) return

        val aspect = player.getVideoOutAspect()
        Log.d(TAG, "video-params/aspect changed: $aspect")
        pipHelper.updatePictureInPictureParams()
        val aspectOverride = MPVLib.getPropertyDouble("video-aspect-override") ?: -1.0
        if (playerPreferences.orientation.get() == PlayerOrientation.Video &&
            aspect != null &&
            aspectOverride <= 0.0) {
          setOrientation()
        }
      }
    }
  }

  internal fun onObserverEvent(
    property: String,
    value: String,
  ) {
  }

  internal fun onObserverEvent(_property: String) {}

  internal fun event(eventId: Int) {
    when (eventId) {
      MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
        handleFileLoaded()
        isReady = true
      }

      MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
        player.isExiting = false
        if (!isReady) {
          isReady = true
        }
      }
    }
  }

  private fun handleFileLoaded() {
    if (fileName.isBlank()) {
      fileName = getFileName(intent)
      if (fileName.isBlank()) {
        fileName = intent.data?.lastPathSegment ?: "Unknown Video"
      }
    }
    mediaIdentifier = getMediaIdentifier(intent, fileName)

    startBackgroundPlayback()

    val currentUri = if (playlist.isNotEmpty() && playlistIndex in playlist.indices) {
      playlist[playlistIndex]
    } else {
      extractUriFromIntent(intent)
    }
    currentUri?.let { viewModel.calculateVideoHash(it) }

    viewModel.clearABLoop()

    progressSaveManager.resetTracking()

    viewModel.clearPlaylistLoadingState()

    setIntentExtras(intent.extras)

    lifecycleScope.launch(Dispatchers.IO) {
      val hasState = loadVideoPlaybackState()

      trackSelector.onFileLoaded(hasState)

      if (!hasState) {
        withContext(Dispatchers.Main) {
          val zoomPreference = playerPreferences.defaultVideoZoom.get()
          MPVLib.setPropertyDouble("video-zoom", zoomPreference.toDouble())
          viewModel.setVideoZoom(zoomPreference)
        }
      }

      withContext(Dispatchers.Main) {
        val savedAspect = playerPreferences.defaultVideoAspect.get()
        val savedCustomRatio = playerPreferences.defaultCustomAspectRatio.get()

        if (savedCustomRatio > 0) {
          viewModel.setCustomAspectRatio(savedCustomRatio)
        } else {
          viewModel.changeVideoAspect(savedAspect, showUpdate = false)
        }
      }
    }

    lifecycleScope.launch(Dispatchers.IO) {
      if (playlist.isNotEmpty()) {
        if (playlistIndex >= 0 && playlistIndex < playlist.size) {
          saveRecentlyPlayedForUri(playlist[playlistIndex], fileName)
        } else {
          Log.w(TAG, "Cannot save recently played: invalid playlist index $playlistIndex")
        }
      } else {
        saveRecentlyPlayed()
      }
    }

    if (playerPreferences.orientation.get() != PlayerOrientation.Video) {
      setOrientation()
    } else {
      lifecycleScope.launch {
        kotlinx.coroutines.delay(100)
        if (mpvInitialized && !player.isExiting && !isFinishing) {
          val aspect = player.getVideoOutAspect()
          if (aspect != null && aspect > 0) {
            setOrientation()
          }
        }
      }
    }

    applySubtitlePreferences()

    updateDisplayTitle()

    viewModel.unpause()

    if (subtitlesPreferences.autoloadMatchingSubtitles.get()) {
      lifecycleScope.launch {
        val networkFilePath = intent.getStringExtra("network_file_path")
        val networkConnectionId = intent.getLongExtra("network_connection_id", -1L)

        if (networkFilePath != null && networkConnectionId != -1L) {
          SubtitleOps.autoloadSubtitles(
            videoFilePath = networkFilePath,
            videoFileName = fileName,
            networkConnectionId = networkConnectionId,
          )
        } else {
          val filePath = parsePathFromIntent(intent)
          if (filePath != null) {
            SubtitleOps.autoloadSubtitles(
              videoFilePath = filePath,
              videoFileName = fileName,
            )
          }
        }
      }
    }

    fetchNetworkStreamTitle()
  }

  private fun fetchNetworkStreamTitle() {
    lifecycleScope.launch(Dispatchers.IO) {
      try {
        val uri = extractUriFromIntent(intent)
        if (uri == null || !HttpUtils.isNetworkStream(uri)) {
          return@launch
        }

        if (isCurrentStreamM3U()) {
          return@launch
        }

        if (intent.hasExtra("title") || intent.hasExtra("filename")) {
          return@launch
        }

        val hostName = uri.host?.lowercase()
        if (hostName == "127.0.0.1" || hostName == "localhost" || hostName == "0.0.0.0") {
          return@launch
        }

        val url = uri.toString()
        val betterFilename = HttpUtils.extractFilenameFromUrl(url)
        if (!betterFilename.isNullOrBlank() &&
          betterFilename != fileName &&
          betterFilename != uri.host &&
          betterFilename != "Network Stream"
        ) {

          fileName = betterFilename

          withContext(Dispatchers.Main) {
            updateDisplayTitle()
          }

          val filePath = when (uri.scheme) {
            "file" -> uri.path ?: uri.toString()
            "content" -> {
              contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null,
              )?.use { cursor ->
                if (cursor.moveToFirst()) {
                  val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                  if (columnIndex != -1) cursor.getString(columnIndex) else null
                } else null
              } ?: uri.toString()
            }

            else -> uri.toString()
          }

          val updatedDuration = runCatching {
            (MPVLib.getPropertyDouble("duration") ?: 0.0).times(1000).toLong()
          }.getOrDefault(0L)

          val updatedFileSize = runCatching {
            MPVLib.getPropertyDouble("file-size")?.toLong()
              ?: MPVLib.getPropertyDouble("stream-end")?.toLong()
              ?: 0L
          }.getOrDefault(0L)

          val updatedWidth = runCatching {
            MPVLib.getPropertyInt("width") ?: MPVLib.getPropertyInt("video-params/w") ?: 0
          }.getOrDefault(0)

          val updatedHeight = runCatching {
            MPVLib.getPropertyInt("height") ?: MPVLib.getPropertyInt("video-params/h") ?: 0
          }.getOrDefault(0)

          runCatching {
            RecentlyPlayedOps.updateVideoMetadata(
              filePath,
              fileName,
              updatedDuration,
              updatedFileSize,
              updatedWidth,
              updatedHeight,
            )
          }.onFailure { e ->
            Log.e(TAG, "Error updating video metadata in recently played", e)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error fetching network stream title", e)
      }
    }
  }

  private fun applySubtitlePreferences() {
    val font = subtitlesPreferences.font.get()
    MPVLib.setPropertyString("sub-font", font)
    MPVLib.setPropertyString("secondary-sub-font", font)
    MPVLib.setPropertyInt("sub-font-size", subtitlesPreferences.fontSize.get())
    MPVLib.setPropertyBoolean("sub-bold", subtitlesPreferences.bold.get())
    MPVLib.setPropertyBoolean("sub-italic", subtitlesPreferences.italic.get())
    MPVLib.setPropertyString("sub-justify", subtitlesPreferences.justification.get().value)
    MPVLib.setPropertyString("sub-border-style", subtitlesPreferences.borderStyle.get().value)
    MPVLib.setPropertyInt("sub-outline-size", subtitlesPreferences.borderSize.get())
    MPVLib.setPropertyInt("sub-shadow-offset", subtitlesPreferences.shadowOffset.get())

    MPVLib.setPropertyString("sub-color", subtitlesPreferences.textColor.get().toColorHexString())
    MPVLib.setPropertyString("sub-border-color", subtitlesPreferences.borderColor.get().toColorHexString())
    MPVLib.setPropertyString("sub-back-color", subtitlesPreferences.backgroundColor.get().toColorHexString())

    val overrideAssSubs = subtitlesPreferences.overrideAssSubs.get()
    MPVLib.setPropertyString("sub-ass-override", if (overrideAssSubs) "force" else "scale")
    MPVLib.setPropertyString("secondary-sub-ass-override", if (overrideAssSubs) "force" else "scale")

    val scaleByWindow = subtitlesPreferences.scaleByWindow.get()
    val scaleValue = if (scaleByWindow) "yes" else "no"
    MPVLib.setPropertyString("sub-scale-by-window", scaleValue)
    MPVLib.setPropertyString("sub-use-margins", scaleValue)

    MPVLib.setPropertyFloat("sub-scale", subtitlesPreferences.subScale.get())
    MPVLib.setPropertyInt("sub-pos", subtitlesPreferences.subPos.get())

    Log.d(TAG, "Applied subtitle preferences")
  }

  @OptIn(ExperimentalStdlibApi::class)
  private fun Int.toColorHexString() = "#" + this.toHexString().uppercase()

  /**
   * Captures a snapshot of the current playback state.
   * This MUST be called on the main thread to ensure data consistency.
   */
  private fun capturePlaybackSnapshot(): PlaybackStateSnapshot? {
    if (mediaIdentifier.isBlank()) return null

    return PlaybackStateSnapshot(
      mediaIdentifier = mediaIdentifier,
      position = viewModel.pos ?: 0,
      duration = viewModel.duration ?: 0,
      playbackSpeed = MPVLib.getPropertyDouble("speed") ?: DEFAULT_PLAYBACK_SPEED,
      videoZoom = MPVLib.getPropertyDouble("video-zoom")?.toFloat() ?: 0f,
      sid = player.sid,
      secondarySid = player.secondarySid,
      subDelay = ((MPVLib.getPropertyDouble("sub-delay") ?: 0.0) * MILLISECONDS_TO_SECONDS).toInt(),
      subSpeed = MPVLib.getPropertyDouble("sub-speed") ?: DEFAULT_SUB_SPEED,
      aid = player.aid,
      audioDelay = ((MPVLib.getPropertyDouble("audio-delay") ?: 0.0) * MILLISECONDS_TO_SECONDS).toInt(),
      externalSubtitles = viewModel.externalSubtitles.joinToString("|"),
      savePositionOnQuit = playerPreferences.savePositionOnQuit.get()
    )
  }

  /**
   * Saves current video progress using a state snapshot.
   */
  private fun saveVideoProgress(isImmediate: Boolean = false) {
    val snapshot = capturePlaybackSnapshot() ?: return

    lifecycleScope.launch(Dispatchers.IO) {
      val oldState = runCatching {
        playbackStateRepository.getVideoDataByTitle(snapshot.mediaIdentifier)
      }.getOrNull()

      progressSaveManager.saveProgress(
        snapshot = snapshot,
        oldState = oldState,
        isImmediate = isImmediate
      )
    }
  }

  private suspend fun loadVideoPlaybackState(): Boolean {
    if (mediaIdentifier.isBlank()) return false

    return runCatching {
      val state = playbackStateRepository.getVideoDataByTitle(mediaIdentifier)

      applyPlaybackState(state)
      applyDefaultSettings(state)

      state != null
    }.onFailure { e ->
      Log.e(TAG, "Error loading playback state", e)
    }.getOrDefault(false)
  }

  private fun applyPlaybackState(state: PlaybackStateEntity?) {
    if (state == null) return

    val subDelay = state.subDelay / DELAY_DIVISOR
    val audioDelay = state.audioDelay / DELAY_DIVISOR

    if (state.externalSubtitles.isNotBlank()) {
      val externalSubUris = state.externalSubtitles.split("|").filter { it.isNotBlank() }
      for (subUri in externalSubUris) {
        viewModel.addSubtitle(Uri.parse(subUri), select = false, silent = true)
      }
    }

    if (state.sid > 0) {
      player.sid = state.sid
    }

    if (state.secondarySid > 0) {
      player.secondarySid = state.secondarySid
    }

    if (state.aid > 0) {
      player.aid = state.aid
    }

    MPVLib.setPropertyDouble("sub-delay", subDelay)
    MPVLib.setPropertyDouble("speed", state.playbackSpeed)
    MPVLib.setPropertyDouble("audio-delay", audioDelay)
    MPVLib.setPropertyDouble("sub-speed", state.subSpeed)

    MPVLib.setPropertyDouble("video-zoom", state.videoZoom.toDouble())
    viewModel.setVideoZoom(state.videoZoom)

    if (playerPreferences.savePositionOnQuit.get() && state.lastPosition != 0) {
      MPVLib.setPropertyInt("time-pos", state.lastPosition)
    }
  }

  private fun applyDefaultSettings(state: PlaybackStateEntity?) {
    if (state == null) {
      val defaultSubSpeed = subtitlesPreferences.defaultSubSpeed.get().toDouble()
      MPVLib.setPropertyDouble("sub-speed", defaultSubSpeed)
    }
  }

  private suspend fun saveRecentlyPlayed() {
    runCatching {
      val uri = extractUriFromIntent(intent) ?: return@runCatching

      val filePath =
        when (uri.scheme) {
          "file" -> {
            uri.path ?: uri.toString()
          }

          "content" -> {
            contentResolver
              .query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null,
              )?.use { cursor ->
                if (cursor.moveToFirst()) {
                  val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                  if (columnIndex != -1) cursor.getString(columnIndex) else null
                } else {
                  null
                }
              } ?: uri.toString()
          }

          else -> {
            uri.toString()
          }
        }

      val launchSource =
        when {
          intent.getStringExtra("launch_source") != null -> intent.getStringExtra("launch_source")
          intent.action == Intent.ACTION_SEND -> "share"
          else -> "normal"
        }

      val videoTitle = runCatching {
        MPVLib.getPropertyString("media-title")
      }.getOrNull()?.takeIf { it.isNotBlank() && it != fileName }

      val duration = runCatching {
        (MPVLib.getPropertyDouble("duration") ?: 0.0).times(1000).toLong()
      }.getOrDefault(0L)

      val fileSize = runCatching {
        MPVLib.getPropertyDouble("file-size")?.toLong()
          ?: MPVLib.getPropertyDouble("stream-end")?.toLong()
          ?: 0L
      }.getOrDefault(0L)

      val width = runCatching {
        MPVLib.getPropertyInt("width") ?: MPVLib.getPropertyInt("video-params/w") ?: 0
      }.getOrDefault(0)

      val height = runCatching {
        MPVLib.getPropertyInt("height") ?: MPVLib.getPropertyInt("video-params/h") ?: 0
      }.getOrDefault(0)

      RecentlyPlayedOps.addRecentlyPlayed(
        filePath = filePath,
        fileName = fileName,
        videoTitle = videoTitle,
        duration = duration,
        fileSize = fileSize,
        width = width,
        height = height,
        launchSource = launchSource,
      )
    }.onFailure { e ->
      Log.e(TAG, "Error saving recently played", e)
    }
  }

  private fun setReturnIntent() {
    val resultIntent =
      Intent(RESULT_INTENT).apply {
        viewModel.pos?.let { putExtra("position", it * MILLISECONDS_TO_SECONDS) }
        viewModel.duration?.let { putExtra("duration", it * MILLISECONDS_TO_SECONDS) }
      }

    setResult(RESULT_OK, resultIntent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    setIntent(intent)

    val hasPlaylistExtras = intent.hasExtra("playlist_id") ||
      intent.hasExtra("playlist")

    val playlistFromIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableArrayListExtra("playlist", Uri::class.java) ?: emptyList()
    } else {
      @Suppress("DEPRECATION")
      intent.getParcelableArrayListExtra("playlist") ?: emptyList()
    }

    if (hasPlaylistExtras || playlistFromIntent.isNotEmpty()) {
      val newPlaylistId = intent.getIntExtra("playlist_id", -1).takeIf { it != -1 }
      playlistId = newPlaylistId
      playlistIndex = intent.getIntExtra("playlist_index", 0)
      playlistWindowOffset = 0
      playlistTotalCount = -1
      playlist = playlistFromIntent
    }

    if (playlist.isEmpty() && playlistId != null) {
      lifecycleScope.launch(Dispatchers.IO) {
        val pid = playlistId ?: return@launch
        try {
          val totalCount = playlistRepository.getPlaylistItemCount(pid)
          val items = playlistRepository.getPlaylistItemsAsUris(pid)
          withContext(Dispatchers.Main) {
            playlist = items
            playlistTotalCount = totalCount
          }
        } catch (e: Exception) {
          Log.e(TAG, "onNewIntent: Failed to load playlist from database", e)
        }
      }
    }

    if (playlist.isEmpty() && playlistId == null && playerPreferences.playlistMode.get()) {
      val path = parsePathFromIntent(intent)
      if (path != null) {
        generatePlaylistFromFolder(path)
      }
    }

    fileName = getFileName(intent)
    if (fileName.isBlank()) {
      fileName = intent.data?.lastPathSegment ?: "Unknown Video"
    }
    mediaIdentifier = getMediaIdentifier(intent, fileName)

    setHttpHeadersFromExtras(intent.extras)

    getPlayableUri(intent)?.let { uri ->
      val currentUri = intent.data ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_STREAM)
      }
      currentUri?.let { viewModel.calculateVideoHash(it) }

      lifecycleScope.launch(Dispatchers.Default) {
        MPVLib.command("loadfile", uri)
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration,
  ) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

    pipHelper.onPictureInPictureModeChanged(isInPictureInPictureMode)

    binding.controls.alpha = if (isInPictureInPictureMode) 0f else 1f

    runCatching {
      if (isInPictureInPictureMode) {
        enterPipUIMode()
      } else {
        exitPipUIMode()
      }
    }.onFailure { e ->
      Log.e(TAG, "Error handling PiP mode change", e)
    }
  }

  private fun enterPipUIMode() {
    window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    WindowCompat.setDecorFitsSystemWindows(window, true)
    try {
      WindowCompat.getInsetsController(window, window.decorView).apply {
        show(WindowInsetsCompat.Type.systemBars())
        show(WindowInsetsCompat.Type.navigationBars())
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to show system bars for PiP mode", e)
    }
  }

  @RequiresApi(Build.VERSION_CODES.P)
  private fun exitPipUIMode() {
    setupWindowFlags()
    setupSystemUI()
  }

  fun enterPipModeHidingOverlay() {
    runCatching {
      enterPipUIMode()
    }.onFailure { e ->
      Log.e(TAG, "Error entering PiP mode with hidden overlay", e)
    }

    binding.controls.alpha = 0f

    pipHelper.enterPipMode()
  }

  private fun setOrientation() {
    val orientationPref = playerPreferences.orientation.get()

    requestedOrientation =
      when (orientationPref) {
        PlayerOrientation.Free -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        PlayerOrientation.Video -> {
          val aspect = runCatching { player.getVideoOutAspect() }.getOrNull()
          if (aspect == null || aspect <= 0.0) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
          } else {
            if (aspect > 1.0) {
              ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
              ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
          }
        }
        PlayerOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        PlayerOrientation.ReversePortrait -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        PlayerOrientation.SensorPortrait -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        PlayerOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        PlayerOrientation.ReverseLandscape -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        PlayerOrientation.SensorLandscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      }
  }

  @Suppress("ReturnCount", "CyclomaticComplexMethod", "LongMethod")
  override fun onKeyDown(
    keyCode: Int,
    event: KeyEvent?,
  ): Boolean {
    val isTrackSheetOpen =
      viewModel.sheetShown.value == Sheets.SubtitleTracks ||
        viewModel.sheetShown.value == Sheets.AudioTracks
    val isNoSheetOpen = viewModel.sheetShown.value == Sheets.None

    when (keyCode) {
      KeyEvent.KEYCODE_DPAD_UP -> {
        return super.onKeyDown(keyCode, event)
      }

      KeyEvent.KEYCODE_DPAD_DOWN,
      KeyEvent.KEYCODE_DPAD_RIGHT,
      KeyEvent.KEYCODE_DPAD_LEFT,
        -> {
        if (isTrackSheetOpen) {
          return super.onKeyDown(keyCode, event)
        }

        if (isNoSheetOpen) {
          when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
              viewModel.handleRightDoubleTap()
              return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
              viewModel.handleLeftDoubleTap()
              return true
            }
          }
        }
        return super.onKeyDown(keyCode, event)
      }

      KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
        if (isTrackSheetOpen) {
          return super.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
      }

      KeyEvent.KEYCODE_SPACE -> {
        viewModel.pauseUnpause()
        return true
      }

      KeyEvent.KEYCODE_VOLUME_UP -> {
        viewModel.changeVolumeBy(1)
        viewModel.displayVolumeSlider()
        return true
      }

      KeyEvent.KEYCODE_VOLUME_DOWN -> {
        viewModel.changeVolumeBy(-1)
        viewModel.displayVolumeSlider()
        return true
      }

      KeyEvent.KEYCODE_MEDIA_STOP -> {
        finishAndRemoveTask()
        return true
      }

      KeyEvent.KEYCODE_MEDIA_REWIND -> {
        viewModel.handleLeftDoubleTap()
        return true
      }

      KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
        viewModel.handleRightDoubleTap()
        return true
      }

      else -> {
        event?.let { player.onKey(it) }
        return super.onKeyDown(keyCode, event)
      }
    }
  }

  override fun onKeyUp(
    keyCode: Int,
    event: KeyEvent?,
  ): Boolean {
    event?.let {
      if (player.onKey(it)) return true
    }
    return super.onKeyUp(keyCode, event)
  }

  private val serviceConnection =
    object : ServiceConnection {
      override fun onServiceConnected(
        name: ComponentName?,
        service: IBinder?,
      ) {
        val binder = service as? MediaPlaybackService.MediaPlaybackBinder ?: return
        mediaPlaybackService = binder.getService()
        serviceBound = true
      }

      override fun onServiceDisconnected(name: ComponentName?) {
        mediaPlaybackService = null
        serviceBound = false
      }
    }

  private fun startBackgroundPlayback() {
    if (fileName.isBlank() || !isReady) {
      return
    }

    if (serviceBound) {
      return
    }

    MediaPlaybackService.createNotificationChannel(this)

    val artist = runCatching { MPVLib.getPropertyString("metadata/artist") }.getOrNull() ?: ""
    val thumbnail = runCatching { MPVLib.grabThumbnail(1080) }.getOrNull()

    val intent = Intent(this, MediaPlaybackService::class.java).apply {
      putExtra("media_title", fileName)
      putExtra("media_artist", artist)
    }

    MediaPlaybackService.thumbnail = thumbnail

    try {
      startForegroundService(intent)
      bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    } catch (e: Exception) {
      Log.e(TAG, "Error starting/binding service", e)
    }
  }

  private fun endBackgroundPlayback() {
    if (serviceBound) {
      try {
        unbindService(serviceConnection)
      } catch (e: Exception) {
        Log.e(TAG, "Error unbinding service", e)
      }
      serviceBound = false
    }

    try {
      stopService(Intent(this, MediaPlaybackService::class.java))
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping service", e)
    }

    mediaPlaybackService = null
  }

  override val context: Context
    get() = this
  override val windowInsetsController: WindowInsetsControllerCompat
    get() = WindowCompat.getInsetsController(window, window.decorView)
  override val hostWindow: android.view.Window
    get() = window
  override val hostWindowManager: WindowManager
    get() = windowManager
  override val hostContentResolver: android.content.ContentResolver
    get() = contentResolver
  override val audioManager: AudioManager
    get() = getSystemService(AUDIO_SERVICE) as AudioManager
  override var hostRequestedOrientation: Int
    get() = requestedOrientation
    set(value) {
      requestedOrientation = value
    }

  fun hasNext(): Boolean {
    if (playlist.isEmpty()) return false

    if (viewModel.shouldRepeatPlaylist()) return true

    val effectiveSize = if (playlistTotalCount > 0) playlistTotalCount else playlist.size

    return if (viewModel.shuffleEnabled.value) {
      shuffledPosition < shuffledIndices.size - 1
    } else {
      playlistIndex < effectiveSize - 1
    }
  }

  fun hasPrevious(): Boolean {
    if (playlist.isEmpty()) return false

    if (viewModel.shouldRepeatPlaylist()) return true

    return if (viewModel.shuffleEnabled.value) {
      shuffledPosition > 0
    } else {
      playlistIndex > 0
    }
  }

  private fun generateShuffledIndices() {
    if (playlist.isEmpty()) return

    val indices = playlist.indices.filter { it != playlistIndex }.toMutableList()
    indices.shuffle()

    shuffledIndices = listOf(playlistIndex) + indices
    shuffledPosition = 0
  }

  fun onShuffleToggled(enabled: Boolean) {
    if (enabled && playlist.isNotEmpty()) {
      generateShuffledIndices()
    } else {
      shuffledIndices = emptyList()
      shuffledPosition = 0
    }
  }

  fun playNext() {
    if (playlist.isEmpty()) return

    val effectiveSize = if (playlistTotalCount > 0) playlistTotalCount else playlist.size

    if (viewModel.shuffleEnabled.value) {
      if (shuffledIndices.isEmpty()) {
        generateShuffledIndices()
      }

      if (shuffledPosition < shuffledIndices.size - 1) {
        shuffledPosition++
        playlistIndex = shuffledIndices[shuffledPosition]
        loadPlaylistItem(playlistIndex)
      } else if (viewModel.shouldRepeatPlaylist()) {
        generateShuffledIndices()
        shuffledPosition = 0
        playlistIndex = shuffledIndices[0]
        loadPlaylistItem(playlistIndex)
      }
    } else {
      if (playlistIndex < effectiveSize - 1) {
        playlistIndex++
        loadPlaylistItem(playlistIndex)
      } else if (viewModel.shouldRepeatPlaylist()) {
        playlistIndex = 0
        loadPlaylistItem(0)
      }
    }
  }

  fun playPrevious() {
    if (playlist.isEmpty()) return

    val effectiveSize = if (playlistTotalCount > 0) playlistTotalCount else playlist.size

    if (viewModel.shuffleEnabled.value) {
      if (shuffledIndices.isEmpty()) {
        generateShuffledIndices()
      }

      if (shuffledPosition > 0) {
        shuffledPosition--
        playlistIndex = shuffledIndices[shuffledPosition]
        loadPlaylistItem(playlistIndex)
      } else if (viewModel.shouldRepeatPlaylist()) {
        shuffledPosition = shuffledIndices.size - 1
        playlistIndex = shuffledIndices[shuffledPosition]
        loadPlaylistItem(playlistIndex)
      }
    } else {
      if (playlistIndex > 0) {
        playlistIndex--
        loadPlaylistItem(playlistIndex)
      } else if (viewModel.shouldRepeatPlaylist()) {
        playlistIndex = effectiveSize - 1
        loadPlaylistItem(playlistIndex)
      }
    }
  }

  private fun loadPlaylistItem(index: Int) {
    if (index < 0 || index >= playlist.size) {
      return
    }
    loadPlaylistItemInternal(index)
  }

  private fun loadPlaylistItemInternal(index: Int) {
    if (index < 0 || index >= playlist.size) {
      return
    }

    // Capture snapshot for the PREVIOUS video
    if (fileName.isNotBlank()) {
      val snapshot = capturePlaybackSnapshot()
      lifecycleScope.launch(Dispatchers.IO) {
        if (snapshot != null) {
          val oldState = runCatching {
            playbackStateRepository.getVideoDataByTitle(snapshot.mediaIdentifier)
          }.getOrNull()

          progressSaveManager.saveProgress(
            snapshot = snapshot,
            oldState = oldState,
            isImmediate = true
          )
        }
      }
    }

    val uri = playlist[index]
    playlistIndex = index

    // Immediate UI Feedback: Predict filename and update UI before heavy processing
    val rawFileName = getFileNameFromUri(uri)
    fileName = rawFileName
    mediaIdentifier = getMediaIdentifier(intent, rawFileName)
    val displayTitle = formatTitle(rawFileName, isUriM3U(uri))
    viewModel.setMediaTitle(displayTitle)

    lifecycleScope.launch(Dispatchers.Default) {
      // Step 1: Open content FD and load file as fast as possible
      val playableUri = uri.openContentFd(this@PlayerActivity) ?: uri.toString()
      MPVLib.command("loadfile", playableUri)

      // Step 2: Handle heavy background tasks after engine has been poked
      setHttpHeadersForUri(uri)

      playlistId?.let { id ->
        val filePath = when (uri.scheme) {
          "file" -> uri.path ?: uri.toString()
          "content" -> {
            contentResolver.query(
              uri,
              arrayOf(MediaStore.MediaColumns.DATA),
              null,
              null,
              null,
            )?.use { cursor ->
              if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                if (columnIndex != -1) cursor.getString(columnIndex) else null
              } else null
            } ?: uri.toString()
          }
          else -> uri.toString()
        }

        runCatching {
          playlistRepository.updatePlayHistory(id, filePath)
        }.onFailure { e ->
          Log.e(TAG, "Error updating playlist history", e)
        }
      }

      // Step 3: Refresh UI and metadata without artificial delay
      withContext(Dispatchers.Main) {
        updateDisplayTitle()
        viewModel.refreshPlaylistItems()
      }
    }
  }

  private fun formatTitle(title: String, isStream: Boolean = false): String {
    if (playerPreferences.showFileExtension.get()) return title
    if (isStream) return title

    val lastDotIndex = title.lastIndexOf('.')
    return if (lastDotIndex > 0) {
      title.substring(0, lastDotIndex)
    } else {
      title
    }
  }

  private fun getFileNameFromUri(uri: Uri): String {
    return getDisplayNameFromUri(uri) ?: extractFileNameFromUri(uri)
  }

  fun getTitleForControls(): String {
    if (isCurrentStreamM3U()) {
      val rawTitle = MPVLib.getPropertyString("media-title")
      if (!rawTitle.isNullOrBlank()) {
        return rawTitle
      }
    }
    return formatTitle(fileName)
  }

  private fun updateDisplayTitle() {
    if (isCurrentStreamM3U()) return

    val displayTitle = formatTitle(fileName)
    MPVLib.setPropertyString("force-media-title", displayTitle)
    viewModel.setMediaTitle(displayTitle)

    // Push title/artist/thumbnail to the Service, which owns the MediaSession + notification.
    if (serviceBound && mediaPlaybackService != null) {
      val artist = runCatching { MPVLib.getPropertyString("metadata/artist") }.getOrNull() ?: ""
      val thumbnail = runCatching { MPVLib.grabThumbnail(1080) }.getOrNull()
      mediaPlaybackService?.setMediaInfo(title = displayTitle, artist = artist, thumbnail = thumbnail)
    }
  }

  private fun isCurrentStreamM3U(): Boolean {
    val uri = extractUriFromIntent(intent)
    if (uri != null && isUriM3U(uri)) {
      return true
    }

    if (playlist.isNotEmpty() && playlistIndex >= 0 && playlistIndex < playlist.size) {
      return isUriM3U(playlist[playlistIndex])
    }

    return false
  }

  private fun isUriM3U(uri: Uri): Boolean {
    val lowerUrl = uri.toString().lowercase()
    return lowerUrl.contains(".m3u8") || lowerUrl.contains(".m3u") ||
      lowerUrl.endsWith(".m3u8") || lowerUrl.endsWith(".m3u")
  }

  private suspend fun saveRecentlyPlayedForUri(
    uri: Uri,
    name: String,
  ) {
    runCatching {
      val filePath =
        when (uri.scheme) {
          "file" -> {
            uri.path ?: uri.toString()
          }

          "content" -> {
            contentResolver
              .query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATA),
                null,
                null,
                null,
              )?.use { cursor ->
                if (cursor.moveToFirst()) {
                  val columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                  if (columnIndex != -1) cursor.getString(columnIndex) else null
                } else {
                  null
                }
              } ?: uri.toString()
          }

          else -> {
            uri.toString()
          }
        }

      val videoTitle = runCatching {
        MPVLib.getPropertyString("media-title")
      }.getOrNull()?.takeIf { it.isNotBlank() && it != name }

      val duration = runCatching {
        (MPVLib.getPropertyDouble("duration") ?: 0.0).times(1000).toLong()
      }.getOrDefault(0L)

      val fileSize = runCatching {
        MPVLib.getPropertyDouble("file-size")?.toLong()
          ?: MPVLib.getPropertyDouble("stream-end")?.toLong()
          ?: 0L
      }.getOrDefault(0L)

      val width = runCatching {
        MPVLib.getPropertyInt("width") ?: MPVLib.getPropertyInt("video-params/w") ?: 0
      }.getOrDefault(0)

      val height = runCatching {
        MPVLib.getPropertyInt("height") ?: MPVLib.getPropertyInt("video-params/h") ?: 0
      }.getOrDefault(0)

      RecentlyPlayedOps.addRecentlyPlayed(
        filePath = filePath,
        fileName = name,
        videoTitle = videoTitle,
        duration = duration,
        fileSize = fileSize,
        width = width,
        height = height,
        launchSource = "playlist",
        playlistId = playlistId,
      )
    }.onFailure { e ->
      Log.e(TAG, "Error saving recently played for playlist item", e)
    }
  }

  private fun getMediaIdentifier(intent: Intent, fileName: String): String {
    val networkFilePath = intent.getStringExtra("network_file_path")
    val networkConnectionId = intent.getLongExtra("network_connection_id", -1L)

    if (networkFilePath != null && networkConnectionId != -1L) {
      return "network_${networkConnectionId}_${networkFilePath.hashCode()}"
    }

    val uri = extractUriFromIntent(intent)
    return getMediaIdentifierFromUri(uri, fileName)
  }

  /**
   * Returns the persisted-playback-state identifier for an arbitrary playlist URI,
   * mirroring how the currently-playing item's [mediaIdentifier] is derived so the
   * playlist sheet can look up each item's watched state from the database.
   */
  internal fun getPlaylistItemMediaIdentifier(uri: Uri): String =
    getMediaIdentifierFromUri(uri, getFileNameFromUri(uri))

  private fun getMediaIdentifierFromUri(uri: Uri?, fileName: String): String {
    if (uri == null) return fileName
    return if (uri.scheme?.startsWith("http") == true || uri.scheme == "rtmp" || uri.scheme == "ftp" || uri.scheme == "rtsp" || uri.scheme == "mms") {
      "${fileName}_${uri.toString().hashCode()}"
    } else {
      fileName
    }
  }

  private fun generatePlaylistFromFolder(currentPath: String) {
    lifecycleScope.launch(Dispatchers.IO) {
      runCatching {
        val currentFile = File(currentPath)
        if (!currentFile.exists()) return@runCatching

        val parentFolder = currentFile.parentFile ?: return@runCatching

        val files = parentFolder.listFiles { file ->
          file.isFile &&
            FileTypeUtils.isVideoFile(file) &&
            !FileFilterUtils.shouldSkipFile(file)
        } ?: return@runCatching

        val launchSource = intent.getStringExtra("launch_source") ?: ""
        val siblingFiles = if (launchSource == "video_list" || launchSource == "recently_played_button" || launchSource == "first_video_button") {
          val videoSortType = browserPreferences.videoSortType.get()
          val videoSortOrder = browserPreferences.videoSortOrder.get()
          val bucketId = parentFolder.absolutePath.replace("\\", "/")
          val videosInFolder =
            app.marlboroadvance.mpvex.repository.MediaFileRepository.getVideosForBuckets(
              context,
              setOf(bucketId)
            )
          val sortedVideos = app.marlboroadvance.mpvex.utils.sort.SortUtils.sortVideos(videosInFolder, videoSortType, videoSortOrder)
          sortedVideos.mapNotNull { video -> files.find { it.absolutePath == video.path } }
        } else {
          files.sortedWith { f1, f2 -> app.marlboroadvance.mpvex.utils.sort.SortUtils.NaturalOrderComparator.DEFAULT.compare(f1.name, f2.name) }
        }

        if (siblingFiles.size <= 1) return@runCatching

        val newPlaylist = siblingFiles.map { it.toUri() }

        val newIndex = siblingFiles.indexOfFirst { it.absolutePath == currentFile.absolutePath }

        if (newIndex != -1) {
          withContext(Dispatchers.Main) {
            playlist = newPlaylist
            playlistIndex = newIndex
            if (viewModel.shuffleEnabled.value) {
              onShuffleToggled(true)
            }
          }
        }
      }.onFailure { e ->
        Log.e(TAG, "Failed to auto-generate playlist", e)
      }
    }
  }

  fun isCurrentPlaylistM3U(): Boolean = isM3uPlaylist


  companion object {
    private const val RESULT_INTENT = "app.marlboroadvance.mpvex.ui.player.PlayerActivity.result"
    private const val BRIGHTNESS_NOT_SET = -1f
    private const val POSITION_NOT_SET = 0
    private const val MAX_MPV_VOLUME = 100
    private const val MILLISECONDS_TO_SECONDS = 1000
    private const val DELAY_DIVISOR = 1000.0
    private const val DEFAULT_PLAYBACK_SPEED = 1.0
    private const val DEFAULT_SUB_SPEED = 1.0
    const val TAG = "mpvex"
  }
}
