package com.hexagonal.cytokinesis

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat

/// Data network "Generations"
// Note: Prefixed with underscores due to digits in the 1st char
enum class DataGeneration(val icon: Int, val gen: String) {
    _5G(R.drawable.ic_baseline_5g_24, "5G"),
    _LTE(R.drawable.ic_baseline_lte_mobiledata_24, "LTE"),
    _4G(R.drawable.ic_baseline_4g_mobiledata_24, "4G"),
    _3G(R.drawable.ic_baseline_3g_mobiledata_24, "3G"),
    _2G(R.drawable.ic_baseline_e_mobiledata_24, "2G"),
    _IWLAN(R.drawable.old_wifi_bars, "IWLAN"),
    _UNKNOWN(R.drawable.ic_baseline_question_mark_24, ". . ."),
    _PERMISSION_DENIED(R.drawable.ic_baseline_signal_cellular_connected_no_internet_0_bar_24, "<Missing Permission>")
}

// Decide network "Generation" based on TelephonyDisplayInfo (More accurate)
fun TelephonyDisplayInfo.getDataGeneration(): DataGeneration {
    return when (this.networkType) {
        TelephonyManager.NETWORK_TYPE_NR -> DataGeneration._5G
        TelephonyManager.NETWORK_TYPE_LTE -> {
            // See if there's an override network for this type
            when (this.overrideNetworkType) {
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> DataGeneration._5G
                else -> DataGeneration._LTE
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

/// Decide network "Generation" based on a Network object (less accurate)
fun Network.getDataGeneration(context: Context): DataGeneration {
    // Manager
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // Check for READ_PHONE_STATE permission
    return if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        return DataGeneration._PERMISSION_DENIED
    }
    else {
        // Manager
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        // Return less-accurate network gen
        // See also: TelephonyDisplayInfo.getDataGeneration()
        when (telephonyManager.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_NR -> DataGeneration._5G
            TelephonyManager.NETWORK_TYPE_LTE -> {
                // I don't think this is accurate, but it seems to work on my device...
                // TODO: Properly distinguish between 5G and LTE from network capabilities alone
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
}