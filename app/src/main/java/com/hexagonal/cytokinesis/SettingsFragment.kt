package com.hexagonal.cytokinesis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : PreferenceFragmentCompat() {

    enum class RequestedPermission(val preferenceKey: String, val permissionString: String) {
        AccessNetworkState("permission_accessnetworkstate", Manifest.permission.ACCESS_NETWORK_STATE),
        ChangeNetworkState("permission_changenetworkstate", Manifest.permission.CHANGE_NETWORK_STATE),
        ReadPhoneState("permission_readphonestate", Manifest.permission.READ_PHONE_STATE),
        AccessWifiState("permission_accesswifistate", Manifest.permission.ACCESS_WIFI_STATE),
        ChangeWifiState("permission_changewifistate", Manifest.permission.CHANGE_WIFI_STATE),
        Internet("permission_internet", Manifest.permission.INTERNET),
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
        for (permission in RequestedPermission.values()) {

            // Get corresponding preference for this item
            val switchPreference = findPreference<SwitchPreferenceCompat>(permission.preferenceKey)!!

            // Update preference with current permission status (it can be changed by the system)
            switchPreference.isChecked = activity?.checkSelfPermission(permission.permissionString) == PackageManager.PERMISSION_GRANTED

            // Switch flip listener
            switchPreference.setOnPreferenceChangeListener { _, v -> onPermissionSwitchChanged(v, permission) }
        }

    }

    private fun onWifiSubheadChange(preference: Preference, newValue: Any): Boolean {

        return true
    }

    private fun onDataIconTypeChange(preference: Preference, newValue: Any): Boolean {

        return true
    }

    private fun onDataSubheadChange(preference: Preference, newValue: Any): Boolean {

        return true
    }

    private fun onPermissionSwitchChanged(value: Any, permission: RequestedPermission): Boolean {
        if (value is Boolean) {
            when (value) {
                true -> {
                    // Need to request this permission

                    if (activity?.checkSelfPermission(permission.permissionString) != PackageManager.PERMISSION_GRANTED) {
                        // Request the permission from the user
                        activity?.requestPermissions(arrayOf(permission.permissionString), 0)
                    }
                }
                false -> {
                    // TODO: Can you drop permissions?
                    Snackbar.make(requireView(), "\"${permission.permissionString}\" is already granted.", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }

            return true
        }
        return false
    }
}