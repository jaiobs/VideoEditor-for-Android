package com.obs.videoeditor.audioRangeSlider

import android.content.Context
import android.content.res.Resources
import android.media.MediaMetadataRetriever
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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obs.videoeditor.databinding.FragmentAudioRangeSliderBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioRangeSliderFragment : BottomSheetDialogFragment(),
        AudioRangeSelector.AudioRangeSelectorListener
{


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


    private lateinit var audioFilePath: String
    private var exoPlayer: ExoPlayer? = null
    private var frameSizeSeconds: Float = 1f

    private var audioFile: File? = null
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
        val file = File(audioFilePath)
        if (file.exists()) {
            audioFile = file
            audioLengthMillis = getVideoDuration(requireContext(), audioFile!!)
        }
    }

    private fun setupExoPlayer() {
        exoPlayer = ExoPlayer.Builder(requireContext()).build()

        val mediaItem = MediaItem.fromUri(Uri.parse(audioFilePath))
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()

        exoPlayer?.addListener(object : Player.Listener {

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
                Log.e("TEST_exo", "onPlayerError: error = $error")
            }

            override fun onPlayerErrorChanged(error: PlaybackException?) {
                super.onPlayerErrorChanged(error)
                Log.e("TEST_exo", "onPlayerErrorChanged: error = $error")
            }
        })
    }

    private fun initAudioView() {
        if (audioFile?.exists() == false) {
            Toast.makeText(requireContext(), "Audio File Path is blank", Toast.LENGTH_SHORT).show()
            return
        }

        binding.audioRangeSelector.initializeViews(
            audioFile = audioFile!!,
            audioLengthMillis = audioLengthMillis,
            frameSizeSeconds = frameSizeSeconds,
            listener = this
        )
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

    override fun playAudioAt(startPosSeconds: Float) {
        val startPosMillis = (startPosSeconds * 1000).toLong()
        exoPlayer?.seekTo(startPosMillis)
        exoPlayer?.play()
        updateAudioWaveProgress(startPosMillis)
    }

    private fun pauseAudio() {
        exoPlayer?.pause()
    }


    override fun updateStartPosition(startPosMillis: Long) {
        currStartPositionMillis = startPosMillis
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

    private fun getVideoDuration(context: Context, file: File): Long{
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(file))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0"
        val timeInMillis = time.toLong()
        retriever.release()
        return timeInMillis
    }
}