/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.zeapo.pwdstore

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import com.github.ajalt.timberkt.Timber.DebugTree
import com.github.ajalt.timberkt.Timber.plant
import com.zeapo.pwdstore.git.config.setUpBouncyCastleForSshj
import com.zeapo.pwdstore.utils.PreferenceKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@Suppress("Unused")
class Application : android.app.Application(), SharedPreferences.OnSharedPreferenceChangeListener, LifecycleObserver {

    private var prefs: SharedPreferences? = null
    private val requiresAuthentication = MutableLiveData(true)
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val authTimeout = 15

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (BuildConfig.ENABLE_DEBUG_FEATURES || prefs?.getBoolean(PreferenceKeys.ENABLE_DEBUG_LOGGING, false) ==
            true) {
            plant(DebugTree())
        }
        prefs?.registerOnSharedPreferenceChangeListener(this)
        setNightMode()
        setUpBouncyCastleForSshj()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onTerminate() {
        prefs?.unregisterOnSharedPreferenceChangeListener(this)
        super.onTerminate()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String) {
        when (key) {
            PreferenceKeys.APP_THEME -> setNightMode()
        }
    }

    private fun setNightMode() {
        AppCompatDelegate.setDefaultNightMode(when (prefs?.getString(PreferenceKeys.APP_THEME, getString(R.string.app_theme_def))) {
            "light" -> MODE_NIGHT_NO
            "dark" -> MODE_NIGHT_YES
            "follow_system" -> MODE_NIGHT_FOLLOW_SYSTEM
            else -> MODE_NIGHT_AUTO_BATTERY
        })
    }

    private fun updateAuthTimeout() {
    }

    @OptIn(ExperimentalTime::class)
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun onPause() {
        coroutineScope.launch {
            delay(authTimeout.seconds)
            requiresAuthentication.postValue(true)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onBackground() {
        job.cancel()
        requiresAuthentication.postValue(true);
    }
}
