/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.msfjarvis.aps.ng.git

import android.content.Context
import kotlinx.coroutines.launch
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullCommand

/**
 * Creates a new git operation
 *
 * @param fileDir the git working tree directory
 * @param context the application context
 */
class PullOperation(fileDir: File, context: Context) : GitOperation(fileDir, context) {
  private lateinit var pullCommand: PullCommand

  /**
   * Sets the command
   *
   * @return the current object
   */
  fun setCommand(): PullOperation {
    pullCommand = Git(repository).pull().setRebase(true).setRemote("origin")
    return this
  }

  override fun execute() {
    pullCommand.setCredentialsProvider(provider)

    scope.launch {
      val result = gitTasksExecutor.performOperation(pullCommand)
      if (result is GitResult.Error) {
        clearAuthData(result.error)
      }
      mutableOperationResult.postValue(result)
    }
  }

}
