/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.utils

import android.Manifest
import android.os.Build

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
    }
}


fun isAndroidQAndAbove(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}