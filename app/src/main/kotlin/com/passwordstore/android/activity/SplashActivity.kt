/*
 * Copyright © 2014-2019 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.passwordstore.android.activity

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.passwordstore.android.databinding.ActivitySplashBinding
import com.passwordstore.android.ui.EdgeToEdge
import com.passwordstore.android.viewmodel.SplashViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel

@ExperimentalCoroutinesApi
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        EdgeToEdge.setUpRoot(binding.root as ViewGroup)
        setContentView(binding.root)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.isFirstRun.observe(this, Observer {
            if (it == true) {
                startActivity(Intent(this, FirstRunActivity::class.java))
            }
        })
    }
}
