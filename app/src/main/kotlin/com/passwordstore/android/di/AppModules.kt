/*
 * Copyright © 2014-2019 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.passwordstore.android.di

import com.passwordstore.android.di.factory.PreferencesFactory
import com.passwordstore.android.viewmodel.SplashViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

@ExperimentalCoroutinesApi
val preferenceModule = module {
    single(named("sharedPrefs")) { PreferencesFactory.provideSharedPreferences(androidContext()) }
    single(named("flowPrefs")) { PreferencesFactory.provideFlowSharedPreferences(get(named("sharedPrefs"))) }
}

@ExperimentalCoroutinesApi
val viewModelModule = module(override = true) {
    viewModel { SplashViewModel(get(named("flowPrefs"))) }
}
