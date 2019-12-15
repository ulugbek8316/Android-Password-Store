/*
 * Copyright © 2014-2019 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.passwordstore.android.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.passwordstore.android.databinding.FragmentCloneBinding

class CloneFragment : Fragment() {

    private lateinit var binding: FragmentCloneBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCloneBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        fun newInstance(): CloneFragment {
            return CloneFragment()
        }
    }
}
