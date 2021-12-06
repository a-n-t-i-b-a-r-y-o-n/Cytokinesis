package com.hexagonal.cytokinesis

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.*
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.preference.PreferenceManager
import java.lang.Exception
import java.util.concurrent.Executors

class DataTileService : TileService() {

    // Object representing the data network's state
    class DataNetworkMetadata {
        var network: Network? = null
        var displayInfo: TelephonyDisplayInfo? = null
        var state: DataNetworkStates = DataNetworkStates.DISCONNECTED
        fun getDataGeneration(context: Context): DataGeneration {
            // Prefer version from TelephonyDisplayInfo, but fall back to Network info if necessary
            return displayInfo?.getDataGeneration() ?:
            network?.getDataGeneration(context) ?:
            DataGeneration._UNKNOWN
        }
    }

    // Instance cached network info/state
    private var networkMetadata: DataNetworkMetadata = DataNetworkMetadata()

    // Network type to watch
    private val request: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .setIncludeOtherUidNetworks(true)
        .build()

    // ConnectivityManager.NetworkCallback
    private val netCallback: DataNetworkCallback = DataNetworkCallback { state, network -> onStateChange(state, network)}
    // TelephonyManager.TelephonyCallback
    private val telCallback: TelephonyChangeCallback = TelephonyChangeCallback { info -> onTelephonyChange(info) }

    // Called right before tile enters view
    override fun onStartListening() {
        super.onStartListening()

        // Register the network connectivity change callback
        getConnectivityManager(applicationContext)
            .registerNetworkCallback(request, netCallback)

        // Register the network "display info" change callback
        getTelephonyManager(applicationContext)
            .registerTelephonyCallback(Executors.newSingleThreadExecutor(), telCallback)

        updateTile()
    }

    // Called after tile leaves view
    override fun onStopListening() {
        super.onStopListening()
        // Weakly try to unregister the network callback
        try {
            getConnectivityManager(applicationContext)
                .unregisterNetworkCallback(netCallback)
        } catch (e: Exception) { }
        // Weakly try to unregister the telephony callback
        try {
            getTelephonyManager(applicationContext)
                .unregisterTelephonyCallback(telCallback)
        } catch (e: Exception) { }
    }

    // Called when the tile is clicked/tapped
    override fun onClick() {
        super.onClick()

        //val intent = Intent(Settings.ACTION_DATA_USAGE_SETTINGS) // Sorta...
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityAndCollapse(intent)
    }

