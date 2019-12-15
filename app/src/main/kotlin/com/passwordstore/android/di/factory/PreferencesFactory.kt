/*
 * Copyright © 2014-2019 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.passwordstore.android.di.factory

import android.content.Context
import androidx.preference.PreferenceManager
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi

object PreferencesFactory {

    @ExperimentalCoroutinesApi
    fun provideFlowSharedPreferences(context: Context): FlowSharedPreferences {
        return FlowSharedPreferences(PreferenceManager.getDefaultSharedPreferences(context))
    }
}
