package app.marlboroadvance.mpvex.repository

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Lifecycle observer to disconnect network shares when the app goes to background
 */
class NetworkLifecycleObserver(
    private val networkRepository: NetworkRepository
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d("NetworkLifecycle", "App in background: Disconnecting all idle network shares to save battery")
        scope.launch {
            try {
                networkRepository.disconnectAll()
            } catch (e: Exception) {
                Log.e("NetworkLifecycle", "Error disconnecting shares", e)
            }
        }
    }
}
