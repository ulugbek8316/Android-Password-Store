/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.zeapo.pwdstore.git

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.ajalt.timberkt.Timber.tag
import com.github.ajalt.timberkt.e
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zeapo.pwdstore.R
import com.zeapo.pwdstore.git.config.ConnectionMode
import com.zeapo.pwdstore.git.config.Protocol
import com.zeapo.pwdstore.git.operation.BreakOutOfDetached
import com.zeapo.pwdstore.git.operation.CloneOperation
import com.zeapo.pwdstore.git.operation.GitOperation
import com.zeapo.pwdstore.git.operation.PullOperation
import com.zeapo.pwdstore.git.operation.PushOperation
import com.zeapo.pwdstore.git.operation.ResetToRemoteOperation
import com.zeapo.pwdstore.git.operation.SyncOperation
import com.zeapo.pwdstore.utils.PasswordRepository
import com.zeapo.pwdstore.utils.PreferenceKeys
import com.zeapo.pwdstore.utils.getEncryptedPrefs
import java.io.File
import java.net.URI
import org.openintents.ssh.authentication.SshAuthenticationApi
import kotlinx.coroutines.launch

/**
 * Abstract AppCompatActivity that holds some information that is commonly shared across git-related
 * tasks and makes sense to be held here.
 */
abstract class BaseGitActivity : AppCompatActivity() {

    lateinit var protocol: Protocol
    lateinit var connectionMode: ConnectionMode
    var url: String? = null
    lateinit var serverHostname: String
    lateinit var serverPort: String
    lateinit var serverUser: String
    lateinit var serverPath: String
    lateinit var username: String
    lateinit var email: String
    lateinit var branch: String
    lateinit var settings: SharedPreferences
        private set
    private lateinit var encryptedSettings: SharedPreferences

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = PreferenceManager.getDefaultSharedPreferences(this)
        encryptedSettings = getEncryptedPrefs("git_operation")
        protocol = Protocol.fromString(settings.getString(PreferenceKeys.GIT_REMOTE_PROTOCOL, null))
        connectionMode = ConnectionMode.fromString(settings.getString(PreferenceKeys.GIT_REMOTE_AUTH, null))
        serverHostname = settings.getString(PreferenceKeys.GIT_REMOTE_SERVER, null) ?: ""
        serverPort = settings.getString(PreferenceKeys.GIT_REMOTE_PORT, null) ?: ""
        serverUser = settings.getString(PreferenceKeys.GIT_REMOTE_USERNAME, null) ?: ""
        serverPath = settings.getString(PreferenceKeys.GIT_REMOTE_LOCATION, null) ?: ""
        username = settings.getString(PreferenceKeys.GIT_CONFIG_USER_NAME, null) ?: ""
        email = settings.getString(PreferenceKeys.GIT_CONFIG_USER_EMAIL, null) ?: ""
        branch = settings.getString(PreferenceKeys.GIT_BRANCH_NAME, null) ?: "master"
        updateUrl()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    enum class GitUpdateUrlResult(val textRes: Int) {
        Ok(0),
        CustomPortRequiresAbsoluteUrlError(R.string.git_config_error_custom_port_absolute),
        EmptyHostnameError(R.string.git_config_error_hostname_empty),
        GenericError(R.string.git_config_error_generic),
        NonNumericPortError(R.string.git_config_error_nonnumeric_port)
    }

