package com.leven.videoplayer.video;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
//import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;

import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberCompletionListener;
import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberErrorListener;
import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberPreparedListener;
import com.leven.videoplayer.video.CyberPlayerSurface.OnCyberSeekCompleteListener;

public interface VideoInterface {

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) ;
    public void setOnCompletionListener(OnCompletionListener l) ;
    public void setOnErrorListener(OnErrorListener l) ;
    public void setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener l);
    
    
    public void setOnCyberPreparedListener(OnCyberPreparedListener l) ;
    public void setOnCyberCompletionListener(OnCyberCompletionListener l) ;
    public void setOnCyberErrorListener(OnCyberErrorListener l) ;
    public void setOnCyberSeekCompleteListener(OnCyberSeekCompleteListener l);
    
    public void setVideoPath(Uri uri);
    
    public void setOnTouchListener(OnTouchListener l);
//    public void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener l);
    public void setSystemUiVisibility(int visibility);
    
    public boolean isSurfaceCreated();    
    public boolean isPreparing();
    public boolean isPlaying();
    public boolean isPaused();
    public boolean startFromPos(int playPosition, boolean showContinueToast);
    public void stopPlayback();
    public int getCurrentPosition();
    public int getDuration();
    public void start();
    public void start(Context context, Uri uri);
    public void pause();
    public void seekTo(int msec,boolean flag);
    public int getSeekPos(boolean flag);
    public int getPausePosition();
    public void setVideoSize(boolean addFlag);
    public void resetScreenSize(boolean Ori_Hor);

    public String getVideoFactor();
    public void configurationChanged();
    public void setViewVisible();
    public void setViewVisible(int visible);
    public boolean requestFocus();
    public int getCurState();
}
