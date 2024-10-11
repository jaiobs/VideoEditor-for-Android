/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.videoeditor.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.obs.videoeditor.editor.OptiConstant
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object VideoEditorUtils {

    fun getVideoDuration(context: Context, file: File): Long{
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(file))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0"
        val timeInMillis = time.toLong()
        retriever.release()
        return timeInMillis
    }

    fun createAudioFile(context: Context): File {
        val imageFileName: String = getFileNameWithTime()
        val storageDir: File? = context.filesDir
        return File.createTempFile(imageFileName, OptiConstant.AUDIO_FORMAT, storageDir)
    }

    fun createVideoFile(context: Context): File {
        val imageFileName: String = getFileNameWithTime()
        val storageDir: File? = context.filesDir
        return File.createTempFile(imageFileName, OptiConstant.VIDEO_FORMAT, storageDir)
    }

    fun getFileNameWithTime(): String {
        val timeStamp: String = getFormattedTime()
        val imageFileName: String = OptiConstant.APP_NAME + timeStamp + "_"
        return imageFileName
    }

    fun getFormattedTime(): String {
        val timeStamp: String =
            SimpleDateFormat(OptiConstant.DATE_FORMAT, Locale.getDefault()).format(Date())
        return timeStamp
    }

    fun millisecondToTime(totalSeconds: Long): String {
        return String.format(
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(totalSeconds),
            TimeUnit.MILLISECONDS.toMinutes(totalSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(totalSeconds)),
            TimeUnit.MILLISECONDS.toSeconds(totalSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalSeconds))
        )
    }

}