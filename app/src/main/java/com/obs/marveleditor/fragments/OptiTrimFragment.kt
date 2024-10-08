/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import com.github.guilhe.views.SeekBarRangedView
import com.github.guilhe.views.addActionListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obs.marveleditor.utils.OptiConstant
import com.obs.marveleditor.OptiVideoEditor
import com.obs.marveleditor.R
import com.obs.marveleditor.interfaces.OptiFFMpegCallback
import com.obs.marveleditor.utils.OptiUtils
import com.obs.marveleditor.utils.VideoUtils
import java.io.File

class OptiTrimFragment : BottomSheetDialogFragment(), OptiFFMpegCallback {

    private var tagName: String = OptiTrimFragment::class.java.simpleName
    private lateinit var rootView: View
    private lateinit var ivClose: ImageView
    private lateinit var ivDone: ImageView
    private var videoFile: File? = null
    private var helper: OptiBaseCreatorDialogFragment.CallBacks? = null
    private var sbrvVideoTrim: SeekBarRangedView? = null
    private var actvStartTime: AppCompatTextView? = null
    private var actvEndTime: AppCompatTextView? = null
    private var duration: Long? = null
    private var mContext: Context? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.opti_fragment_trim, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivClose = rootView.findViewById(R.id.iv_close)
        ivDone = rootView.findViewById(R.id.iv_done)
        sbrvVideoTrim = rootView.findViewById(R.id.sbrvVideoTrim)
        actvStartTime = rootView.findViewById(R.id.actvStartTime)
        actvEndTime = rootView.findViewById(R.id.actvEndTime)

        mContext = context

        ivClose.setOnClickListener {
            dismiss()
        }

        ivDone.setOnClickListener {
            //output file is generated and send to video processing
            val outputFile = OptiUtils.createVideoFile(requireContext())
            Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

            OptiVideoEditor.with(requireContext())
                .setType(OptiConstant.VIDEO_TRIM)
                .setFile(videoFile!!)
                .setOutputPath(outputFile.path)
                .setStartTime(actvStartTime?.text.toString())
                .setEndTime(actvEndTime?.text.toString())
                .setCallback(this)
                .main()

            helper?.showLoading(true)
            dismiss()
        }

        Log.v(tagName, "duration: $duration")
        Log.v(tagName, "duration: " + VideoUtils.secToTime(duration!!))

        sbrvVideoTrim?.setMinValue(0f)
        sbrvVideoTrim?.setMaxValue(duration?.toFloat()!!)
        actvStartTime?.text = VideoUtils.secToTime(0)
        actvEndTime?.text = VideoUtils.secToTime(duration!!)

        sbrvVideoTrim?.addActionListener { minValue, maxValue ->
            Log.v(tagName, "minValue: $minValue, maxValue: $maxValue")
            actvStartTime?.text = VideoUtils.secToTime(minValue.toLong())
            actvEndTime?.text = VideoUtils.secToTime(maxValue.toLong())
        }
    }

    fun setHelper(helper: OptiBaseCreatorDialogFragment.CallBacks) {
        this.helper = helper
    }

    fun setFilePathFromSource(file: File, duration: Long) {
        videoFile = file
        this.duration = duration
    }

    override fun onProgress(progress: String) {
        Log.d(tagName, "onProgress() $progress")
        helper?.showLoading(true)
    }

    override fun onSuccess(convertedFile: File, type: String) {
        Log.d(tagName, "onSuccess()")
        helper?.showLoading(false)
        helper?.onFileProcessed(convertedFile)
    }

    override fun onFailure(error: Exception) {
        Log.d(tagName, "onFailure() " + error.localizedMessage)
        Toast.makeText(mContext, "Video processing failed", Toast.LENGTH_LONG).show()
        helper?.showLoading(false)
    }

    override fun onNotAvailable(error: Exception) {
        Log.d(tagName,"onNotAvailable() " + error.message)
        Log.v(tagName, "Exception: ${error.localizedMessage}")
    }

    override fun onFinish() {
        Log.d(tagName, "onFinish()")
        helper?.showLoading(false)
    }
}