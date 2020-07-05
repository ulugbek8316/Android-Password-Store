package com.zeapo.pwdstore

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.zeapo.pwdstore.utils.BiometricAuthenticator
import com.zeapo.pwdstore.utils.PreferenceKeys

class AuthActivity : AppCompatActivity() {
    private val application by lazy { getApplication() as Application }
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        performBiometricAuth()
    }

    private fun performBiometricAuth() {
        BiometricAuthenticator.authenticate(this) {
            when (it) {
                is BiometricAuthenticator.Result.Success -> {
                    application.requiresAuthentication = false
                    finishAndRemoveTask()
                }
                is BiometricAuthenticator.Result.HardwareUnavailableOrDisabled -> {
                    prefs.edit { remove(PreferenceKeys.BIOMETRIC_AUTH) }
                    application.requiresAuthentication = false
                    finishAndRemoveTask()
                }
                is BiometricAuthenticator.Result.Failure, BiometricAuthenticator.Result.Cancelled -> {
                    application.requiresAuthentication = true
                    finishAffinity()
                }
            }
        }
    }

    // This feels like a hack but it solves the issue when a user goes to home directly without authenticating.
    override fun onStop() {
        super.onStop()
        application.requiresAuthentication = true
        finishAndRemoveTask()
    }
}
