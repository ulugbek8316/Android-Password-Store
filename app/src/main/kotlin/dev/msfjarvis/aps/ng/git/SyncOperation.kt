/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.msfjarvis.aps.ng.git

import android.content.Context
import dev.msfjarvis.aps.R
import kotlinx.coroutines.launch
import java.io.File
import org.eclipse.jgit.api.AddCommand
import org.eclipse.jgit.api.CommitCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.PullCommand
import org.eclipse.jgit.api.PushCommand
import org.eclipse.jgit.api.StatusCommand

/**
 * Creates a new git operation
 *
 * @param fileDir the git working tree directory
 * @param callingActivity the calling activity
 */
class SyncOperation constructor(private val fileDir: File, context: Context) : GitOperation(fileDir, context) {
  private lateinit var addCommand: AddCommand
  private lateinit var statusCommand: StatusCommand
  private lateinit var commitCommand: CommitCommand
  private lateinit var pullCommand: PullCommand
  private lateinit var pushCommand: PushCommand

  /**
   * Sets the command
   *
   * @return the current object
   */
  fun setCommands(): SyncOperation {
    val git = Git(repository)
    addCommand = git.add().addFilepattern(".")
    statusCommand = git.status()
    commitCommand = git.commit().setAll(true).setMessage("[Android Password Store] Sync")
    pullCommand = git.pull().setRebase(true).setRemote("origin")
    pushCommand = git.push().setPushAll().setRemote("origin")
    return this
  }

  override fun execute() {
    if (provider != null) {
      pullCommand.setCredentialsProvider(provider)
      pushCommand.setCredentialsProvider(provider)
    }
    scope.launch {
      val result = gitTasksExecutor.performOperation(addCommand, statusCommand, commitCommand, pullCommand, pushCommand)
      if (result is GitResult.Error) {
        result.error = """
          Error occured during the sync operation,
          \nPlease check the FAQ for possible reasons why this error might occur.
          \n ${context.resources.getString(R.string.jgit_error_dialog_text)}
          \n ${result.error}
        """.trimIndent()

        clearAuthData(result.error)
      }
      mutableOperationResult.postValue(result)
    }
  }

}
