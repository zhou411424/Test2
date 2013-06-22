/*
 * Copyright (C) 2009 The Android Open Source Project
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

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Toast;

import com.leven.videoplayer.DownloadSoActivity;
import com.leven.videoplayer.R;
import com.leven.videoplayer.VideoPlayActivity;
import com.leven.videoplayer.subtitle.SrtSubtitleViewCallback;
import com.leven.videoplayer.subtitle.Subtitle;
import com.leven.videoplayer.subtitle.SubtitleView;
import com.leven.videoplayer.subtitle.parser.ISubtitle.Callback;
import com.leven.videoplayer.subtitle.parser.Item;
import com.leven.videoplayer.subtitle.parser.Parser;
import com.leven.videoplayer.utils.DownloadSoUtils;
import com.leven.videoplayer.utils.LogUtil;
import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberCompletionListener;
import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberErrorListener;
import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberPreparedListener;
//import com.android.internal.telephony.Connection.PostDialState;

public class MoviePlayer implements MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, ControllerOverlay.Listener,
        MovieControllerOverlay.VideoListener {
    @SuppressWarnings("unused")
    public static final String TAG = "MoviePlayer";
    // Copied from MediaPlaybackService in the Music Player app.
    private static final String SERVICECMD = "com.android.music.musicservicecommand";
    private static final String CMDNAME = "command";
    private static final String CMDPAUSE = "pause";
    private static final int UPDATE_SUBTITLE = 100;
    private Context mContext;
    // public final BaiduVideoView mVideoView;
    public final VideoInterface mVideoView;
    private SubtitleView mSubtitleView;
    private Subtitle mSubtitle;

    private SubtitleView.Callback mSubtitleCallback;
    private Uri mUri;

    private String mUriString;
    private final Handler mHandler = new Handler();
    // If the time bar is being dragged.
    private boolean mDragging;
    private boolean mIsPause;
    // If the time bar is visible.
    private boolean mShowing;
    boolean mReceiverRegisted = false;
    private int mBaterryLevel;
    private boolean mbCharing = false;
    private boolean mbPlugIN = false;
    private boolean mbSecond = false;
    private String mstrPath;
    private final MovieControllerOverlay mController;
    private final AudioBecomingNoisyReceiver mAudioBecomingNoisyReceiver;
   
    private VideoPlayActivity mcallbackVideoPlayerAcitivty = null;

    private final Runnable mPlayingChecker = new Runnable() {
        @Override
        public void run() {
            if (mVideoView.isPlaying()) {
                mController.showPlaying();
            } else {
                mHandler.postDelayed(mPlayingChecker, 250);
            }
        }
    };

    private final Runnable mProgressChecker = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            mHandler.postDelayed(mProgressChecker, 1000 - (pos % 1000));
        }
    };

    public MoviePlayer(ViewGroup rootView,
            final VideoPlayActivity VideoPlayActivity, Uri videoUri,
            Bundle savedInstance, boolean canReplay, Handler handler,
            final boolean isPlugIn) {
        mContext = VideoPlayActivity.getApplicationContext();
        mbPlugIN = isPlugIn;
        if (!isPlugIn) {
            mVideoView = (VideoInterface) rootView
                    .findViewById(R.id.video_view);
            mVideoView.setViewVisible();
        } else {
            ViewStub viewStub = (ViewStub) rootView
                    .findViewById(R.id.viewstub_video_plugin_view);
            if (null != viewStub) {
                viewStub.inflate();
            }

            mVideoView = (VideoInterface) rootView
                    .findViewById(R.id.baidu_plugin_video_view);
            if (null == mVideoView) {
                VideoPlayActivity.finish();
            } else {
                mVideoView.setViewVisible();
            }

            mVideoView
                    .setOnCyberPreparedListener(new OnCyberPreparedListener() {
                        @Override
                        public void onPrepared(CyberPlayerSurface mp) {
                            if (null != mController) {
                                mController.hidepreparing();
                                mController.showPlaying();
                            }
                        }
                    });

            mVideoView
                    .setOnCyberCompletionListener(new OnCyberCompletionListener() {
                        @Override
                        public void onCompletion(CyberPlayerSurface mp) {
                            if (mbSecond) {
                                mbSecond = false;
                                mUri = Uri.parse("file://" + mstrPath);
                                mVideoView.start(VideoPlayActivity, mUri);
                            } else {
                                VideoPlayActivity.finish();
                            }
                        }
                    });

            mVideoView.setOnCyberErrorListener(new OnCyberErrorListener() {

                @Override
                public boolean onError(CyberPlayerSurface mp, int what,
                        int extra) {
                    Toast.makeText(mContext, R.string.cannot_play, 1000).show();
                    VideoPlayActivity.finish();
                    return false;
                }
            });

            boolean bIsDownloaded = DownloadSoUtils.hasDownloaded(
                    VideoPlayActivity, DownloadSoActivity.LIBFFMPEG_NAME);
            boolean bIsDetele = DownloadSoUtils.hasDecoderPlugInExists();
            if (bIsDownloaded && bIsDetele) {
                VideoPlayActivity.deleteFile(DownloadSoUtils.STANDARD_PLUG_IN_LIB);
                bIsDownloaded = false;
            } else if (!bIsDownloaded && !bIsDetele) {
                if (DownloadSoUtils.hasDownloadedDecoderPlunIn(VideoPlayActivity)) {
                    bIsDownloaded = true;
                }
            }

            String strPath = videoUri.toString();
            if (!bIsDownloaded
                    && (-1 != strPath.lastIndexOf(".rm") || -1 != strPath
                            .lastIndexOf(".rmvb"))) {
                Toast.makeText(VideoPlayActivity, R.string.file_damaged,
                        Toast.LENGTH_LONG).show();
                VideoPlayActivity.finish();
            }
        }
        
        LogUtil.d(TAG, "MoviePlayer()==>mbPlugIN="+mbPlugIN);
        mSubtitleView = (SubtitleView) rootView.findViewById(R.id.subtitle_view);
        mUri = videoUri;
        LogUtil.d(TAG, "mUri="+mUri);
        mController = new MovieControllerOverlay(mContext, handler, rootView);
        mController.setVideoListener(this);
        rootView.addView(mController.getView());
        rootView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                //mController.show();
                mController.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    LogUtil.d(TAG, "MotionEvent.ACTION_UP");
                    if (isPaused() && !mController.isLockedScreen())
                        playVideo();

                    return false;
                }

                return true;
            }
        });

        mcallbackVideoPlayerAcitivty = VideoPlayActivity;

        mController.setListener(this);
        mController.setCanReplay(canReplay);

        mVideoView.setOnErrorListener(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setVideoPath(mUri);
        mVideoView.setOnPreparedListener(new OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                startSubtitle(mp);
                mController.hidepreparing();
                mVideoView.configurationChanged();
            }
        });

        mAudioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver();
        mAudioBecomingNoisyReceiver.register();

        Intent i = new Intent(SERVICECMD);
        i.putExtra(CMDNAME, CMDPAUSE);
        VideoPlayActivity.sendBroadcast(i);

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {

                Toast.makeText(mContext, R.string.file_damaged,
                        Toast.LENGTH_LONG).show();

                return false;
            }
        });

        if (!isPlugIn) {
            LogUtil.d(TAG, "start video");
            startVideo();
        }
    }
    
    public void onlockScreenOrientation(boolean bLock){
        if(mcallbackVideoPlayerAcitivty != null){
            mcallbackVideoPlayerAcitivty.lockOrientation(bLock);
        }
    }

    public void configTime(){
        mController.configtime();
    }

    public void setUri(Uri uri) {
        mUri = uri;
    }

    public void restartplay(Uri uri,int pos) {
        mUri = uri;
        mVideoView.setVideoPath(uri);
        
        String scheme = mUri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "rtsp".equalsIgnoreCase(scheme)) {
            mController.showLoading();
            mHandler.removeCallbacks(mPlayingChecker);
            mHandler.postDelayed(mPlayingChecker, 250);
        } else {
            mController.showPreparing();
        }
        mVideoView.startFromPos(pos, pos > 0);
        setProgress();
    }
    
    private void showSystemUi(boolean visible) {
//        int flag = visible ? 0 : View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_LOW_PROFILE;
        int flag = visible ? 0 : 8;
        mVideoView.setSystemUiVisibility(flag);
    }

    // Returns a (localized) string for the given duration (in seconds).
    public static String formatDuration(final Context context, int duration) {
        int h = duration / 3600;
        int m = (duration - h * 3600) / 60;
        int s = duration - (h * 3600 + m * 60);
        String durationValue;
        if (h == 0) {
            durationValue = String.format(
                    context.getString(R.string.details_ms), m, s);
        } else {
            durationValue = String.format(
                    context.getString(R.string.details_hms), h, m, s);
        }
        return durationValue;
    }

    public boolean isVideoSurfaceCreated() {
        return mVideoView.isSurfaceCreated();
    }

    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        if (mVideoView != null) {
            mVideoView.pause();
            mController.showPaused();
        }
    }

    public boolean isPlaying() {
        boolean bPaused = false;
        if (mVideoView.isPlaying()) {
            bPaused = true;
        } else {
            bPaused = false;
        }
        return bPaused;
    }

    public boolean isPaused() {
        boolean bPaused = false;
        if (mVideoView.isPaused()) {
            bPaused = true;
        } else {
            bPaused = false;
        }
        return bPaused;

    }

    public void refreshUI() {
        if (mVideoView.isPreparing()) {
            mController.showPreparing();
            return;
        }
        if (mVideoView.isPlaying()) {
            mController.showPlaying();
        } else {
            mController.showPaused();
        }
    }

    public void hideUI() {
        mController.hide();
    }

    public void onResume(int iPos, boolean bShowContinueToast) {
        if (bShowContinueToast) {
            Toast.makeText(mContext, R.string.continue_play, 1000).show();
        }

        mVideoView.startFromPos(iPos, bShowContinueToast);
        mController.showPreparing();
        mHandler.post(mProgressChecker);
    }

    public void onDestroy() {
        if (mSubtitle != null) {
            mSubtitle.release();
            mSubtitle = null;
        }
        mVideoView.stopPlayback();
        mAudioBecomingNoisyReceiver.unregister();
    }

    // This updates the time bar display (if necessary). It is called every
    // second by mProgressChecker and also from places where the time bar needs
    // to be updated immediately.
    private int setProgress() {
        if (mDragging || !mShowing) {
            return 0;
        }
        int position = mVideoView.getCurrentPosition();
        int duration = mVideoView.getDuration();

        if (position < 0)
            position = 0;
        if (duration < 0)
            duration = 0;
        mController.setTimes(position, duration);
        return position;
    }

    public void setUriString(String uri) {
        mUriString = uri;
    }

   
    
    private void startVideo() {
        // For streams that we expect to be slow to start up, show a
        // progress spinner until playback starts.
        String scheme = mUri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "rtsp".equalsIgnoreCase(scheme)) {
            mController.showLoading();
            mHandler.removeCallbacks(mPlayingChecker);
            mHandler.postDelayed(mPlayingChecker, 250);
        } else {
            mController.showPreparing();
        }
        mVideoView.start();
        setProgress();
    }

    private void startSubtitle(MediaPlayer mp) {
        String videoPath = mUriString == null ? mUri.getPath() : mUriString;
        mSubtitle = Subtitle.createSubtitle(videoPath);
        if (mSubtitle == null) {
            return;
        }

        mSubtitle.bindPlayer(mp);
        mSubtitle.registerCallback(new Callback() {

            @Override
            public void onSubtitleChanged(Item[] items) {
                mSubtitleView.drawSubtitle(items);
            }
        });
        mSubtitle.start();
        registerSubtitleViewCallback();
    }

    private void registerSubtitleViewCallback() {
        switch (mSubtitle.getSubtitleType()) {
        case Parser.TYPE_SRT:
            mSubtitleCallback = new SrtSubtitleViewCallback(mContext);
            break;
        case Parser.TYPE_ASS:

            break;
        default:
            break;
        }
        mSubtitleView.registCallback(mSubtitleCallback);
    }

    private void playVideo() {
        mVideoView.start();
        mController.showPlaying();
        setProgress();
    }

    private void pauseVideo() {
        mVideoView.pause();
        mController.showPaused();
    }

    // Below are notifications from VideoView
    @Override
    public boolean onError(MediaPlayer player, int arg1, int arg2) {
        mHandler.removeCallbacksAndMessages(null);
        // VideoView will show an error dialog if we return false, so no need
        // to show more message.
        mController.showErrorMessage("");
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mController.showEnded();
        onCompletion();
    }

    public void onCompletion() {
    }

    // Below are notifications from ControllerOverlay
    @Override
    public void onPlayPause() {
        if (mVideoView.isPlaying()) {
            pauseVideo();
        } else {
            playVideo();
        }
    }

    @Override
    public void onSeekStart() {
        mDragging = true;
        if (mVideoView.isPlaying()) {
            String scheme = mUri.getScheme();
            if (!"http".equalsIgnoreCase(scheme)) {
                mVideoView.pause();
                mController.showPaused();
                mIsPause = true;
            }
        } else {
            mIsPause = false;
        }
    }

    @Override
    public void onSeekMove(int time) {

        String scheme = mUri.getScheme();
        mDragging = true;
        if ("http".equalsIgnoreCase(scheme)) {
            return;
        }

        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            mController.showPaused();
            mIsPause = true;
        }

        if (!(mVideoView instanceof CyberPlayerSurface)) { //rmvb plug-in do not need to seek when moving and will seek by end point
            mVideoView.seekTo(time, false);
        }
    }

    @Override
    public void onSeekEnd(int time) {
        if (mSubtitle != null) {
            mSubtitle.seekTo(time);
        }
        mDragging = false;
        mVideoView.seekTo(time, true);
        int iDuration = mVideoView.getDuration();
        int iPos = setProgress();

        String scheme = mUri.getScheme();
        if ("http".equalsIgnoreCase(scheme)) {
            return;
        }

        if (0 != iDuration && iDuration == iPos) {
            onPause();
            return;
        }

        if (mIsPause) {
            mIsPause = false;
            mVideoView.start();
            mController.showPlaying();
        }
    }

    @Override
    public int onGetSeekPos() {
        if (null != mVideoView) {
            if(mDragging)
               return  mVideoView.getSeekPos(false);
            else
               return mVideoView.getSeekPos(true);
        }

        return 0;
    }

    @Override
    public void onShown() {
        mShowing = true;
        // showSystemUi(true);
        setProgress();
    }

    @Override
    public void onHidden() {
        mShowing = false;
        // showSystemUi(false);
    }

    @Override
    public void onReplay() {
        startVideo();
    }

    // Below are key events passed from MovieActivity.
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // Some headsets will fire off 7-10 events on a single click
        if (event.getRepeatCount() > 0) {
            return isMediaKey(keyCode);
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_HEADSETHOOK:
        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            if (mVideoView.isPlaying()) {
                pauseVideo();
            } else {
                playVideo();
            }
            return true;
        /*case KeyEvent.KEYCODE_MEDIA_PAUSE:
            if (mVideoView.isPlaying()) {
                pauseVideo();
            }
            return true;
        case KeyEvent.KEYCODE_MEDIA_PLAY:
            if (!mVideoView.isPlaying()) {
                playVideo();
            }
            return true;*/
        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
        case KeyEvent.KEYCODE_MEDIA_NEXT:
            // TODO: Handle next / previous accordingly, for now we're
            // just consuming the events.
            return true;
        }
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // CL-32717: We process the volume-key-up event in function onKeyUp() of MovieControllerOverlay.java, so we
        //           should not process volume-key-up event here, otherwise it will cause uncertain volume change.
        return isMediaKey(keyCode);
    }

    private static boolean isMediaKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                /*|| keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE*/;
    }

    // We want to pause when the headset is unplugged.
    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {

        public void register() {
            mContext.registerReceiver(this, new IntentFilter(
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }

        public void unregister() {
            mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mVideoView.isPlaying()) {
                pauseVideo();
            }
        }
    }

    public int getDuration() {
        if (null != mVideoView) {
            return mVideoView.getDuration();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (null != mVideoView) {

            return mVideoView.getPausePosition();
        }
        return 0;
    }

    public void updateBatteryInfo(int level) {
        mBaterryLevel = level;

        if (null != mController && mController.isShow()) {
            mController.updateBatteryShow();
        }
    }

    public void updateChargingStatus(boolean bCharging) {
        mbCharing = bCharging;
    }

    @Override
    public void setOnVideoSize(boolean addflag) {
        if (null != mVideoView) {
            mVideoView.setVideoSize(addflag);
        }
    }

    @Override
    public void setViewPortSize(boolean bSize) {
        if (null != mVideoView) {
            mVideoView.setVideoSize(bSize);
        }
    }

    @Override
    public String getOnVideoFactor() {
        if (null != mVideoView) {
            return mVideoView.getVideoFactor();
        }
        return "";
    }
    
    public void setVideoStartFlag() {
        mController.setVideoStartFlag();
        mController.showPlaying();
    }

    @Override
    public boolean isCharging() {
        return mbCharing;
    }

    @Override
    public int getBatteryCap() {
        return mBaterryLevel;
    }

    @Override
    public String getVideoTitle() {
        // TODO Auto-generated method stub
        if (null != mUri) {
            //CL-33291 resolved irregular URI, whick has no valid path,  lead to java.lang.NullPointerException error.
            if (null == mUri.getPath())
            {
                return null;
            }
            
            File file = new File(mUri.getPath());

            if (null == file)
            {
                return null;
            }
            //CL-33291 end
            
            String strName = file.getName();
            if (null != strName) {
                int idot = strName.lastIndexOf('.');
                if (idot != -1) {
                    strName = strName.substring(0, idot);
                } else {
                    strName = " ";
                }
            }
            return strName;
        }
        return null;
    }

    public boolean isLockedScreen() {
        if (null != mController) {
            return mController.isLockedScreen();
        }
        return false;
    }

    public void  resetBtRect(int oritation){
        mController.resetBtRect(oritation);
    }
    
    @Override
    public void onPlayNext() {
        mcallbackVideoPlayerAcitivty.playNextVideo();
    }

    @Override
    public void onPlayPrev() {
        mcallbackVideoPlayerAcitivty.playPreviousVideo();
    }

    public void setPrevEnable(boolean bEnable) {
        mController.setPrevVideoEnable(bEnable);
        mController.resetNextPrevButton();
    }

    public void setNextEnable(boolean bEnable) {
        mController.setNextVideoEnable(bEnable);
        mController.resetNextPrevButton();
    }

    public void setSomeVideo(final String strPath) {
        mbSecond = true;
        mstrPath = strPath;
    }
}
