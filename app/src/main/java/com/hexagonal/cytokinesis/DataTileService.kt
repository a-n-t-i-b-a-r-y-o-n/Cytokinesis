package com.hexagonal.cytokinesis

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.*
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.TelephonyManager
import android.util.Log
import java.lang.Exception

class DataTileService() : TileService() {

    // Network type to watch
    private val request: NetworkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .setIncludeOtherUidNetworks(true)
        .build()

    // Custom ConnectivityManager.NetworkCallback class
    private val netCallback: DataNetworkCallback = DataNetworkCallback { state, network -> onStateChange(state, network)}

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

        //val intent = Intent(Settings.ACTION_DATA_USAGE_SETTINGS) // Sorta...
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivityAndCollapse(intent)
    }

    // Helper to update the tile to active
    private fun setTileActive(subtitle: String) {
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.icon = Icon.createWithResource(applicationContext, R.drawable.ic_baseline_signal_cellular_4_bar_24)
        qsTile.subtitle = subtitle

        qsTile.updateTile()
    }

    // Helper to update the tile to inactive
    private fun setTileInactive() {
        qsTile.state = Tile.STATE_INACTIVE
        qsTile.icon = Icon.createWithResource(applicationContext, R.drawable.ic_baseline_signal_cellular_off_24)
        qsTile.subtitle = "Disconnected"

        qsTile.updateTile()
    }

    // TODO: Check permissions!
    @SuppressLint("MissingPermission")
    fun onStateChange(state: DataNetworkStates, network: Network?) {
        if (qsTile != null) {
            enumValues<DataNetworkStates>().forEach { possibleState ->
                if (possibleState == state) {
                    if (state == DataNetworkStates.CONNECTED && network != null) {
                        /*
                        // DEBUG: Write network data to log
                        Log.w("[DataTileService]", "Network Capabilities: " + getConnectionManager(applicationContext).getNetworkCapabilities(network)?.toString())
                        Log.w("[DataTileService]", "Link Properties: " + getConnectionManager(applicationContext).getLinkProperties(network)?.toString())
                        Log.w("[DataTileService]", "dataNetworkType: " + getTelephonyManager(applicationContext).dataNetworkType)
                         */

                        // Determine wireless network generation/type
                        // TODO: Find out how to differentiate between 4G LTE and 5G (both return 'LTE', hover 'NR' for info) - maybe network speed?
                        val generation = when (getTelephonyManager(applicationContext).dataNetworkType) {
                            TelephonyManager.NETWORK_TYPE_NR -> "5G"
                            TelephonyManager.NETWORK_TYPE_LTE -> {
                                // TODO: I don't think this is accurate, but it seems to work on my device...
                                val capabilities = getConnectionManager(applicationContext).getNetworkCapabilities(network)
                                if (capabilities != null && capabilities.linkDownstreamBandwidthKbps > 18000) {
                                    "5G"
                                }
                                else {
                                    "LTE"
                                }
                            }
                            TelephonyManager.NETWORK_TYPE_EHRPD,
                            TelephonyManager.NETWORK_TYPE_EVDO_0,
                            TelephonyManager.NETWORK_TYPE_EVDO_A,
                            TelephonyManager.NETWORK_TYPE_EVDO_B,
                            TelephonyManager.NETWORK_TYPE_HSPA,
                            TelephonyManager.NETWORK_TYPE_HSPAP,
                            TelephonyManager.NETWORK_TYPE_HSDPA,
                            TelephonyManager.NETWORK_TYPE_HSUPA,
                            TelephonyManager.NETWORK_TYPE_TD_SCDMA,
                            TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                            TelephonyManager.NETWORK_TYPE_1xRTT,
                            TelephonyManager.NETWORK_TYPE_CDMA,
                            TelephonyManager.NETWORK_TYPE_EDGE,
                            TelephonyManager.NETWORK_TYPE_GSM,
                            TelephonyManager.NETWORK_TYPE_GPRS,
                            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
                            TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
                            TelephonyManager.NETWORK_TYPE_UNKNOWN -> "Unknown"
                            else -> "Unknown"
                        }

                        setTileActive(generation)
                    }
                }
            }
        }
        else {
            /*
            // DEBUG: Note incorrect state change in logs
            Log.w("[DataTileService]", "Tried to call onStateChange() but qsTile is null.")
             */
        }
    }

    // Helper to get the ConnectionManager with given context
    // NOTE: This must be called from a given function override. There is no context at construction.
    private fun getConnectionManager(context: Context): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Helper to get the TelephonyManager with given context
    // NOTE: This must be called from a given function override. There is no context at construction.
    private fun getTelephonyManager(context: Context): TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    enum class DataNetworkStates {
        CONNECTED,
        DISCONNECTED,
        UNAVAILABLE,
    }

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
}