/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.videoeditor.avmerger.audioRangeSelector

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.slider.LabelFormatter
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
    private var frameSizeMillis: Long = 0L


    init {
        _binding = AudioRangeSelectorBinding.inflate(
            LayoutInflater.from(context), this, true
        )

        with(binding) {
            rangeSlider.trackActiveTintList = ColorStateList.valueOf(Color.WHITE)
            rangeSlider.trackInactiveTintList = ColorStateList.valueOf(Color.GRAY)
            rangeSlider.thumbTintList = ColorStateList.valueOf(Color.WHITE)
            rangeSlider.isTickVisible = false
            rangeSlider.labelBehavior = LabelFormatter.LABEL_GONE
            rangeSlider.trackHeight = toPx(4).toInt()
            rangeSlider.thumbRadius = toPx(2).toInt()  // Try a small value
            // Remove any thumb shadow or elevation
            rangeSlider.thumbElevation = 0f  // Remove elevation to eliminate shadow
        }
//        initializeViews()
    }

    fun initializeViews(
        audioFile: File,
        audioLengthMillis: Long,
        frameSizeMillis: Long,
        listener: AudioRangeSelectorListener,
    ) {
        this.listener = listener
        this.audioLengthMillis = audioLengthMillis
        this.currStartPositionMillis = 0L
        this.frameSizeMillis = frameSizeMillis


        binding.seekBarAudioProgress.setSampleFrom(audioFile)
        binding.seekBarAudioProgress.isEnabled = false

        val audioSelectViewWidth = screenWidth / 3
        binding.audioSelectView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = audioSelectViewWidth
        }

        binding.audioSelectView.post {
            // Calculate width for the seekBarAudioProgress based on audio length
            binding.seekBarAudioProgress.updateLayoutParams<FrameLayout.LayoutParams> {
                val seekBarWidth = (audioSelectViewWidth.toDouble() / frameSizeMillis.toDouble()) * audioLengthMillis.toDouble()
                width = seekBarWidth.toInt()
                val remainingHorizontalArea = binding.clAudioSelector.width - audioSelectViewWidth
                val marginEachSide = (remainingHorizontalArea / 2) + toPx(4).toInt() // 4dp due to border width of audioSelectView
                marginStart = marginEachSide
                marginEnd = marginEachSide
            }
        }


        binding.rangeSlider.valueFrom = 0f
        binding.rangeSlider.valueTo = audioLengthMillis.toFloat()
        val defaultPositions = defaultPositions()
        binding.rangeSlider.setValues(defaultPositions.first, defaultPositions.second)
        updateSelectedTime(0L, frameSizeMillis)
        listener.playAudioAt(0L)


        // Update RangeSlider when user scrolls through the audio selector
        binding.audioScroller.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            // Get maximum scrollable width (total width - visible width)
            val maxScroll = binding.audioScroller.getChildAt(0).width - binding.audioScroller.width
            // Calculate scroll ratio (how far we have scrolled compared to total scrollable width)
            val scrollRatio = scrollX.toFloat() / maxScroll.toFloat()
            // Calculate new start position in the audio based on scroll ratio
            val start = scrollRatio * (audioLengthMillis - frameSizeMillis)
            val end = start + frameSizeMillis
            val (adjustedStart, adjustedEnd) = getAdjustedPositions(start, end)

            binding.rangeSlider.setValues(adjustedStart, adjustedEnd)
            updateSelectedTime(adjustedStart.toLong(), adjustedEnd.toLong())
            listener.playAudioAt(adjustedStart.toLong())
        }

        // Update the scrolling position when the user changes the RangeSlider
        binding.rangeSlider.addOnChangeListener { slider, _, fromUser ->
            if (fromUser.not())    return@addOnChangeListener

            var left = slider.values[0]
            var right = slider.values[1]

            val isLeftChanged = left.toLong() != currStartPositionMillis
            Log.e("TEST_aud", "initializeViews: isLeftChanged = $isLeftChanged, left = ${left.toLong()}, curr = ${currStartPositionMillis}, slider.values = ${slider.values}", )

            val start: Float
            val end: Float
            if (isLeftChanged) {
                start = left
                end = start + frameSizeMillis.toFloat()
            } else {
                end = right
                start = end - frameSizeMillis.toFloat()
            }

            val (adjustedStart, adjustedEnd) = getAdjustedPositions(start, end)

            // Set the thumbs to indicated the frameSize
            slider.setValues(adjustedStart, adjustedEnd)
            updateSelectedTime(adjustedStart.toLong(), adjustedEnd.toLong())

            // Calculate the scroll position based on the new start time
            val maxScroll = binding.audioScroller.getChildAt(0).width - binding.audioScroller.width
            val scrollPosition = (adjustedStart / (audioLengthMillis - frameSizeMillis)) * maxScroll
            binding.audioScroller.smoothScrollTo(scrollPosition.toInt(), 0)
            listener.playAudioAt(adjustedStart.toLong())
        }
    }

    fun updateAudioWaveProgress(playerPosMillis: Long) {
        val progress = (playerPosMillis.toFloat() / audioLengthMillis) * 100f
        if (progress < 0 || progress > 100f)    return
        binding.seekBarAudioProgress.progress = progress
    }

    fun getSelectedTimeRange(): Pair<Long, Long> {
        return Pair(currStartPositionMillis, currStartPositionMillis + frameSizeMillis)
    }

    private fun getAdjustedPositions(
        start: Float?,
        end: Float?
    ): Pair<Float, Float> {
        val defaultPositions = defaultPositions()

        var newStartPosition = start ?: defaultPositions.first
        var newEndPosition = end ?: defaultPositions.second

        if (newStartPosition < 0) {
            newStartPosition = 0f
            newEndPosition = frameSizeMillis.toFloat()
        } else if (newEndPosition > audioLengthMillis) {
            newEndPosition = audioLengthMillis.toFloat()
            newStartPosition = newEndPosition - frameSizeMillis.toFloat()
        }
        return Pair(newStartPosition, newEndPosition)
    }

    private fun defaultPositions() = Pair(0f, frameSizeMillis.toFloat())


    private fun updateSelectedTime(startPosition: Long, endPosition: Long) {
        currStartPositionMillis = startPosition
        listener?.updateStartPosition(currStartPositionMillis)
        binding.selectedRangeTime.text = formatTime(startPosition) + " - " + formatTime(endPosition)
    }

    private fun formatTime(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    fun toPx(dp: Int): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics)

    interface AudioRangeSelectorListener {
        fun playAudioAt(startPosMillis: Long)
        fun updateStartPosition(startPosMillis: Long)
    }
}