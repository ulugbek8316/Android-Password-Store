/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.zeapo.pwdstore

import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import com.github.ajalt.timberkt.Timber.DebugTree
import com.github.ajalt.timberkt.Timber.d
import com.github.ajalt.timberkt.Timber.plant
import com.zeapo.pwdstore.git.config.setUpBouncyCastleForSshj
import com.zeapo.pwdstore.utils.PreferenceKeys

@Suppress("Unused")
class Application : android.app.Application(), SharedPreferences.OnSharedPreferenceChangeListener, LifecycleObserver {

    private lateinit var prefs: SharedPreferences
    @Volatile var isAuthEnabled = false
    @Volatile var requiresAuthentication = true

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (BuildConfig.ENABLE_DEBUG_FEATURES || prefs.getBoolean(PreferenceKeys.ENABLE_DEBUG_LOGGING, false)) {
            plant(DebugTree())
        }
        prefs.registerOnSharedPreferenceChangeListener(this)
        setAuthentication()
        setNightMode()
        setUpBouncyCastleForSshj()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onTerminate() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onTerminate()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        when (key) {
            PreferenceKeys.APP_THEME -> setNightMode()
            PreferenceKeys.BIOMETRIC_AUTH -> setAuthentication()
        }
    }

    private fun setNightMode() {
        AppCompatDelegate.setDefaultNightMode(when (prefs.getString(PreferenceKeys.APP_THEME, getString(R.string.app_theme_def))) {
            "light" -> MODE_NIGHT_NO
            "dark" -> MODE_NIGHT_YES
            "follow_system" -> MODE_NIGHT_FOLLOW_SYSTEM
            else -> MODE_NIGHT_AUTO_BATTERY
        })
    }

    private fun setAuthentication() {
        isAuthEnabled = prefs.getBoolean(PreferenceKeys.BIOMETRIC_AUTH, false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        d { "onResume : $requiresAuthentication" }
        if (isAuthEnabled && requiresAuthentication) {
            requiresAuthentication = false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onBackground() {
        d { "onBackground" }
        requiresAuthentication = true
    }
}
