package app.marlboroadvance.mpvex.ui.browser.networkstreaming.proxy

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Lifecycle observer to properly manage the proxy server lifecycle
 */
class ProxyLifecycleObserver : DefaultLifecycleObserver {

  companion object {
    private const val TAG = "ProxyLifecycleObserver"
  }

  override fun onStop(owner: LifecycleOwner) {
    super.onStop(owner)
    // Shut down proxy to save battery when app is in background
    Log.d(TAG, "Stopping proxy due to app entering background")
    NetworkStreamingProxy.stopInstance()
  }

  override fun onDestroy(owner: LifecycleOwner) {
    super.onDestroy(owner)
    // Clean up proxy when app is destroyed
    NetworkStreamingProxy.stopInstance()
  }
}
