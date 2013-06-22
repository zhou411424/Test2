/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leven.videoplayer.video;

import com.leven.videoplayer.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

/**
 * The time bar view, which includes the current and total time, the progress bar,
 * and the scrubber.
 */
public class TimeBar extends View {
   private static final String                TAG = "TimeBar";
  private static final int                    SCRUBBER_PADDING_IN_DP = 10;
  // The total padding, top plus bottom
  private static final int                    V_PADDING_IN_DP = 30;
  private static final int                    TEXT_SIZE_IN_DP = 14;
  private final Listener                      mListener;
  // the bars we use for displaying the progress
  private final Rect                          mProgressBar;
  private final Rect                          mPlayedBar;
  private final Bitmap                        mProgressBarBg;
  private final Bitmap                        mPlayedBarBg;
  private final Paint                         mTimeTextPaint;
  private       Bitmap                        mScrubber;
  private final int                           mScrubberPadding; // adds some touch tolerance around the scrubber
  private int                                 mScrubberLeft;
  private int                                 mScrubberTop;
  private int                                 mScrubberCorrection;
  private boolean                             mScrubbing;
  private boolean                             mShowTimes;
  private boolean                             mShowScrubber;
  private int                                 mTotalTime;
  private int                                 mCurrentTime;
  private final Rect                          mTimeBounds;
  private int                                 mPaddingInPx;
  private boolean                             mIsSeek = false;
  private int                                 mDeltaX;
  private int                                 mTouchbegan_x;
  private NinePatch                           mplayedninepatch;
  private NinePatch                           mprogressbgninepatch;
  public interface Listener {
       void onScrubbingStart();
       void onScrubbingMove(int time,int pos);
       void onScrubbingEnd(int time);
     }
  
    public TimeBar(Context context, Listener listener) {
        super(context);
        this.mListener = checkNotNull(listener);
        mShowTimes = true;
        mShowScrubber = true;

        mProgressBar = new Rect();
        mPlayedBar = new Rect();

        mProgressBarBg = BitmapFactory.decodeResource(getResources(), R.drawable.ic_video_element_slide_frame_normal);
        mprogressbgninepatch  = new NinePatch(mProgressBarBg, mProgressBarBg.getNinePatchChunk(), null);

        mPlayedBarBg = BitmapFactory.decodeResource(getResources(), R.drawable.ic_video_element_slide_frame_loading);
        mplayedninepatch = new NinePatch(mPlayedBarBg, mPlayedBarBg.getNinePatchChunk(), null);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float textSizeInPx = metrics.density * TEXT_SIZE_IN_DP;
        mTimeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTimeTextPaint.setColor(0xFFCECECE);
        mTimeTextPaint.setTextSize(textSizeInPx);
        mTimeTextPaint.setTextAlign(Paint.Align.CENTER);

        mTimeBounds = new Rect();
        mTimeTextPaint.getTextBounds("0:00:00", 0, 7, mTimeBounds);

        mScrubber = BitmapFactory.decodeResource(getResources(), R.drawable.ic_video_element_slide_normal);
        mScrubberPadding = (int) (metrics.density * SCRUBBER_PADDING_IN_DP);

        mPaddingInPx = (int) (metrics.density * V_PADDING_IN_DP);
    }

    private void update() {
        mPlayedBar.set(mProgressBar);

        if (mTotalTime > 0) { 
            if(mCurrentTime >  mTotalTime)
            {
                Log.e(TAG, "mCurrentTime >  mTotalTime in timebar update()");
                mCurrentTime = mTotalTime;
            }
            mPlayedBar.right = mPlayedBar.left + (int) ((mProgressBar.width() * (long)mCurrentTime) / mTotalTime);
        } else { 
            mPlayedBar.right = mProgressBar.left;
        }

        mScrubberLeft = mPlayedBar.right - mScrubber.getWidth() / 2;
        invalidate();
    }

    /**
     * @return the preferred height of this view, including invisible padding
     */
    public int getPreferredHeight() {
        return mTimeBounds.height() + mPaddingInPx + mScrubberPadding;
    }

    /**
     * @return the height of the time bar, excluding invisible padding
     */
    public int getBarHeight() {
        return mTimeBounds.height() + mPaddingInPx;
    }

    public void setTime(int currentTime, int totalTime) {
        if (this.mCurrentTime == currentTime && this.mTotalTime == totalTime) {
            return;
        }
        this.mCurrentTime = currentTime;
        this.mTotalTime = totalTime;
        update();
    }

    public void setShowTimes(boolean showTimes) {
        this.mShowTimes = showTimes;
        requestLayout();
    }

    public void resetTime() {
        setTime(0, 0);
    }

