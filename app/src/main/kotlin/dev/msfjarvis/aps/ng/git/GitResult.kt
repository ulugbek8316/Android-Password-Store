/*
 * Copyright © 2019-2020 The Android Password Store Authors. All Rights Reserved.
 *  SPDX-License-Identifier: GPL-3.0-only
 *
 */

package dev.msfjarvis.aps.ng.git

sealed class GitResult {
  data class Success(val data: String) : GitResult()
  data class Error(var error: String) : GitResult()
}