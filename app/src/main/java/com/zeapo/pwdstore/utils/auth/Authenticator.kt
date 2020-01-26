/*
 * Copyright © 2014-2019 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.zeapo.pwdstore.utils.auth

import android.os.Handler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.zeapo.pwdstore.R
import timber.log.Timber

internal class Authenticator(
    private val fragmentActivity: FragmentActivity,
    private val callback: (AuthenticationResult) -> Unit
) {
    private val handler = Handler()
    private val biometricManager = BiometricManager.from(fragmentActivity)

    private val authCallback = object : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            Timber.tag(TAG).d("Error: $errorCode: $errString")
            callback(AuthenticationResult.UnrecoverableError(errorCode, errString))
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            Timber.tag(TAG).d("Failed")
            callback(AuthenticationResult.Failure)
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            Timber.tag(TAG).d("Success")
            callback(AuthenticationResult.Success(result.cryptoObject))
        }
    }

    private val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            { runnable -> handler.post(runnable) },
            authCallback
    )

    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(fragmentActivity.getString(R.string.biometric_prompt_title))
            .setDeviceCredentialAllowed(true)
            .build()

    fun authenticate() {
        if (biometricManager.canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
            callback(AuthenticationResult.UnrecoverableError(
                    0,
                    fragmentActivity.getString(R.string.biometric_prompt_no_hardware)
            ))
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    companion object {
        private const val TAG = "Authenticator"
    }
}
