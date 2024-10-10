package com.obs.videoeditor.audioRangeSlider

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.obs.videoeditor.databinding.FragmentAudioRangeSliderBinding
import com.obs.videoeditor.editor.OptiConstant
import com.obs.videoeditor.editor.OptiFFMpegCallback
import com.obs.videoeditor.editor.OptiVideoEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AudioRangeSliderFragment : BottomSheetDialogFragment(),
        AudioRangeSelector.AudioRangeSelectorListener
{


    companion object {
        const val VIDEO_FILE_PATH = "VIDEO_FILE_PATH"
        const val AUDIO_FILE_PATH = "AUDIO_FILE_PATH"

        fun newInstance(
            videoFilePath: String,
            audioFilePath: String,
        ) = AudioRangeSliderFragment().apply {
            arguments = bundleOf(
                VIDEO_FILE_PATH to videoFilePath,
                AUDIO_FILE_PATH to audioFilePath,
            )
        }
    }

    private var _binding: FragmentAudioRangeSliderBinding? = null
    private val binding: FragmentAudioRangeSliderBinding
        get() = requireNotNull(_binding)

    private var listener: AvMergerCallbackListener? = null

    private lateinit var audioFilePath: String
    private lateinit var videoFilePath: String
    private var exoPlayer: ExoPlayer? = null
    private var frameSizeSeconds: Float = 1f

    private lateinit var videoFile: File
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoFilePath = arguments?.getString(VIDEO_FILE_PATH) ?: ""
        audioFilePath = arguments?.getString(AUDIO_FILE_PATH) ?: ""

        if (File(audioFilePath).exists().not()) {
            Toast.makeText(requireContext(), "Audio File Path is blank", Toast.LENGTH_SHORT).show()
        } else if (File(videoFilePath).exists().not()) {
            Toast.makeText(requireContext(), "Video File Path is blank", Toast.LENGTH_SHORT).show()
        } else {
            audioFile = File(audioFilePath)
            videoFile = File(videoFilePath)
            audioLengthMillis = getVideoDuration(requireContext(), audioFile)
            val vidTimeInMillis = getVideoDuration(requireContext(), videoFile)
            frameSizeSeconds = (vidTimeInMillis / 1000).toFloat()
            setupExoPlayer()
            initViews()
        }
    }

    override fun onStart() {
        super.onStart()
        setFullScreenHeight()
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

    private fun initViews() {
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


        binding.btnDone.setOnClickListener {
            trimAudioFile()
        }
    }

    private fun trimAudioFile() {
        val outputFile = createAudioFile(requireContext())
        Log.v("TEST_audio", "outputFile: ${outputFile.absolutePath}")

        val (startTimeMillis, endTimeMillis) = binding.audioRangeSelector.getSelectedTimeRange()
        val startTime = millisecondToTime(startTimeMillis)
        val endTime = millisecondToTime(endTimeMillis)


        // TODO - show progress bar
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
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                    error.printStackTrace()
                }

            })
        optiVideoEditor.main()
    }

    private fun mergeAudioVideo() {
        //output file is generated and send to video processing
        val outputFile = createVideoFile(requireContext())
        Log.v("TEST_audio", "outputFile for video: ${outputFile.absolutePath}")

        val optiVideoEditor = OptiVideoEditor.with(requireContext())
            .setType(OptiConstant.VIDEO_AUDIO_MERGE)
            .setFile(videoFile)
            .setAudioFile(audioFile)
            .setOutputPath(outputFile.path)
            .setCallback(object : OptiFFMpegCallback {
                override fun onSuccess(convertedFile: File, type: String) {
                    listener?.onAudioVideoMerged(convertedFile)
                    dismissAllowingStateLoss()
                }

                override fun onFailure(error: Exception) {
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                    error.printStackTrace()
                }

            })
        optiVideoEditor.main()
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

    private fun createAudioFile(context: Context): File {
        val imageFileName: String = getFileNameWithTime()
        val storageDir: File? = context.filesDir
        return File.createTempFile(imageFileName, OptiConstant.AUDIO_FORMAT, storageDir)
    }

    private fun createVideoFile(context: Context): File {
        val imageFileName: String = getFileNameWithTime()
        val storageDir: File? = context.filesDir
        return File.createTempFile(imageFileName, OptiConstant.VIDEO_FORMAT, storageDir)
    }

    private fun getFileNameWithTime(): String {
        val timeStamp: String = getFormattedTime()
        val imageFileName: String = OptiConstant.APP_NAME + timeStamp + "_"
        return imageFileName
    }

    private fun getFormattedTime(): String {
        val timeStamp: String =
            SimpleDateFormat(OptiConstant.DATE_FORMAT, Locale.getDefault()).format(Date())
        return timeStamp
    }

    private fun millisecondToTime(totalSeconds: Long): String {
        return String.format(
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(totalSeconds),
            TimeUnit.MILLISECONDS.toMinutes(totalSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(totalSeconds)),
            TimeUnit.MILLISECONDS.toSeconds(totalSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalSeconds))
        )
    }

    interface AvMergerCallbackListener{
        fun onAudioVideoMerged(mergedVideoFile: File)
    }
}