/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.msfjarvis.aps.ng.git

import android.app.Activity
import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.msfjarvis.aps.R
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

    gitTasksExecutor.execute(*this.commands.toTypedArray())
  }

  override fun onError(errorMessage: String) {
    MaterialAlertDialogBuilder(callingActivity)
      .setTitle(callingActivity.resources.getString(R.string.jgit_error_dialog_title))
      .setMessage("Error occurred when checking out another branch operation $errorMessage")
      .setPositiveButton(callingActivity.resources.getString(R.string.dialog_ok)) { _, _ ->
        callingActivity.finish()
      }.show()
  }

  override fun onSuccess() {
    MaterialAlertDialogBuilder(callingActivity)
      .setTitle(callingActivity.resources.getString(R.string.git_abort_and_push_title))
      .setMessage("There was a conflict when trying to rebase. " +
        "Your local master branch was pushed to another branch named conflicting-master-....\n" +
        "Use this branch to resolve conflict on your computer")
      .setPositiveButton(callingActivity.resources.getString(R.string.dialog_ok)) { _, _ ->
        callingActivity.finish()
      }.show()
  }
}
