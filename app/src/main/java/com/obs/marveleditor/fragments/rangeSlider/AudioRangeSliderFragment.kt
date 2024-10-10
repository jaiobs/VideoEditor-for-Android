/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.fragments.rangeSlider

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obs.marveleditor.databinding.FragmentAudioRangeSliderBinding
import com.obs.marveleditor.utils.OptiUtils
import java.io.File

class AudioRangeSliderFragment : BottomSheetDialogFragment() {


    companion object {
        const val AUDIO_FILE_PATH = "AUDIO_FILE_PATH"

        fun newInstance(
            audioFilePath: String
        ) = AudioRangeSliderFragment().apply {
            arguments = bundleOf(
                AUDIO_FILE_PATH to audioFilePath
            )
        }
    }

    private var _binding: FragmentAudioRangeSliderBinding? = null
    private val binding: FragmentAudioRangeSliderBinding
        get() = requireNotNull(_binding)


    private val screenWidth: Int = Resources.getSystem().displayMetrics.widthPixels

    private lateinit var audioFilePath: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentAudioRangeSliderBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioFilePath = arguments?.getString(AUDIO_FILE_PATH) ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAudioView()
    }

    override fun onStart() {
        super.onStart()
        setFullScreenHeight()
    }

    private fun setFullScreenHeight() {
        val dialog = dialog as BottomSheetDialog
        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT

        val behavior = BottomSheetBehavior.from(bottomSheet!!)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true

    }

    private fun initAudioView() {
        if (audioFilePath.isBlank()) {
            Toast.makeText(requireContext(), "Audio File Path is blank", Toast.LENGTH_SHORT).show()
            return
        }

        val audioFile = File(audioFilePath)
        binding.seekBarAudioProgress.setSampleFrom(audioFile)

        val audioSelectViewWidth = screenWidth / 3
        binding.audioSelectView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = audioSelectViewWidth
        }

        val timeInMillis = OptiUtils.getVideoDuration(requireContext(), audioFile)
        val audioLength = (timeInMillis / 1000).toFloat() // Length of the audio in seconds
        val frameSize = 10f

        binding.audioSelectView.post {
            // Calculate width for the seekBarAudioProgress based on audio length
            binding.seekBarAudioProgress.updateLayoutParams<FrameLayout.LayoutParams> {
                val seekBarWidth = (audioSelectViewWidth / frameSize) * audioLength
                width = seekBarWidth.toInt()
                val remainingHorizontalArea = binding.clAudioSelector.width - audioSelectViewWidth
                marginStart = remainingHorizontalArea / 2
                marginEnd = remainingHorizontalArea / 2
            }
        }


        binding.rangeSlider.valueFrom = 0f
        binding.rangeSlider.valueTo = audioLength
        binding.rangeSlider.setValues(0f, frameSize)
        updateSelectedTime(0, frameSize.toInt())


        // Update RangeSlider when user scrolls through the audio selector
        binding.audioScroller.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            // Get maximum scrollable width (total width - visible width)
            val maxScroll = binding.audioScroller.getChildAt(0).width - binding.audioScroller.width
            // Calculate scroll ratio (how far we have scrolled compared to total scrollable width)
            val scrollRatio = scrollX.toFloat() / maxScroll.toFloat()
            // Calculate new start position in the audio based on scroll ratio
            val newStartPosition = scrollRatio * (audioLength - frameSize)
            val newEndPosition = minOf(newStartPosition + frameSize, audioLength)

            binding.rangeSlider.setValues(newStartPosition, newEndPosition)
            updateSelectedTime(newStartPosition.toInt(), newEndPosition.toInt())
        }

        // Update the scrolling position when the user changes the RangeSlider
        binding.rangeSlider.addOnChangeListener { slider, _, _ ->
            val startPosition = slider.values[0]
            val endPosition = minOf(startPosition + frameSize, audioLength)

            // Set the thumbs to indicated the frameSize
            slider.setValues(startPosition, endPosition)
            updateSelectedTime(startPosition.toInt(), endPosition.toInt())

            // Calculate the scroll position based on the new start time
            val maxScroll = binding.audioScroller.getChildAt(0).width - binding.audioScroller.width
            val scrollPosition = (startPosition / (audioLength - frameSize)) * maxScroll
            binding.audioScroller.smoothScrollTo(scrollPosition.toInt(), 0)
        }
    }

    private fun updateSelectedTime(startPosition: Int, endPosition: Int) {
        binding.selectedRangeTime.text = formatTime(startPosition) + " - " + formatTime(endPosition)
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}