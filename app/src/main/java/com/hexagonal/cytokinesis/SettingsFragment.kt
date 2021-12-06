package com.hexagonal.cytokinesis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.*;

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
        super.onPreferenceTreeClick(preference)

        // Handle permissions switches and About button
        if (preference != null) {
            if (preference.key == "about") {
                // Navigate to AboutFragment
                findNavController().navigate(R.id.action_SettingsFragment_to_AboutFragment)
            }
            else if (preference.key.startsWith("permission")) {
                // Get current preference state
                val state = (preference as TwoStatePreference).isChecked
                // Identify associated RequestedPermission
                val permission = RequestedPermission.values().first { p -> p.preferenceKey == preference.key }
                // Request/drop permission and invert state
                onPermissionSwitchChanged(state, permission)
            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) = runBlocking {
        // Ensure permissions switches match preference values
        launch { resetPermissionsSwitches() }
        super.onCreate(savedInstanceState)
    }

    override fun onResume() = runBlocking {
        // Reset permissions switches in case we came back from ignored permission request
        launch { resetPermissionsSwitches() }
        super.onResume()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissionList: Array<out String>, grantResults: IntArray) {
        // Handle multiple permissions, even though we only request them one at a time
        for (i in 0..permissionList.size) {
            // Identify preference key associated with the permission
            val key = RequestedPermission.values()
                .first { p -> p.permissionString == permissionList[i] }
                .preferenceKey

            // Update the user preference, which will also update the UI
            findPreference<TwoStatePreference>(key)
                ?.isChecked = grantResults[i] == PackageManager.PERMISSION_GRANTED
        }

        super.onRequestPermissionsResult(requestCode, permissionList, grantResults)
    }

    // Reset all permissions switches
    private suspend fun resetPermissionsSwitches() {
        for (permission in RequestedPermission.values()) {

            // Get corresponding preference for this item
            val switchPreference = findPreference<SwitchPreferenceCompat>(permission.preferenceKey)!!

            // Update preference with current permission status (it can be changed by the system)
            switchPreference.isChecked = activity?.checkSelfPermission(permission.permissionString) == PackageManager.PERMISSION_GRANTED

            // Switch flip listener
            switchPreference.setOnPreferenceChangeListener { _, _ -> true }
        }
    }

    // Switch flip handler
    private fun onPermissionSwitchChanged(value: Boolean, permission: RequestedPermission) {
        when (value) {
            true -> {
                // Need to request this permission

                if (activity?.checkSelfPermission(permission.permissionString) != PackageManager.PERMISSION_GRANTED) {
                    // Request the permission from the user
                    activity?.requestPermissions(arrayOf(permission.permissionString), 0)
                }
            }
            false -> {
                // Need to drop this permission

                // TODO: Can you drop permissions?
                Snackbar.make(requireView(), "Drop \"${permission.permissionString}\".", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }
}