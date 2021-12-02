package com.hexagonal.cytokinesis

import android.content.Context
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager

// Decide displayable network "Generation" with extra metadata given by TelephonyDisplayInfo
fun TelephonyDisplayInfo.getDataGeneration(): DataGeneration {
    return when (this.networkType) {
        TelephonyManager.NETWORK_TYPE_NR,
        TelephonyManager.NETWORK_TYPE_LTE -> {
            when (this.overrideNetworkType) {
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> DataGeneration._LTE
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA -> DataGeneration._5G
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE -> DataGeneration._UNKNOWN
                else -> DataGeneration._UNKNOWN
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