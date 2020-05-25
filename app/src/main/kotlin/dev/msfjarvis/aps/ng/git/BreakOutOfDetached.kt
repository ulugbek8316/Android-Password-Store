/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.msfjarvis.aps.ng.git

import android.app.Activity
import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.msfjarvis.aps.R
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.*
import java.io.File

class BreakOutOfDetached(fileDir: File, context: Context) : GitOperation(fileDir, context) {
  private lateinit var rebaseCommand: RebaseCommand
  private lateinit var checkoutCreateBranchCommand: CheckoutCommand
  private lateinit var pushCommand: PushCommand
  private lateinit var checkoutCommand: CheckoutCommand

  /**
   * Sets the command
   *
   * @return the current object
   */
  fun setCommands(): BreakOutOfDetached {
    val git = Git(repository)
    val branchName = "conflicting-master-${System.currentTimeMillis()}"
    // abort the rebase
    rebaseCommand = git.rebase().setOperation(RebaseCommand.Operation.ABORT)
    // git checkout -b conflict-branch
    checkoutCreateBranchCommand = git.checkout().setCreateBranch(true).setName(branchName)
    // push the changes
    pushCommand = git.push().setRemote("origin")
    // switch back to master
    checkoutCommand = git.checkout().setName("master")

    return this
  }

  override fun execute() {
    val git = Git(repository)
    if (!git.repository.repositoryState.isRebasing) {
      val result = GitResult.Error(context.resources.getString(R.string.dialog_ok))
      mutableOperationResult.postValue(result);
      return
    }

    if (provider != null) {
      // set the credentials for push command
      pushCommand.setCredentialsProvider(provider)
    }

    scope.launch {
      val result = gitTasksExecutor.performOperation(rebaseCommand, checkoutCreateBranchCommand, pushCommand, checkoutCommand)

      when (result) {
        is GitResult.Error -> {
          result.error = """
          Error occurred when checking out another branch operation,
          \n ${context.resources.getString(R.string.jgit_error_dialog_text)}
          \n ${result.error}
          """.trimIndent()
        }

        is GitResult.Success -> {
          result.data = """
            There was a conflict when trying to rebase.
            \nYour local master branch was pushed to another branch named conflicting-master-....
            \nUse this branch to resolve conflict on your computer
          """.trimIndent()
        }
      }

      mutableOperationResult.postValue(result)
    }
  }
}
