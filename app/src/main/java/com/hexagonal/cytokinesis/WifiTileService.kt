package com.hexagonal.cytokinesis

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.*
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.preference.PreferenceManager
import java.lang.Exception

class WifiTileService : TileService() {

    // Network type to watch
    private val request: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .setIncludeOtherUidNetworks(true)
        .build()

    // Custom ConnectivityManager.NetworkCallback class
    private val netCallback: WifiNetworkCallback = WifiNetworkCallback { state, network -> onStateChange(state, network)}

    // Called right before tile enters view
    override fun onStartListening() {
        super.onStartListening()
        // Assume off by default, since we apparently only get callbacks when connected
        if (qsTile != null) {
            updateTile(
                Tile.STATE_INACTIVE,
                "Disconnected",
                R.drawable.wifi_strength_off_outline,
            )
        }
        // Register the network callback
        getConnectionManager(applicationContext)
            .registerNetworkCallback(request, netCallback)
    }

    // Called after tile leaves view
    override fun onStopListening() {
        super.onStopListening()
        // Weakly try to unregister the network callback
        try {
            getConnectionManager(applicationContext)
                .unregisterNetworkCallback(netCallback)
        } catch (e: Exception) { }
    }

    // Called when the tile is clicked/tapped
    override fun onClick() {
        super.onClick()

        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityAndCollapse(intent)
    }

    // Update all title properties in a uniform way
    private fun updateTile(state: Int, subhead: String, icon: Int) {
        if (qsTile != null) {
            // Set properties
            qsTile.state = state
            qsTile.subtitle = subhead
            qsTile.icon = Icon.createWithResource(applicationContext, icon)
            // Perform UI update
            qsTile.updateTile()
        }
    }

    fun onStateChange(state: WifiNetworkStates, network: Network?) {
        // DEBUG: Write network data to log
        Log.w("[WifiTileService]", "WiFi network state: " + state.name)
        Log.w("[WifiTileService]", "Network Capabilities: " + getConnectionManager(applicationContext).getNetworkCapabilities(network)?.toString())
        Log.w("[WifiTileService]", "Link Properties: " + getConnectionManager(applicationContext).getLinkProperties(network)?.toString())
        if (qsTile != null && network != null) {
            if (state == WifiNetworkStates.CONNECTED) {
                updateTile(
                    Tile.STATE_ACTIVE,
                    network.getWifiSubhead(applicationContext!!),
                    network.getWifiIcon(applicationContext!!),
                )
            }
            else {
                // DEBUG: Note changes to other states in logs. Can't seem to observe one directly.
                Log.w("[WifTileService", "Network $network changed state to: ${state.name}")
                // TODO: Check other states?
                // TODO: Change tile active status or icon/subhead?
                updateTile(
                    Tile.STATE_INACTIVE,
                    "Disconnected",
                    R.drawable.wifi_strength_off_outline,
                )
            }
        }
        else {
            /*
            // DEBUG: Note incorrect state change in logs
            Log.w("[WifiTileService]", "Tried to call onStateChange() but qsTile is null.")
             */
        }
    }

    /// Helper to get the ConnectionManager with given context
    // NOTE: This must be called from a given function override. There is no context at construction.
    private fun getConnectionManager(context: Context): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /// Helper to get the WifiManager with given context
    // NOTE: This must be called from a given function override. There is no context at construction.
    private fun getWifiManager(context: Context): WifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /// Possible network states
    enum class WifiNetworkStates {
        CONNECTED,
        DISCONNECTED,
        UNAVAILABLE,
    }

    /// Network callback
    class WifiNetworkCallback(val onNetworkChange: (state: WifiNetworkStates, network: Network?) -> Unit) : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            onNetworkChange(WifiNetworkStates.CONNECTED, network)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            onNetworkChange(WifiNetworkStates.UNAVAILABLE, null)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            onNetworkChange(WifiNetworkStates.DISCONNECTED, network)
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            onNetworkChange(WifiNetworkStates.DISCONNECTED, network)
        }
    }
}