/*
 * Copyright © 2014-2019 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.passwordstore.android.di.factory

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi

object PreferencesFactory {

    fun provideSharedPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @ExperimentalCoroutinesApi
    fun provideFlowSharedPreferences(sharedPreferences: SharedPreferences): FlowSharedPreferences {
        return FlowSharedPreferences(sharedPreferences)
    }
}
