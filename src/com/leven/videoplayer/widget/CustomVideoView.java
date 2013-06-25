/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.leven.videoplayer.widget;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.leven.videoplayer.video.VideoInterface;
import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberCompletionListener;
import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberErrorListener;
import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberPreparedListener;
import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberSeekCompleteListener;
import com.leven.videoplayer.R;

public class CustomVideoView extends SurfaceView implements VideoInterface {
    private final static String TAG = "CustomVideoView";
    private final static String SCALE_1X = "1X";
    private final static String SCALE_2X = "2X";
    
    private Context mContext;
    private Uri mUri;
    private int mDuration;
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private static final int STATE_SUSPEND = 6;
    public static final int MIN_BRIGHTNESS = 33;
    public static final int BRIGHTNESS_SEEK_MAX = 67;
    public static final int DEFAULT_BRIGHTNESS = 17;
    public static final float DETLA_VALUE = 0.5f;
    // mCurrentState is a VideoView object's current state.
    // mTargetState is the state that a method caller intends to reach.
    // For instance, regardless the VideoView object's current state,
    // calling pause() intends to bring the object to a target state
    // of STATE_PAUSED.
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;
    // All the stuff we need for playing and showing a video
    private SurfaceHolder mSurfaceHolder = null;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth;
    private int mVideoHeight;
    private int mWidth;
    private int mHeight;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mScreenWidth;
    private int mScreenHeight;
    private float mfScaleMax = 0.0f;
    private OnCompletionListener mOnCompletionListener;
    private OnErrorListener mOnErrorListener;
    private OnSeekCompleteListener mOnSeekCompleteListener;
    private int mCurrentBufferPercentage;
    private int mSeekWhenPrepared; // recording the seek position while
                                   // preparing
    private boolean mCanPause;
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;
    private boolean mbIsSurfaceCreated;
    private int mBaterryLevel = 0;
    private long mstartplaytime = 0;
    private long mendplaytime = 0;
    private String mstrCurrPos = SCALE_1X;
    private int                         mSeekTime;
    // private AudioManager mAudioManager;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;

    public CustomVideoView(Context context) {
        super(context);
        mContext = context;
        initVideoView();
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        mContext = context;
        initVideoView();
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initVideoView();
    }

    public void updateBatteryInfo(int level) {
        mBaterryLevel = level;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //mWidth = mVideoWidth;
       // mHeight = mVideoHeight;
        
        int width = getDefaultSize(mWidth, widthMeasureSpec);
        int height = getDefaultSize(mHeight, heightMeasureSpec);

        if (mMediaPlayer != null) {
            setMeasuredDimension(mWidth, mHeight);
        } else {
            setMeasuredDimension(width, height);
        }
    }

    public int resolveAdjustedSize(int desiredSize, int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        switch (specMode) {
        case MeasureSpec.UNSPECIFIED:
            result = desiredSize;
            break;

        case MeasureSpec.AT_MOST:
            result = Math.min(desiredSize, specSize);
            break;

        case MeasureSpec.EXACTLY:
            // No choice. Do what we are told.
            result = specSize;
            break;
        }
        return result;
    }

