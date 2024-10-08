/*
 *
 *  Created by Optisol on Aug 2019.
 *  Copyright © 2019 Optisol Business Solutions pvt ltd. All rights reserved.
 *
 */

package com.obs.marveleditor.videoTrimmer.view;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import com.obs.marveleditor.R;
import java.util.List;
import java.util.Vector;

public class OptiThumb {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    private int mIndex;
    private float mVal;
    private float mPos;
    private Bitmap mBitmap;
    private int mWidthBitmap;
    private int mHeightBitmap;

    private float mLastTouchX;

    private OptiThumb() {
        mVal = 0;
        mPos = 0;
    }

    int getIndex() {
        return mIndex;
    }

    private void setIndex(int index) {
        mIndex = index;
    }

    float getVal() {
        return mVal;
    }

    void setVal(float val) {
        mVal = val;
    }

    float getPos() {
        return mPos;
    }

    void setPos(float pos) {
        mPos = pos;
    }

    Bitmap getBitmap() {
        return mBitmap;
    }

    private void setBitmap(@NonNull Bitmap bitmap) {
        mBitmap = bitmap;
        mWidthBitmap = bitmap.getWidth();
        mHeightBitmap = bitmap.getHeight();
    }

    @NonNull
    static List<OptiThumb> initThumbs(Resources resources) {

        List<OptiThumb> thumbs = new Vector<>();

        for (int i = 0; i < 2; i++) {
            OptiThumb th = new OptiThumb();
            th.setIndex(i);
            if (i == 0) {
                int resImageLeft = R.drawable.apptheme_text_select_handle_left;
                th.setBitmap(BitmapFactory.decodeResource(resources, resImageLeft));
            } else {
                int resImageRight = R.drawable.apptheme_text_select_handle_right;
                th.setBitmap(BitmapFactory.decodeResource(resources, resImageRight));
            }

            thumbs.add(th);
        }

        return thumbs;
    }

    static int getWidthBitmap(@NonNull List<OptiThumb> thumbs) {
        return thumbs.get(0).getWidthBitmap();
    }

    static int getHeightBitmap(@NonNull List<OptiThumb> thumbs) {
        return thumbs.get(0).getHeightBitmap();
    }

    float getLastTouchX() {
        return mLastTouchX;
    }

    void setLastTouchX(float lastTouchX) {
        mLastTouchX = lastTouchX;
    }

    public int getWidthBitmap() {
        return mWidthBitmap;
    }

    private int getHeightBitmap() {
        return mHeightBitmap;
    }
}
