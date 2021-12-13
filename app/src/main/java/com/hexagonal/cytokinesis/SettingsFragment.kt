package com.hexagonal.cytokinesis

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsFragment : PreferenceFragmentCompat() {

    enum class RequiredPermission(val preferenceKey: String, val permissionString: String) {
        AccessNetworkState("permission_accessnetworkstate", Manifest.permission.ACCESS_NETWORK_STATE),
        ChangeNetworkState("permission_changenetworkstate", Manifest.permission.CHANGE_NETWORK_STATE),
        ReadPhoneState("permission_readphonestate", Manifest.permission.READ_PHONE_STATE),
        AccessWifiState("permission_accesswifistate", Manifest.permission.ACCESS_WIFI_STATE),
        ChangeWifiState("permission_changewifistate", Manifest.permission.CHANGE_WIFI_STATE),
        Internet("permission_internet", Manifest.permission.INTERNET),
    }

    // Change listener for the underlying preferences
    private var changeListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key.startsWith("permission")) {
                // Identify associated RequiredPermission value
                val permission = RequiredPermission.values().first { p -> p.preferenceKey == key }
                // Request/drop permission and invert state
                onPermissionSwitchChanged(sharedPreferences.getBoolean(key, false), permission)
            }
        }

    // Build preference list
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    // Handle clicks, specifically for the About button.
    // NOTE: Probably shouldn't do anything computationally heavy in this method.
    override fun onPreferenceTreeClick(preference: Preference?) = runBlocking {
        // Handle click event on the About button
        launch {
            if (preference != null && preference.key == "about") {
                findNavController().navigate(R.id.action_SettingsFragment_to_AboutFragment)
            }
        }
        super.onPreferenceTreeClick(preference)
    }

    // Initial view creation
    override fun onCreate(savedInstanceState: Bundle?) = runBlocking {
        // Ensure permissions switches match preference values
        launch { resetPermissionsSwitches() }
        super.onCreate(savedInstanceState)
    }

    // Resume (or return from ignored permissions request)
    override fun onResume() = runBlocking {
        // Reset permissions switches in case we came back from ignored permission request
        launch {
            resetPermissionsSwitches()
        }
        // Register the preference change listener
        launch {
            try {
                preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener)
            } catch (e: Exception) {
                Log.d("[SettingsFragment]", "Unable to register preference listener.")
            }
        }
        super.onResume()
    }

    // Pause/leave
    override fun onPause() = runBlocking {
        launch {
            try {
                preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener)
            } catch (e: Exception) {
                Log.d("[SettingsFragment]", "Unable to unregister preference listener.")
            }
        }
        super.onPause()
    }

    // Return from permission request explicitly accepted or denied
    override fun onRequestPermissionsResult(requestCode: Int, permissionList: Array<out String>, grantResults: IntArray) {
        // Handle multiple permissions, even though we only request them one at a time
        for (i in 0..permissionList.size) {
            // Identify preference key associated with the permission
            val key = RequiredPermission.values()
                .first { p -> p.permissionString == permissionList[i] }
                .preferenceKey

            // Update the user preference, which will also update the UI
            findPreference<TwoStatePreference>(key)
                ?.isChecked = grantResults[i] == PackageManager.PERMISSION_GRANTED
        }

        // TODO: Is super.onRequestPermissionsResult deprecated?
        // super.onRequestPermissionsResult(requestCode, permissionList, grantResults)
    }

    // Reset all permissions switches
    private fun resetPermissionsSwitches() {
        for (permission in RequiredPermission.values()) {

            // Get corresponding preference for this item
            val switchPreference = findPreference<SwitchPreferenceCompat>(permission.preferenceKey)!!

            // Update preference with current permission status (it can be changed by the system)
            switchPreference.isChecked = activity?.checkSelfPermission(permission.permissionString) == PackageManager.PERMISSION_GRANTED

            // Switch flip listener
            switchPreference.setOnPreferenceChangeListener { _, _ -> true }
        }
    }

    // Switch flip handler
    private fun onPermissionSwitchChanged(value: Boolean, permission: RequiredPermission) {
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