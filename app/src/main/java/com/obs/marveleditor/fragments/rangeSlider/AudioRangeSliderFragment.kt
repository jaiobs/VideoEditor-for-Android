/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.fragments.rangeSlider

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import com.obs.marveleditor.utils.OptiConstant
import com.obs.marveleditor.R
import com.obs.marveleditor.interfaces.OptiFFMpegCallback
import com.obs.marveleditor.utils.VideoUtils.buildMediaSource
import com.obs.marveleditor.utils.VideoUtils.secToTime
import com.obs.marveleditor.utils.VideoFrom
import com.obs.marveleditor.interfaces.OptiDialogueHelper
import com.github.guilhe.views.SeekBarRangedView
import com.github.guilhe.views.addActionListener
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Util
import com.obs.marveleditor.OptiVideoEditor
import com.obs.marveleditor.databinding.FragmentAudioRangeSliderBinding
import com.obs.marveleditor.fragments.OptiBaseCreatorDialogFragment
import com.obs.marveleditor.utils.OptiCommonMethods
import com.obs.marveleditor.utils.OptiUtils
import com.obs.marveleditor.utils.saveMediaToFile
import java.io.File
import kotlin.math.roundToLong

class AudioRangeSliderFragment : DialogFragment() {


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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentAudioRangeSliderBinding.inflate(layoutInflater)
        audioFilePath = arguments?.getString(AUDIO_FILE_PATH) ?: ""
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initAudioView()
    }

    private fun initAudioView() {
        if (audioFilePath.isBlank()) {
            Toast.makeText(requireContext(), "Audio File Path is blank", Toast.LENGTH_SHORT).show()
            return
        }

        val audioFile = File(audioFilePath)
        binding.seekBarAudioProgress.setSampleFrom(audioFile)


        binding.audioSelectView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = screenWidth / 3
        }

//        rangeSlider.values = listOf(10f, 30f)

        val timeInMillis = OptiUtils.getVideoDuration(requireContext(), audioFile)
        val audioLength = (timeInMillis / 1000).toFloat() // Length of the audio in seconds
        val frameSize = 30f // Fixed 30-second frame

        // Setup RangeSlider
        binding.rangeSlider.valueFrom = 0f
        binding.rangeSlider.valueTo = audioLength
        binding.rangeSlider.setValues(0f, frameSize)



        // Update RangeSlider when user scrolls through the audio selector
        binding.audioScroller.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            // Get maximum scrollable width (total width - visible width)
            val maxScroll = binding.audioScroller.getChildAt(0).width - binding.audioScroller.width

            // Calculate scroll ratio (how far we have scrolled compared to total scrollable width)
            val scrollRatio = scrollX.toFloat() / maxScroll.toFloat()

            // Calculate new start position in the audio based on scroll ratio
            val newStartPosition = scrollRatio * (audioLength - frameSize)

            // Set new values for the range slider
            binding.rangeSlider.setValues(newStartPosition, newStartPosition + frameSize)

            // Update the displayed selected range time
            binding.selectedRangeTime.text = formatTime(newStartPosition.toInt()) + " - " +
                    formatTime((newStartPosition + frameSize).toInt())
        }

        // Update the scrolling position when the user changes the RangeSlider
        binding.rangeSlider.addOnChangeListener { slider, _, _ ->
            val startPosition = slider.values[0]
            var endPosition = startPosition + frameSize

            if (endPosition > audioLength) {
                endPosition = audioLength
            }

            // Set the second thumb to always be 30 seconds ahead of the first thumb
            slider.setValues(startPosition, endPosition)

            // Calculate the scroll position based on the new start time
            val maxScroll = binding.audioScroller.getChildAt(0).width - binding.audioScroller.width
            val scrollPosition = (startPosition / (audioLength - frameSize)) * maxScroll

            // Smoothly scroll to the calculated position
            binding.audioScroller.smoothScrollTo(scrollPosition.toInt(), 0)

            // Update the time labels
            binding.selectedRangeTime.text = formatTime(startPosition.toInt()) + " - " + formatTime(endPosition.toInt())
        }
    }

    // Helper function to format time into MM:SS
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