/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package dev.msfjarvis.aps.ng.git

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dev.msfjarvis.aps.R
import dev.msfjarvis.aps.git.GitOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.*
import org.eclipse.jgit.transport.RemoteRefUpdate

class GitTasksExecutor(private val context: Context) {

  private fun executeCommands(vararg commands: GitCommand<*>): GitResult {
    var nbChanges: Int? = null
    for (command in commands) {
      try {
        if (command is StatusCommand) { // in case we have changes, we want to keep track of it
          val status = command.call()
          nbChanges = status.changed.size + status.missing.size
        } else if (command is CommitCommand) { // the previous status will eventually be used to avoid a commit
          if (nbChanges == null || nbChanges > 0) command.call()
        } else if (command is PullCommand) {
          val result = command.call()
          val rr = result.rebaseResult
          if (rr.status === RebaseResult.Status.STOPPED) {
            return GitResult.Error(context.getString(R.string.git_pull_fail_error))
          }
        } else if (command is PushCommand) {
          for (result in command.call()) { // Code imported (modified) from Gerrit PushOp, license Apache v2
            for (rru in result.remoteUpdates) {
              when (rru.status) {
                RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD -> return GitResult.Error(context.getString(R.string.git_push_nff_error))
                RemoteRefUpdate.Status.REJECTED_NODELETE, RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED, RemoteRefUpdate.Status.NON_EXISTING, RemoteRefUpdate.Status.NOT_ATTEMPTED ->
                  return GitResult.Error(context.getString(R.string.git_push_generic_error) + rru.status.name)
                RemoteRefUpdate.Status.REJECTED_OTHER_REASON -> return if ("non-fast-forward" == rru.message) {
                  GitResult.Error(context.getString(R.string.git_push_other_error))
                } else {
                  GitResult.Error(context.getString(R.string.git_push_generic_error) + rru.message)
                }
                else -> {
                }
              }
            }
          }
        } else {
          command.call()
        }
      } catch (e: Exception) {
        e.printStackTrace()
        return GitResult.Error(e.message + "\nCaused by:\n" + e.cause)
      }
    }
    return GitResult.Success("")
  }

  fun performOperation(vararg commands: GitCommand<*>): GitResult {
    return executeCommands(*commands)
  }

}