package com.hexagonal.cytokinesis

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Exception

class WifiTileService : TileService() {

    /// Possible network states
    enum class WifiNetworkStates {
        CONNECTED,
        DISCONNECTED,
        LOST,
        UNAVAILABLE,
    }

    // Object representing wifi network's state
    class WifiNetworkMetadata {
        var network: Network? = null
        var state: WifiNetworkStates = WifiNetworkStates.DISCONNECTED
    }

    // Instance cached network info/state
    private var networkMetadata: WifiNetworkMetadata = WifiNetworkMetadata()

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

        // Register the network callback
        getConnectivityManager(applicationContext)
            .registerNetworkCallback(request, netCallback)

        updateTile()
    }

    // Called after tile leaves view
    override fun onStopListening() {
        super.onStopListening()
        // Weakly try to unregister the network callback
        try {
            getConnectivityManager(applicationContext)
                .unregisterNetworkCallback(netCallback)
        } catch (e: Exception) {
            Log.d("[WifiTileService]", "Unable to unregister network callback.")
        }
    }

    // Called when the tile is clicked/tapped
    override fun onClick() {
        super.onClick()

        // Take the user to the WiFi settings screen
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityAndCollapse(intent)
    }

    // Update all title properties in a uniform way
    private fun updateTile() {
        if (qsTile != null) {
            // Perform preference checks and such concurrently
            runBlocking {
                // Set tile active state
                launch {
                    qsTile.state = when (networkMetadata.state) {
                        WifiNetworkStates.CONNECTED,
                        WifiNetworkStates.LOST -> Tile.STATE_ACTIVE
                        else -> Tile.STATE_INACTIVE
                    }
                }
                // Set tile heading
                launch {
                    val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                        .getString("wifi_heading", "network_name") ?: "network_name"
                    qsTile.label = getWifiHeading(applicationContext, preference)
                }
                // Set tile subheading
                launch {
                    val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                        .getString("wifi_subheading", "ipv4") ?: "ipv4"
                    qsTile.subtitle = getWifiHeading(applicationContext, preference)
                }
                // Set tile icon drawable
                launch { qsTile.icon = Icon.createWithResource(applicationContext, getWifiIcon(applicationContext)) }
            }
            // Perform UI update
            qsTile.updateTile()
        }
    }

    // Update tile in response to network state change
    private fun onStateChange(state: WifiNetworkStates, network: Network?) {
        /*
        // DEBUG: Write network data to log
        Log.w("[WifiTileService]", "WiFi network state: " + state.name)
        Log.w("[WifiTileService]", "Network Capabilities: " + getConnectionManager(applicationContext).getNetworkCapabilities(network)?.toString())
        Log.w("[WifiTileService]", "Link Properties: " + getConnectionManager(applicationContext).getLinkProperties(network)?.toString())
         */

        networkMetadata.state = state

        if (network != null) {
            networkMetadata.network = network
        }

        updateTile()
    }

    /// Helper to get the ConnectionManager with given context
    // NOTE: This must be called from a given function override. There is no context at construction.
    private fun getConnectivityManager(context: Context): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /// Helper to get the WifiManager with given context
    // NOTE: This must be called from a given function override. There is no context at construction.
    private fun getWifiManager(context: Context): WifiManager =
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /// WiFi icon
    private fun getWifiIcon(context: Context): Int {
        // Managers
        val connectivityManager = getConnectivityManager(context)
        val wifiManager = getWifiManager(context)
        // Transport info
        val transportInfo = connectivityManager.getNetworkCapabilities(networkMetadata.network)?.transportInfo as WifiInfo?

        // Pick icon
        return if (transportInfo != null) {
            when (wifiManager.calculateSignalLevel(transportInfo.rssi)) {
                0 -> R.drawable.wifi_strength_off_outline
                1 -> R.drawable.wifi_strength_1
                2 -> R.drawable.wifi_strength_2
                3 -> R.drawable.wifi_strength_3
                4 -> R.drawable.wifi_strength_4
                else -> R.drawable.ic_baseline_question_mark_24
            }
        }
        else {
            R.drawable.ic_baseline_question_mark_24
        }
    }
    /// Wifi heading and/or subheading
    private fun getWifiHeading(context: Context, preference: String): String {
        return when(networkMetadata.state) {
            WifiNetworkStates.CONNECTED -> {
                /* As much as I would love to show the SSID, this only returns <unknown_ssid> even with every possible type of location permission enabled.
                        setTileActive(getWifiManager(applicationContext).connectionInfo.ssid)  */

                // Managers
                val connectivityManager = getConnectivityManager(context)
                val wifiManager = getWifiManager(context)
                // Get network properties & capabilities
                val properties = connectivityManager.getLinkProperties(networkMetadata.network)
                val capabilities = connectivityManager.getNetworkCapabilities(networkMetadata.network)
                // Transport info
                val transportInfo = connectivityManager.getNetworkCapabilities(networkMetadata.network)?.transportInfo as WifiInfo?

                if (properties != null && capabilities != null && transportInfo != null) {
                    when (preference) {
                        "network_name" -> getString(R.string.network_wifi)
                        "ipv4" -> {
                            // Pick first IPv4-formatted address
                            val ipv4 = properties.linkAddresses.firstOrNull { linkAddress ->
                                Regex("(\\d{1,3}\\.){3}\\d{1,3}").containsMatchIn(linkAddress.address.toString())
                            }
                            ipv4?.address?.toString()?.substring(1) ?: getString(R.string.no_ipv4)
                        }
                        "ipv6" -> {
                            // Pick first IPv6-formatted address
                            val ipv6 = properties.linkAddresses.firstOrNull { linkAddress ->
                                Regex("fe80:(:[\\w\\d]{0,4}){0,4}").containsMatchIn(linkAddress.address.toString())
                            }
                            ipv6?.address?.toString()?.substring(1) ?: getString(R.string.no_ipv6)
                        }
                        "speed" -> transportInfo.linkSpeed.toString() + "Mbps"
                        "frequency" -> transportInfo.frequency.toString() + "MHz"
                        "rssi" -> transportInfo.rssi.toString()
                        "strength" -> {
                            // Calculate a signal strength value "which can be displayed to a user" per the docs
                            val strength = wifiManager.calculateSignalLevel(transportInfo.rssi)
                            // Turn this n/4 to a percentage
                            "${strength * 25}%"
                        }
                        "interface_name" -> {
                            properties.interfaceName ?: getString(R.string.unknown_interface)
                        }
                        else -> getString(R.string.not_set)
                    }
                }
                else {
                    // Unable to get properties or capabilities
                    getString(R.string.error_read_wifi_info)
                }
            }
            WifiNetworkStates.DISCONNECTED -> getString(R.string.state_disconnected)
            WifiNetworkStates.LOST -> getString(R.string.state_lost)
            WifiNetworkStates.UNAVAILABLE -> getString(R.string.state_unavailable)
        }

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
            onNetworkChange(WifiNetworkStates.LOST, network)
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            onNetworkChange(WifiNetworkStates.DISCONNECTED, network)
        }
    }
}