/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

//  The MIT License (MIT)

//  Copyright (c) 2018 Intuz Solutions Pvt Ltd.

//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
//  (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
//  merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:

//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
//  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
//  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.obs.marveleditor.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.obs.marveleditor.R;
import com.obs.marveleditor.interfaces.OptiOnRangeSeekBarChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OptiCustomRangeSeekBar extends View {

    private int mHeightTimeLine;
    private List<OptiBarThumb> mBarThumbs;
    private List<OptiOnRangeSeekBarChangeListener> mListeners;
    private float mMaxWidth;
    private float mThumbWidth;
    private float mThumbHeight;
    private int mViewWidth;
    private float mPixelRangeMin;
    private float mPixelRangeMax;
    private float mScaleRangeMax;
    private boolean mFirstRun;

    private final Paint mShadow = new Paint();
    private final Paint mLine = new Paint();

    public OptiCustomRangeSeekBar(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptiCustomRangeSeekBar(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBarThumbs = OptiBarThumb.Companion.initThumbs(getResources());
        mThumbWidth = OptiBarThumb.Companion.getWidthBitmap(mBarThumbs);
        mThumbHeight = OptiBarThumb.Companion.getHeightBitmap(mBarThumbs);

        mScaleRangeMax = 100;
        mHeightTimeLine = getContext().getResources().getDimensionPixelOffset(R.dimen._60sdp);

        setFocusable(true);
        setFocusableInTouchMode(true);

        mFirstRun = true;

        int shadowColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
        mShadow.setAntiAlias(true);
        mShadow.setColor(shadowColor);
        mShadow.setAlpha(177);

        int lineColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
        mLine.setAntiAlias(true);
        mLine.setColor(lineColor);
        mLine.setAlpha(200);
    }

    public void initMaxWidth() {
        mMaxWidth = mBarThumbs.get(1).getPos() - mBarThumbs.get(0).getPos();

        onSeekStop(this, 0, mBarThumbs.get(0).getVal());
        onSeekStop(this, 1, mBarThumbs.get(1).getVal());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int minW = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        mViewWidth = resolveSizeAndState(minW, widthMeasureSpec, 1);

        int minH = getPaddingBottom() + getPaddingTop() + (int) mThumbHeight;
        int viewHeight = resolveSizeAndState(minH, heightMeasureSpec, 1);

        setMeasuredDimension(mViewWidth, viewHeight);

        mPixelRangeMin = 0;
        mPixelRangeMax = mViewWidth - mThumbWidth;

        if (mFirstRun) {
            for (int i = 0; i < mBarThumbs.size(); i++) {
                OptiBarThumb th = mBarThumbs.get(i);
                th.setVal(mScaleRangeMax * i);
                th.setPos(mPixelRangeMax * i);
            }
            // Fire listener callback
            onCreate(this, currentThumb, getThumbValue(currentThumb));
            mFirstRun = false;
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        drawShadow(canvas);
        drawThumbs(canvas);
    }

    private int currentThumb = 0;

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        final OptiBarThumb mBarThumb;
        final OptiBarThumb mBarThumb2;
        final float coordinate = ev.getX();
        final int action = ev.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // Remember where we started
                currentThumb = getClosestThumb(coordinate);

                if (currentThumb == -1) {
                    return false;
                }

                mBarThumb = mBarThumbs.get(currentThumb);
                mBarThumb.setLastTouchX(coordinate);
                onSeekStart(this, currentThumb, mBarThumb.getVal());
                return true;
            }
            case MotionEvent.ACTION_UP: {

                if (currentThumb == -1) {
                    return false;
                }

                mBarThumb = mBarThumbs.get(currentThumb);
                onSeekStop(this, currentThumb, mBarThumb.getVal());
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                mBarThumb = mBarThumbs.get(currentThumb);
                mBarThumb2 = mBarThumbs.get(currentThumb == 0 ? 1 : 0);
                // Calculate the distance moved
                final float dx = coordinate - mBarThumb.getLastTouchX();
                final float newX = mBarThumb.getPos() + dx;

                if (currentThumb == 0) {

                    if ((newX + mBarThumb.getWidthBitmap()) >= mBarThumb2.getPos()) {
                        mBarThumb.setPos(mBarThumb2.getPos() - mBarThumb.getWidthBitmap());
                    } else if (newX <= mPixelRangeMin) {
                        mBarThumb.setPos(mPixelRangeMin);
                        if ((mBarThumb2.getPos() - (mBarThumb.getPos() + dx)) > mMaxWidth) {
                            mBarThumb2.setPos(mBarThumb.getPos() + dx + mMaxWidth);
                            setThumbPos(1, mBarThumb2.getPos());
                        }
                    } else {
                        //Check if thumb is not out of max width
//                        checkPositionThumb(mBarThumb, mBarThumb2, dx, true, coordinate);
                        if ((mBarThumb2.getPos() - (mBarThumb.getPos() + dx)) > mMaxWidth) {
                            mBarThumb2.setPos(mBarThumb.getPos() + dx + mMaxWidth);
                            setThumbPos(1, mBarThumb2.getPos());
                        }
                        // Move the object
                        mBarThumb.setPos(mBarThumb.getPos() + dx);

                        // Remember this touch position for the next move event
                        mBarThumb.setLastTouchX(coordinate);
                    }

                } else {
                    if (newX <= mBarThumb2.getPos() + mBarThumb2.getWidthBitmap()) {
                        mBarThumb.setPos(mBarThumb2.getPos() + mBarThumb.getWidthBitmap());
                    } else if (newX >= mPixelRangeMax) {
                        mBarThumb.setPos(mPixelRangeMax);
                        if (((mBarThumb.getPos() + dx) - mBarThumb2.getPos()) > mMaxWidth) {
                            mBarThumb2.setPos(mBarThumb.getPos() + dx - mMaxWidth);
                            setThumbPos(0, mBarThumb2.getPos());
                        }
                    } else {
                        //Check if thumb is not out of max width
//                        checkPositionThumb(mBarThumb2, mBarThumb, dx, false, coordinate);
                        if (((mBarThumb.getPos() + dx) - mBarThumb2.getPos()) > mMaxWidth) {
                            mBarThumb2.setPos(mBarThumb.getPos() + dx - mMaxWidth);
                            setThumbPos(0, mBarThumb2.getPos());
                        }
                        // Move the object
                        mBarThumb.setPos(mBarThumb.getPos() + dx);
                        // Remember this touch position for the next move event
                        mBarThumb.setLastTouchX(coordinate);
                    }
                }

                setThumbPos(currentThumb, mBarThumb.getPos());

                // Invalidate to request a redraw
                invalidate();
                return true;
            }
        }
        return false;
    }

    private void checkPositionThumb(@NonNull OptiBarThumb mBarThumbLeft, @NonNull OptiBarThumb mBarThumbRight, float dx, boolean isLeftMove, float coordinate) {

        if (isLeftMove && dx < 0) {
            if ((mBarThumbRight.getPos() - (mBarThumbLeft.getPos() + dx)) > mMaxWidth) {
                mBarThumbRight.setPos(mBarThumbLeft.getPos() + dx + mMaxWidth);
                setThumbPos(1, mBarThumbRight.getPos());
            }
        } else if (!isLeftMove && dx > 0) {
            if (((mBarThumbRight.getPos() + dx) - mBarThumbLeft.getPos()) > mMaxWidth) {
                mBarThumbLeft.setPos(mBarThumbRight.getPos() + dx - mMaxWidth);
                setThumbPos(0, mBarThumbLeft.getPos());
            }
        }

    }



    private float pixelToScale(int index, float pixelValue) {
        float scale = (pixelValue * 100) / mPixelRangeMax;
        if (index == 0) {
            float pxThumb = (scale * mThumbWidth) / 100;
            return scale + (pxThumb * 100) / mPixelRangeMax;
        } else {
            float pxThumb = ((100 - scale) * mThumbWidth) / 100;
            return scale - (pxThumb * 100) / mPixelRangeMax;
        }
    }

    private float scaleToPixel(int index, float scaleValue) {
        float px = (scaleValue * mPixelRangeMax) / 100;
        if (index == 0) {
            float pxThumb = (scaleValue * mThumbWidth) / 100;
            return px - pxThumb;
        } else {
            float pxThumb = ((100 - scaleValue) * mThumbWidth) / 100;
            return px + pxThumb;
        }
    }

    private void calculateThumbValue(int index) {
        if (index < mBarThumbs.size() && !mBarThumbs.isEmpty()) {
            OptiBarThumb th = mBarThumbs.get(index);
            th.setVal(pixelToScale(index, th.getPos()));
            onSeek(this, index, th.getVal());
        }
    }

    private void calculateThumbPos(int index) {
        if (index < mBarThumbs.size() && !mBarThumbs.isEmpty()) {
            OptiBarThumb th = mBarThumbs.get(index);
            th.setPos(scaleToPixel(index, th.getVal()));
        }
    }

    private float getThumbValue(int index) {
        return mBarThumbs.get(index).getVal();
    }

    public void setThumbValue(int index, float value) {
        mBarThumbs.get(index).setVal(value);
        calculateThumbPos(index);
        // Tell the view we want a complete redraw
        invalidate();
    }

    private void setThumbPos(int index, float pos) {
        mBarThumbs.get(index).setPos(pos);
        calculateThumbValue(index);
        // Tell the view we want a complete redraw
        invalidate();
    }

    private int getClosestThumb(float coordinate) {
        int closest = -1;
        if (!mBarThumbs.isEmpty()) {
            for (int i = 0; i < mBarThumbs.size(); i++) {
                // Find thumb closest to x coordinate
                final float tcoordinate = mBarThumbs.get(i).getPos() + mThumbWidth;
                if (coordinate >= mBarThumbs.get(i).getPos() && coordinate <= tcoordinate) {
                    closest = mBarThumbs.get(i).getIndex();
                }
            }
        }
        return closest;
    }

    private void drawShadow(@NonNull Canvas canvas) {
        if (!mBarThumbs.isEmpty()) {

            for (OptiBarThumb th : mBarThumbs) {
                if (th.getIndex() == 0) {
                    final float x = th.getPos();
                    if (x > mPixelRangeMin) {
                        Rect mRect = new Rect(0, (int) (mThumbHeight - mHeightTimeLine) / 2,
                                (int) (x + (mThumbWidth / 2)), mHeightTimeLine + (int) (mThumbHeight - mHeightTimeLine) / 2);
                        canvas.drawRect(mRect, mShadow);
                    }
                } else {
                    final float x = th.getPos();
                    if (x < mPixelRangeMax) {
                        Rect mRect = new Rect((int) (x + (mThumbWidth / 2)), (int) (mThumbHeight - mHeightTimeLine) / 2,
                                (mViewWidth), mHeightTimeLine + (int) (mThumbHeight - mHeightTimeLine) / 2);
                        canvas.drawRect(mRect, mShadow);
                    }
                }
            }
        }
    }

    private void drawThumbs(@NonNull Canvas canvas) {

        if (!mBarThumbs.isEmpty()) {
            for (OptiBarThumb th : mBarThumbs) {
                if (th.getIndex() == 0) {
                    canvas.drawBitmap(Objects.requireNonNull(th.getBitmap()), th.getPos() + getPaddingLeft(), getPaddingTop(), null);
                } else {
                    canvas.drawBitmap(Objects.requireNonNull(th.getBitmap()), th.getPos() - getPaddingRight(), getPaddingTop(), null);
                }
            }
        }
    }

    public void addOnRangeSeekBarListener(OptiOnRangeSeekBarChangeListener listener) {

        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }

        mListeners.add(listener);
    }

    private void onCreate(OptiCustomRangeSeekBar CustomRangeSeekBar, int index, float value) {
        if (mListeners == null)
            return;

        for (OptiOnRangeSeekBarChangeListener item : mListeners) {
            item.onCreate(CustomRangeSeekBar, index, value);
        }
    }

    private void onSeek(OptiCustomRangeSeekBar CustomRangeSeekBar, int index, float value) {
        if (mListeners == null)
            return;

        for (OptiOnRangeSeekBarChangeListener item : mListeners) {
            item.onSeek(CustomRangeSeekBar, index, value);
        }
    }

    private void onSeekStart(OptiCustomRangeSeekBar CustomRangeSeekBar, int index, float value) {
        if (mListeners == null)
            return;

        for (OptiOnRangeSeekBarChangeListener item : mListeners) {
            item.onSeekStart(CustomRangeSeekBar, index, value);
        }
    }

    private void onSeekStop(OptiCustomRangeSeekBar CustomRangeSeekBar, int index, float value) {
        if (mListeners == null)
            return;

        for (OptiOnRangeSeekBarChangeListener item : mListeners) {
            item.onSeekStop(CustomRangeSeekBar, index, value);
        }
    }

    public List<OptiBarThumb> getThumbs() {
        return mBarThumbs;
    }
}
