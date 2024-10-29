/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.videoeditor.avmerger

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obs.videoeditor.R
import com.obs.videoeditor.avmerger.audioRangeSelector.AudioRangeSelector
import com.obs.videoeditor.databinding.FragmentAudioVideoMergerBinding
import com.obs.videoeditor.editor.OptiConstant
import com.obs.videoeditor.editor.OptiFFMpegCallback
import com.obs.videoeditor.editor.OptiVideoEditor
import com.obs.videoeditor.utils.CustomLoadingDialog
import com.obs.videoeditor.utils.VideoEditorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioVideoMergerFragment : Fragment(),
    AudioRangeSelector.AudioRangeSelectorListener {

    companion object {
        const val VIDEO_FILE_PATH = "VIDEO_FILE_PATH"
        const val AUDIO_FILE_PATH = "AUDIO_FILE_PATH"

        fun newInstance(
            videoFilePath: String,
            audioFilePath: String,
        ) = AudioVideoMergerFragment().apply {
            arguments = bundleOf(
                VIDEO_FILE_PATH to videoFilePath,
                AUDIO_FILE_PATH to audioFilePath,
            )
        }
    }

    private var _binding: FragmentAudioVideoMergerBinding? = null
    private val binding: FragmentAudioVideoMergerBinding
        get() = requireNotNull(_binding)

    private var listener: AvMergerCallbackListener? = null

    private var exoPlayerAudio: ExoPlayer? = null
    private var exoPlayerVideo: ExoPlayer? = null

    private lateinit var audioFilePath: String
    private lateinit var videoFilePath: String
    private var frameSizeMillis: Long = 0

    private lateinit var videoFile: File
    private lateinit var audioFile: File
    private var audioLengthMillis: Long = 0L
    private var currStartPositionMillis: Long = 0L

    private var progressUpdateJob: Job? = null
    private var loadingDialog: CustomLoadingDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentAudioVideoMergerBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoFilePath = arguments?.getString(VIDEO_FILE_PATH) ?: ""
        audioFilePath = arguments?.getString(AUDIO_FILE_PATH) ?: ""

        if (File(audioFilePath).exists().not()) {
            showErrorLayout(getString(R.string.audio_file_not_found))
        } else if (File(videoFilePath).exists().not()) {
            showErrorLayout(getString(R.string.video_file_not_found))
        } else {
            audioFile = File(audioFilePath)
            videoFile = File(videoFilePath)
            audioLengthMillis = VideoEditorUtils.getVideoDuration(requireContext(), audioFile)
            frameSizeMillis = VideoEditorUtils.getVideoDuration(requireContext(), videoFile)

            if (audioLengthMillis < frameSizeMillis) {
                showErrorLayout(getString(
                    R.string.audio_too_short,
                    audioLengthMillis.toString(),
                    frameSizeMillis.toString()
                ))
            } else {
                setupAudioExoPlayer()
                setupVideoExoPlayer()
                initViews()
            }
        }
    }

    private fun setupAudioExoPlayer() {
        exoPlayerAudio = ExoPlayer.Builder(requireContext()).build()

        val mediaItem = MediaItem.fromUri(Uri.parse(audioFilePath))
        exoPlayerAudio?.setMediaItem(mediaItem)
        exoPlayerAudio?.prepare()

        exoPlayerAudio?.addListener(object : Player.Listener {

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    startUpdatingAudioWave()
                } else {
                    stopUpdatingAudioWave()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                showErrorLayout(errorMsg = getString(R.string.failed_to_play_audio) + "\n" + error.message)
                Log.e("TEST_exo", "onPlayerError: error = $error")
            }

            override fun onPlayerErrorChanged(error: PlaybackException?) {
                super.onPlayerErrorChanged(error)
                showErrorLayout(getString(R.string.failed_to_play_audio) + "\n" + error?.message)
                Log.e("TEST_exo", "onPlayerErrorChanged: error = $error")
            }
        })
    }

    private fun setupVideoExoPlayer() {
        exoPlayerVideo = ExoPlayer.Builder(requireContext())
            .setRenderersFactory(
                DefaultRenderersFactory(requireContext())
                    .setEnableDecoderFallback(true)
            )
            .build()

        val mediaItem = MediaItem.fromUri(Uri.parse(videoFilePath))
        exoPlayerVideo?.setMediaItem(mediaItem)
        exoPlayerVideo?.prepare()
        exoPlayerVideo?.playWhenReady = true
        exoPlayerVideo?.volume = 0f

        binding.videoPlayerView.player = exoPlayerVideo

        exoPlayerVideo?.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                showErrorLayout(getString(R.string.failed_to_play_video) + "\n" + error.message)
                Log.e("TEST_exo", "videoExo - onPlayerError: error = $error")
            }

            override fun onPlayerErrorChanged(error: PlaybackException?) {
                super.onPlayerErrorChanged(error)
                showErrorLayout(getString(R.string.failed_to_play_video) + "\n" + error?.message)
                Log.e("TEST_exo", "videoExo - onPlayerErrorChanged: error = $error")
            }
        })
    }

    private fun initViews() {
        binding.audioRangeSelector.initializeViews(
            audioFile = audioFile,
            audioLengthMillis = audioLengthMillis,
            frameSizeMillis = frameSizeMillis,
            listener = this
        )

        binding.btnDone.setOnClickListener {
            trimAudioFile()
        }
    }

    private fun showErrorLayout(errorMsg: String) {
        hideLoadingDialog()
        releasePlayers()
        binding.clContainer.isVisible = false
        binding.errorLayout.isVisible = true
        binding.tvError.text = errorMsg
        binding.btnClose.setOnClickListener {
            listener?.close()
        }
    }

    private fun trimAudioFile() {
        val outputFile = VideoEditorUtils.createAudioFile(requireContext())
        Log.v("TEST_audio", "outputFile: ${outputFile.absolutePath}")

        val (startTimeMillis, endTimeMillis) = binding.audioRangeSelector.getSelectedTimeRange()
        val startTime = VideoEditorUtils.millisecondToTime(startTimeMillis)
        val endTime = VideoEditorUtils.millisecondToTime(endTimeMillis)

        showLoadingDialog()

        val optiVideoEditor = OptiVideoEditor.with(requireContext())
            .setType(OptiConstant.AUDIO_TRIM)
            .setAudioFile(audioFile)
            .setOutputPath(outputFile.absolutePath)
            .setStartTime(startTime)
            .setEndTime(endTime)
            .setCallback(object : OptiFFMpegCallback {
                override fun onSuccess(convertedFile: File, type: String) {
                    audioFile = convertedFile
                    mergeAudioVideo()
                }

                override fun onFailure(error: Exception) {
                    showErrorLayout(errorMsg = getString(R.string.failed_to_trim_audio) + "\n" + error.message)
                    error.printStackTrace()
                }

            })
        optiVideoEditor.main()
    }

    private fun mergeAudioVideo() {
        //output file is generated and send to video processing
        val outputFile = VideoEditorUtils.createVideoFile(requireContext())
        Log.v("TEST_audio", "outputFile for video: ${outputFile.absolutePath}")

        val optiVideoEditor = OptiVideoEditor.with(requireContext())
            .setType(OptiConstant.VIDEO_AUDIO_MERGE)
            .setFile(videoFile)
            .setAudioFile(audioFile)
            .setOutputPath(outputFile.path)
            .setCallback(object : OptiFFMpegCallback {
                override fun onSuccess(convertedFile: File, type: String) {
                    hideLoadingDialog()
                    listener?.onAudioVideoMerged(convertedFile)
                    listener?.close()
                }

                override fun onFailure(error: Exception) {
                    hideLoadingDialog()
                    showErrorLayout(errorMsg = getString(R.string.failed_to_trim_audio) + "\n" + error.message)
                    error.printStackTrace()
                }

            })
        optiVideoEditor.main()
    }


    private fun startUpdatingAudioWave() {
        progressUpdateJob?.cancel()
        progressUpdateJob = lifecycleScope.launch(Dispatchers.Main) {
            while (isActive) {
                val currentPosition = exoPlayerAudio?.currentPosition ?: return@launch
                if (currentPosition >= currStartPositionMillis + frameSizeMillis) {
                    pausePlayers()
                    return@launch
                }
                updateAudioWaveProgress(currentPosition)
                delay(150)
            }
        }
    }

    private fun updateAudioWaveProgress(playerPosMillis: Long) {
        binding.audioRangeSelector.updateAudioWaveProgress(playerPosMillis)
    }


    private fun stopUpdatingAudioWave() {
        progressUpdateJob?.cancel()
    }

    override fun playAudioAt(startPosMillis: Long) {
        exoPlayerAudio?.seekTo(startPosMillis)
        exoPlayerAudio?.play()

        exoPlayerVideo?.seekTo(0)
        exoPlayerVideo?.play()

        updateAudioWaveProgress(startPosMillis)
    }

    private fun pausePlayers() {
        exoPlayerAudio?.pause()
    }

    private fun releasePlayers() {
        exoPlayerAudio?.release()
        exoPlayerVideo?.release()
    }


    override fun updateStartPosition(startPosMillis: Long) {
        currStartPositionMillis = startPosMillis
    }

    override fun onPause() {
        super.onPause()
        pausePlayers()
        stopUpdatingAudioWave()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        hideLoadingDialog()
    }

    override fun onDestroy() {
        releasePlayers()
        super.onDestroy()
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val parentFragment = parentFragment
        val activity = activity
        if (parentFragment is AvMergerCallbackListener) {
            listener = parentFragment
        } else if (activity is AvMergerCallbackListener) {
            listener = activity
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }


    private fun showLoadingDialog() = activity?.runOnUiThread {
        if (loadingDialog == null) loadingDialog = CustomLoadingDialog()
        activity?.let { mActivity -> loadingDialog?.show(mActivity) }
    }

    private fun hideLoadingDialog() = activity?.runOnUiThread {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    interface AvMergerCallbackListener {
        fun onAudioVideoMerged(mergedVideoFile: File)
        fun close()
    }
}