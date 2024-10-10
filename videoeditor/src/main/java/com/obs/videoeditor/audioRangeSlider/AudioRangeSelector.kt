/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.videoeditor.audioRangeSlider

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.obs.videoeditor.R
import com.obs.videoeditor.databinding.AudioRangeSelectorBinding
import java.io.File

class AudioRangeSelector @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var _binding: AudioRangeSelectorBinding? = null
    private val binding: AudioRangeSelectorBinding
        get() = requireNotNull(_binding)

    private val screenWidth: Int = Resources.getSystem().displayMetrics.widthPixels
    private var audioLengthMillis: Long = 0L
    private var listener: AudioRangeSelectorListener? = null
    private var currStartPositionMillis: Long = 0L
    private var frameSizeSecondsMillis: Long = 0L


    init {
        _binding = AudioRangeSelectorBinding.inflate(
            LayoutInflater.from(context), this, true
        )

        binding.rangeSlider.apply {
            trackActiveTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.white)
            )
            trackInactiveTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.slider_track_grey)
            )

        }
//        initializeViews()
    }

    fun initializeViews(
        audioFile: File,
        audioLengthMillis: Long,
        frameSizeSeconds: Float,
        listener: AudioRangeSelectorListener,
    ) {
        this.listener = listener
        this.audioLengthMillis = audioLengthMillis
        this.currStartPositionMillis = 0L
        this.frameSizeSecondsMillis = frameSizeSeconds.toLong() * 1000

        binding.rangeSlider.thumbRadius = 0

        binding.seekBarAudioProgress.setSampleFrom(audioFile)
        binding.seekBarAudioProgress.isEnabled = false

        val audioSelectViewWidth = screenWidth / 3
        binding.audioSelectView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = audioSelectViewWidth
        }

        val audioLengthSeconds = (audioLengthMillis / 1000).toFloat()

        binding.audioSelectView.post {
            // Calculate width for the seekBarAudioProgress based on audio length
            binding.seekBarAudioProgress.updateLayoutParams<FrameLayout.LayoutParams> {
                val seekBarWidth = (audioSelectViewWidth / frameSizeSeconds) * audioLengthSeconds
                width = seekBarWidth.toInt()
                val remainingHorizontalArea = binding.clAudioSelector.width - audioSelectViewWidth
                marginStart = remainingHorizontalArea / 2
                marginEnd = remainingHorizontalArea / 2
            }
        }


        binding.rangeSlider.valueFrom = 0f
        binding.rangeSlider.valueTo = audioLengthSeconds
        binding.rangeSlider.setValues(0f, frameSizeSeconds)
        updateSelectedTime(0, frameSizeSeconds.toInt())
        listener?.playAudioAt(0f)


        // Update RangeSlider when user scrolls through the audio selector
        binding.audioScroller.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            // Get maximum scrollable width (total width - visible width)
            val maxScroll = binding.audioScroller.getChildAt(0).width - binding.audioScroller.width
            // Calculate scroll ratio (how far we have scrolled compared to total scrollable width)
            val scrollRatio = scrollX.toFloat() / maxScroll.toFloat()
            // Calculate new start position in the audio based on scroll ratio
            val newStartPosition = scrollRatio * (audioLengthSeconds - frameSizeSeconds)
            val newEndPosition = minOf(newStartPosition + frameSizeSeconds, audioLengthSeconds)

            binding.rangeSlider.setValues(newStartPosition, newEndPosition)
            updateSelectedTime(newStartPosition.toInt(), newEndPosition.toInt())

            listener?.playAudioAt(newStartPosition)
        }

        // Update the scrolling position when the user changes the RangeSlider
        binding.rangeSlider.addOnChangeListener { slider, _, _ ->
            val startPosition = slider.values[0]
            val endPosition = minOf(startPosition + frameSizeSeconds, audioLengthSeconds)

            // Set the thumbs to indicated the frameSize
            slider.setValues(startPosition, endPosition)
            updateSelectedTime(startPosition.toInt(), endPosition.toInt())

            // Calculate the scroll position based on the new start time
            val maxScroll = binding.audioScroller.getChildAt(0).width - binding.audioScroller.width
            val scrollPosition = (startPosition / (audioLengthSeconds - frameSizeSeconds)) * maxScroll
            binding.audioScroller.smoothScrollTo(scrollPosition.toInt(), 0)
            listener?.playAudioAt(startPosition)
        }
    }

    fun updateAudioWaveProgress(playerPosMillis: Long) {
        val progress = (playerPosMillis.toFloat() / audioLengthMillis) * 100f
        if (progress < 0 || progress > 100f)    return
        binding.seekBarAudioProgress.progress = progress
    }

    fun getSelectedTimeRange(): Pair<Long, Long> {
        return Pair(currStartPositionMillis, currStartPositionMillis + frameSizeSecondsMillis)
    }

    private fun updateSelectedTime(startPosition: Int, endPosition: Int) {
        currStartPositionMillis = startPosition.toLong() * 1000
        listener?.updateStartPosition(currStartPositionMillis)
        binding.selectedRangeTime.text = formatTime(startPosition) + " - " + formatTime(endPosition)
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }


    interface AudioRangeSelectorListener {
        fun playAudioAt(startPosSeconds: Float)
        fun updateStartPosition(startPosMillis: Long)
    }
}