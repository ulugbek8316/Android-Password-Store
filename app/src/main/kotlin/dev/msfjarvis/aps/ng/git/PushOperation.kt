/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.msfjarvis.aps.ng.git

import android.app.Activity
import kotlinx.coroutines.launch
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PushCommand

/**
 * Creates a new git operation
 *
 * @param fileDir the git working tree directory
 * @param callingActivity the calling activity
 */
class PushOperation(fileDir: File, callingActivity: Activity) : GitOperation(fileDir, callingActivity) {
  private lateinit var pushCommand: PushCommand

  /**
   * Sets the command
   *
   * @return the current object
   */
  fun setCommand(): PushOperation {
    pushCommand = Git(repository).push().setPushAll().setRemote("origin")
    return this
  }

  override fun execute() {
    pushCommand.setCredentialsProvider(provider)

    scope.launch {
      val result = gitTasksExecutor.performOperation(pushCommand)
      if (result is GitResult.Error) {
        clearAuthData(result.error)
      }
      mutableOperationResult.postValue(result)
    }
  }

}
