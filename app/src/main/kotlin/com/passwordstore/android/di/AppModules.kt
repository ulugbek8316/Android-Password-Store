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
import org.koin.dsl.module

@ExperimentalCoroutinesApi
val preferenceModule = module {
    single { PreferencesFactory.provideFlowSharedPreferences(androidContext()) }
}

@ExperimentalCoroutinesApi
val viewModelModule = module {
    viewModel { SplashViewModel(get()) }
}
