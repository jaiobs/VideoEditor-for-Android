/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.fragments

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obs.videoeditor.editor.OptiConstant
import com.obs.videoeditor.editor.OptiVideoEditor
import com.obs.marveleditor.R
import com.obs.marveleditor.adapter.OptiClipArtAdapter
import com.obs.marveleditor.adapter.OptiPositionAdapter
import com.obs.marveleditor.interfaces.OptiClipArtListener
import com.obs.marveleditor.interfaces.OptiPositionListener
import com.obs.videoeditor.editor.OptiFFMpegCallback
import com.obs.marveleditor.utils.OptiUtils
import java.io.File

class OptiAddClipArtFragment : BottomSheetDialogFragment(), OptiClipArtListener, OptiPositionListener,
    OptiFFMpegCallback {

    private var tagName: String = OptiAddClipArtFragment::class.java.simpleName
    private lateinit var rootView: View
    private lateinit var linearLayoutManagerOne: LinearLayoutManager
    private lateinit var linearLayoutManagerTwo: LinearLayoutManager
    private lateinit var rvClipArt: RecyclerView
    private lateinit var rvPosition: RecyclerView
    private lateinit var ivClose: ImageView
    private lateinit var ivDone: ImageView
    private var videoFile: File? = null
    private var clipArtFilePath: ArrayList<String> = ArrayList()
    private var positionList: ArrayList<String> = ArrayList()
    private var helper: OptiBaseCreatorDialogFragment.CallBacks? = null
    private lateinit var optiClipArtAdapter: OptiClipArtAdapter
    private lateinit var optiPositionAdapter: OptiPositionAdapter
    private var selectedPositionItem: String? = null
    private var selectedFilePath: String? = null
    private var mContext: Context? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.opti_fragment_add_clip_art, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvClipArt = rootView.findViewById(R.id.rvClipArt)
        rvPosition = rootView.findViewById(R.id.rvPosition)
        ivClose = rootView.findViewById(R.id.iv_close)
        ivDone = rootView.findViewById(R.id.iv_done)
        linearLayoutManagerOne = LinearLayoutManager(requireActivity().applicationContext)
        linearLayoutManagerTwo = LinearLayoutManager(requireActivity().applicationContext)

        linearLayoutManagerOne.orientation = LinearLayoutManager.HORIZONTAL
        rvClipArt.layoutManager = linearLayoutManagerOne
        linearLayoutManagerTwo.orientation = LinearLayoutManager.HORIZONTAL
        rvPosition.layoutManager = linearLayoutManagerTwo

        mContext = context

        val listFile: Array<File>

        val file = File(
            Environment.getExternalStorageDirectory(),
            File.separator + OptiConstant.APP_NAME + File.separator + OptiConstant.CLIP_ARTS + File.separator
        )

        if (file.isDirectory) {
            listFile = file.listFiles()
            for (i in listFile.indices) {
                clipArtFilePath.add(listFile[i].absolutePath)
            }
        }

        optiClipArtAdapter = OptiClipArtAdapter(clipArtFilePath, requireActivity().applicationContext, this)
        rvClipArt.adapter = optiClipArtAdapter
        optiClipArtAdapter.notifyDataSetChanged()

        positionList.add(OptiConstant.BOTTOM_LEFT)
        positionList.add(OptiConstant.BOTTOM_RIGHT)
        positionList.add(OptiConstant.CENTRE)
        positionList.add(OptiConstant.TOP_LEFT)
        positionList.add(OptiConstant.TOP_RIGHT)

        optiPositionAdapter = OptiPositionAdapter(positionList, requireActivity().applicationContext, this)
        rvPosition.adapter = optiPositionAdapter
        optiPositionAdapter.notifyDataSetChanged()

        ivClose.setOnClickListener {
            dismiss()
        }

        ivDone.setOnClickListener {
            optiClipArtAdapter.setClipArt()
            optiPositionAdapter.setPosition()

            if (selectedFilePath != null) {
                if (selectedPositionItem != null) {
                    dismiss()
                    //apply clip art based on selected position
                    when (selectedPositionItem) {
                        OptiConstant.BOTTOM_LEFT -> {
                            addClipArtAction(selectedFilePath!!, OptiVideoEditor.BOTTOM_LEFT)
                        }

                        OptiConstant.BOTTOM_RIGHT -> {
                            addClipArtAction(selectedFilePath!!, OptiVideoEditor.BOTTOM_RIGHT)
                        }

                        OptiConstant.CENTRE -> {
                            addClipArtAction(selectedFilePath!!, OptiVideoEditor.CENTER_ALLIGN)
                        }

                        OptiConstant.TOP_LEFT -> {
                            addClipArtAction(selectedFilePath!!, OptiVideoEditor.TOP_LEFT)
                        }

                        OptiConstant.TOP_RIGHT -> {
                            addClipArtAction(selectedFilePath!!, OptiVideoEditor.TOP_RIGHT)
                        }
                    }
                } else {
                    OptiUtils.showGlideToast(requireActivity(), getString(R.string.error_select_sticker_pos))
                }
            } else {
                OptiUtils.showGlideToast(requireActivity(), getString(R.string.error_select_sticker))
            }
        }
    }

    private fun addClipArtAction(imgPath: String, position: String) {
        //output file is generated and it is send to video processing
        val outputFile = OptiUtils.createVideoFile(requireContext())
        Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

        OptiVideoEditor.with(requireContext())
            .setType(OptiConstant.VIDEO_CLIP_ART_OVERLAY)
            .setFile(videoFile!!)
            .setOutputPath(outputFile.path)
            .setImagePath(imgPath)
            .setPosition(position)
            .setCallback(this)
            .main()

        helper?.showLoading(true)
    }

    fun setHelper(helper: OptiBaseCreatorDialogFragment.CallBacks) {
        this.helper = helper
    }

    fun setFilePathFromSource(file: File) {
        videoFile = file
    }

    override fun selectedPosition(position: String) {
        selectedPositionItem = position
    }

    override fun selectedClipArt(path: String) {
        selectedFilePath = path
    }

    override fun onProgress(progress: String) {
        Log.v(tagName, "onProgress()")
    }

    override fun onSuccess(convertedFile: File, type: String) {
        Log.v(tagName, "onSuccess()")
        helper?.showLoading(false)
        helper?.onFileProcessed(convertedFile)
    }

    override fun onFailure(error: Exception) {
        Log.v(tagName, "onFailure() ${error.localizedMessage}")
        Toast.makeText(mContext, "Video processing failed", Toast.LENGTH_LONG).show()
        helper?.showLoading(false)
    }

    override fun onNotAvailable(error: Exception) {
        Log.v(tagName, "onNotAvailable() ${error.localizedMessage}")
    }

    override fun onFinish() {
        Log.v(tagName, "onFinish()")
        helper?.showLoading(false)
    }
}