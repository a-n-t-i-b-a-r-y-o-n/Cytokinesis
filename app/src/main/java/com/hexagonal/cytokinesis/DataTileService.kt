package com.hexagonal.cytokinesis

import android.annotation.SuppressLint
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
import android.util.Log
import androidx.preference.PreferenceManager
import java.lang.Exception
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class DataTileService : TileService() {

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
        // Assume off by default, since we apparently only get callbacks when connected
        if (qsTile != null) {
            updateTile(
                Tile.STATE_INACTIVE,
                "Disconnected",
                R.drawable.ic_baseline_mobiledata_off_24,
            )
        }
        // Register the network connectivity change callback
        getConnectivityManager(applicationContext)
            .registerNetworkCallback(request, netCallback)

        // Register the network "display info" change callback
        getTelephonyManager(applicationContext)
            .registerTelephonyCallback(Executors.newSingleThreadExecutor(), telCallback)
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

    // Update tile in response to network state
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
    }

    // Update tile in response to telephony "display info" changes
    fun onTelephonyChange(displayInfo: TelephonyDisplayInfo) {
        // Proceed only if the tile is active
        if (qsTile != null && qsTile.state == Tile.STATE_ACTIVE) {
            // Check preferences to see if user wants icon or subheading to reflect display info
            val iconPref = PreferenceManager.getDefaultSharedPreferences(applicationContext!!)
                .getString("data_icon_type", "signal_strength")
            val subheadPref = PreferenceManager.getDefaultSharedPreferences(applicationContext!!)
                .getString("data_subheading", "network_type")
            if (iconPref == "network_type") {
                // Update the tile icon
                qsTile.icon = Icon.createWithResource(applicationContext, displayInfo.getDataGeneration().icon)
                qsTile.updateTile()
            }
            if (subheadPref == "network_type") {
                // Update the subheading
                qsTile.subtitle = displayInfo.getDataGeneration().gen
                qsTile.updateTile()
            }
        }
    }

    // Helper to get the ConnectionManager with given context
    // NOTE: This must be called from a given function override. There is no context at construction.
    private fun getConnectivityManager(context: Context): ConnectivityManager =
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

    class TelephonyChangeCallback(val onTelephonyChange: (info: TelephonyDisplayInfo) -> Unit) : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
            onTelephonyChange(telephonyDisplayInfo)
        }
    }
}