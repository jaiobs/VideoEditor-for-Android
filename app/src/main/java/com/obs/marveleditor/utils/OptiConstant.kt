/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.utils

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class OptiConstant {
    companion object {

        const val APP_NAME = "MarvelEditor"

        const val VIDEO_FLIRT = 1
        const val VIDEO_TRIM = 2
        const val AUDIO_TRIM = 3
        const val VIDEO_AUDIO_MERGE = 4
        const val VIDEO_PLAYBACK_SPEED = 5
        const val VIDEO_TEXT_OVERLAY = 6
        const val VIDEO_CLIP_ART_OVERLAY = 7
        const val MERGE_VIDEO = 8
        const val VIDEO_TRANSITION = 9
        const val CONVERT_AVI_TO_MP4 = 10

        const val FLIRT = "filter"
        const val TRIM = "trim"
        const val MUSIC = "music"
        const val PLAYBACK = "playback"
        const val TEXT = "text"
        const val OBJECT = "object"
        const val MERGE = "merge"
        const val TRANSITION = "transition"

        const val SPEED_0_25 = "0.25x"
        const val SPEED_0_5 = "0.5x"
        const val SPEED_0_75 = "0.75x"
        const val SPEED_1_0 = "1.0x"
        const val SPEED_1_25 = "1.25x"
        const val SPEED_1_5 = "1.5x"

        const val VIDEO_GALLERY = 101
        const val RECORD_VIDEO = 102
        const val AUDIO_GALLERY = 103
        const val VIDEO_MERGE_1 = 104
        const val VIDEO_MERGE_2 = 105
        const val ADD_ITEMS_IN_STORAGE = 106
        const val MAIN_VIDEO_TRIM = 107

        val PERMISSION_CAMERA = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE)
        val PERMISSION_STORAGE = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE)

        const val BOTTOM_LEFT = "BottomLeft"
        const val BOTTOM_RIGHT = "BottomRight"
        const val CENTRE = "Center"
        const val CENTRE_ALIGN = "CenterAlign"
        const val CENTRE_BOTTOM = "CenterBottom"
        const val TOP_LEFT = "TopLeft"
        const val TOP_RIGHT = "TopRight"

        const val CLIP_ARTS = ".ClipArts"
        const val FONT = ".Font"
        const val DEFAULT_FONT = "roboto_black.ttf"
        const val MY_VIDEOS = "MyVideos"

        const val DATE_FORMAT = "yyyyMMdd_HHmmss"
        const val VIDEO_FORMAT = ".mp4"
        const val AUDIO_FORMAT = ".mp3"
        const val AVI_FORMAT = ".avi"

        const val VIDEO_LIMIT = 4 //4 minutes

        fun hasCameraAndStoragePermission(context: Context): Boolean {
            return hasCameraPermission(context) && hasStoragePermission(context)
        }

        fun hasCameraPermission(context: Context): Boolean {
            return checkPermission(context, Manifest.permission.CAMERA)
        }

        fun hasStoragePermission(context: Context): Boolean {
            return if (isAndroidTiramisuAndAbove()) {
                checkPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
                        && checkPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
                        && checkPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
            } else if (isAndroidQAndAbove()) {
                checkPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                checkPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                        && checkPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        fun checkPermission(context: Context, permString: String): Boolean {
            return ContextCompat.checkSelfPermission(
                context, permString
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

fun isAndroidTiramisuAndAbove(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}

fun isAndroidQAndAbove(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}

fun Context.saveMediaToFile(uri: Uri, presetFileName: String? = null): String {
    val subDirectory: String
    val fileName: String

    var mimeType: String? = ""
    var fileExtensions: String? = ""
    if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT)) {
        mimeType = contentResolver.getType(uri)
        fileExtensions = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    } else {
        fileExtensions = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtensions)
    }

    when(mimeType) {
        "image" -> {
            subDirectory = "images"
            fileName = "image_" + (presetFileName ?: System.currentTimeMillis()) + "." + fileExtensions
        }
        "video" -> {
            subDirectory = "videos"
            fileName = "video_" + (presetFileName ?: System.currentTimeMillis()) + "." + fileExtensions
        }
        "audio" -> {
            subDirectory = "documents"
            fileName = "document_" + (presetFileName ?: System.currentTimeMillis()) + "." + fileExtensions
        }
        else -> {
            subDirectory = "files"
            fileName = "file_" + (presetFileName ?: System.currentTimeMillis()) + "." + fileExtensions
        }
    }

    val directory = File(filesDir.path + File.separator + subDirectory)

    val fileToSave = File(directory, fileName)

    if (!directory.exists()) {
        directory.mkdirs()
    }

    if (!fileToSave.exists()) {
        fileToSave.createNewFile()
    } else {
        fileToSave.delete()
        fileToSave.createNewFile()
    }

    var bis: BufferedInputStream? = null
    var bos: BufferedOutputStream? = null
    try {
        bis = BufferedInputStream(
            contentResolver
                .openInputStream(uri)
        )
        bos = BufferedOutputStream(
            FileOutputStream(
                fileToSave.path,
                false
            )
        )
        val buffer = ByteArray(1024)
        bis.read(buffer)
        do {
            bos.write(buffer)
        } while (bis.read(buffer) != -1)
    } catch (ioe: IOException) {
        ioe.printStackTrace()
    } catch (e: SecurityException) {
        e.printStackTrace()
    } catch (e: RuntimeException){
        e.printStackTrace()
    }
    finally {
        try {
            bis?.close()
            bos?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    return fileToSave.path
}

fun Context.saveMediaToFile(path: String): String {
    val uri = File(path).toUri()

    val subDirectory: String
    val fileName: String

    val fileExtensions = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtensions)

    when {
        mimeType.toString().contains("image") -> {
            subDirectory = "images"
            fileName = "image_" + System.currentTimeMillis() + "." + fileExtensions
        }
        mimeType.toString().contains("video") -> {
            subDirectory = "videos"
            fileName = "video_" + System.currentTimeMillis() + "." + fileExtensions
        }
        mimeType.toString().contains("audio") -> {
            subDirectory = "documents"
            fileName = "document_" + System.currentTimeMillis() + "." + fileExtensions
        }
        else -> {
            subDirectory = "files"
            fileName = "file_" + System.currentTimeMillis() + "." + fileExtensions
        }
    }

    val directory = File(filesDir.path + File.separator + subDirectory)

    val fileToSave = File(directory, fileName)

    if (!directory.exists()) {
        directory.mkdirs()
    }

    if (!fileToSave.exists()) {
        fileToSave.createNewFile()
    }

    var bis: BufferedInputStream? = null
    var bos: BufferedOutputStream? = null
    try {
        bis = BufferedInputStream(
            contentResolver
                .openInputStream(uri)
        )
        bos = BufferedOutputStream(
            FileOutputStream(
                fileToSave.path,
                false
            )
        )
        val buffer = ByteArray(1024)
        bis.read(buffer)
        do {
            bos.write(buffer)
        } while (bis.read(buffer) != -1)
    } catch (ioe: IOException) {
        ioe.printStackTrace()
    } finally {
        try {
            bis?.close()
            bos?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    return fileToSave.path
}
