/*
 * Copyright (C) 2014  P1nGu1n
 *
 * This file is part of VolumeSteps+.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.p1ngu1n.volumesteps;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.CheckBox;

/**
 * Fragment container the preferences.
 */
public class SettingsFragment extends PreferenceFragment {
    private static boolean rebootMessageShown = false;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.settings);

        // Set the version number in the about screen
        findPreference("pref_about").setTitle(getString(R.string.pref_about_title, BuildConfig.VERSION_NAME));
        // Set change listener to the 'show in launcher' preference
        findPreference("pref_launcher").setOnPreferenceChangeListener(changeListenerLauncher);

        // Disabling safe headset volume requires Android 4.2
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            CheckBoxPreference safeHeadsetVolumePreference = (CheckBoxPreference) findPreference("pref_safe_headset_volume_disabled");
            safeHeadsetVolumePreference.setEnabled(false);
            safeHeadsetVolumePreference.setSummaryOff(getString(R.string.require_android_Version, "Android 4.2"));
        }
        // Volume keys control music requires Android 4.1
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            CheckBoxPreference volumeKeysControlMusic = (CheckBoxPreference) findPreference("pref_volume_keys_control_music");
            volumeKeysControlMusic.setEnabled(false);
            volumeKeysControlMusic.setSummaryOff(getString(R.string.require_android_Version, "Android 4.1"));
        }

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (!rebootMessageShown && sharedPreferences.getBoolean("pref_show_reboot_dialog", true)) {
            rebootMessageShown = true;
            createRebootDialog().show();
        }
    }

    /**
     * Hides or shows the icon in the launcher when the preference changed.
     */
    private final Preference.OnPreferenceChangeListener changeListenerLauncher = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            int componentState = ((Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED);

            Activity activity = getActivity();
            ComponentName alias = new ComponentName(activity, "com.p1ngu1n.volumesteps.SettingsActivity-Alias");
            activity.getPackageManager().setComponentEnabledSetting(alias, componentState, PackageManager.DONT_KILL_APP);
            return true;
        }
    };

    private AlertDialog createRebootDialog() {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.app_name);
        //builder.setIcon(android.R.drawable.ic_dialog_info);

        // build view from layout
        View dialogView = View.inflate(activity, R.layout.reboot_dialog, null);
        final CheckBox neverShowAgainCheckBox = (CheckBox) dialogView.findViewById(R.id.reboot_dialog_checkbox);
        builder.setView(dialogView);

        builder.setPositiveButton(activity.getString(R.string.ok_understand), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // set preference to never show reboot dialog again if checkbox is checked
                if (neverShowAgainCheckBox.isChecked()) {
                    SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("pref_show_reboot_dialog", false);
                    editor.apply();
                }
            }
        });
        return builder.create();
    }
}
