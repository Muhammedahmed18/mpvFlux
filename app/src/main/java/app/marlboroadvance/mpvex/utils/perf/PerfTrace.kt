package app.marlboroadvance.mpvex.utils.perf

import android.os.SystemClock
import android.os.Trace
import android.util.Log
import app.marlboroadvance.mpvex.BuildConfig

object PerfTrace {
  @Volatile
  var logEnabled: Boolean = BuildConfig.DEBUG

  inline fun <T> section(
    name: String,
    block: () -> T,
  ): T {
    Trace.beginSection(name)
    return try {
      block()
    } finally {
      Trace.endSection()
    }
  }

  inline fun <T> time(
    tag: String,
    name: String,
    thresholdMs: Long = 0,
    block: () -> T,
  ): T {
    if (!logEnabled) return block()
    val startNs = SystemClock.elapsedRealtimeNanos()
    val result = block()
    val elapsedMs = (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000L
    if (elapsedMs >= thresholdMs) {
      Log.d(tag, "$name took ${elapsedMs}ms")
    }
    return result
  }
}
