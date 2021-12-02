package com.hexagonal.cytokinesis

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.telephony.TelephonyManager
import androidx.preference.PreferenceManager

/*
    Network class extension functions to pick tile icons & subheadings based on
    the network state and user preference
 */

/// Data icon
fun Network.getDataIcon(context: Context, networkActive: Boolean): Int {
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
            if (networkActive) {
                // TODO: Find a normal arrows "on" icon
                R.drawable.ic_baseline_question_mark_24
            }
            else {
                R.drawable.ic_baseline_mobiledata_off_24
            }
        }
        "network_type" -> {
            // Return icon based on network "Generation"
            this.getDataGeneration(context).icon
        }
        else -> {
            // Unknown user icon preference
            R.drawable.ic_baseline_question_mark_24
        }
    }
}

/// Data subheading
fun Network.getDataSubhead(context: Context): String {
    // Load user preference
    val preference = PreferenceManager.getDefaultSharedPreferences(context)
        .getString("data_subheading", "network_type")
    // Manager
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // Network properties & capabilities
    val properties = connectivityManager.getLinkProperties(this)
    val capabilities = connectivityManager.getNetworkCapabilities(this)

    return if(properties != null && capabilities != null) {
        when(preference) {
            "network_type" -> {
                this.getDataGeneration(context).gen
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

// TODO: Check permissions!
@SuppressLint("MissingPermission")
/// Data network type/rating helper
fun Network.getDataGeneration(context: Context): DataGeneration {
    // Managers
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    return when (telephonyManager.dataNetworkType) {
        TelephonyManager.NETWORK_TYPE_NR -> DataGeneration._5G
        TelephonyManager.NETWORK_TYPE_LTE -> {
            // TODO: I don't think this is accurate, but it seems to work on my device...
            // TODO: Properly distinguish between 5G and LTE
            val capabilities = connectivityManager.getNetworkCapabilities(this)
            if (capabilities != null && capabilities.linkDownstreamBandwidthKbps > 18000) {
                DataGeneration._5G
            }
            else {
                DataGeneration._LTE
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
        TelephonyManager.NETWORK_TYPE_UMTS -> DataGeneration._3G
        TelephonyManager.NETWORK_TYPE_1xRTT,
        TelephonyManager.NETWORK_TYPE_CDMA,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GSM,
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_IDEN -> DataGeneration._2G
        TelephonyManager.NETWORK_TYPE_IWLAN -> DataGeneration._IWLAN
        TelephonyManager.NETWORK_TYPE_UNKNOWN -> DataGeneration._UNKNOWN
        else -> DataGeneration._UNKNOWN
    }
}

/// Data network "Generations"
// Note: Prefixed with underscores due to digits in the 1st char
enum class DataGeneration(val icon: Int, val gen: String) {
    _5G(R.drawable.ic_baseline_5g_24, "5G"),
    _LTE(R.drawable.ic_baseline_lte_mobiledata_24, "LTE"),
    _4G(R.drawable.ic_baseline_4g_mobiledata_24, "4G"),
    _3G(R.drawable.ic_baseline_3g_mobiledata_24, "3G"),
    _2G(R.drawable.ic_baseline_e_mobiledata_24, "2G"),
    _IWLAN(R.drawable.ic_baseline_question_mark_24, "IWLAN"),
    _UNKNOWN(R.drawable.ic_baseline_question_mark_24, "<Unknown Network Type>"),
}