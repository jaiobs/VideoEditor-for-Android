package com.obs.videoeditor.avmerger

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obs.videoeditor.avmerger.AudioVideoMergerFragment.Companion.AUDIO_FILE_PATH
import com.obs.videoeditor.avmerger.AudioVideoMergerFragment.Companion.VIDEO_FILE_PATH
import com.obs.videoeditor.databinding.BottomSheetAudioVideoMergerBinding
import java.io.File

class AudioVideoMergerBS : BottomSheetDialogFragment(),
    AudioVideoMergerFragment.AvMergerCallbackListener {

    companion object {

        fun newInstance(
            videoFilePath: String,
            audioFilePath: String,
        ) = AudioVideoMergerBS().apply {
            arguments = bundleOf(
                VIDEO_FILE_PATH to videoFilePath,
                AUDIO_FILE_PATH to audioFilePath,
            )
        }
    }

    private var _binding: BottomSheetAudioVideoMergerBinding? = null
    private val binding: BottomSheetAudioVideoMergerBinding
        get() = requireNotNull(_binding)

    private var listener: AvMergerBSListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = BottomSheetAudioVideoMergerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpBottomSheet()
        attachAvMergerFragment()
    }

    private fun setUpBottomSheet() {
        val dialog = dialog as BottomSheetDialog
        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT

        val behavior = BottomSheetBehavior.from(bottomSheet!!)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        behavior.isDraggable = false
    }

    private fun attachAvMergerFragment() {
        val videoFilePath = arguments?.getString(VIDEO_FILE_PATH) ?: ""
        val audioFilePath = arguments?.getString(AUDIO_FILE_PATH) ?: ""

        childFragmentManager.beginTransaction()
            .replace(
                binding.fragmentContainer.id,
                AudioVideoMergerFragment.newInstance(
                    videoFilePath = videoFilePath,
                    audioFilePath = audioFilePath
                )
            )
            .commitNow()
    }


    override fun onAudioVideoMerged(mergedVideoFile: File) {
        listener?.onAudioVideoMerged(mergedVideoFile)
    }

    override fun close() {
        dismissAllowingStateLoss()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parentFragment = parentFragment
        val activity = activity
        if (parentFragment is AvMergerBSListener) {
            listener = parentFragment
        } else if (activity is AvMergerBSListener) {
            listener = activity
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getBottomSheetBehaviour(): BottomSheetBehavior<View>? {
        if (dialog !is BottomSheetDialog) return null
        val bottomSheetDialog = dialog as BottomSheetDialog? ?: return null
        val bottomSheet =
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return null
        return BottomSheetBehavior.from(bottomSheet)
    }

    interface AvMergerBSListener {
        fun onAudioVideoMerged(mergedVideoFile: File)
    }

}