    public void setShowScrubber(boolean showScrubber) {
        this.mShowScrubber = showScrubber;
        if (!mShowScrubber && mScrubbing) {
            mListener.onScrubbingEnd((int)getScrubberTime());
            mScrubbing = false;
        }
        requestLayout();
    }

    private boolean inScrubber(float x, float y) {
        int scrubberRight = mScrubberLeft + mScrubber.getWidth();
        int scrubberBottom = mScrubberTop + mScrubber.getHeight();
        return mScrubberLeft - mScrubberPadding < x && x < scrubberRight + mScrubberPadding && mScrubberTop - mScrubberPadding < y
                && y < scrubberBottom + mScrubberPadding;
    }

    private void clampScrubber() {
        int half = mScrubber.getWidth() / 2;
        int max = mProgressBar.right - half;
        int min = mProgressBar.left - half;
        mScrubberLeft = Math.min(max, Math.max(min, mScrubberLeft));
    }

    private int getScrubberTime() {
         return (int)(((long)mScrubberLeft + mScrubber.getWidth() / 2 - mProgressBar.left) * mTotalTime / mProgressBar.width());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        int h = b - t;
        if (!mShowTimes && !mShowScrubber) {
            mProgressBar.set(0, 0, w, h);
        } else {
            int margin = mScrubber.getWidth() / 3;
            if (mShowTimes) {
                margin += mTimeBounds.width();
            }
            int progressY = (h + mScrubberPadding -mProgressBarBg.getHeight()) / 2;
            mScrubberTop = progressY - (mScrubber.getHeight()-mProgressBarBg.getHeight()) / 2;
            mProgressBar.set(getPaddingLeft() + margin, progressY, w - getPaddingRight() - margin, progressY + mProgressBarBg.getHeight());
        }
        update();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // draw progress bars
       // canvas.drawBitmap(mProgressBarBg, null, mProgressBar, null);
        mprogressbgninepatch.draw(canvas, mProgressBar);
        //canvas.drawBitmap(mPlayedBarBg, null, mPlayedBar, null);
        mplayedninepatch.draw(canvas, mPlayedBar);
        // draw scrubber and timers

        if (mShowScrubber) {
            canvas.drawBitmap(mScrubber, mScrubberLeft, mScrubberTop, null);
        }

        if (mShowTimes) {
            canvas.drawText(stringForTime((int)mCurrentTime), mTimeBounds.width() / 2 + getPaddingLeft(), mTimeBounds.height()
                    + mPaddingInPx / 2 + mScrubberPadding + 1, mTimeTextPaint);
            canvas.drawText(stringForTime(mTotalTime), getWidth() - getPaddingRight() - mTimeBounds.width() / 2,
                    mTimeBounds.height() + mPaddingInPx / 2 + mScrubberPadding + 1, mTimeTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mShowScrubber) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (inScrubber(x, y)) {
                    mTouchbegan_x = x;
                    mScrubbing = true;
                    mScrubberCorrection = x - mScrubberLeft;
                    mListener.onScrubbingStart();
                    mScrubber = BitmapFactory.decodeResource(getResources(), R.drawable.ic_video_element_slide_pressed);
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mScrubbing) {
                    mScrubberLeft = x - mScrubberCorrection;
                } else if (mIsSeek) {
                    mScrubberLeft = mScrubberLeft + mDeltaX;
                } else {
                    break;
                }
                clampScrubber();


                mCurrentTime = getScrubberTime();
                int half_width = mScrubber.getWidth()/2;
                mListener.onScrubbingMove((int) mCurrentTime, mScrubberLeft+half_width);

                invalidate();
                requestLayout();
                return true;
            case MotionEvent.ACTION_UP:
                if (mScrubbing) {
                    mListener.onScrubbingEnd((int)getScrubberTime());
                    mScrubbing = false;
                    mScrubber = BitmapFactory.decodeResource(getResources(), R.drawable.ic_video_element_slide_normal);
                    mIsSeek = false;
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private String stringForTime(long millis) {
        if(millis > 0 && millis < 1000)
        {
            millis = 1000;
        }
        int totalSeconds = (int) millis/ 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return String.format("%02d:%02d", minutes, seconds).toString();
        }
    }
    
    // Throws NullPointerException if the input is null.
    public static <T> T checkNotNull(T object) {
        if (object == null)
            throw new NullPointerException();
        return object;
    }

    public void setSeekFlag(boolean flag) {
        mIsSeek = flag;
        if (!mIsSeek) {
            mListener.onScrubbingEnd(mCurrentTime);
        }
    }

    public void setSeekDeltaX(int deltaX, int startPos) {
        mDeltaX = deltaX;
    }
}
