package com.hexagonal.cytokinesis

import android.os.Bundle
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

class SettingsFragment : PreferenceFragmentCompat() {

    enum class RequestedPermissions(val pref_key: String) {
        AccessNetworkState("permission_accessnetworkstate"),
        ChangeNetworkState("permission_changenetworkstate"),
        ReadPhoneState("permission_readphonestate"),
        AccessWifiState("permission_accesswifistate"),
        ChangeWifiState("permission_changewifistate"),
        Internet("permission_internet"),
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        Log.d("Preference", "Preference tree clicked")

        return super.onPreferenceTreeClick(preference)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // WiFi subheading
        findPreference<ListPreference>("wifi_subheading")
            ?.setOnPreferenceChangeListener { p, v -> onWifiSubheadChange(p, v) }

        // Mobile data icon type
        findPreference<ListPreference>("data_icon_type")
            ?.setOnPreferenceChangeListener { p, v -> onDataIconTypeChange(p, v) }

        // Mobile data subheading
        findPreference<ListPreference>("data_subheading")
            ?.setOnPreferenceChangeListener { p, v -> onDataSubheadChange(p, v) }

        // Permissions switches
        for (permission in RequestedPermissions.values()) {
            findPreference<ListPreference>(permission.pref_key)
                ?.setOnPreferenceChangeListener { p, v -> onPermissionChanged(p, v) }
        }

    }

    fun onWifiSubheadChange(preference: Preference, newValue: Any): Boolean {

        return true
    }

    fun onDataIconTypeChange(preference: Preference, newValue: Any): Boolean {

        return true
    }

    fun onDataSubheadChange(preference: Preference, newValue: Any): Boolean {

        return true
    }

    fun onPermissionChanged(preference: Preference, newValue: Any): Boolean {
        // Find matching preference
        when (RequestedPermissions.values().firstOrNull { p -> p.pref_key == preference.key }) {
            RequestedPermissions.AccessNetworkState -> TODO()
            RequestedPermissions.ChangeNetworkState -> TODO()
            RequestedPermissions.ReadPhoneState -> TODO()
            RequestedPermissions.AccessWifiState -> TODO()
            RequestedPermissions.ChangeWifiState -> TODO()
            RequestedPermissions.Internet -> TODO()
            null -> {
                // Somehow flipped a switch for an unknown preference
                Log.d("Cytokinesis", "onPermissionChanged event for unknown preference: \"${preference.key}\"")
            }
        }
        return true
    }
}