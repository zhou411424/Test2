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

import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.leven.videoplayer.R;
import com.leven.videoplayer.widget.ProgressView;

/**
 * The playback controller for the Movie Player.
 */
public class MovieControllerOverlay extends FrameLayout implements
        ControllerOverlay, OnClickListener, AnimationListener,
        TimeBar.Listener, GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    private static final String SCREEN_CAPTRUE = "android.intent.action.video.screencapture.start";
    private static final String CAPTRUE_SAVE = "android.intent.action.video.screencapture.save";
    private static final String CAPTRUE_SHARE = "android.intent.action.video.screencapture.share";
    private static final String CAPTRUE_CANCEL = "android.intent.action.video.screencapture.cancel";
    private final static String SCALE_1X = "1X";
    private final static String SCALE_2X = "2X";
    
    private static final String TAG = "MovieControllerOverlay";
    final static private int DELAY_BRIGHTNESS_SET = 2000;
    private float mfBrightnessSize = 0.0f;
    private float mfVolumeNum = 0.0f;
    private AudioManager mAudioManager;
    private GestureDetector mGestureDetector = null;
    private ScaleGestureDetector mScaleGestureDetector;
    private static final float ERROR_MESSAGE_RELATIVE_PADDING = 1.0f / 6;
    private Listener listener;
    private TimeBar timeBar;
    private View mainView;
    private LinearLayout loadingView;
    private RelativeLayout mTitleBarLayout;
    private RelativeLayout mBottomBarLayout;
    private RelativeLayout mGestureLayout;
    private ProgressBar mScaleValueBar;
    private ImageView mScaleView;
    private TextView errorView;
    private TextView mScaleNumView;
    private ImageView playPauseReplayView;
    private LinearLayout mLockScreenBtn;
    private TextView mLockTextview;
    private Rect mLockScreenBtnRect;
    private Rect mSwitchViewPortBtnRect;
    private Rect mPreBtnRect;
    private Rect mNextBtnRect;
    private Handler handler;
    private Handler mMainhandler;
    private Runnable startHidingRunnable;
    private Animation hideAnimation;
    private State state;
    private boolean hidden;
    private boolean canReplay = true;
    private boolean mbIsLockedScreen;
    private Context mContext;
    private int mScreenHeight;
    private int mScreenWidth;
    private final static int SLIDE_DELTA = 25;
    private final static int VIDEO_POS_DELTA = 4;
    private final static float MINI_DELTA = 0.02f;
    private int DEFAULT_BRIGHTNESS = 0;
    private boolean mbIsScale = false; // if scale the video size, will not use
                                       // other gesture
    private boolean mbIsHorizonSeek = false; // if adjust brightness or volume
                                             // will not use other gesture
    private boolean mbIsVerticalSeek = false;
    private boolean mbIsVideoPos;
    private boolean mbIsLockScreenBtnPress;
    private boolean mbIsPreBtnPress;
    private boolean mbIsNextBtnPress;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private View mbtview;
    VideoListener mVideoListener;
    private TextView mSysTimeText = null;
    private TextView mVideoTitleView = null;
    private ImageView mBaterryIcon = null;
    private int miCharging = 0;
    private int miShowInter;
    private MotionEvent mDownEvent;
    private boolean mbDoNeedScaleUp = false;
    private PopupWindow mPopupWindow, mCapturePopup;
    private View mViewInflate, mCaptureInflate;
    private View mbtns[], mCaptureBtns[];
    private ImageView mScreenLockCapture;
    private ImageButton mBtPlayNext;
    private ImageButton mBtPlayPre;
    private ImageButton mBtPausePlay;

    private boolean mbNextEnable = false;
    private boolean mbPrevEnable = false;
    private boolean mbScaleUp = true;
    private View mpop;
    private PopupWindow mTimePopupWindow;
    private View mrootview;
    private int miHistoryPoint;
    private boolean mIsTIME_24 = false;

    public MovieControllerOverlay(Context context, Handler mainHandler,
            View rootView) {
        super(context);
        mContext = context;
        mrootview = rootView;
        initHandler();

        initValues(context, mainHandler);

        initControls(context, rootView);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private boolean is24TimeMode() { // for CL-35369
        return mIsTIME_24;
    }
    
    public void setCanReplay(boolean canReplay) {
        this.canReplay = canReplay;
    }

    public View getView() {
        return this;
    }

    public void hidepreparing() {
        loadingView.setVisibility(View.INVISIBLE);
    }

    public void showPreparing() {
        showMainView(loadingView);
        showBtPlayPause(true);
    }

    public void showPlaying() {
        state = State.PLAYING;
        showMainView(playPauseReplayView);
        showBtPlayPause(true);

    }

    public void showPaused() {
        state = State.PAUSED;
        showMainView(playPauseReplayView);
        showBtPlayPause(false);
    }

    public void showBtPlayPause(boolean bPlaying) {
        if (bPlaying) {
            mBtPausePlay
                    .setBackgroundResource(R.drawable.ic_video_pause_normal);
            mBtPausePlay.setImageResource(R.drawable.buttonpause_in_playview);
        } else {
            mBtPausePlay.setBackgroundResource(R.drawable.ic_video_play_normal);
            mBtPausePlay.setImageResource(R.drawable.buttonplay_in_playview);

        }
    }

    public void showEnded() {
        state = State.ENDED;
        showMainView(playPauseReplayView);
    }

    public void showLoading() {
        state = State.LOADING;
        showMainView(loadingView);
    }

    public boolean isShow() {
        return !hidden;
    }

    public void showBrightnessView(int iValue) {
        if (mGestureLayout.getVisibility() != View.VISIBLE) {
            mGestureLayout.setVisibility(View.VISIBLE);
        }

        mScaleView
                .setBackgroundResource(R.drawable.ic_video_botton_light_normal);

        showScaleView(mGestureLayout, iValue);
    }

    public void setNextVideoEnable(boolean bEnable) {
        mbNextEnable = bEnable;

    }

    public void setPrevVideoEnable(boolean bEnable) {
        mbPrevEnable = bEnable;

    }

    public void resetNextPrevButton() {
        mBtPlayNext.setEnabled(mbNextEnable);
        if (mbNextEnable) {
            mBtPlayNext.setBackgroundResource(R.drawable.ic_video_next_normal);
        } else {
            mBtPlayNext.setBackgroundResource(R.drawable.ic_video_next_disable);
        }

        mBtPlayPre.setEnabled(mbPrevEnable);
        if (mbPrevEnable) {
            mBtPlayPre
                    .setBackgroundResource(R.drawable.ic_video_previous_normal);
        } else {
            mBtPlayPre
                    .setBackgroundResource(R.drawable.ic_video_previous_disable);
        }
    }

    public void showVolumeView(int iValue) {
        if (mGestureLayout.getVisibility() != View.VISIBLE) {
            mGestureLayout.setVisibility(View.VISIBLE);
        }

        if (mfVolumeNum < 0.01f) {
            mScaleView.setBackgroundResource(R.drawable.ic_video_botton_sound_closed_normal);
        } else {
            mScaleView.setBackgroundResource(R.drawable.ic_video_botton_sound_open_normal);
        }

        showScaleView(mGestureLayout, iValue);
    }

    public void showProgressView(boolean bIsForward, int iValue) {

        if (mGestureLayout.getVisibility() != View.VISIBLE) {
            mGestureLayout.setVisibility(View.VISIBLE);
        }

        if (bIsForward) {
            mScaleView
                    .setBackgroundResource(R.drawable.ic_video_botton_fastforward);
        } else {
            mScaleView
                    .setBackgroundResource(R.drawable.ic_video_botton_fastreverse);
        }

        showScaleView(mGestureLayout, iValue);
    }

    public void showErrorMessage(String message) {
        state = State.ERROR;
        int padding = (int) (getMeasuredWidth() * ERROR_MESSAGE_RELATIVE_PADDING);
        errorView.setPadding(padding, 10, padding, 10);
        errorView.setText(message);
        errorView.setBackgroundColor(0xCC000000);
        showMainView(errorView);
    }

    public void showScaleMessage(String message) {
        mScaleNumView.setVisibility(View.VISIBLE);

        if (errorView.getVisibility() != View.INVISIBLE) {
            errorView.setVisibility(View.INVISIBLE);
        }

        if (playPauseReplayView.getVisibility() != View.INVISIBLE) {
            playPauseReplayView.setVisibility(View.INVISIBLE);
        }

        if (loadingView.getVisibility() != View.INVISIBLE) {
            loadingView.setVisibility(View.INVISIBLE);
        }

        if (mGestureLayout.getVisibility() != View.INVISIBLE) {
            mGestureLayout.setVisibility(View.INVISIBLE);
        }

        mScaleNumView.setText(message);
        mainView = mScaleNumView;
        maybeStartHiding();
        requestLayout();
    }

    public void resetTime() {
        timeBar.resetTime();
    }

    public void setTimes(int currentTime, int totalTime) {
        timeBar.setTime(currentTime, totalTime);
    }

    public void hide() {
        // boolean wasHidden = hidden;
        hidden = true;
        mainView = playPauseReplayView;
        mTitleBarLayout.setVisibility(View.INVISIBLE);
        mLockScreenBtn.setVisibility(View.INVISIBLE);
        mBottomBarLayout.setVisibility(View.INVISIBLE);
        mGestureLayout.setVisibility(View.INVISIBLE);
        mScaleNumView.setVisibility(View.INVISIBLE);
        timeBar.setVisibility(View.INVISIBLE);

        setVisibility(View.INVISIBLE);
        setFocusable(true);
        requestFocus();

        if (listener != null/* && wasHidden != hidden */) {
            listener.onHidden();
        }
    }

    public void hideView() {
        playPauseReplayView.setVisibility(View.INVISIBLE);
        loadingView.setVisibility(View.INVISIBLE);
        timeBar.setVisibility(View.INVISIBLE);
        if (mPopupWindow.isShowing())
            mPopupWindow.dismiss();
        setVisibility(View.INVISIBLE);
    }

    public void showControlButtos() {
        timeBar.setVisibility(View.VISIBLE);
        mbtview.setVisibility(View.VISIBLE);
    }

    public void hideControlButtons() {
        timeBar.setVisibility(View.INVISIBLE);
        mbtview.setVisibility(View.INVISIBLE);
    }

    private void showMainView(View view) {
        mainView = view;
        mScaleNumView.setVisibility(mainView == mScaleNumView ? View.VISIBLE
                : View.INVISIBLE);
        errorView.setVisibility(mainView == errorView ? View.VISIBLE
                : View.INVISIBLE);
        /*loadingView.setVisibility(mainView == loadingView ? View.VISIBLE
                : View.INVISIBLE);*/  //the onprepare function will hide. and at this local hide view is too early
        mGestureLayout.setVisibility(mainView == mGestureLayout ? View.VISIBLE
                : View.INVISIBLE);
        playPauseReplayView
                .setVisibility(mainView == playPauseReplayView ? View.VISIBLE
                        : View.INVISIBLE);

        if (mainView == playPauseReplayView) {
            if (mGestureLayout.getVisibility() != View.INVISIBLE) {
                mGestureLayout.setVisibility(View.INVISIBLE);
            }

            if (errorView.getVisibility() != View.INVISIBLE) {
                errorView.setVisibility(View.INVISIBLE);
            }
        }

        show();
    }

    private void showVideoSwitch(View view, boolean isNext) {
        mainView = view;
        /*
         * if (isNext) {
         * playPauseReplayView.setImageResource(R.drawable.ic_video_next); }
         * else {
         * playPauseReplayView.setImageResource(R.drawable.ic_video_previous); }
         */

        if (errorView.getVisibility() != View.INVISIBLE) {
            errorView.setVisibility(View.INVISIBLE);
        }

        if (playPauseReplayView.getVisibility() != View.VISIBLE) {
            playPauseReplayView.setVisibility(View.INVISIBLE);
        }

        if (loadingView.getVisibility() != View.INVISIBLE) {
            loadingView.setVisibility(View.INVISIBLE);
        }

        if (mGestureLayout.getVisibility() != View.INVISIBLE) {
            mGestureLayout.setVisibility(View.INVISIBLE);
        }

        if (mScaleValueBar.getVisibility() != View.VISIBLE) {
            mScaleValueBar.setVisibility(View.VISIBLE);
        }

        maybeStartHiding();
        requestLayout();
    }

    private void showScaleView(View view, int iValue) {
        mainView = view;
        if (errorView.getVisibility() != View.INVISIBLE) {
            errorView.setVisibility(View.INVISIBLE);
        }

        if (mScaleNumView.getVisibility() != View.INVISIBLE) {
            mScaleNumView.setVisibility(View.INVISIBLE);
        }

        if (playPauseReplayView.getVisibility() != View.INVISIBLE) {
            playPauseReplayView.setVisibility(View.INVISIBLE);
        }

        if (loadingView.getVisibility() != View.INVISIBLE) {
            loadingView.setVisibility(View.INVISIBLE);
        }

        if (mGestureLayout.getVisibility() != View.VISIBLE) {
            mGestureLayout.setVisibility(View.VISIBLE);
        }

        if (mScaleValueBar.getVisibility() != View.VISIBLE) {
            mScaleValueBar.setVisibility(View.VISIBLE);
        }
        mScaleValueBar.setProgress(iValue);

        maybeStartHiding();
        requestLayout();
    }

    public void show() {
        if (mbIsLockedScreen) {
            showLockScreenButton();
            return;
        }
        // boolean wasHidden = hidden;
        hidden = false;
        updateViews();
        setVisibility(View.VISIBLE);
        setFocusable(false);
        if (listener != null /* && wasHidden != hidden */) {
            listener.onShown();
        }

        maybeStartHiding();
        updateStatusBar();
        mTitleBarLayout.setVisibility(View.VISIBLE);
        mBottomBarLayout.setVisibility(View.VISIBLE);
        mLockScreenBtn.setVisibility(View.VISIBLE);
        timeBar.setVisibility(View.VISIBLE);
        mbtview.setVisibility(View.VISIBLE);
        mGestureLayout.setVisibility(View.INVISIBLE);
        playPauseReplayView.setVisibility(View.INVISIBLE);
        // show when unlockscreen
        mSysTimeText.setVisibility(View.VISIBLE);
        mVideoTitleView.setVisibility(View.VISIBLE);
        mBaterryIcon.setVisibility(View.VISIBLE);
    }

    private void maybeStartHiding() {
        cancelHiding();
        /*
         * if (state == State.PLAYING) {
         * handler.postDelayed(startHidingRunnable, 2500); }
         */
        handler.postDelayed(startHidingRunnable, 3000);
    }

    private void HidingImmediately() {
        cancelHiding();
        handler.postDelayed(startHidingRunnable, 100);
    }

    private void startHiding() {
        startHideAnimation(timeBar);
        
        startHideAnimation(playPauseReplayView);
        if (mGestureLayout.getVisibility() == View.VISIBLE) {
            startHideAnimation(mGestureLayout);
        }

        if (errorView.getVisibility() == View.VISIBLE) {
            startHideAnimation(errorView);
        }

        if (mLockScreenBtn.getVisibility() == View.VISIBLE) {
            startHideAnimation(mLockScreenBtn);
        }
        if (mbtview.getVisibility() == View.VISIBLE) {
            startHideAnimation(mbtview);
        }
        if (mTitleBarLayout.getVisibility() == View.VISIBLE) {
            startHideAnimation(mTitleBarLayout);
        }
    }

    private void startHideAnimation(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.startAnimation(hideAnimation);
        }
    }

    private void cancelHiding() {
        handler.removeCallbacks(startHidingRunnable);
        timeBar.setAnimation(null);
        playPauseReplayView.setAnimation(null);
    }

    public void onAnimationStart(Animation animation) {
        // Do nothing.
    }

    public void onAnimationRepeat(Animation animation) {
        // Do nothing.
    }

    public void onAnimationEnd(Animation animation) {
        hide();
    }

    public void onClick(View view) {
        if (mbIsLockedScreen) {
            mbIsLockedScreen = !mbIsLockedScreen;
            mScreenLockCapture
                    .setImageResource(R.drawable.ic_video_moreoverflow_normal);
            if (listener != null) {
                listener.onShown();
            }
            show();
            return;
        }
        if (listener != null) {
            if (view == mScaleView || errorView == view) {
                view = playPauseReplayView;
                mGestureLayout.setVisibility(View.INVISIBLE);
                showMainView(playPauseReplayView);
            }
            if (view == playPauseReplayView) {
                if (state == State.ENDED) {
                    if (canReplay) {
                        listener.onReplay();
                    }
                } else if (state == State.PAUSED || state == State.PLAYING) {
                    listener.onPlayPause();
                }
            }
            if (view == mBtPausePlay) {
                listener.onPlayPause();
            }

            if (view == mBtPlayNext) {
                listener.onPlayNext();

            }
            if (view == mBtPlayPre) {
                listener.onPlayPrev();

            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_UP:
            setVolumeByKey(true);
            break;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
            setVolumeByKey(false);
            break;
        default:
            break;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (hidden) {
            show();
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isLockScreenBtnPressed(MotionEvent event) {
        if (null == event) {
            return false;
        }
        return mLockScreenBtnRect.contains((int) event.getX(),
                (int) event.getY());
    }

    private boolean isPreBtnPressed(MotionEvent event) {
        if (mbIsLockedScreen) {
            return false;
        }

        if (null == event) {
            return false;
        }

        boolean bret = mPreBtnRect.contains((int) event.getX(),
                (int) event.getY());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (bret && mBtPlayPre.isEnabled()) {
                mBtPlayPre.setPressed(true);
                mBtPlayPre.playSoundEffect(SoundEffectConstants.CLICK);
                mbIsPreBtnPress = true;
            } else {
                mbIsPreBtnPress = false;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            mBtPlayPre.setPressed(false);
            if (mbIsPreBtnPress && bret) {
                if (mBtPlayPre.isEnabled()) {
                    mTitleBarLayout.setVisibility(View.INVISIBLE);
                    listener.onPlayPrev();
                }

                return mbIsPreBtnPress;
            }

            return mbIsPreBtnPress;
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            mBtPlayNext.setPressed(false);

        }
        return mbIsPreBtnPress;
    }

    private boolean isNextBtnPressed(MotionEvent event) {
        if (mbIsLockedScreen) {
            return false;
        }
        if (null == event) {
            return false;
        }

        boolean bret = mNextBtnRect.contains((int) event.getX(),
                (int) event.getY());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (bret && mBtPlayNext.isEnabled()) {
                mBtPlayNext.setPressed(true);
                mBtPlayPre.playSoundEffect(SoundEffectConstants.CLICK);
                mbIsNextBtnPress = true;
            } else {
                mbIsNextBtnPress = false;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            mBtPlayNext.setPressed(false);
            if (mbIsNextBtnPress && bret) {
                if (mBtPlayNext.isEnabled()) {
                    mTitleBarLayout.setVisibility(View.INVISIBLE);
                    listener.onPlayNext();
                }

                return mbIsNextBtnPress;
            }

            return mbIsNextBtnPress;
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            mBtPlayNext.setPressed(false);

        }
        return mbIsNextBtnPress;
    }

    private void showLockScreenButton() {
        boolean wasHidden = hidden;
        hidden = !hidden;

        mainView = playPauseReplayView;

        mTitleBarLayout.setVisibility(View.VISIBLE);
        mbtview.setVisibility(View.VISIBLE);
        mLockScreenBtn.setVisibility(View.VISIBLE);
        playPauseReplayView.setVisibility(View.INVISIBLE);
        mBottomBarLayout.setVisibility(View.INVISIBLE);
        mGestureLayout.setVisibility(View.INVISIBLE);
        // hide when lockscreen
        mSysTimeText.setVisibility(View.INVISIBLE);
        mVideoTitleView.setVisibility(View.INVISIBLE);
        mBaterryIcon.setVisibility(View.INVISIBLE);

        setVisibility(View.INVISIBLE);
        setFocusable(true);
        requestFocus();
        if (listener != null && wasHidden != hidden) {
            listener.onHidden();
            listener.onShown();
        }

        maybeStartHiding();
    }

    private void showLockScreenStatus() {
        boolean wasHidden = hidden;
        hidden = !hidden;

        mainView = playPauseReplayView;
        mTitleBarLayout.setVisibility(View.VISIBLE);
        mbtview.setVisibility(View.VISIBLE);
        mLockScreenBtn.setVisibility(View.VISIBLE);
        playPauseReplayView.setVisibility(View.INVISIBLE);
        mBottomBarLayout.setVisibility(View.INVISIBLE);
        mGestureLayout.setVisibility(View.INVISIBLE);

        mSysTimeText.setVisibility(View.INVISIBLE);
        mVideoTitleView.setVisibility(View.INVISIBLE);
        mBaterryIcon.setVisibility(View.INVISIBLE);
        setVisibility(View.INVISIBLE);
        setFocusable(true);
        requestFocus();
        if (listener != null && wasHidden != hidden) {
            listener.onHidden();
            listener.onShown();
        }

        maybeStartHiding();
    }

    private void doLockScreen() {
        mTitleBarLayout.setBackgroundColor(Color.TRANSPARENT);
        mbIsLockedScreen = true;
        showLockScreenStatus();
        if(listener != null){
            listener.onlockScreenOrientation(true);
        }
        mScreenLockCapture
                .setImageResource(R.drawable.ic_video_botton_lockscreen_normal);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            if(!mbIsLockedScreen){
                if (super.onTouchEvent(event) || isPreBtnPressed(event)
                        || isNextBtnPressed(event)
                        || !mScaleGestureDetector.onTouchEvent(event)
                        || mGestureDetector.onTouchEvent(event)) {
                    return true;
                }
            }

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (isLockScreenBtnPressed(event)) {
                    doLockScreen();
                    return true;
                }

                if (mbIsVideoPos) {
                    mbIsVideoPos = false;
                    timeBar.setSeekFlag(false);
                }

                if (!hidden) {
                    if (mbIsHorizonSeek || mbIsVerticalSeek || mbIsScale) {
                        mbIsHorizonSeek = false;
                        mbIsVerticalSeek = false;
                        mbIsScale = false;
                        maybeStartHiding();
                        return true;
                    }
                    hide();
                    Log.d(TAG, "hide the controller bar immediately.");
                    return true;
                } else {
                    show();
                    Log.d(TAG, "show the controller bar immediately.");
                    return true;
                }
            }

            if (mbIsLockScreenBtnPress) {
                mbIsLockScreenBtnPress = false;
                mLockScreenBtn.setPressed(false);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return true;
        
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int bw;
        int bh;
        // int y;
        int h = b - t;
        int w = r - l;

        bw = timeBar.getBarHeight();
        bh = bw;
        // y = b - bh;

        int width = (int) (42* getResources().getDisplayMetrics().density);
        timeBar.layout(l, b - timeBar.getPreferredHeight() - width, r, b - width);
        // Needed, otherwise the framework will not re-layout in case only the
        // padding is changed
        timeBar.requestLayout();
        int bottomheight = (int) (54* getResources().getDisplayMetrics().density);
        mbtview.layout(l, b - bottomheight, r, b);
        mbtview.requestLayout();
        // mbtview.setBackgroundResource(R.drawable.background);
        // play pause / next / previous buttons
        int cx = l + w / 2; // center x
        int playbackButtonsCenterline = t + h / 2;
        bw = playPauseReplayView.getMeasuredWidth();
        bh = playPauseReplayView.getMeasuredHeight();
        playPauseReplayView.layout(cx - bw / 2, playbackButtonsCenterline - bh
                / 2, cx + bw / 2, playbackButtonsCenterline + bh / 2);

        // Space available on each side of the error message for the next and
        // previous buttons
        if (mainView != null) {
            layoutCenteredView(mainView, l, t, r, b);
        }

        
        DisplayMetrics dm2 = getResources().getDisplayMetrics();  

        

        int width_const = (int) (54* getResources().getDisplayMetrics().density); 
        int height_const = (int) (70* getResources().getDisplayMetrics().density);
        int middle = dm2.widthPixels/2;
        
        int left_pos     =  middle - (int)(36* getResources().getDisplayMetrics().density);
        int right_pos    =  middle + (int)(36* getResources().getDisplayMetrics().density);
       
        mPreBtnRect = new Rect(left_pos-height_const, dm2.heightPixels -width_const, left_pos, dm2.heightPixels);
        mNextBtnRect = new Rect(right_pos, dm2.heightPixels -width_const, right_pos + height_const, dm2.heightPixels);
        // updateStatusBar();
    }

    private void layoutCenteredView(View view, int l, int t, int r, int b) {
        int cw = view.getMeasuredWidth();
        int ch = view.getMeasuredHeight();
        int cl = (r - l - cw) / 2;
        int ct = (b - t - ch) / 2;
        view.layout(cl, ct, cl + cw, ct + ch);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    private void updateViews() {
        if (hidden) {
            return;
        }

        timeBar.setVisibility(View.VISIBLE);
        mbtview.setVisibility(View.VISIBLE);
        playPauseReplayView
                .setImageResource(state == State.PAUSED ? R.drawable.ic_video_botton_play_normal
                        : R.drawable.ic_video_botton_suspend_normal);

        playPauseReplayView
                .setVisibility((state != State.LOADING && state != State.ERROR && !(state == State.ENDED && !canReplay)) ? View.VISIBLE
                        : View.GONE);
        requestLayout();
    }

    private String stringForTime(int millis) {
        int totalSeconds = (int) millis / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds)
                    .toString();
        } else if (minutes > 0) {
            return String.format("00:%d:%02d", minutes, seconds).toString();
        } else {
            return String.format("00:%02d", seconds).toString();
        }
    }

    // TimeBar listener
    

    public void onScrubbingStart() {
        cancelHiding();
        listener.onSeekStart();
        int height = -mBottomBarLayout.getHeight() - timeBar.getHeight()*4/5;
        mpop = LayoutInflater.from(mContext).inflate(
                R.layout.timebar_scrubber_popup, null);
        mTimePopupWindow = new PopupWindow(mpop, 129, 69);
        if (mTimePopupWindow != null)
            mTimePopupWindow.showAtLocation(mrootview, Gravity.NO_GRAVITY,
                    0, height);

    }

    public void onScrubbingMove(int time, int pos) {
        cancelHiding();
        listener.onSeekMove(time);
        if (mpop != null) {
            TextView tv = (TextView) mpop.findViewById(R.id.scruber_popuptime);
            if (tv != null) {
                tv.setText(stringForTime(time));
                if (mTimePopupWindow != null) {
                    
                    int height = (int) (0 - mContext.getResources().getDimension(R.dimen.timebar_popupwindow_PosTop));//
                    int half_width = mTimePopupWindow.getWidth()/2;
                    mTimePopupWindow.update(mrootview, pos - half_width , height,
                            mTimePopupWindow.getWidth(), mTimePopupWindow.getHeight());
                    Log.d("scrubberDraw","Draw popup is "+pos);
                }
            }
        }

    }

    public void onScrubbingEnd(int time) {
        maybeStartHiding();
        if (mTimePopupWindow != null) {
            mTimePopupWindow.dismiss();
        }

        listener.onSeekEnd(time);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.d(TAG, "onDoubleTap: e: ");

        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // TODO Auto-generated method stub
        timeBar.setSeekFlag(true);
        mbIsVideoPos = false;
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isXYinPosRect(float f,float g){
        int left = mBottomBarLayout.getLeft();
        int right = mBottomBarLayout.getRight();
        int top = mBottomBarLayout.getTop();
        int bottom = mBottomBarLayout.getBottom();
        if( f > left && f < right){
            if(g > top && g < bottom){
                return false;
            }
        }
        return true;
    }
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float fSpeedX,
            float fSpeedY) {
        // TODO Auto-generated method stub
        if (mbIsScale) {
            return true;
        }

        if (null == e1 || null == e2) {
            return true;
        }

        float deltaX = e2.getX() - e1.getX();
        float deltaY = e2.getY() - e1.getY();
        if (isSeekVideoPos(Math.abs(deltaX), Math.abs(deltaY))&&isXYinPosRect(e1.getX(), e1.getY())&&isXYinPosRect(e2.getX(), e2.getY())) {
            int iPoint = (int)e2.getX();
            if (Math.abs(iPoint - miHistoryPoint) < 3) { //same point need not to seek
                return true;
            }
            miHistoryPoint = iPoint;
            setVideoPos(e2, (int) e1.getX(), fSpeedX < 0.0f);
        } else if (isSetBrightness(e1.getX(), e2.getX(), mScreenWidth,
                (int) deltaY)) {
            setBrightnessByScrol(fSpeedY > 0);
        } else if (isSetVolume(e1.getX(), e2.getX(), mScreenWidth,
                (int) deltaY)) {
            setVolumeByScrol(fSpeedY > 0);
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        mbIsScale = false;
        if (mbIsVideoPos) {
            mbIsVideoPos = false;
            timeBar.setSeekFlag(false);
        }

        mbIsHorizonSeek = false;
        mbIsVerticalSeek = false;

        return true;
    }

    class ScaleGestureListener implements
            ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mbIsScale = true;

            if (Math.abs(detector.getCurrentSpan() - detector.getPreviousSpan()) < 5.0f) {
                return false;
            }
            
            if (mbDoNeedScaleUp) { // only need to hands up
                return false;
            }
            mbDoNeedScaleUp = true;
            mbScaleUp = detector.getCurrentSpan() > detector.getPreviousSpan();
            if (null != mVideoListener) {
                mVideoListener.setOnVideoSize(mbScaleUp);
                showScaleMessage(mVideoListener.getOnVideoFactor());
            }

            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mbDoNeedScaleUp = false;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            mScaleNumView.setVisibility(View.INVISIBLE);
        }
    }

    public float getVolum() {
        if (mAudioManager == null)
            mAudioManager = (AudioManager) mContext
                    .getSystemService(Context.AUDIO_SERVICE);

        int index = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        int max = getMaxVolume();
        if (max == 0) {
            max = 1;
        }
        return (float) index / max;
    }

    private int getMaxVolume() {
        if (mAudioManager == null)
            mAudioManager = (AudioManager) mContext
                    .getSystemService(Context.AUDIO_SERVICE);

        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return max;
    }

    public void setVolume(float VolumnPercentage) {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mContext
                    .getSystemService(Context.AUDIO_SERVICE);
        }

        int flags = 0;
        flags &= ~AudioManager.FLAG_PLAY_SOUND;
        flags &= ~AudioManager.FLAG_SHOW_UI;
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                (int) (VolumnPercentage * getMaxVolume()), flags);
    }

    public void setBrightness(float brightPercentage) {
        if (brightPercentage < 0.0f || brightPercentage > 1.0f) {
            return;
        }
        mMainhandler.removeMessages(DELAY_BRIGHTNESS_SET);
        Message msg = mMainhandler.obtainMessage(DELAY_BRIGHTNESS_SET);
        msg.obj = new Float(brightPercentage);
        if (false == mMainhandler.sendMessageDelayed(msg, 10)) {
            Log.w(TAG, "failed to set brightness.");
        }
    }

    void setBrightnessByScrol(boolean bIsAdded) {
        if (bIsAdded) {
            mfBrightnessSize = mfBrightnessSize + MINI_DELTA;
        } else {
            mfBrightnessSize = mfBrightnessSize - MINI_DELTA;
        }

        if (mfBrightnessSize > 1.0f) {
            mfBrightnessSize = 1.0f;
        } else if (mfBrightnessSize < 0.1f) {
            mfBrightnessSize = 0.1f;
        }

        // Log.d(TAG, "landscape.mfBrightnessSize: " + mfBrightnessSize);
        setBrightness(mfBrightnessSize);
        showBrightnessView((int) (mfBrightnessSize * 100));
    }

    void setVolumeByKey(boolean bIsAdded) {
        mfVolumeNum = getVolum();
        if (bIsAdded) {
            AdjustMediaVolum(AudioManager.ADJUST_RAISE);
        } else {
            AdjustMediaVolum(AudioManager.ADJUST_LOWER);
        }

        setVolume(mfVolumeNum);
    }

    private void AdjustMediaVolum(int direction) {
        if (mAudioManager == null)
            mAudioManager = (AudioManager) mContext
                    .getSystemService(Context.AUDIO_SERVICE);

        if (mAudioManager != null) {
            Log.v(TAG, "direction = " + direction);
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    direction, AudioManager.FLAG_SHOW_UI);
        }
    }

    void setVolumeByScrol(boolean bIsAdded) {
        if (bIsAdded) {
            mfVolumeNum = mfVolumeNum + MINI_DELTA;
        } else {
            mfVolumeNum = mfVolumeNum - MINI_DELTA;
        }

        if (mfVolumeNum > 1.0f) {
            mfVolumeNum = 1.0f;
        } else if (mfVolumeNum < 0.0f) {
            mfVolumeNum = 0.0f;
        }

        setVolume(mfVolumeNum);
        showVolumeView((int) (mfVolumeNum * 100));
    }

    void setVideoPos(MotionEvent e2, int iStartPos, boolean mbDirect) {
        if (mbIsHorizonSeek && (!mbIsVerticalSeek)) {
            int iCurrPos = 0;
            float density = 1;
            if(mContext != null)
            {
                density =  mContext.getResources().getDisplayMetrics().density;
            }
            
            if (mbDirect) {
                iCurrPos = (int) (VIDEO_POS_DELTA * density);
            } else {
                iCurrPos = (int) (-VIDEO_POS_DELTA * density);
            }

            timeBar.setSeekDeltaX(iCurrPos, iStartPos);
            timeBar.onTouchEvent(e2);
            if (null != listener) {
                iCurrPos = listener.onGetSeekPos();
            }

            showProgressView(mbDirect, iCurrPos);
            mbIsVideoPos = true;
        }
    }

    boolean isSetVolume(float iPosDown, float iposMov, int iResolution,
            int iDelta) {

        if (Math.abs(iDelta) < SLIDE_DELTA) {
            return false;
        }

        if (((int) iPosDown > 2 * iResolution / 3)
                && ((int) iposMov > 2 * iResolution / 3)) {
            mbIsHorizonSeek = false;
            mbIsVerticalSeek = true;
            return true;
        }

        return false;
    }

    boolean isSetBrightness(float iPosDown, float iposMov, int iResolution,
            int iDelta) {

        if (Math.abs(iDelta) < SLIDE_DELTA) {
            return false;
        }

        if (((int) iPosDown < iResolution / 3)
                && ((int) iposMov < iResolution / 3)) {
            mbIsHorizonSeek = false;
            mbIsVerticalSeek = true;
            return true;
        }

        return false;
    }

    boolean isSeekVideoPos(float fx, float fy) {
        float fSlope = 0.0f;

        if (Math.abs(fx) < SLIDE_DELTA) {
            return false;
        }

        fSlope = fy / fx;
        if (fSlope < 1.0f) {
            mbIsHorizonSeek = true;

            return true;
        }

        return false;
    }

    public void setVideoListener(VideoListener listener) {
        mVideoListener = listener;
    }

    interface VideoListener {
        void setOnVideoSize(boolean addflag);

        String getOnVideoFactor();

        boolean isCharging();

        int getBatteryCap();

        String getVideoTitle();
        
        void setViewPortSize(boolean bSize);
    }

    public interface ControlVideoPlayer {
        void onPlayNextVideo();

        void onPlayPreviousVideo();
    }

    private enum State {
        PLAYING, PAUSED, ENDED, ERROR, LOADING
    }

    public void setVideoStartFlag() {
        if (null != playPauseReplayView) {
            playPauseReplayView
                    .setImageResource(state == State.PAUSED ? R.drawable.ic_video_botton_play_normal
                            : R.drawable.ic_video_botton_suspend_normal);
        }
    }

    private String stringForSysTime() {
        Date timeNow = new Date();
        int hour = timeNow.getHours();
        int minues = timeNow.getMinutes();

        mFormatBuilder.setLength(0);
        if(is24TimeMode())
        {
            return  mFormatter.format(" %2d:%02d", hour, minues).toString();
        }
        else
        {
            if (hour < 12) {
                if(hour == 0)
                {
                    hour = 12;
                }
                return mContext.getResources().getString(R.string.morning)
                        + mFormatter.format(" %2d:%02d", hour, minues).toString();
            } else {
                if(hour != 12)
                {
                    hour -= 12;
                }
                
                return mContext.getResources().getString(R.string.afternoon)
                        + mFormatter.format(" %2d:%02d", hour, minues).toString();
            } 
        }
       
    }

    public void updateBatteryShow() {
        updateBatteryImageStat(mVideoListener.getBatteryCap(),
                mVideoListener.isCharging());
    }

    private void updateStatusBar() {
        if (mVideoListener == null) {
            return;
        }

        mMainhandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                mTitleBarLayout.setVisibility(View.VISIBLE);
                mbtview.setVisibility(View.VISIBLE);
                mSysTimeText.setText(stringForSysTime());
                updateBatteryImageStat(mVideoListener.getBatteryCap(),
                        mVideoListener.isCharging());
                if (mVideoListener.getVideoTitle() != null) {
                    mVideoTitleView.setText(mVideoListener.getVideoTitle());
                    mVideoTitleView.setVisibility(View.VISIBLE);
                    mVideoTitleView.setHorizontallyScrolling(true);
                    mVideoTitleView.requestFocus();
                    mVideoTitleView.setMovementMethod(ScrollingMovementMethod
                            .getInstance());
                } else {
                    mVideoTitleView.setVisibility(View.GONE);
                }
            }
        }, 100);

    }

    private void updateBatteryImageStat(int percentage, boolean bCharging) {
        if (mBaterryIcon == null) {
            return;
        }

        if (bCharging) {
            if (++miCharging % 2 == 0) {
                mBaterryIcon.getDrawable().setLevel(101);
            } else {
                mBaterryIcon.getDrawable().setLevel(102);
            }
            return;
        }
        mBaterryIcon.getDrawable().setLevel(percentage);
    }

    private void initHandler() {
        handler = new Handler();
        startHidingRunnable = new Runnable() {
            public void run() {
                startHiding();
            }
        };
    }

    private void initValues(Context context, Handler mainHandler) {
        try {
            DEFAULT_BRIGHTNESS = Settings.System.getInt(
                    context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
            mfBrightnessSize = DEFAULT_BRIGHTNESS / 255.0f;
        } catch (SettingNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        mfVolumeNum = getVolum();
        mMainhandler = mainHandler;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mScreenHeight = metrics.heightPixels;
        // mScreenWidth = metrics.widthPixels;

        state = State.LOADING;

        mGestureDetector = new GestureDetector(this);
        mScaleGestureDetector = new ScaleGestureDetector(context,
                new ScaleGestureListener());
        mbIsLockedScreen = false;
        configtime();
    }

    public void configtime(){
        ContentResolver cv = mContext.getContentResolver();
        String strTimeFormat = android.provider.Settings.System.getString(cv,
                                           android.provider.Settings.System.TIME_12_24);
        if(strTimeFormat.equals("24")) {
            mIsTIME_24 = true;
        } else {
            mIsTIME_24 = false;
        }
    }
    public void resetBtRect(int orientation){
        if(mContext == null){
            return ;
        }
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        mScreenHeight = metrics.heightPixels;
        mScreenWidth = metrics.widthPixels;
        
        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            float density = mContext.getResources().getDisplayMetrics().density;
            int top = 0;
            int constheight = (int) (density*28);
            int bottom = constheight + top;
            int constwidth  = (int) (40*density);
            mLockScreenBtnRect = new Rect(mScreenWidth - constwidth, top, mScreenWidth,
                    bottom);
        }
        else{
            float density = mContext.getResources().getDisplayMetrics().density;
            int top = 0;
            int constheight = (int) (density*28);
            int bottom = constheight + top;
            int constwidth  = (int) (40*density);
            mLockScreenBtnRect = new Rect(mScreenWidth - constwidth, top, mScreenWidth,bottom);
        }
        
    }
    private void switchViewPort() {
        if (SCALE_1X.contentEquals(mVideoListener.getOnVideoFactor())) {
            mVideoListener.setViewPortSize(true);
            mbScaleUp = true;
        } else if(SCALE_2X.contentEquals(mVideoListener.getOnVideoFactor())) {
            mVideoListener.setViewPortSize(mbScaleUp);
        } else {
            mVideoListener.setViewPortSize(false);
            mbScaleUp = false;
        }
        showScaleMessage(mVideoListener.getOnVideoFactor());

    }
    
    private void initControls(Context context, View rootView) {
        if (null != rootView) {
            mSysTimeText = (TextView) rootView.findViewById(R.id.sys_time);
            mVideoTitleView = (TextView) rootView
                    .findViewById(R.id.video_title);
            mBaterryIcon = (ImageView) rootView
                    .findViewById(R.id.baterry_image);
            mTitleBarLayout = (RelativeLayout) rootView
                    .findViewById(R.id.Status_Container);
            mGestureLayout = (RelativeLayout) rootView
                    .findViewById(R.id.gesture_container);
            mBottomBarLayout = (RelativeLayout) rootView
                    .findViewById(R.id.bottom_bg);

            mScaleNumView = (TextView) rootView.findViewById(R.id.scale_num);
            mScaleView = (ImageView) rootView.findViewById(R.id.gestrue_show);
            mScaleValueBar = (ProgressBar) rootView
                    .findViewById(R.id.scale_show);
            mGestureLayout.setVisibility(View.INVISIBLE);

            mLockScreenBtn = (LinearLayout) rootView
                    .findViewById(R.id.lock_screen_bg);
            mLockScreenBtn.setOnClickListener(this);
            mLockScreenBtn.requestFocus();

            float density = context.getResources().getDisplayMetrics().density;
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            mScreenHeight = metrics.heightPixels;
            mScreenWidth = metrics.widthPixels;
            int top = 0;
            int right = (int) (mScreenWidth);
            int constheight = (int) (density*28);
            int bottom = constheight + top;
            int constwidth  = (int) (40*density);
            mLockScreenBtnRect = new Rect(mScreenWidth - constwidth, top, mScreenWidth,
                    bottom);

            right = (int) (56*density);
            top = mScreenHeight - (int) (54*density);
            bottom = mScreenWidth;
            mSwitchViewPortBtnRect= new Rect(0, top, right, bottom);
            
            initPopupWindow(R.layout.popup_lock_screen);
            InitCaptureSaveCancelPopup(R.layout.popup_save_share);
            mScreenLockCapture = (ImageView) rootView
                    .findViewById(R.id.lock_capture);
            
            mScreenLockCapture.setVisibility(View.VISIBLE);
            mScreenLockCapture.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mbIsLockedScreen) {
                        mbIsLockedScreen = !mbIsLockedScreen;
                        // mLockTextview.setText(R.string.lockscreen);
                        mTitleBarLayout
                                .setBackgroundResource(R.drawable.ic_video_statusbar_backgroud);
                        mScreenLockCapture
                                .setImageResource(R.drawable.ic_video_botton_unlockscreen_normal);
                        if (listener != null) {
                            listener.onShown();
                            listener.onlockScreenOrientation(false);
                        }
                        
                        show();
                        return;
                    } else {

                    }
                }
            });
        }

        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        LayoutParams wrapContent = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        LayoutParams matchParent = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        float fdensity = getResources().getDisplayMetrics().density;
        int iHeight = (int) (fdensity * 78);
        int iWidth = iHeight;

        LayoutParams scaleParams = new LayoutParams(iWidth, iHeight);

        timeBar = new TimeBar(context, this);
        addView(timeBar, wrapContent);

        loadingView = new LinearLayout(context);
        loadingView.setOrientation(LinearLayout.VERTICAL);
        loadingView.setGravity(Gravity.CENTER_HORIZONTAL);
        ProgressBar spinner = new ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
        spinner.setIndeterminate(true);
        loadingView.addView(spinner, wrapContent);
        addView(loadingView, scaleParams);

        playPauseReplayView = new ImageView(context);
        playPauseReplayView
                .setImageResource(R.drawable.ic_video_botton_play_normal);
        playPauseReplayView
                .setBackgroundResource(R.drawable.ic_video_functionbar_background);
        playPauseReplayView.setScaleType(ScaleType.CENTER);
        playPauseReplayView.setFocusable(true);
        playPauseReplayView.setClickable(true);
        playPauseReplayView.setOnClickListener(this);
        addView(playPauseReplayView, scaleParams);

        errorView = new TextView(context);
        errorView.setGravity(Gravity.CENTER);
        errorView.setTextColor(0xFFFFFFFF);
        errorView.setFocusable(true);
        errorView.setClickable(true);
        errorView.setOnClickListener(this);
        addView(errorView, matchParent);

        hideAnimation = AnimationUtils
                .loadAnimation(context, R.anim.player_out);
        hideAnimation.setAnimationListener(this);
        
        mbtview = RelativeLayout.inflate(mContext, R.layout.control_btn_layout,
                null);
        RelativeLayout.LayoutParams paramss = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        mBtPausePlay = (ImageButton) mbtview.findViewById(R.id.bt_playorpause);
        mBtPausePlay.setOnClickListener(this);
        mBtPlayNext = (ImageButton) mbtview.findViewById(R.id.bt_next);
        mBtPlayNext.setOnClickListener(this);
        mBtPlayPre = (ImageButton) mbtview.findViewById(R.id.bt_previous);
        mBtPlayPre.setOnClickListener(this);
        addView(mbtview, paramss);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        setLayoutParams(params);
        hide();
    }

    public boolean isLockedScreen() {
        return mbIsLockedScreen;
    }

    private void initPopupWindow(int resId) {
        LayoutInflater mLayoutInflater = (LayoutInflater) mContext
                .getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
        mViewInflate = mLayoutInflater.inflate(resId, null);
        mPopupWindow = new PopupWindow(mViewInflate, LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        mPopupWindow.setOutsideTouchable(false);
        mPopupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        mPopupWindow.update();
        mPopupWindow.setTouchable(true);
        mPopupWindow.setFocusable(true);
        mbtns = new View[2];

        mbtns[0] = mViewInflate.findViewById(R.id.popup_screen_lock);
        mbtns[0].setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mTitleBarLayout.setBackgroundColor(Color.TRANSPARENT);
                mbIsLockedScreen = true;
                showLockScreenStatus();
                mScreenLockCapture
                        .setImageResource(R.drawable.ic_video_botton_lockscreen_normal);
                mPopupWindow.dismiss();
            }
        });
        mbtns[1] = mViewInflate.findViewById(R.id.popup_screen_capture);
        mbtns[1].setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "send broadcast");
                Intent intent = new Intent();
                intent.setAction(SCREEN_CAPTRUE);
                mContext.sendBroadcast(intent);
                mPopupWindow.dismiss();
                showCaptureSaveCancelPopup(mScreenLockCapture);
            }
        });
    }

    private void showPopupWindow(View view) {
        if (!mPopupWindow.isShowing()) {
            mPopupWindow.showAsDropDown(view);
        }
    }

    private void InitCaptureSaveCancelPopup(int resId) {
        LayoutInflater mLayoutInflater = (LayoutInflater) mContext
                .getSystemService(mContext.LAYOUT_INFLATER_SERVICE);
        mCaptureInflate = mLayoutInflater.inflate(resId, null);
        mCapturePopup = new PopupWindow(mCaptureInflate,
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mCapturePopup.setOutsideTouchable(false);
        mCapturePopup.setAnimationStyle(android.R.style.Animation_Dialog);
        mCapturePopup.update();
        mCapturePopup.setTouchable(true);
        mCapturePopup.setFocusable(true);

        mCaptureBtns = new View[3];

        mCaptureBtns[0] = mCaptureInflate
                .findViewById(R.id.popup_screen_capture_save);
        mCaptureBtns[1] = mCaptureInflate
                .findViewById(R.id.popup_screen_capture_share);
        mCaptureBtns[2] = mCaptureInflate
                .findViewById(R.id.popup_screen_capture_cancel);

        mCaptureBtns[0].setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(CAPTRUE_SAVE);
                mContext.sendBroadcast(intent);
                mCapturePopup.dismiss();
            }
        });
        mCaptureBtns[1].setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(CAPTRUE_SHARE);
                mContext.sendBroadcast(intent);
                mCapturePopup.dismiss();
            }
        });
        mCaptureBtns[2].setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(CAPTRUE_CANCEL);
                mContext.sendBroadcast(intent);
                mCapturePopup.dismiss();
            }
        });
    }

    private void showCaptureSaveCancelPopup(View view) {
        if (!mCapturePopup.isShowing()) {
            mCapturePopup.showAsDropDown(view);
        }
    }

}
