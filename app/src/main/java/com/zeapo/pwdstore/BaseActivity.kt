package com.zeapo.pwdstore

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.zeapo.pwdstore.utils.BiometricAuthenticator
import com.zeapo.pwdstore.utils.PreferenceKeys

open class BaseActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var application: Application

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        application = getApplication() as Application
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onResume() {
        if (application.requiresAuthentication) {
            performBiometricAuth()
        }
        super.onResume()
    }

    private fun performBiometricAuth() {
        BiometricAuthenticator.authenticate(this) {
            when (it) {
                is BiometricAuthenticator.Result.Success -> {
                    application.requiresAuthentication = false
                }
                is BiometricAuthenticator.Result.HardwareUnavailableOrDisabled -> {
                    prefs.edit { remove(PreferenceKeys.BIOMETRIC_AUTH) }
                    application.requiresAuthentication = false
                }
                is BiometricAuthenticator.Result.Failure, BiometricAuthenticator.Result.Cancelled -> {
                    application.requiresAuthentication = true
                    finish()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }
}