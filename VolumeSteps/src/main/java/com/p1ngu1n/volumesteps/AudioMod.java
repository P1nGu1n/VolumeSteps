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

import android.content.res.XResources;
import android.media.AudioManager;
import android.os.Build;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Changes the number of volume steps and disables the safe headset volume warning.
 * This class contains the code to be executed by Xposed.
 *
 * XDA-thread used as inspiration for changing the maximum volume:      http://forum.xda-developers.com/showthread.php?t=1411317
 * Source code of AudioService in which the max volumes are replaced:   https://github.com/android/platform_frameworks_base/blob/master/media/java/android/media/AudioService.java (< Marshmallow)
 *                                                                      https://github.com/android/platform_frameworks_base/blob/master/services/core/java/com/android/server/audio/AudioService.java (>= Marshmallow)
 * Source code of the config file which values are replaced:            https://github.com/android/platform_frameworks_base/blob/master/core/res/res/values/config.xml
 */
public class AudioMod implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private static final String LOG_TAG = "VolumeSteps+: ";

    private static final int STREAM_ALARM_DEFAULT = 7;
    private static final int STREAM_MUSIC_DEFAULT = 15;
    private static final int STREAM_NOTIFICATION_DEFAULT = 7;
    private static final int STREAM_RING_DEFAULT = 7;
    private static final int STREAM_SYSTEM_DEFAULT = 7;
    private static final int STREAM_VOICECALL_DEFAULT = 5;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            initHooks(null);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                loadPackageParam.packageName.equals("android") &&
                loadPackageParam.processName.equals("android")) {
            initHooks(loadPackageParam.classLoader);
        }
    }

    private void initHooks(ClassLoader classLoader) {
        // Load the user's preferences
        final XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID);
        final boolean debugging = prefs.getBoolean("pref_debug", false);
        final boolean compatibilityModeLG = prefs.getBoolean("pref_compatibility_mode_lg", false);

        if (debugging) {
            XposedBridge.log(LOG_TAG + "Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");

            Map<String, ?> sortedKeys = new TreeMap<String, Object>(prefs.getAll());
            for (Map.Entry<String, ?> entry : sortedKeys.entrySet()) {
                XposedBridge.log(LOG_TAG + entry.getKey() + "=" + entry.getValue().toString());
            }

            if (compatibilityModeLG) {
                XposedBridge.log(LOG_TAG + "Using LG compatibility mode");
            }
        }

        String audioServiceClassName;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioServiceClassName = "com.android.server.audio.AudioService";
        } else if (compatibilityModeLG) {
            audioServiceClassName = "android.media.AudioServiceEx";
        } else {
            audioServiceClassName = "android.media.AudioService";
        }

        final Class<?> audioServiceClass = XposedHelpers.findClass(audioServiceClassName, classLoader);
        final Class<?> audioSystemClass = XposedHelpers.findClass("android.media.AudioSystem", classLoader);
        final String maxStreamVolumeField = (compatibilityModeLG ? "MAX_STREAM_VOLUME_Ex" : "MAX_STREAM_VOLUME");

        // Hook createAudioSystemThread, this method is called very early in the constructor of AudioService
        XposedHelpers.findAndHookMethod(audioServiceClass, "createAudioSystemThread", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // Retrieve array containing the maximum stream volumes, depends on Android version
                int[] maxStreamVolume;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    maxStreamVolume = (int[]) XposedHelpers.getStaticObjectField(audioServiceClass, maxStreamVolumeField);
                } else {
                    maxStreamVolume = (int[]) XposedHelpers.getObjectField(param.thisObject, maxStreamVolumeField);
                }

                XposedBridge.log(LOG_TAG + "MAX_STREAM_VOLUME before: " + Arrays.toString(maxStreamVolume));

                // Get the max volumes and set them at the index of the right stream
                maxStreamVolume[AudioManager.STREAM_ALARM] = prefs.getInt("pref_stream_alarm", STREAM_ALARM_DEFAULT);
                maxStreamVolume[AudioManager.STREAM_MUSIC] = prefs.getInt("pref_stream_music", STREAM_MUSIC_DEFAULT);
                maxStreamVolume[AudioManager.STREAM_NOTIFICATION] = prefs.getInt("pref_stream_notification", STREAM_NOTIFICATION_DEFAULT);
                maxStreamVolume[AudioManager.STREAM_RING] = prefs.getInt("pref_stream_ring", STREAM_RING_DEFAULT);
                maxStreamVolume[AudioManager.STREAM_SYSTEM] = prefs.getInt("pref_stream_system", STREAM_SYSTEM_DEFAULT);
                maxStreamVolume[AudioManager.STREAM_VOICE_CALL] = prefs.getInt("pref_stream_voicecall", STREAM_VOICECALL_DEFAULT);

                XposedBridge.log(LOG_TAG + "MAX_STREAM_VOLUME after: " + Arrays.toString(maxStreamVolume));
            }
        });


        // Whether the Safe Headset Volume Warning is disabled, default is false
        boolean saveHeadsetVolumeDisabled = prefs.getBoolean("pref_safe_headset_volume_disabled", false);

        if (saveHeadsetVolumeDisabled) {
            // Disable the safe headset volume warning
            XResources.setSystemWideReplacement("android", "bool", "config_safe_media_volume_enabled", false);
            XposedBridge.log(LOG_TAG + "Safe Headset Volume is disabled");
        } else {
            // Calculate the new headset volume warning to comply with the new maximum music volume
            int maxMusicSteps = prefs.getInt("pref_stream_music", 15);
            int safeHeadsetVolume = (int) Math.round(maxMusicSteps * (2.0 / 3.0));

            XResources.setSystemWideReplacement("android", "integer", "config_safe_media_volume_index", safeHeadsetVolume);
            XposedBridge.log(LOG_TAG + "Safe Headset Volume set to " + safeHeadsetVolume);
        }


        // Whether the volume keys control the music stream or the ringer volume
        boolean volumeKeysControlMusic = prefs.getBoolean("pref_volume_keys_control_music", false);
        XposedBridge.log(LOG_TAG + "Volume keys control " + (volumeKeysControlMusic ? "music" : "ringer"));

        if (volumeKeysControlMusic) {
            XposedHelpers.findAndHookMethod(audioServiceClass, "getActiveStreamType", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        boolean platformVoice = (boolean) XposedHelpers.callMethod(param.thisObject, "isPlatformVoice");
                        if (!platformVoice) return;
                    } else {
                        boolean voiceCapable = XposedHelpers.getBooleanField(param.thisObject, "mVoiceCapable");
                        if (!voiceCapable) return;
                    }

                    boolean isInCommunication = (Boolean) XposedHelpers.callMethod(param.thisObject, "isInCommunication");
                    if (isInCommunication) return;

                    int suggestedStreamType = (Integer) param.args[0];
                    if (suggestedStreamType != AudioManager.USE_DEFAULT_STREAM_TYPE) return;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        boolean isAfMusicActiveRecently = (Boolean) XposedHelpers.callMethod(param.thisObject, "isAfMusicActiveRecently", 5000);
                        if (isAfMusicActiveRecently) return;
                    } else {
                        boolean musicStreamActive = (Boolean) XposedHelpers.callStaticMethod(audioSystemClass, "isStreamActive", AudioManager.STREAM_MUSIC, 5000);
                        if (musicStreamActive) return;
                    }

                    // 4.4 and higher call checkUpdateRemoteStateIfActive at the MediaFocusControl class instead of AudioService
                    Object objContainingRemoteStreamMethod = param.thisObject;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        objContainingRemoteStreamMethod = XposedHelpers.getObjectField(param.thisObject, "mMediaFocusControl");
                    }
                    boolean activeRemoteStream = (Boolean) XposedHelpers.callMethod(objContainingRemoteStreamMethod, "checkUpdateRemoteStateIfActive", AudioManager.STREAM_MUSIC);
                    if (activeRemoteStream) return;

                    param.setResult(AudioManager.STREAM_MUSIC);
                    if (debugging) XposedBridge.log(LOG_TAG + "Event: intercepted getActiveStreamType call; returned STREAM_MUSIC");
                }
            });
        }
    }
}