    /**
     * Update the [url] field with the values that build it up. This function returns a
     * [GitUpdateUrlResult] indicating whether the values could be used to build a URL and only adds
     * the `origin` remote when they were. This check is not perfect, it is mostly meant to catch
     * syntax-related typos.
     */
    fun updateUrl(): GitUpdateUrlResult {
        if (serverHostname.isEmpty())
            return GitUpdateUrlResult.EmptyHostnameError
        if (!serverPort.isDigitsOnly())
            return GitUpdateUrlResult.NonNumericPortError

        val previousUrl = url ?: ""
        // Whether we need the leading ssh:// depends on the use of a custom port.
        val hostnamePart = serverHostname.removePrefix("ssh://")
        val newUrl = when (protocol) {
            Protocol.Ssh -> {
                val userPart = if (serverUser.isEmpty()) "" else "${serverUser.trimEnd('@')}@"
                val portPart =
                    if (serverPort == "22" || serverPort.isEmpty()) "" else ":$serverPort"
                if (portPart.isEmpty()) {
                    "$userPart$hostnamePart:$serverPath"
                } else {
                    // Only absolute paths are supported with custom ports.
                    if (!serverPath.startsWith('/'))
                        return GitUpdateUrlResult.CustomPortRequiresAbsoluteUrlError
                    val pathPart = serverPath
                    // We have to specify the ssh scheme as this is the only way to pass a custom
                    // port.
                    "ssh://$userPart$hostnamePart$portPart$pathPart"
                }
            }
            Protocol.Https -> {
                val portPart =
                    if (serverPort == "443" || serverPort.isEmpty()) "" else ":$serverPort"
                val pathPart = serverPath.trimStart('/', ':')
                val urlWithFreeEntryScheme = "$hostnamePart$portPart/$pathPart"
                val url = when {
                    urlWithFreeEntryScheme.startsWith("https://") -> urlWithFreeEntryScheme
                    urlWithFreeEntryScheme.startsWith("http://") -> urlWithFreeEntryScheme.replaceFirst("http", "https")
                    else -> "https://$urlWithFreeEntryScheme"
                }
                try {
                    if (URI(url).rawAuthority != null)
                        url
                    else
                        return GitUpdateUrlResult.GenericError
                } catch (_: Exception) {
                    return GitUpdateUrlResult.GenericError
                }
            }
        }
        if (PasswordRepository.isInitialized)
            PasswordRepository.addRemote("origin", newUrl, true)
        // When the server changes, remote password and host key file should be deleted.
        if (previousUrl.isNotEmpty() && newUrl != previousUrl) {
            encryptedSettings.edit { remove(PreferenceKeys.HTTPS_PASSWORD) }
            File("$filesDir/.host_key").delete()
        }
        url = newUrl
        return GitUpdateUrlResult.Ok
    }

    /**
     * Attempt to launch the requested Git operation.
     *
     * @param operation The type of git operation to launch
     */
    suspend fun launchGitOperation(operation: Int) {
        if (url == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        try {
            val localDir = requireNotNull(PasswordRepository.getRepositoryDirectory(this))
            val op = when (operation) {
                REQUEST_CLONE, GitOperation.GET_SSH_KEY_FROM_CLONE -> CloneOperation(localDir, url!!, this)
                REQUEST_PULL -> PullOperation(localDir, this)
                REQUEST_PUSH -> PushOperation(localDir, this)
                REQUEST_SYNC -> SyncOperation(localDir, this)
                BREAK_OUT_OF_DETACHED -> BreakOutOfDetached(localDir, this)
                REQUEST_RESET -> ResetToRemoteOperation(localDir, this)
                else -> {
                    tag(TAG).e { "Operation not recognized : $operation" }
                    setResult(RESULT_CANCELED)
                    finish()
                    return
                }
            }
            op.executeAfterAuthentication(connectionMode, serverUser)
        } catch (e: Exception) {
            e.printStackTrace()
            MaterialAlertDialogBuilder(this).setMessage(e.message).show()
        }
    }

    companion object {

        const val REQUEST_ARG_OP = "OPERATION"
        const val REQUEST_PULL = 101
        const val REQUEST_PUSH = 102
        const val REQUEST_CLONE = 103
        const val REQUEST_SYNC = 104
        const val BREAK_OUT_OF_DETACHED = 105
        const val REQUEST_RESET = 106
        const val TAG = "AbstractGitActivity"
    }
}
