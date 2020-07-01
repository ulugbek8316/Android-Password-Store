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
        if (application.isAuthEnabled && application.requiresAuthentication) {
            prefs = PreferenceManager.getDefaultSharedPreferences(this)
            BiometricAuthenticator.authenticate(this) {
                when (it) {
                    is BiometricAuthenticator.Result.Success -> {
                        application.requiresAuthentication = false
                        finish()
                    }
                    is BiometricAuthenticator.Result.HardwareUnavailableOrDisabled -> {
                        prefs.edit { remove(PreferenceKeys.BIOMETRIC_AUTH) }
                        application.requiresAuthentication = false
                        finish()
                    }
                    is BiometricAuthenticator.Result.Failure, BiometricAuthenticator.Result.Cancelled -> {
                        application.requiresAuthentication = true
                        finishAffinity()
                    }
                }
            }
        }
    }
}