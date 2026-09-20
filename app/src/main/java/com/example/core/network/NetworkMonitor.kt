package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Section 65: Connectivity-Aware Synchronization
 * Observes network availability using Android ConnectivityManager.
 * When connectivity becomes available, triggers immediate outbox synchronization.
 */
class NetworkMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onNetworkRestored: () -> Unit = {}
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnlineFlow = MutableStateFlow(checkIsOnline())
    val isOnlineFlow: StateFlow<Boolean> = _isOnlineFlow.asStateFlow()

    val isOnline: Boolean
        get() = _isOnlineFlow.value

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val hasInternet = checkIsOnline()
            Log.d(TAG, "Network became available. Has validated internet: $hasInternet")
            if (hasInternet) {
                val wasOffline = !_isOnlineFlow.value
                _isOnlineFlow.value = true
                if (wasOffline) {
                    scope.launch {
                        try {
                            onNetworkRestored()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error executing onNetworkRestored: ${e.message}")
                        }
                    }
                }
            }
        }

        override fun onLost(network: Network) {
            val isStillOnline = checkIsOnline()
            Log.d(TAG, "Network lost. Still online: $isStillOnline")
            _isOnlineFlow.value = isStillOnline
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val wasOffline = !_isOnlineFlow.value
            _isOnlineFlow.value = hasInternet
            if (wasOffline && hasInternet) {
                scope.launch {
                    try {
                        onNetworkRestored()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onNetworkRestored during capabilities change: ${e.message}")
                    }
                }
            }
        }
    }

    fun startMonitoring() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
            _isOnlineFlow.value = checkIsOnline()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    fun stopMonitoring() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister network callback: ${e.message}")
        }
    }

    fun checkIsOnline(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // For testing / simulation purposes
    fun setSimulatedOnline(online: Boolean) {
        val wasOffline = !_isOnlineFlow.value
        _isOnlineFlow.value = online
        if (wasOffline && online) {
            scope.launch {
                try {
                    onNetworkRestored()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in simulated onNetworkRestored: ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "NetworkMonitor"
    }
}
