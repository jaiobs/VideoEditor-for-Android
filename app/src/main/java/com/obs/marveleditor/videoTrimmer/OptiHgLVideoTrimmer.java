/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.videoTrimmer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.obs.marveleditor.R;
import com.obs.marveleditor.videoTrimmer.interfaces.OptiOnHgLVideoListener;
import com.obs.marveleditor.videoTrimmer.interfaces.OptiOnProgressVideoListener;
import com.obs.marveleditor.videoTrimmer.interfaces.OptiOnRangeSeekBarListener;
import com.obs.marveleditor.videoTrimmer.interfaces.OptiOnTrimVideoListener;
import com.obs.marveleditor.videoTrimmer.utils.OptiBackgroundExecutor;
import com.obs.marveleditor.videoTrimmer.utils.OptiUiThreadExecutor;
import com.obs.marveleditor.videoTrimmer.view.OptiProgressBarView;
import com.obs.marveleditor.videoTrimmer.view.OptiRangeSeekBarView;
import com.obs.marveleditor.videoTrimmer.view.OptiThumb;
import com.obs.marveleditor.videoTrimmer.view.OptiTimeLineView;

import static com.obs.marveleditor.videoTrimmer.utils.OptiTrimVideoUtils.stringForTime;

public class OptiHgLVideoTrimmer extends FrameLayout {

    private static final String TAG = OptiHgLVideoTrimmer.class.getSimpleName();
    //private static final int MIN_TIME_FRAME = 1000;
    private static final int SHOW_PROGRESS = 2;

    private SeekBar mHolderTopView;
    private OptiRangeSeekBarView mRangeSeekBarView;
    private RelativeLayout mLinearVideo;
    private View mTimeInfoContainer;
    private VideoView mVideoView;
    private ImageView mPlayView;
    private TextView mTextSize;
    private TextView mTextTimeFrame;
    private TextView mTextTime;
    private OptiTimeLineView mTimeLineView;

    private OptiProgressBarView mVideoProgressIndicator;
    private Uri mSrc;
    private String mFinalPath;

    private int mMaxDuration;
    private List<OptiOnProgressVideoListener> mListeners;

    private OptiOnTrimVideoListener mOnTrimVideoListener;
    private OptiOnHgLVideoListener mOnHgLVideoListener;

    private int mDuration = 0;
    private int mTimeVideo = 0;
    private int mStartPosition = 0;
    private int mEndPosition = 0;

    private long mOriginSizeFile;
    private boolean mResetSeekBar = true;
    private final MessageHandler mMessageHandler = new MessageHandler(this);

    public OptiHgLVideoTrimmer(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptiHgLVideoTrimmer(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.opti_view_time_line, this, true);

        mHolderTopView = findViewById(R.id.handlerTop);
        mVideoProgressIndicator = findViewById(R.id.timeVideoView);
        mRangeSeekBarView = findViewById(R.id.timeLineBar);
        mLinearVideo = findViewById(R.id.layout_surface_view);
        mVideoView = findViewById(R.id.video_loader);
        mPlayView = findViewById(R.id.icon_video_play);
        mTimeInfoContainer = findViewById(R.id.timeText);
        mTextSize = findViewById(R.id.textSize);
        mTextTimeFrame = findViewById(R.id.textTimeSelection);
        mTextTime = findViewById(R.id.textTime);
        mTimeLineView = findViewById(R.id.timeLineView);

        setUpListeners();
        setUpMargins();
    }

