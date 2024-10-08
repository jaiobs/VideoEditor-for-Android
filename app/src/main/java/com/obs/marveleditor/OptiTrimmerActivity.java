/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import com.obs.marveleditor.videoTrimmer.OptiHgLVideoTrimmer;
import com.obs.marveleditor.videoTrimmer.interfaces.OptiOnHgLVideoListener;
import com.obs.marveleditor.videoTrimmer.interfaces.OptiOnTrimVideoListener;

public class OptiTrimmerActivity extends AppCompatActivity implements OptiOnTrimVideoListener, OptiOnHgLVideoListener {

    private OptiHgLVideoTrimmer mVideoTrimmer;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.opti_activity_trimmer);

        Intent extraIntent = getIntent();
        String path = "";
        int maxDuration = 10;

        if (extraIntent != null) {
            path = extraIntent.getStringExtra("VideoPath");
            maxDuration = extraIntent.getIntExtra("VideoDuration", 10);
        }

        //setting progressbar
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(getString(R.string.trimming_progress));

        mVideoTrimmer = findViewById(R.id.timeLine);
        if (mVideoTrimmer != null) {
            //get total duration of video file
            Log.e("tg", "maxDuration = " + maxDuration);
            mVideoTrimmer.setMaxDuration(maxDuration);
            mVideoTrimmer.setOnTrimVideoListener(this);
            mVideoTrimmer.setOnHgLVideoListener(this);
            mVideoTrimmer.setVideoURI(Uri.parse(path));
            mVideoTrimmer.setVideoInformationVisibility(true);
        }
    }

    @Override
    public void onTrimStarted(int startPosition, int endPosition) {
        //mProgressDialog.show();
        //selected startPosition & endPosition is passed
        Intent intent = new Intent();
        intent.putExtra("startPosition", startPosition);
        intent.putExtra("endPosition", endPosition);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void getResult(final Uri contentUri) {
        mProgressDialog.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("tg", "resultCode = " + resultCode + " data " + data);
    }

    @Override
    public void cancelAction() {
        mProgressDialog.cancel();
        mVideoTrimmer.destroy();
        finish();
    }

    @Override
    public void onError(final String message) {
        mProgressDialog.cancel();

        runOnUiThread(() -> {
           // Toast.makeText(OptiTrimmerActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onVideoPrepared() {
        runOnUiThread(() -> {
           // Toast.makeText(OptiTrimmerActivity.this, "onVideoPrepared", Toast.LENGTH_SHORT).show();
        });
    }
}
