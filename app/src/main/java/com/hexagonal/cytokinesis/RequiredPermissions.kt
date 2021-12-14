package com.hexagonal.cytokinesis

import android.Manifest

enum class RequiredPermission(val preferenceKey: String, val permissionString: String) {
    AccessNetworkState("permission_accessnetworkstate", Manifest.permission.ACCESS_NETWORK_STATE),
    ChangeNetworkState("permission_changenetworkstate", Manifest.permission.CHANGE_NETWORK_STATE),
    ReadPhoneState("permission_readphonestate", Manifest.permission.READ_PHONE_STATE),
    AccessWifiState("permission_accesswifistate", Manifest.permission.ACCESS_WIFI_STATE),
    ChangeWifiState("permission_changewifistate", Manifest.permission.CHANGE_WIFI_STATE),
    Internet("permission_internet", Manifest.permission.INTERNET),
}