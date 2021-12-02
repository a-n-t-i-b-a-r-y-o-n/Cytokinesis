package com.hexagonal.cytokinesis

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.preference.PreferenceManager

/*
    Network class extension functions to pick tile icons & subheadings based on
    the network state and user preference
 */

/// WiFi icon
fun Network.getWifiIcon(context: Context): Int {
    // Managers
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    // Transport info
    val transportInfo = connectivityManager.getNetworkCapabilities(this)?.transportInfo as WifiInfo
    // Pick icon
    return when (wifiManager.calculateSignalLevel(transportInfo.rssi)) {
        0 -> R.drawable.wifi_strength_off_outline
        1 -> R.drawable.wifi_strength_1
        2 -> R.drawable.wifi_strength_2
        3 -> R.drawable.wifi_strength_3
        4 -> R.drawable.wifi_strength_4
        else -> R.drawable.ic_baseline_question_mark_24
    }
}
/// Wifi subheading
fun Network.getWifiSubhead(context: Context): String {
    /* As much as I would love to show the SSID, this only returns <unknown_ssid> even with every possible type of location permission enabled.
                    setTileActive(getWifiManager(applicationContext).connectionInfo.ssid)  */

    // Load user preference
    val preference = PreferenceManager.getDefaultSharedPreferences(context)
        .getString("wifi_subheading", "ipv4")
    // Managers
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    // Get network properties & capabilities
    val properties = connectivityManager.getLinkProperties(this)
    val capabilities = connectivityManager.getNetworkCapabilities(this)
    // Transport info
    val transportInfo = connectivityManager.getNetworkCapabilities(this)?.transportInfo as WifiInfo

    return if (properties != null && capabilities != null) {
        when (preference) {
            "ipv4" -> {
                // Pick first IPv4-formatted address
                val ipv4 = properties.linkAddresses.firstOrNull { address -> Regex("(\\d{1,3}\\.){3}\\d{1,3}").containsMatchIn(address.address.toString()) }
                if (ipv4 != null)
                    ipv4.address.toString().substring(1)
                else
                    "[Unknown IPv4 address]"
            }
            "ipv6" -> {
                // Pick first IPv6-formatted address
                val ipv6 = properties.linkAddresses.firstOrNull { address -> Regex("fe80:(:[\\w\\d]{0,4}){0,4}").containsMatchIn(address.address.toString()) }
                if (ipv6 != null)
                    ipv6.address.toString().substring(1)
                else
                    "[Unknown IPv6 address]"
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
            else -> {
                "[Unrecognized subhead preference]"
            }
        }
    }
    else {
        // Unable to get properties or capabilities
        "[Error reading WiFi info]"
    }
}