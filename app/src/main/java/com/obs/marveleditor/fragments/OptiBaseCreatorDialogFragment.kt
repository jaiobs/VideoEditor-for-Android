/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.fragments

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.github.hiteshsondhi88.libffmpeg.FFmpeg
import java.io.File
import android.webkit.MimeTypeMap
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.obs.marveleditor.R

abstract class OptiBaseCreatorDialogFragment : DialogFragment() {
    private var permissionsRequired = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.share_dialog1)

        this.isCancelable = false
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            130 -> {
                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity, permission)) {
                        //denied
                        Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                    } else {
                        if (ActivityCompat.checkSelfPermission(requireContext(), permissionsRequired[0]) == PackageManager.PERMISSION_GRANTED) {
                            //SaveImage()
                        } else {
                            callPermissionSettings()
                        }
                    }
                }
                return
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        stopRunningProcess()
    }

    private fun callPermissionSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", requireContext().applicationContext.packageName, null)
        intent.data = uri
        startActivityForResult(intent, 300)
    }

    override fun onResume() {
        super.onResume()

        if (ActivityCompat.checkSelfPermission(requireContext(), permissionsRequired[0]) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissionsRequired, 130)
        }
    }

    interface CallBacks {

        fun onDidNothing()

        fun onFileProcessed(file: File)

        fun getFile(): File?

        fun reInitPlayer()

        fun onAudioFileProcessed(convertedAudioFile: File)

        fun showLoading(isShow: Boolean)

        fun openGallery()

        fun openCamera()
    }

    abstract fun permissionsBlocked()


    fun stopRunningProcess() {
        FFmpeg.getInstance(activity).killRunningProcesses()
    }

    fun isRunning(): Boolean {
        return FFmpeg.getInstance(activity).isFFmpegCommandRunning
    }

    fun showInProgressToast() {
        Toast.makeText(activity, "Operation already in progress! Try again in a while.", Toast.LENGTH_SHORT).show()
    }

    fun getMimeType(url: String): String? {
        var type: String? = null
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return type
    }
}