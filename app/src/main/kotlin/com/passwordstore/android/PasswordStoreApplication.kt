/*
 * Copyright © 2014-2019 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.passwordstore.android

import androidx.room.Room
import com.passwordstore.android.db.Database
import com.passwordstore.android.di.preferenceModule
import com.passwordstore.android.di.viewModelModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

@ExperimentalCoroutinesApi
class PasswordStoreApplication : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(
                applicationContext,
                Database::class.java, "aps-database"
        ).build()

        startKoin {
            androidLogger()
            androidContext(this@PasswordStoreApplication)
            modules(listOf(viewModelModule, preferenceModule))
        }
    }
}