    // Update all title properties in a uniform way
    private fun updateTile() {
        if (qsTile != null) {
            // Set tile active state
            qsTile.state = if (networkMetadata.state == DataNetworkStates.CONNECTED) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            }
            // Set tile subheading
            qsTile.subtitle = getDataSubhead(applicationContext)
            // Set tile icon drawable
            qsTile.icon = Icon.createWithResource(applicationContext, getDataIcon(applicationContext))
            // Perform UI update
            qsTile.updateTile()
        }
    }

    // Update tile in response to network state
    private fun onStateChange(state: DataNetworkStates, network: Network?) {
        /*
        // DEBUG: Write network data to log
        Log.w("[DataTileService]", "Network Capabilities: " + getConnectionManager(applicationContext).getNetworkCapabilities(network)?.toString())
        Log.w("[DataTileService]", "Link Properties: " + getConnectionManager(applicationContext).getLinkProperties(network)?.toString())
        Log.w("[DataTileService]", "dataNetworkType: " + getTelephonyManager(applicationContext).dataNetworkType)
         */

        networkMetadata.state = state

        if (network != null) {
            networkMetadata.network = network
        }

        updateTile()
    }

    // Update tile in response to telephony "display info" changes
    private fun onTelephonyChange(displayInfo: TelephonyDisplayInfo) {

        networkMetadata.displayInfo = displayInfo

        updateTile()
    }

    private fun getDataIcon(context: Context): Int {
        // Load user preference
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
            .getString("data_icon_type", "signal_strength")
        // Manager
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        return when (preference) {
            "signal_strength" -> {
                when (telephonyManager.signalStrength?.level) {
                    0 -> R.drawable.network_strength_off_outline
                    1 -> R.drawable.network_strength_1
                    2 -> R.drawable.network_strength_2
                    3 -> R.drawable.network_strength_3
                    4 -> R.drawable.network_strength_4
                    else -> R.drawable.ic_baseline_question_mark_24
                }
            }
            "arrows" -> {
                when (networkMetadata.state) {
                    DataNetworkStates.CONNECTED -> {
                        // TODO: Find a normal arrows "on" icon
                        R.drawable.ic_baseline_question_mark_24
                    }
                    DataNetworkStates.DISCONNECTED,
                    DataNetworkStates.UNAVAILABLE -> R.drawable.ic_baseline_mobiledata_off_24
                }
            }
            "network_type" -> {
                // Try to get the data "generation", which may require READ_PHONE_STATE permission
                networkMetadata.getDataGeneration(context).icon
            }
            else -> {
                // Unknown user icon preference
                R.drawable.ic_baseline_question_mark_24
            }
        }
    }

    private fun getDataSubhead(context: Context): String {
        // Only show info if connected
        return when (networkMetadata.state) {
            DataNetworkStates.CONNECTED -> {
                // Load user preference
                val preference = PreferenceManager.getDefaultSharedPreferences(context)
                    .getString("data_subheading", "network_type")
                // Manager
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                // Network properties & capabilities
                val properties = connectivityManager.getLinkProperties(networkMetadata.network)
                val capabilities = connectivityManager.getNetworkCapabilities(networkMetadata.network)

                if(properties != null && capabilities != null) {
                    when(preference) {
                        "network_type" -> {
                            // Try to get the data "generation", which requires READ_PHONE_STATE permission
                            networkMetadata.getDataGeneration(context).gen
                        }
                        "ipv4" -> {
                            // Pick first IPv4-formatted address
                            val ipv4 = properties.linkAddresses.firstOrNull { address -> Regex("(\\d{1,3}\\.){3}\\d{1,3}").containsMatchIn(address.address.toString()) }
                            if (ipv4 != null)
                                ipv4.address.toString().substring(1)
                            else
                                "<Unknown IPv4 address>"
                        }
                        "ipv6" -> {
                            // Pick first IPv6-formatted address
                            val ipv6 = properties.linkAddresses.firstOrNull { address -> Regex("fe80:(:[\\w\\d]{0,4}){0,4}").containsMatchIn(address.address.toString()) }
                            if (ipv6 != null)
                                ipv6.address.toString().substring(1)
                            else
                                "<Unknown IPv6 address>"
                        }
                        "speed_mbps" -> "${capabilities.linkUpstreamBandwidthKbps / 1000} Mbps / ${capabilities.linkDownstreamBandwidthKbps / 1000} Mbps"
                        "speed_kbps" -> "${capabilities.linkUpstreamBandwidthKbps} Kbps / ${capabilities.linkDownstreamBandwidthKbps} Kbps"
                        "interface_name" -> properties.interfaceName ?: "<Unknown Interface>"
                        else -> "<Not Set>"
                    }
                }
                else {
                    // Unable to get properties or capabilities
                    "<Error reading mobile network info>"
                }
            }
            DataNetworkStates.DISCONNECTED -> "<Disconnected>"
            DataNetworkStates.UNAVAILABLE -> "<Unavailable>"
        }

    }

    // Helper to get the ConnectionManager with given context
    // Used to to register/deregister callbacks
    private fun getConnectivityManager(context: Context): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Helper to get the TelephonyManager with given context
    // Used to to register/deregister callbacks
    private fun getTelephonyManager(context: Context): TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    // States the data network could be in
    enum class DataNetworkStates {
        CONNECTED,
        DISCONNECTED,
        UNAVAILABLE,
    }

    // Callback for changes to the data network
    class DataNetworkCallback(val onNetworkChange: (state: DataNetworkStates, network: Network?) -> Unit) : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            onNetworkChange(DataNetworkStates.CONNECTED, network)
        }

        override fun onUnavailable() {
            super.onUnavailable()
            onNetworkChange(DataNetworkStates.UNAVAILABLE, null)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            onNetworkChange(DataNetworkStates.DISCONNECTED, network)
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            super.onLosing(network, maxMsToLive)
            onNetworkChange(DataNetworkStates.DISCONNECTED, network)
        }
    }

    // Callback for changes to the telephony info
    class TelephonyChangeCallback(val onTelephonyChange: (info: TelephonyDisplayInfo) -> Unit) : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
            onTelephonyChange(telephonyDisplayInfo)
        }
    }
}