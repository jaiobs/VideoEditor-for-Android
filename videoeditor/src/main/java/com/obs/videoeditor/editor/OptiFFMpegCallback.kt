package com.obs.videoeditor.editor

import java.io.File

abstract interface OptiFFMpegCallback {

    fun onProgress(progress: String) {}

    fun onSuccess(convertedFile: File, type: String)

    fun onFailure(error: Exception)

    fun onNotAvailable(error: Exception) {}

    fun onFinish() {}

}