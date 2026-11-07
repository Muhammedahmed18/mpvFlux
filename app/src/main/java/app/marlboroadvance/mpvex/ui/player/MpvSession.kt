package app.marlboroadvance.mpvex.ui.player

import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Tracks whether the single, process-wide libmpv instance is alive and serializes the slow
 * work done on it off the main thread.
 *
 * libmpv lives in native globals: calling into it after MPVLib.destroy(), or calling
 * MPVLib.create() while an old instance still exists, makes the native layer call exit(1).
 * Everything that may run concurrently with teardown (the notification artwork grab and
 * the teardown itself) therefore goes through one worker thread, in order:
 * - [runIfAlive] tasks are skipped once teardown has been requested.
 * - [teardown] marks the instance dead immediately and destroys it after any task that
 *   was already running.
 * - [awaitPendingTeardown] lets a new player wait for the previous instance to be gone
 *   before it creates the next one.
 */
object MpvSession {
  private const val TAG = "MpvSession"
  private const val TEARDOWN_WAIT_MS = 5_000L

  private val worker: ExecutorService =
    Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable, "mpv-worker").apply { isDaemon = true }
    }

  @Volatile
  var isAlive: Boolean = false
    private set

  @Volatile
  private var pendingTeardown: Future<*>? = null

  /** Call right after MPVLib.create()/init() succeeded. */
  fun markCreated() {
    isAlive = true
  }

  /** Runs [block] on the mpv worker thread, unless mpv has been torn down by then. */
  fun runIfAlive(block: () -> Unit) {
    worker.execute {
      if (!isAlive) return@execute
      runCatching(block).onFailure { e -> Log.e(TAG, "mpv task failed", e) }
    }
  }

  /** Marks mpv as dead now and runs [block] (which destroys it) on the worker thread. */
  fun teardown(block: () -> Unit) {
    isAlive = false
    pendingTeardown =
      worker.submit {
        runCatching(block).onFailure { e -> Log.e(TAG, "mpv teardown failed", e) }
      }
  }

  /**
   * Blocks until a teardown requested earlier has finished. Normally it finished long ago;
   * this only waits when a new player is opened right after the previous one closed.
   */
  fun awaitPendingTeardown() {
    val teardown = pendingTeardown ?: return
    runCatching { teardown.get(TEARDOWN_WAIT_MS, TimeUnit.MILLISECONDS) }
      .onFailure { e -> Log.w(TAG, "Previous mpv teardown did not finish in time", e) }
    if (teardown.isDone) pendingTeardown = null
  }
}
