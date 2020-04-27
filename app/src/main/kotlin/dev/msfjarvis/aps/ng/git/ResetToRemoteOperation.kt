/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.msfjarvis.aps.ng.git

import android.content.Context
import kotlinx.coroutines.launch
import java.io.File
import org.eclipse.jgit.api.AddCommand
import org.eclipse.jgit.api.FetchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand

/**
 * Creates a new git operation
 *
 * @param fileDir the git working tree directory
 * @param callingActivity the calling activity
 */
class ResetToRemoteOperation(fileDir: File, context: Context) : GitOperation(fileDir, context) {
  private lateinit var addCommand: AddCommand
  private lateinit var fetchCommand: FetchCommand
  private lateinit var resetCommand: ResetCommand

  /**
   * Sets the command
   *
   * @return the current object
   */
  fun setCommands(): ResetToRemoteOperation {
    val git = Git(repository)
    addCommand = git.add().addFilepattern(".")
    fetchCommand = git.fetch().setRemote("origin")
    resetCommand = git.reset().setRef("origin/master").setMode(ResetCommand.ResetType.HARD)
    return this
  }

  override fun execute() {
    fetchCommand.setCredentialsProvider(provider)

    scope.launch {
      val result = gitTasksExecutor.performOperation(addCommand, fetchCommand, resetCommand)
      if (result is GitResult.Error) {
        clearAuthData(result.error)
      }
      mutableOperationResult.postValue(result)
    }
  }

}
