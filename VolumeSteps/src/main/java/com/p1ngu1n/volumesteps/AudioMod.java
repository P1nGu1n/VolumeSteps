/*
 * AudioMod.java created on 2014-09-16.
 *
 * Copyright (C) 2014 P1nGu1n
 *
 * This file is part of VolumeSteps+.
 *
 * Snapshare is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Snapshare is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * a gazillion times. If not, see <http://www.gnu.org/licenses/>.
 */
package com.p1ngu1n.volumesteps;

import android.content.res.XResources;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Changes the number of volume steps and disables the safe headset volume warning.
 * This class contains the code to be executed by Xposed.
 *
 * XDA-thread used as inspiration for changing the maximum volume:      http://forum.xda-developers.com/showthread.php?t=1411317
 * Source code of AudioService in which the max volumes are replaced:   https://github.com/android/platform_frameworks_base/blob/master/media/java/android/media/AudioService.java
 * Source code of the config file which values are replaced:            https://github.com/android/platform_frameworks_base/blob/master/core/res/res/values/config.xml
 */
public class AudioMod implements IXposedHookZygoteInit {

    /* Class name of AudioService */
    private static final String AUDIO_SERVICE_CLASS = "android.media.AudioService";
    /* Index of the Music Stream in MAX_STREAM_VOLUME */
    private static final int STREAM_INDEX_MUSIC = 3;
    /* Tag used for logging */
    private static final String LOG_TAG = "VolumeSteps+: ";

    // Temporarily variables until a GUI is made
    private static final int VOLUME_STEPS = 30;
    private static final boolean SAVE_HEADSET_VOLUME_DISABLED = false;
    private static final boolean DEBUGGING = true;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        final Class<?> audioServiceClass = XposedHelpers.findClass(AUDIO_SERVICE_CLASS, null);

        // Hook the constructor of AudioService, change the volumes before initialization
        XposedBridge.hookAllConstructors(audioServiceClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // Retrieve array containing the maximum stream volumes
                int[] maxStreamVolume = (int[]) XposedHelpers.getStaticObjectField(audioServiceClass, "MAX_STREAM_VOLUME");
                // Set the volume at the index of the Music stream
                maxStreamVolume[STREAM_INDEX_MUSIC] = VOLUME_STEPS;
                xposedLog("MAX_STREAM_VOLUME for music stream set to " + VOLUME_STEPS);
            }
        });

        if (SAVE_HEADSET_VOLUME_DISABLED) {
            // Disable the save headset volume warning
            XResources.setSystemWideReplacement("android", "bool", "config_safe_media_volume_enabled", false);
            xposedLog("Save Headset Volume is disabled");
        } else {
            // Calculate the new headset volume warning to comply with the new maximum volume
            int safeHeadsetVolume = (int) Math.round(VOLUME_STEPS * (2.0 / 3.0));
            XResources.setSystemWideReplacement("android", "integer", "config_safe_media_volume_index", safeHeadsetVolume);
            xposedLog("Save Headset Volume set to " + safeHeadsetVolume);
        }
    }

    public static void xposedLog(String message) {
        if (DEBUGGING) {
            XposedBridge.log(LOG_TAG + message);
        }
    }
}

