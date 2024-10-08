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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obs.marveleditor.utils.OptiConstant
import com.obs.marveleditor.OptiVideoEditor
import com.obs.marveleditor.R
import com.obs.marveleditor.adapter.OptiPlaybackSpeedAdapter
import com.obs.marveleditor.interfaces.OptiPlaybackSpeedListener
import com.obs.marveleditor.interfaces.OptiFFMpegCallback
import com.obs.marveleditor.utils.OptiUtils
import com.obs.marveleditor.interfaces.OptiDialogueHelper
import java.io.File

class OptiPlaybackSpeedDialogFragment : BottomSheetDialogFragment(), OptiDialogueHelper,
    OptiFFMpegCallback, OptiPlaybackSpeedListener {

    private var tagName: String = OptiPlaybackSpeedDialogFragment::class.java.simpleName
    private lateinit var rootView: View
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var rvPlaybackSpeed: RecyclerView
    private lateinit var optiPlaybackSpeedAdapter: OptiPlaybackSpeedAdapter
    private var playbackSpeed: ArrayList<String> = ArrayList()
    private lateinit var ivClose: ImageView
    private lateinit var ivDone: ImageView
    private var masterFile: File? = null
    private var isHavingAudio = true
    private var helper: OptiBaseCreatorDialogFragment.CallBacks? = null
    private var mContext: Context? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.opti_fragment_playback_speed_dialog, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPlaybackSpeed = rootView.findViewById(R.id.rvPlaybackSpeed)
        ivClose = rootView.findViewById(R.id.iv_close)
        ivDone = rootView.findViewById(R.id.iv_done)
        linearLayoutManager = LinearLayoutManager(requireActivity().applicationContext)

        mContext = context

        ivClose.setOnClickListener {
            dismiss()
        }

        ivDone.setOnClickListener {
            optiPlaybackSpeedAdapter.setPlayback()
        }

        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvPlaybackSpeed.layoutManager = linearLayoutManager

        playbackSpeed.add(OptiConstant.SPEED_0_25)
        playbackSpeed.add(OptiConstant.SPEED_0_5)
        playbackSpeed.add(OptiConstant.SPEED_0_75)
        playbackSpeed.add(OptiConstant.SPEED_1_0)
        playbackSpeed.add(OptiConstant.SPEED_1_25)
        playbackSpeed.add(OptiConstant.SPEED_1_5)

        optiPlaybackSpeedAdapter = OptiPlaybackSpeedAdapter(playbackSpeed, requireActivity().applicationContext, this)
        rvPlaybackSpeed.adapter = optiPlaybackSpeedAdapter
        optiPlaybackSpeedAdapter.notifyDataSetChanged()
    }

    companion object {
        fun newInstance() = OptiPlaybackSpeedDialogFragment()
    }

    override fun setHelper(helper: OptiBaseCreatorDialogFragment.CallBacks) {
        this.helper = helper
    }

    override fun setMode(mode: Int) {
    }

    override fun setFilePathFromSource(file: File) {
        masterFile = file
        isHavingAudio = OptiUtils.isVideoHaveAudioTrack(file.absolutePath)
        Log.d(tagName, "isHavingAudio $isHavingAudio")
    }

    override fun setDuration(duration: Long) {

    }

    override fun onProgress(progress: String) {
        Log.d(tagName, "onProgress() $progress")
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
        helper?.showLoading(false)
    }

    override fun onFinish() {
        Log.d(tagName, "onFinish()")
        helper?.showLoading(false)
    }

    /*private fun processFSVideo(ffMpegCommand: String) {
        val outputFile = OptiUtils.createVideoFile(requireContext())
        Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

        VideoSpeed.with(requireContext())
            .setFile(masterFile!!)
            .setIsHavingAudio(isHavingAudio)
            .setCallback(this@OptiPlaybackSpeedDialogFragment)
            .setOutputPath(outputFile.absolutePath)
            .setOutputFileName(outputFile.name)
            .processFS(ffMpegCommand)
    }*/

    override fun processVideo(playbackSpeed: String, tempo: String) {
        if(playbackSpeed != "0.0") {
            //output file is generated and send to video processing
            val outputFile = OptiUtils.createVideoFile(requireContext())
            Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

            OptiVideoEditor.with(requireContext())
                .setType(OptiConstant.VIDEO_PLAYBACK_SPEED)
                .setFile(masterFile!!)
                .setOutputPath(outputFile.absolutePath)
                .setIsHavingAudio(isHavingAudio)
                .setSpeedTempo(playbackSpeed, tempo)
                .setCallback(this)
                .main()

            helper?.showLoading(true)
            dismiss()
        } else {
            OptiUtils.showGlideToast(requireActivity(), getString(R.string.error_select_speed))
        }
    }
}