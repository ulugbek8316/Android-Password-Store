/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.msfjarvis.aps.ng.git

import android.content.Context
import kotlinx.coroutines.launch
import java.io.File
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git

/**
 * Creates a new clone operation
 *
 * @param fileDir the git working tree directory
 * @param callingActivity the calling activity
 */
class CloneOperation(fileDir: File, context: Context) : GitOperation(fileDir, context) {
  private lateinit var cloneCommand: CloneCommand

  /**
   * Sets the command using the repository uri
   *
   * @param uri the uri of the repository
   * @return the current object
   */
  fun setCommand(uri: String): CloneOperation {
    cloneCommand = Git.cloneRepository()
      .setCloneAllBranches(true)
      .setDirectory(repository?.workTree)
      .setURI(uri)
    return this
  }

  /**
   * sets the authentication for user/pwd scheme
   *
   * @param username the username
   * @param password the password
   * @return the current object
   */
  public override fun setAuthentication(username: String, password: String): CloneOperation {
    super.setAuthentication(username, password)
    return this
  }

  /**
   * sets the authentication for the ssh-key scheme
   *
   * @param sshKey the ssh-key file
   * @param username the username
   * @param passphrase the passphrase
   * @return the current object
   */
  public override fun setAuthentication(sshKey: File, username: String, passphrase: String): CloneOperation {
    super.setAuthentication(sshKey, username, passphrase)
    return this
  }

  override fun execute() {
    cloneCommand.setCredentialsProvider(provider)

    scope.launch {
      val result = gitTasksExecutor.performOperation(cloneCommand)
      if (result is GitResult.Error) {
        clearAuthData(result.error)
      }
      mutableOperationResult.postValue(result)
    }
  }

}
