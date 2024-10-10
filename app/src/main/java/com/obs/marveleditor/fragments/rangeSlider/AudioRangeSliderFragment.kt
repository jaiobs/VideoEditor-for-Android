/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.fragments.rangeSlider

import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obs.marveleditor.R
import com.obs.marveleditor.databinding.FragmentAudioRangeSliderBinding
import com.obs.marveleditor.utils.OptiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    private var exoPlayer: ExoPlayer? = null
    private var frameSizeSeconds: Float = 1f

    private lateinit var audioFile: File
    private var audioLengthMillis: Long = 0L
    private var currStartPositionMillis: Long = 0L

    private var progressUpdateJob: Job? = null

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
        frameSizeSeconds = 10f
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initValues()
        setupExoPlayer()
        initAudioView()
    }

    override fun onStart() {
        super.onStart()
        setFullScreenHeight()
    }

    private fun initValues() {
        audioFile = File(audioFilePath)
        audioLengthMillis = OptiUtils.getVideoDuration(requireContext(), audioFile)
    }

    private fun setupExoPlayer() {
//        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        exoPlayer = ExoPlayerFactory.newSimpleInstance(
            requireContext(), DefaultRenderersFactory(requireContext()),
            DefaultTrackSelector(), DefaultLoadControl()
        )

        val mediaItem = MediaItem.fromUri(Uri.parse(audioFilePath))
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()

        exoPlayer?.addListener(object : Player.EventListener {

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    startUpdatingAudioWave()
                } else {
                    stopUpdatingAudioWave()
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                Log.e("TEST_exo", "onPlayerError: error = $error", )
            }
        })
    }

    private fun initAudioView() {
        if (audioFilePath.isBlank()) {
            Toast.makeText(requireContext(), "Audio File Path is blank", Toast.LENGTH_SHORT).show()
            return
        }

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
        playAudioAt(0f)


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

            playAudioAt(newStartPosition)
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
            playAudioAt(startPosition)
        }
    }

    private fun startUpdatingAudioWave() {
        progressUpdateJob?.cancel()
        progressUpdateJob = lifecycleScope.launch(Dispatchers.Main) {
            while (isActive) {
                val currentPosition = exoPlayer?.currentPosition ?: return@launch
                if (currentPosition >= currStartPositionMillis + (frameSizeSeconds * 1000)) {
                    pauseAudio()
                    return@launch
                }
                val progress = (currentPosition.toFloat() / audioLengthMillis) * 100f
                binding.seekBarAudioProgress.progress = progress
                delay(150)
            }
        }
    }

    private fun stopUpdatingAudioWave() {
        progressUpdateJob?.cancel()
    }

    private fun playAudioAt(startPosSeconds: Float) {
        val startPosMillis = (startPosSeconds * 1000).toLong()
        exoPlayer?.seekTo(startPosMillis)
        exoPlayer?.play()
    }

    private fun pauseAudio() {
        exoPlayer?.pause()
    }

    private fun updateSelectedTime(startPosition: Int, endPosition: Int) {
        currStartPositionMillis = startPosition.toLong() * 1000
        binding.selectedRangeTime.text = formatTime(startPosition) + " - " + formatTime(endPosition)
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    override fun onPause() {
        super.onPause()
        pauseAudio()
        stopUpdatingAudioWave()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
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
}