    private void initVideoView() {
        mVideoWidth = 0;
        mVideoHeight = 0;
        mWidth = 0;
        mHeight = 0;
        getHolder().addCallback(mSHCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
    }

    public void setVideoPath(Uri uri) {
        mUri = uri;
        mSeekWhenPrepared = 0;
        openVideo();
        requestLayout();
        invalidate();
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public void stopPlayback() {
        setEndplaytime();
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
        }
    }

    private void openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // Tell the music playback service to pause
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        mContext.sendBroadcast(i);

        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            // mMediaPlayer.setOnInfoListener(mInfoListener);
            mDuration = -1;
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(mContext, mUri);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer,
                    MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer,
                    MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            return;
        }
    }

    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();
            if (0 == mWidth || 0 == mHeight) {
                mWidth = mVideoWidth;
                mHeight = mVideoHeight;
            }
            
            getMaxScale();
            // getMinScale();
            requestLayout();
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
            }
        }
    };

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            Log.d(TAG, "onPrepared().");
            mCurrentState = STATE_PREPARED;

            mCanPause = mCanSeekBack = mCanSeekForward = true;
            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }

            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();

            /*
             * mSeekWhenPrepared may be changed after seekTo() call
             */
            int seekToPosition = mSeekWhenPrepared;
            if (seekToPosition != 0) {
                seekTo(seekToPosition,true);
            }

            if (mVideoWidth != 0 && mVideoHeight != 0) {
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (((mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight))) {
                    // We didn't actually change the size (it was already at the
                    // size we need), so we won't get a "surface changed"
                    // callback,
                    // so start the video here instead of in the callback.
                    if (mTargetState == STATE_PLAYING) {
                        start();
                    }
                }

            } else {
                // We don't know the video size yet, but should start anyway.
                // The video size might be reported to us later.
                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;

            // seekTo(0);

            if (mHandler.hasMessages(GO_TO_EXIT_CURRENT_VIDEO)) {
                mHandler.removeMessages(GO_TO_EXIT_CURRENT_VIDEO);
            }
            Message msg = mHandler.obtainMessage(GO_TO_EXIT_CURRENT_VIDEO);
            mHandler.sendMessage(msg);
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int framework_err, int impl_err) {
            Log.e(TAG, "MediaPlayer Error: " + framework_err + "," + impl_err);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;

            /* If an error handler has been supplied, use it and finish. */
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mMediaPlayer, framework_err,
                        impl_err)) {
                    return true;
                }
            }

            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mMediaPlayer);
            }
            return true;
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            mCurrentBufferPercentage = percent;
        }
    };

    /**
     * Register a callback to be invoked when the media file is loaded and ready
     * to go.
     * 
     * @param l
     *            The callback that will be run
     */
    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    /**
     * Register a callback to be invoked when the end of a media file has been
     * reached during playback.
     * 
     * @param l
     *            The callback that will be run
     */
    public void setOnCompletionListener(OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    /**
     * Register a callback to be invoked when an error occurs during playback or
     * setup. If no listener is specified, or if the listener returned false,
     * VideoView will inform the user of any errors.
     * 
     * @param l
     *            The callback that will be run
     */
    public void setOnErrorListener(OnErrorListener l) {
        mOnErrorListener = l;
    }

    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener l) {
        mOnSeekCompleteListener = l;
    }

    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
        public void surfaceChanged(SurfaceHolder holder, int format, int w,
                int h) {
            mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState = (mTargetState == STATE_PLAYING);
            boolean hasValidSize = ((mVideoWidth == w && mVideoHeight == h));

            DisplayMetrics metrics = mContext.getResources()
                    .getDisplayMetrics();
            mScreenHeight = metrics.heightPixels;
            mScreenWidth = metrics.widthPixels;
            if (mMediaPlayer == null && holder.getSurface().isValid()) {
                openVideo();
            }

            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared,true);
                }
                start();
            }
            mbIsSurfaceCreated = true;
        }

        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated().");
            mbIsSurfaceCreated = false;
            mSurfaceHolder = holder;
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed().");
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            if (mCurrentState != STATE_SUSPEND) {
                try {
                    release(true);
                } catch (Exception e) {
                    Log.e(TAG,
                            "exception catched in BaiduVideoView.java:surfaceDestroyed");
                    e.printStackTrace();
                }
            }
        }
    };

    public boolean isSurfaceCreated() {
        return mbIsSurfaceCreated;
    }

    /*
     * release the media player in any state
     */
    private void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState = STATE_IDLE;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mHandler.hasMessages(GO_TO_EXIT_CURRENT_VIDEO)) {
            Log.d(TAG, "cancel exit message.");
            mHandler.removeMessages(GO_TO_EXIT_CURRENT_VIDEO);
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // bluetooth even do begin: let the VideoPlayActivity to do this
        switch (keyCode) {
        case KeyEvent.KEYCODE_MEDIA_STOP:
            return false;
        case KeyEvent.KEYCODE_MEDIA_NEXT:
            return false;
        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            return false;
        } // end

        return super.onKeyDown(keyCode, event);
    }

    boolean mNeedShowContinueToast = false;

    public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            if (mCurrentState == STATE_PAUSED || mNeedShowContinueToast) {
                mNeedShowContinueToast = false;
            }
            mCurrentState = STATE_PLAYING;
            setStartplaytime();
        }
        mTargetState = STATE_PLAYING;
    }

    private void setStartplaytime() {
        mstartplaytime = System.currentTimeMillis();
    }

    private void setEndplaytime() {
        mendplaytime = System.currentTimeMillis();
    }

    public void resume() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    public int getSeekPos(boolean flag) {
        if (isInPlaybackState()) {
            int ipos = flag ? mMediaPlayer.getCurrentPosition() :  mSeekTime;
            if (0 == mMediaPlayer.getDuration()) {
                return 0;
            }
            return ipos * 100 / mMediaPlayer.getDuration();
        }
        return 0;
    }

    public boolean startFromPos(int playPosition, boolean showContinueToast) {
        if (null == mUri) {
            return false;
        }

        /* For local video source */
        String scheme = mUri.getScheme();
        if (!"http".equalsIgnoreCase(scheme)
                && !"rtsp".equalsIgnoreCase(scheme)
                && !"content".equalsIgnoreCase(scheme)) {
            File playFile = new File(mUri.getPath());
            if (!playFile.exists()) {
                Log.i(TAG,
                        "Cannot start because file is not exist: "
                                + mUri.getPath() + ", uri is " + mUri);
                ((Activity) mContext).finish();
                return false;
            }
        }

        mNeedShowContinueToast = showContinueToast;
        Log.d(TAG, "startFromPos():" + playPosition);
        if (playPosition > 0) {
            seekTo(playPosition *1000,true);
        }

        start();
        return true;
    }

    public boolean isPaused() {
        if (isInPlaybackState()) {
            return STATE_PAUSED == mCurrentState;
        }
        return false;
    }

    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    // cache duration as mDuration for faster access
    public int getDuration() {
        if (isInPlaybackState()) {
            if (mDuration > 0) {
                return mDuration;
            }
            mDuration = mMediaPlayer.getDuration();
            return mDuration;
        }
        mDuration = -1;
        return mDuration;
    }

    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }

        return 0;
    }

    public int getPausePosition() {
        if (null != mMediaPlayer) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int msec,boolean flag) {
      
        if (mHandler.hasMessages(GO_TO_EXIT_CURRENT_VIDEO)) {
            mHandler.removeMessages(GO_TO_EXIT_CURRENT_VIDEO);
        }
        if (msec < 0)
            return;
        mSeekTime = msec;
        int iSeek = msec;
        if (mSeekWhenPrepared != iSeek) {
            iSeek = msec;
        }

        if (isInPlaybackState()) {
           if (flag)
               mMediaPlayer.seekTo(iSeek);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = iSeek;
        }
    }

    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null && mCurrentState != STATE_ERROR
                && mCurrentState != STATE_IDLE && mCurrentState != STATE_PREPARING);
    }

    public boolean canPause() {
        return mCanPause;
    }

    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    public int getBaterryPercentage() {
        return mBaterryLevel;
    }

    public void updateViews() {
        Log.v(TAG, "updateViews.");
        invalidate();
    }

    private void adjustConfigurationChanged() {
        adjustScreenResolution();
        getMaxScale();
        calcChangedResolution();
        requestLayout();
    }
    
    private void calcChangedResolution() {
        if (!mstrCurrPos.contentEquals(SCALE_1X)) {
            if (mstrCurrPos.contentEquals(SCALE_2X)) {
                if ((mVideoWidth<<1) < mScreenWidth && (mVideoHeight<<1) < mScreenHeight) { //has 3 scale range
                    mWidth = mVideoWidth<<1;
                    mHeight = mVideoHeight<<1;
                } 
            } else {
                mWidth = (int)(mVideoWidth * mfScaleMax);
                mHeight = (int)(mVideoHeight * mfScaleMax);
            }
        } else {
            if (mVideoWidth > mScreenWidth || mVideoHeight > mScreenHeight) { //origin video resource more than screen resource, so will scale
                mWidth = (int)(mVideoWidth * mfScaleMax);
                mHeight = (int)(mVideoHeight * mfScaleMax);
            } else {
                mWidth = mVideoWidth;
                mHeight = mVideoHeight;
            }
        }
    }
    
    public void configurationChanged() {
        adjustConfigurationChanged();
    }

    final static private int GO_TO_EXIT_CURRENT_VIDEO = 1000;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case GO_TO_EXIT_CURRENT_VIDEO:
                if (mOnCompletionListener != null) {
                    mOnCompletionListener.onCompletion(mMediaPlayer);
                }
                break;
            default:
                break;
            }
        }
    };


    public void resetScreenSize(boolean Ori_Hor){
        DisplayMetrics metrics = mContext.getResources()
                .getDisplayMetrics();
        mScreenHeight = metrics.heightPixels;
        mScreenWidth = metrics.widthPixels;
        if((Ori_Hor && (mScreenHeight >= mScreenWidth) )
                ||(!Ori_Hor && (mScreenHeight < mScreenWidth))){
                int temp = mScreenHeight;
                mScreenHeight = mScreenWidth;
                mScreenWidth  = temp;
        }
        getMaxScale();
    }
    
    private void adjustScreenResolution() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) { 
            if (mScreenWidth > mScreenHeight) {
                int temp = mScreenHeight;
                mScreenHeight = mScreenWidth;
                mScreenWidth = temp;
            }
        } else {
            if(mScreenWidth < mScreenHeight) {
                int temp = mScreenHeight;
                mScreenHeight = mScreenWidth;
                mScreenWidth  = temp;
            }
        }
    }
    
    private void calcResolution(boolean addFlag) {
        if (addFlag) {
            if ((mVideoWidth<<1) < mScreenWidth && (mVideoHeight<<1) < mScreenHeight) { //has 3 scale range
                if (mVideoWidth == mWidth) { //scale 1 -> scale 2
                    mWidth = mVideoWidth<<1;
                    mHeight = mVideoHeight<<1;
                    mstrCurrPos = SCALE_2X;
                } else { //scale 2 -> scale 3(full screen)
                    mWidth = (int)(mVideoWidth * mfScaleMax);
                    mHeight = (int)(mVideoHeight * mfScaleMax);
                    mstrCurrPos = mContext.getResources().getString(R.string.fullscreen);
                }
            } else { //only full screen
                mWidth = (int)(mVideoWidth * mfScaleMax);
                mHeight = (int)(mVideoHeight * mfScaleMax);
                mstrCurrPos = mContext.getResources().getString(R.string.fullscreen);
            }
        } else {
            if ((mVideoWidth<<1) <= mScreenWidth && (mVideoHeight<<1) <= mScreenHeight) { //has 3 scale range
                if (mVideoWidth == mWidth) { //has switch current range
                    return; 
                } else if (mVideoWidth<<1 == mWidth){ //scale 2 -> scale 1
                    mWidth = mVideoWidth;
                    mHeight = mVideoHeight;
                    mstrCurrPos = SCALE_1X;
                } else { //scale 3 -> scale 2
                    mWidth = mVideoWidth<<1;
                    mHeight = mVideoHeight<<1;
                    mstrCurrPos = SCALE_2X;
                }
            } else if (mVideoWidth < mScreenWidth && mVideoHeight < mScreenHeight){//has 2 scale range 
                if (mWidth > mVideoWidth) {
                    mWidth = mVideoWidth;
                    mHeight = mVideoHeight;
                    mstrCurrPos = SCALE_1X;
                } else { //has switch current range
                    return;
                }
            } else {//only full screen, no need to scale
                mWidth = (int)(mVideoWidth * mfScaleMax);
                mHeight = (int)(mVideoHeight * mfScaleMax);
                mstrCurrPos = mContext.getResources().getString(R.string.fullscreen);
            }
        }
    }
    
    public void setVideoSize(boolean addFlag) {
        adjustScreenResolution();
        getMaxScale();
        calcResolution(addFlag);
        requestLayout();
        return;
    }

    public String getVideoFactor() {
        return mstrCurrPos;
    }

    public int getCurState() {
        return mCurrentState;
    }

    public void getMaxScale() {
        if(mVideoHeight*mVideoWidth == 0){
            return;
        }
        if (mScreenWidth * mVideoHeight >= mScreenHeight * mVideoWidth) {
            mfScaleMax = (float) (mScreenHeight) / mVideoHeight;
        } else {
            mfScaleMax = (float) (mScreenWidth) / mVideoWidth;
        }
    }

    @Override
    public void setViewVisible() {
        setVisibility(View.VISIBLE);
    }

    @Override
    public void setOnCyberPreparedListener(OnCyberPreparedListener l) {
    }

    @Override
    public void setOnCyberCompletionListener(OnCyberCompletionListener l) {
    }

    @Override
    public void setOnCyberErrorListener(OnCyberErrorListener l) {
    }

    @Override
    public void setOnCyberSeekCompleteListener(OnCyberSeekCompleteListener l) {
    }

    @Override
    public void start(Context context, Uri uri) {
    }

    @Override
    public boolean isPreparing() {
        return mCurrentState == STATE_PREPARING;
    }

    @Override
    public void setViewVisible(int visible) {
        setVisibility(visible);
        
    }

	@Override
	public void setSystemUiVisibility(int visibility) {
		// TODO Auto-generated method stub
		
	}
}
