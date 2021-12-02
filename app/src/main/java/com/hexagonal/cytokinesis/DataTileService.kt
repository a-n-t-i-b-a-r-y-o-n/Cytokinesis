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
            updateTile(
                Tile.STATE_INACTIVE,
                "Disconnected",
                R.drawable.ic_baseline_mobiledata_off_24,
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

        //val intent = Intent(Settings.ACTION_DATA_USAGE_SETTINGS) // Sorta...
        val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
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

    fun onStateChange(state: DataNetworkStates, network: Network?) {
        /*
        // DEBUG: Write network data to log
        Log.w("[DataTileService]", "Network Capabilities: " + getConnectionManager(applicationContext).getNetworkCapabilities(network)?.toString())
        Log.w("[DataTileService]", "Link Properties: " + getConnectionManager(applicationContext).getLinkProperties(network)?.toString())
        Log.w("[DataTileService]", "dataNetworkType: " + getTelephonyManager(applicationContext).dataNetworkType)
         */
        if (qsTile != null && network != null) {
            if (state == DataNetworkStates.CONNECTED) {
                updateTile(
                    Tile.STATE_ACTIVE,
                    network.getDataSubhead(applicationContext!!),
                    network.getDataIcon(applicationContext!!, true),
                )
            }
            else {
                updateTile(
                    Tile.STATE_INACTIVE,
                    "Disconnected",
                    network.getDataIcon(applicationContext!!, false),
                )
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