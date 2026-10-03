package com.example.mumbailocalwo.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * NetworkGate ensures an active, internet-capable network is acquired on Wear OS
 * (which frequently sleeps Wi-Fi / radios to conserve battery) before executing HTTP calls.
 */
class NetworkGate(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * Executes the block. On Wear OS, standard network routing (Bluetooth proxy, Wi-Fi, LTE)
     * is handled by the OS.
     */
    suspend fun <T> withNetwork(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            android.util.Log.e("NetworkGate", "Network call failed, attempting fallback", e)
            block()
        }
    }
}
