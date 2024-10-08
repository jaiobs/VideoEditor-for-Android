/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.obs.marveleditor.R
import com.obs.marveleditor.interfaces.OptiDialogueHelper
import java.io.File

class OptiVideoOptionFragment : OptiBaseCreatorDialogFragment(), OptiDialogueHelper {
    override fun setHelper(helper: CallBacks) {
        this.helper = helper
    }

    override fun setMode(mode: Int) {

    }

    override fun setFilePathFromSource(file: File) {

    }

    override fun setDuration(duration: Long) {

    }

    //private var tagName: String = OptiVideoOptionFragment::class.java.simpleName
    private var acivClose: AppCompatImageView? = null
    private var tvGallery: TextView? = null
    private var tvCamera: TextView? = null
    private var helper: CallBacks? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val inflate = inflater.inflate(R.layout.opti_add_video_includer, container, false)
        initView(inflate)
        return inflate
    }

    private fun initView(inflate: View?) {
        acivClose = inflate?.findViewById(R.id.acivClose)
        tvGallery = inflate?.findViewById(R.id.tvGallery)
        tvCamera = inflate?.findViewById(R.id.tvCamera)

        acivClose?.setOnClickListener {
            dialog?.dismiss()
        }

        tvGallery?.setOnClickListener {
            dialog?.dismiss()
            helper?.openGallery()
        }

        tvCamera?.setOnClickListener {
            dialog?.dismiss()
            helper?.openCamera()
        }
    }

    override fun permissionsBlocked() {

    }

    companion object {
        fun newInstance() = OptiVideoOptionFragment()
    }
}