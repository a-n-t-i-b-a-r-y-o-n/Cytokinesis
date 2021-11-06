package com.hexagonal.cytokinesis

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.*
import android.net.wifi.WifiManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import java.lang.Exception

class WifiTileService() : TileService() {

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
            setTileInactive()
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

    // Helper to update the tile to active
    private fun setTileActive(subtitle: String) {
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.icon = Icon.createWithResource(applicationContext, R.drawable.wifi_strength_4)
        qsTile.subtitle = subtitle

        qsTile.updateTile()
    }

    // Helper to update the tile to inactive
    private fun setTileInactive() {
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.icon = Icon.createWithResource(applicationContext, R.drawable.wifi_strength_off_outline)
        qsTile.subtitle = "Disconnected"

        qsTile.updateTile()
    }

    // TODO: Check permissions!
    @SuppressLint("MissingPermission")
    fun onStateChange(state: WifiNetworkStates, network: Network?) {
        /*
        // DEBUG: Write network data to log
        Log.w("[WifiTileService]", "WiFi network state: " + state.name)
        Log.w("[WifiTileService]", "Network Capabilities: " + getConnectionManager(applicationContext).getNetworkCapabilities(network)?.toString())
        Log.w("[WifiTileService]", "Link Properties: " + getConnectionManager(applicationContext).getLinkProperties(network)?.toString())
         */
        if (qsTile != null) {
            enumValues<WifiNetworkStates>().forEach { possibleState ->
                if (possibleState == state) {
                    if (state == WifiNetworkStates.CONNECTED && network != null) {

                        /*
                        // As much as I would love to show the SSID, this only returns <unknown_ssid>,
                        //  even with every possible type of location permission enabled.

                        // Determine wireless network SSID
                        val ssid = getWifiManager(applicationContext).connectionInfo.ssid
                        setTileActive(ssid)
                         */

                        val properties = getConnectionManager(applicationContext).getLinkProperties(network)
                        if (properties != null) {
                            val ipv4 = properties.linkAddresses[0].address.toString().substring(1)
                            setTileActive(ipv4)
                        }
                        else {
                            setTileActive("")
                        }
                    }
                }
            }
        }
        else {
            /*
            // DEBUG: Note incorrect state change in logs
            Log.w("[WifiTileService]", "Tried to call onStateChange() but qsTile is null.")
             */
        }
    }

    // Helper to get the ConnectionManager with given context
    // NOTE: This must be called from a given function override. There is no context at construction.
    private fun getConnectionManager(context: Context): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Helper to get the WifiManager with given context
    // NOTE: This must be called from a given function override. There is no context at construction.
    private fun getWifiManager(context: Context): WifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    enum class WifiNetworkStates {
        CONNECTED,
        DISCONNECTED,
        UNAVAILABLE,
    }

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