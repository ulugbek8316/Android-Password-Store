/*
 * Copyright © 2014-2019 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.passwordstore.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tfcporciuncula.flow.FlowSharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class SplashViewModel(private val flowPreferences: FlowSharedPreferences) : ViewModel() {

    val isFirstRun = flowPreferences.getBoolean("first_run", true).asFlow().asLiveData()
}
