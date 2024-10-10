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
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obs.videoeditor.editor.OptiConstant
import com.obs.videoeditor.editor.OptiVideoEditor
import com.obs.marveleditor.R
import com.obs.marveleditor.adapter.OptiPositionAdapter
import com.obs.marveleditor.interfaces.OptiPositionListener
import com.obs.videoeditor.editor.OptiFFMpegCallback
import com.obs.marveleditor.utils.OptiUtils
import java.io.File
import java.util.*

class OptiAddTextFragment : BottomSheetDialogFragment(), OptiPositionListener, OptiFFMpegCallback {

    private var tagName: String = OptiAddTextFragment::class.java.simpleName
    private lateinit var rootView: View
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var rvPosition: RecyclerView
    private lateinit var ivClose: ImageView
    private lateinit var ivDone: ImageView
    private var videoFile: File? = null
    private var helper: OptiBaseCreatorDialogFragment.CallBacks? = null
    private lateinit var optiPositionAdapter: OptiPositionAdapter
    private var positionList: ArrayList<String> = ArrayList()
    private var selectedPositionItem: String? = null
    private var etText: EditText? = null
    private var positionStr: String? = null
    private var mContext: Context? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.opti_fragment_add_text, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPosition = rootView.findViewById(R.id.rvPosition)
        ivClose = rootView.findViewById(R.id.iv_close)
        ivDone = rootView.findViewById(R.id.iv_done)
        etText = rootView.findViewById(R.id.etText)
        linearLayoutManager = LinearLayoutManager(requireActivity().applicationContext)

        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvPosition.layoutManager = linearLayoutManager

        mContext = context

        ivClose.setOnClickListener {
            dismiss()
        }

        ivDone.setOnClickListener {

            val text = etText!!.text.toString().trim()
            Log.v(tagName, "userText: $text")

            if (text.isNotEmpty()) {
                optiPositionAdapter.setPosition()

                if (selectedPositionItem != null) {
                    dismiss()

                    //get selected position to apply text on video
                    when (selectedPositionItem) {
                        OptiConstant.BOTTOM_LEFT -> { //not working
                            positionStr = OptiVideoEditor.POSITION_BOTTOM_LEFT
                        }

                        OptiConstant.BOTTOM_RIGHT -> {
                            positionStr = OptiVideoEditor.POSITION_BOTTOM_RIGHT
                        }

                        OptiConstant.CENTRE_ALIGN -> {
                            positionStr = OptiVideoEditor.POSITION_CENTER_ALLIGN
                        }

                        OptiConstant.CENTRE_BOTTOM -> {
                            positionStr = OptiVideoEditor.POSITION_CENTER_BOTTOM
                        }

                        OptiConstant.TOP_LEFT -> {
                            positionStr = OptiVideoEditor.POSITION_TOP_LEFT
                        }

                        OptiConstant.TOP_RIGHT -> {
                            positionStr = OptiVideoEditor.POSITION_TOP_RIGHT
                        }
                    }

                    //output file is generated and send to video processing
                    val outputFile = OptiUtils.createVideoFile(requireContext())
                    Log.v(tagName, "outputFile: ${outputFile.absolutePath}")

                    //get font file
                    val fontFile = File(
                        Environment.getExternalStorageDirectory(),
                        File.separator + OptiConstant.APP_NAME + File.separator + OptiConstant.FONT + File.separator + OptiConstant.DEFAULT_FONT
                    )
                    Log.v(tagName, "fontPath: ${fontFile.absolutePath}")

                    OptiVideoEditor.with(requireContext())
                        .setType(OptiConstant.VIDEO_TEXT_OVERLAY)
                        .setFile(videoFile!!)
                        .setOutputPath(outputFile.path)
                        .setFont(fontFile)
                        .setText(text)
                        .setColor("#FFFFFF")
                        .setSize("32")
                        .addBorder(false)
                        .setPosition(positionStr!!)
                        .setCallback(this)
                        .main()

                    helper?.showLoading(true)
                } else {
                    OptiUtils.showGlideToast(requireActivity(), getString(R.string.error_add_text_pos))
                }
            } else {
                OptiUtils.showGlideToast(requireActivity(), getString(R.string.error_add_text))
            }
        }

        //positionList.add(OptiConstant.BOTTOM_LEFT)
        positionList.add(OptiConstant.BOTTOM_RIGHT)
        positionList.add(OptiConstant.CENTRE_ALIGN)
        positionList.add(OptiConstant.CENTRE_BOTTOM)
        positionList.add(OptiConstant.TOP_LEFT)
        positionList.add(OptiConstant.TOP_RIGHT)

        optiPositionAdapter = OptiPositionAdapter(positionList, requireActivity().applicationContext, this)
        rvPosition.adapter = optiPositionAdapter
        optiPositionAdapter.notifyDataSetChanged()
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