    private void setUpListeners() {
        mListeners = new ArrayList<>();
        mListeners.add((time, max, scale) -> updateVideoProgress(time));
        mListeners.add(mVideoProgressIndicator);

        findViewById(R.id.btCancel)
                .setOnClickListener(
                        view -> onCancelClicked()
                );

        findViewById(R.id.btSave)
                .setOnClickListener(
                        view -> onSaveClicked()
                );

        final GestureDetector gestureDetector = new
                GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        onClickVideoPlayPause();
                        return true;
                    }
                }
        );

        mVideoView.setOnErrorListener((mediaPlayer, what, extra) -> {
            if (mOnTrimVideoListener != null)
                mOnTrimVideoListener.onError("Something went wrong reason : " + what);
            return false;
        });

        mVideoView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        mRangeSeekBarView.addOnRangeSeekBarListener(mVideoProgressIndicator);
        mRangeSeekBarView.addOnRangeSeekBarListener(new OptiOnRangeSeekBarListener() {
                                                        @Override
                                                        public void onCreate(OptiRangeSeekBarView rangeSeekBarView, int index, float value) {

                                                        }

                                                        @Override
                                                        public void onSeek(OptiRangeSeekBarView rangeSeekBarView, int index, float value) {
                                                            onSeekThumbs(index, value);
                                                        }

                                                        @Override
                                                        public void onSeekStart(OptiRangeSeekBarView rangeSeekBarView, int index, float value) {

                                                        }

                                                        @Override
                                                        public void onSeekStop(OptiRangeSeekBarView rangeSeekBarView, int index, float value) {
                                                            onStopSeekThumbs();
                                                        }
                                                    });

        mHolderTopView.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onPlayerIndicatorSeekChanged(progress, fromUser);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onPlayerIndicatorSeekStart();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                onPlayerIndicatorSeekStop(seekBar);
            }
        });

        mVideoView.setOnPreparedListener(this::onVideoPrepared);

        mVideoView.setOnCompletionListener(mp -> onVideoCompleted());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void setUpMargins() {
        int marge = mRangeSeekBarView.getThumbs().get(0).getWidthBitmap();
        int widthSeek = mHolderTopView.getThumb().getMinimumWidth() / 2;

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mHolderTopView.getLayoutParams();
        lp.setMargins(marge - widthSeek, 0, marge - widthSeek, 0);
        mHolderTopView.setLayoutParams(lp);

        lp = (RelativeLayout.LayoutParams) mTimeLineView.getLayoutParams();
        lp.setMargins(marge, 0, marge, 0);
        mTimeLineView.setLayoutParams(lp);

        lp = (RelativeLayout.LayoutParams) mVideoProgressIndicator.getLayoutParams();
        lp.setMargins(marge, 0, marge, 0);
        mVideoProgressIndicator.setLayoutParams(lp);
    }

    private void onSaveClicked() {
        int finalDuration = mEndPosition - mStartPosition;

        //check if timeinmillis duration is less than 4 minutes
        if(finalDuration < 240000){
            mPlayView.setVisibility(View.VISIBLE);
            mVideoView.pause();

            if (mOnTrimVideoListener != null)
                Log.v(TAG, "mStartPosition: " + mStartPosition + " mEndPosition: " + mEndPosition);
            mOnTrimVideoListener.onTrimStarted(mStartPosition, mEndPosition);
        } else {
            Toast.makeText(getContext(), "Please trim video under 4 minutes", Toast.LENGTH_SHORT).show();
        }
    }

    private void onClickVideoPlayPause() {
        if (mVideoView.isPlaying()) {
            mPlayView.setVisibility(View.VISIBLE);
            mMessageHandler.removeMessages(SHOW_PROGRESS);
            mVideoView.pause();
        } else {
            mPlayView.setVisibility(View.GONE);

            if (mResetSeekBar) {
                mResetSeekBar = false;
                mVideoView.seekTo(mStartPosition);
            }

            mMessageHandler.sendEmptyMessage(SHOW_PROGRESS);
            mVideoView.start();
        }
    }

    private void onCancelClicked() {
        mVideoView.stopPlayback();
        if (mOnTrimVideoListener != null) {
            mOnTrimVideoListener.cancelAction();
        }
    }

    /*private String getDestinationPath() {
        if (mFinalPath == null) {
            File folder = Environment.getExternalStorageDirectory();
            mFinalPath = folder.getPath() + File.separator;
            Log.d(TAG, "Using default path " + mFinalPath);
        }
        return mFinalPath;
    }*/

    private void onPlayerIndicatorSeekChanged(int progress, boolean fromUser) {

        int duration = (int) ((mDuration * progress) / 1000L);

        if (fromUser) {
            if (duration < mStartPosition) {
                setProgressBarPosition(mStartPosition);
                duration = mStartPosition;
            } else if (duration > mEndPosition) {
                setProgressBarPosition(mEndPosition);
                duration = mEndPosition;
            }
            setTimeVideo(duration);
        }
    }

    private void onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS);
        mVideoView.pause();
        mPlayView.setVisibility(View.VISIBLE);
        notifyProgressUpdate(false);
    }

    private void onPlayerIndicatorSeekStop(@NonNull SeekBar seekBar) {
        mMessageHandler.removeMessages(SHOW_PROGRESS);
        mVideoView.pause();
        mPlayView.setVisibility(View.VISIBLE);

        int duration = (int) ((mDuration * seekBar.getProgress()) / 1000L);
        mVideoView.seekTo(duration);
        setTimeVideo(duration);
        notifyProgressUpdate(false);
    }

    private void onVideoPrepared(@NonNull MediaPlayer mp) {
        // Adjust the size of the video
        // so it fits on the screen
        int videoWidth = mp.getVideoWidth();
        int videoHeight = mp.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;
        int screenWidth = mLinearVideo.getWidth();
        int screenHeight = mLinearVideo.getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;
        ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();

        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        mVideoView.setLayoutParams(lp);

        mPlayView.setVisibility(View.VISIBLE);

        mDuration = mVideoView.getDuration();
        setSeekBarPosition();

        setTimeFrames();
        setTimeVideo(0);

        if (mOnHgLVideoListener != null) {
            mOnHgLVideoListener.onVideoPrepared();
        }
    }

    private void setSeekBarPosition() {

        if (mDuration >= mMaxDuration) {
            mStartPosition = mDuration / 2 - mMaxDuration / 2;
            mEndPosition = mDuration / 2 + mMaxDuration / 2;

            mRangeSeekBarView.setThumbValue(0, (mStartPosition * 100) / mDuration);
            mRangeSeekBarView.setThumbValue(1, (mEndPosition * 100) / mDuration);

        } else {
            mStartPosition = 0;
            mEndPosition = mDuration;
        }

        setProgressBarPosition(mStartPosition);
        mVideoView.seekTo(mStartPosition);

        mTimeVideo = mDuration;
        mRangeSeekBarView.initMaxWidth();
    }

    private void setTimeFrames() {
        String seconds = getContext().getString(R.string.short_seconds);
        mTextTimeFrame.setText(String.format("%s %s - %s %s", stringForTime(mStartPosition), seconds, stringForTime(mEndPosition), seconds));
    }

    private void setTimeVideo(int position) {
        String seconds = getContext().getString(R.string.short_seconds);
        mTextTime.setText(String.format("%s %s", stringForTime(position), seconds));
    }

    private void onSeekThumbs(int index, float value) {
        switch (index) {
            case OptiThumb.LEFT: {
                mStartPosition = (int) ((mDuration * value) / 100L);
                mVideoView.seekTo(mStartPosition);
                break;
            }
            case OptiThumb.RIGHT: {
                mEndPosition = (int) ((mDuration * value) / 100L);
                break;
            }
        }
        setProgressBarPosition(mStartPosition);

        setTimeFrames();
        mTimeVideo = mEndPosition - mStartPosition;
    }

    private void onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS);
        mVideoView.pause();
        mPlayView.setVisibility(View.VISIBLE);
    }

    private void onVideoCompleted() {
        mVideoView.seekTo(mStartPosition);
    }

    private void notifyProgressUpdate(boolean all) {
        if (mDuration == 0) return;

        int position = mVideoView.getCurrentPosition();
        if (all) {
            for (OptiOnProgressVideoListener item : mListeners) {
                item.updateProgress(position, mDuration, ((position * 100) / mDuration));
            }
        } else {
            mListeners.get(1).updateProgress(position, mDuration, ((position * 100) / mDuration));
        }
    }

    private void updateVideoProgress(int time) {
        if (mVideoView == null) {
            return;
        }

        if (time >= mEndPosition) {
            mMessageHandler.removeMessages(SHOW_PROGRESS);
            mVideoView.pause();
            mPlayView.setVisibility(View.VISIBLE);
            mResetSeekBar = true;
            return;
        }

        if (mHolderTopView != null) {
            // use long to avoid overflow
            setProgressBarPosition(time);
        }
        setTimeVideo(time);
    }

    private void setProgressBarPosition(int position) {
        if (mDuration > 0) {
            long pos = 1000L * position / mDuration;
            mHolderTopView.setProgress((int) pos);
        }
    }

    /**
     * Set video information visibility.
     * For now this is for debugging
     *
     * @param visible whether or not the videoInformation will be visible
     */
    public void setVideoInformationVisibility(boolean visible) {
        mTimeInfoContainer.setVisibility(visible ? VISIBLE : GONE);
    }

    /**
     * Listener for events such as trimming operation success and cancel
     *
     * @param onTrimVideoListener interface for events
     */
    @SuppressWarnings("unused")
    public void setOnTrimVideoListener(OptiOnTrimVideoListener onTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener;
    }

    /**
     * Listener for some {@link VideoView} events
     *
     * @param onHgLVideoListener interface for events
     */
    @SuppressWarnings("unused")
    public void setOnHgLVideoListener(OptiOnHgLVideoListener onHgLVideoListener) {
        mOnHgLVideoListener = onHgLVideoListener;
    }

    /**
     * Sets the path where the trimmed video will be saved
     * Ex: /storage/emulated/0/MyAppFolder/
     *
     * @param finalPath the full path
     */
    @SuppressWarnings("unused")
    public void setDestinationPath(final String finalPath) {
        mFinalPath = finalPath;
        Log.d(TAG, "Setting custom path " + mFinalPath);
    }

    /**
     * Cancel all current operations
     */
    public void destroy() {
        OptiBackgroundExecutor.cancelAll("", true);
        OptiUiThreadExecutor.cancelAll("");
    }

    /**
     * Set the maximum duration of the trimmed video.
     * The trimmer interface wont allow the user to set duration longer than maxDuration
     *
     * @param maxDuration the maximum duration of the trimmed video in seconds
     */
    @SuppressWarnings("unused")
    public void setMaxDuration(int maxDuration) {
       // mMaxDuration = maxDuration * 1000;
        mMaxDuration = maxDuration;
     }

    /**
     * Sets the uri of the video to be trimmer
     *
     * @param videoURI Uri of the video
     */
    @SuppressWarnings("unused")
    public void setVideoURI(final Uri videoURI) {
        mSrc = videoURI;

        if (mOriginSizeFile == 0) {
            File file = new File(mSrc.getPath());

            mOriginSizeFile = file.length();
            long fileSizeInKB = mOriginSizeFile / 1024;

            if (fileSizeInKB > 1000) {
                long fileSizeInMB = fileSizeInKB / 1024;
                mTextSize.setText(String.format("%s %s", fileSizeInMB, getContext().getString(R.string.megabyte)));
            } else {
                mTextSize.setText(String.format("%s %s", fileSizeInKB, getContext().getString(R.string.kilobyte)));
            }
        }

        mVideoView.setVideoURI(mSrc);
        mVideoView.requestFocus();

        mTimeLineView.setVideo(mSrc);
    }

    private static class MessageHandler extends Handler {

        @NonNull
        private final WeakReference<OptiHgLVideoTrimmer> mView;

        MessageHandler(OptiHgLVideoTrimmer view) {
            mView = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            OptiHgLVideoTrimmer view = mView.get();
            if (view == null || view.mVideoView == null) {
                return;
            }

            view.notifyProgressUpdate(true);
            if (view.mVideoView.isPlaying()) {
                sendEmptyMessageDelayed(0, 10);
            }
        }
    }
}
