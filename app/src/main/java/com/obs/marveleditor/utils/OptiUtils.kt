/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import android.media.MediaMetadataRetriever
import android.util.Log
import com.jeevandeshmukh.glidetoastlib.GlideToast
import com.obs.videoeditor.editor.OptiConstant
import java.text.SimpleDateFormat
import java.util.*

object OptiUtils {

    val outputPath: String
        get() {
            val path = Environment.getExternalStorageDirectory().toString() + File.separator + OptiConstant.APP_NAME + File.separator

            val folder = File(path)
            if (!folder.exists())
                folder.mkdirs()

            return path
        }

    fun copyFileToInternalStorage(resourceId: Int, resourceName: String, context: Context): File {
        val path = Environment.getExternalStorageDirectory().toString() + File.separator + OptiConstant.APP_NAME + File.separator + OptiConstant.CLIP_ARTS + File.separator
        val folder = File(path)
            if (!folder.exists())
                folder.mkdirs()

        val dataPath = "$path$resourceName.png"
        Log.v("OptiUtils", "path: $dataPath")
        try {
            val inputStream = context.resources.openRawResource(resourceId)
            inputStream.toFile(dataPath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return File(dataPath)
    }

    fun copyFontToInternalStorage(resourceId: Int, resourceName: String, context: Context): File {
        val path = Environment.getExternalStorageDirectory().toString() + File.separator + OptiConstant.APP_NAME + File.separator + OptiConstant.FONT + File.separator
        val folder = File(path)
        if (!folder.exists())
            folder.mkdirs()

        val dataPath = "$path$resourceName.ttf"
        Log.v("OptiUtils", "path: $dataPath")
        try {
            val inputStream = context.resources.openRawResource(resourceId)
            inputStream.toFile(dataPath)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return File(dataPath)
    }

    private fun InputStream.toFile(path: String) {
        File(path).outputStream().use { this.copyTo(it) }
    }

    fun getConvertedFile(folder: String, fileName: String): File {
        val f = File(folder)

        if (!f.exists())
            f.mkdirs()

        return File(f.path + File.separator + fileName)
    }

    fun refreshGallery(path: String, context: Context) {

        val file = File(path)
        try {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(file)
            mediaScanIntent.data = contentUri
            context.sendBroadcast(mediaScanIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshGalleryAlone(context: Context) {
        try {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            context.sendBroadcast(mediaScanIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isVideoHaveAudioTrack(path: String): Boolean {
        var audioTrack = false

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
        audioTrack = hasAudioStr == "yes"

        return audioTrack
    }

    fun showGlideToast(activity: Activity, content: String) {
        GlideToast.makeToast(
            activity,
            content,
            GlideToast.LENGTHTOOLONG,
            GlideToast.FAILTOAST,
            GlideToast.TOP
        ).show()
    }

    fun createVideoFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat(OptiConstant.DATE_FORMAT, Locale.getDefault()).format(Date())
        val imageFileName: String = OptiConstant.APP_NAME + timeStamp + "_"
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        if (storageDir?.exists() == false) storageDir.mkdirs()
        return File.createTempFile(imageFileName, OptiConstant.VIDEO_FORMAT, storageDir)
    }

    fun createAudioFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat(OptiConstant.DATE_FORMAT, Locale.getDefault()).format(Date())
        val imageFileName: String = OptiConstant.APP_NAME + timeStamp + "_"
        val storageDir: File? = context.filesDir
        return File.createTempFile(imageFileName, OptiConstant.AUDIO_FORMAT, storageDir)
    }

    fun getVideoDuration(context: Context, file: File): Long{
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, Uri.fromFile(file))
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0"
        val timeInMillis = time.toLong()
        retriever.release()
        return timeInMillis
    }
}


