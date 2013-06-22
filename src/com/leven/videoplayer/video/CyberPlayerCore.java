package com.leven.videoplayer.video;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.leven.videoplayer.VideoPlayActivity;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class CyberPlayerCore {
    /**
     * TAG
     */
    public static final String TAG = "CyberPlayerCore";

    /**
     * error code
     */
    public static final int ERROR_NO_INPUTFILE = 301;
    public static final int ERROR_INVALID_INPUTFILE = 302;
    public static final int ERROR_NO_SUPPORTED_CODEC = 303;
    public static final int ERROR_SET_VIDEOMODE = 304;

    /**
     * event code
     */
    private static final int SDL_USEREVENT = 0x8000;
    private static final int CMD_GETCURRPOSITION = (SDL_USEREVENT + 3);
    private static final int CMD_GETDURATION = (SDL_USEREVENT + 4);
    private static final int CMD_GETVIDEOHEIGHT = (SDL_USEREVENT + 5);
    private static final int CMD_GETVIDEOWIDTH = (SDL_USEREVENT + 6);
    private static final int CMD_SETVIDEOSEEKTO = (SDL_USEREVENT + 7);
    private static final int CMD_ISPLAYING = (SDL_USEREVENT + 8);
    private static final int CMD_SETVIDEOSIZE = (SDL_USEREVENT + 9);
    private static final int CMD_PLAYERPAUSE = (SDL_USEREVENT + 10);
    private static final int CMD_PLAYEREXIT = (SDL_USEREVENT + 11);

    /**
     * callback state
     */
    private static final int CURRPOSITON_0 = 0;
    private static final int DURATION_1 = 1;
    private static final int VIDEOWIDTH_2 = 2;
    private static final int VIDEOHEIGH_3 = 3;
    private static final int ISPLAYING_4 = 4;
    private static final int START_5 = 5;
    private static final int ERROR_6 = 6;
    private static final int STOP_7 = 7;
    private static final int CACHE_8 = 8;
    private static final int CACHE_PERCENT_9 = 9;
    private static final int ONSEEK_10 = 10;
    private static final int ONEVENTLOOPPREPARED_11 = 11;

    /**
     * object for sync
     */
    private static final Object SYNC_Duration = new Object();
    private static final Object SYNC_CURRENTPOSTIION = new Object();
    private static final Object SYNC_VIDEOWIDTH = new Object();
    private static final Object SYNC_VIDEOHEIGHT = new Object();
    private static final Object SYNC_ISPLAYING = new Object();
    private static final Object SYNC_SURFACE_RESIZE = new Object();
    private static final Object SYNC_PREPARE = new Object();

    /**
	 * 
	 */
    private static CyberPlayerSurface mCPSurface = null;
    private SurfaceHolder mSurfaceHolder = null;
    private boolean mSurfaceCreated = false;
    private boolean mHasSurface = false;
    /**
     * parameter for playing
     */
    private static ContentValues videoPara = new ContentValues();
    private static final String VIDEO_POSITON = "start-positon";
    private static final String VIDEO_WIDTH = "width";
    private static final String VIDEO_HEIGHT = "height";
    private static final String VIDEO_FORMAT = "format";
    private static final String VIDEO_PATH = "path";
    private static final String UA = "User-Agent";
    private static final String RERERER = "Referer";

    private String mstrVideoPath = null;

    private static EventHandler mEventHandler;
    private PowerManager.WakeLock mWakeLock = null;
    private boolean mScreenOnWhilePlaying;
    private boolean mStayAwake;
    private static final int mWAIT1000MS = 1000;

    // Audio; called by native
    private static Object mbuf = null;
    private static Thread mAudioThread = null;
    private static AudioTrack mAudioTrack = null;

    // This is what SDL runs in. It invokes SDL_main(), eventually
    private Thread mSDLThread = null;

    private static volatile int msiCurrentPosition = 0;
    private static volatile int msiDuration = 0;
    private static volatile int msiVideoWidth = 0;
    private static volatile int msiVideoHeight = 0;
    private static volatile int msiErrorCode = 0;
    private static volatile boolean msbIsPlaying = false;
    private static volatile boolean msbIsStart = false;
    private static volatile boolean msbIsError = false;
    private static volatile boolean msbIsStop = false;
    private static volatile boolean msbIsCache = false;
    private static String mstrUA = null;
    private static String mstrReferer = null;
    private static Context mNativeContext = null;

    public static final int                      START_PLAY_FOR_NEXT_TIME = 1001;
    public static final String                   FORMAT = "rmvb rm";
    public static final String                   REPLAY_POSITON = "position";
    public static final String                   POWER_LOCK = "PLAY_PLUGIN_VIDEO";
    private static HandlerThread                 sProxyThread = null;
    private static Handler                       sHandler = null; 
    private static int                           siCurrPosition = 0;
    private String                               mstrPath = "";
    
    /**
     * Default constructor. Consider using one of the create() methods for
     * synchronously instantiating a MediaPlayer from a Uri or resource.
     * <p>
     * When done with the CyberPlayerCore, you should call {@link #release()},
     * to free the resources. If not released, too many MediaPlayer instances
     * may result in an exception.
     * </p>
     */
    public CyberPlayerCore(Context context) {
        synchronized (CyberPlayerSurface.SYNCOBJECT) {
            mNativeContext = context;
        }
        String strPath = context.getFilesDir() + "/"
                + CyberPlayerSurface.DECODER_LIB;
        String strSysPath = "/system/lib/" + CyberPlayerSurface.DECODER_LIB;
        File file = new File(strPath);
        File sysFile = new File(strSysPath);
        if (file.exists()) {
            System.load(strPath);
        }else if(sysFile.exists()) { 
            System.load(strSysPath);
        } else {
  
            System.loadLibrary("ffmpeg");
        }

        // System.loadLibrary("ffmpeg");
        System.loadLibrary("cyberplayer");
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }
        
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, POWER_LOCK);
                
    }

    /**
     * Sets the SurfaceHolder to use for displaying the video portion of the
     * media. This call is optional. Not calling it when playing back a video
     * will result in only the audio track being played.
     * 
     * @param sh
     *            the SurfaceHolder to use for video display
     */
    public synchronized void setDisplay(CyberPlayerSurface sv) {
        synchronized (CyberPlayerSurface.SYNCOBJECT) {
            mCPSurface = sv;
        }

        if (mCPSurface != null) {
            mSurfaceHolder = mCPSurface.getHolder();
            mSurfaceHolder.addCallback(msfCallback);
        } else {
            mSurfaceHolder = null;
        }
    }

    /**
     * Sets the data source (file-path or http/rtsp URL) to use.
     * 
     * @param path
     *            the path of the file, or the http/rtsp URL of the stream you
     *            want to play
     * @throws IllegalStateException
     *             if it is called in an invalid state
     */
    public void setDataSource(String path) {
        mstrVideoPath = path;
    }

    /**
     * Prepares the player for playback, synchronously.
     * 
     * After setting the datasource and the display surface, you need to either
     * call prepare() or prepareAsync(). For files, it is OK to call prepare(),
     * which blocks until MediaPlayer is ready for playback.
     * 
     * @throws IllegalStateException
     *             if it is called in an invalid state
     */
    public void prepare() throws IOException, IllegalStateException {
        if (mSDLThread == null) {
            videoPara.put(VIDEO_PATH, mstrVideoPath);
            videoPara.put(UA, mstrUA);
            videoPara.put(RERERER, mstrReferer);
            videoPara.put(VIDEO_POSITON, 0);

            mSDLThread = new Thread(new SDLMainThread(videoPara), "SDLThread");
            mSDLThread.start();

            synchronized (SYNC_PREPARE) {
                try {
                    SYNC_PREPARE.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Prepares the player for playback, asynchronously.
     * 
     * After setting the datasource and the display surface, you need to either
     * call prepare() or prepareAsync(). For streams, you should call
     * prepareAsync(), which returns immediately, rather than blocking until
     * enough data has been buffered.
     * 
     * @throws IllegalStateException
     *             if it is called in an invalid state
     */
    public void prepareAsync() throws IllegalStateException {
        if (mSDLThread == null) {
            videoPara.put(VIDEO_PATH, mstrVideoPath);
            videoPara.put(UA, mstrUA);
            videoPara.put(RERERER, mstrReferer);
            videoPara.put(VIDEO_POSITON, 0);

            mSDLThread = new Thread(new SDLMainThread(videoPara), "SDLThread");
            mSDLThread.start();
        }
    }

    /**
     * Starts or resumes playback. If playback had previously been paused,
     * playback will continue from where it was paused. If playback had been
     * stopped, or never started before, playback will start at the beginning.
     * 
     * @throws IllegalStateException
     *             if it is called in an invalid state
     */
    public void start() throws IllegalStateException {
        if (!isPlaying()) {
            onNativeMsgSend(CMD_PLAYERPAUSE, 0);
        }
    }

    /**
     * Stops playback after playback has been stopped or paused.
     * 
     * @throws IllegalStateException
     *             if the internal player engine has not been initialized.
     */
    public void stop() throws IllegalStateException {
        onNativeMsgSend(CMD_PLAYEREXIT, 0);
    }

    /**
     * Pauses playback. Call start() to resume.
     * 
     * @throws IllegalStateException
     *             if the internal player engine has not been initialized.
     */
    public void pause() throws IllegalStateException {
        if (isPlaying()) {
            onNativeMsgSend(CMD_PLAYERPAUSE, 0);
        }
    }

    /**
     * Returns the width of the video.
     * 
     * @return the width of the video, or 0 if there is no video, no display
     *         surface was set, or the width has not been determined yet. The
     *         OnVideoSizeChangedListener can be registered via
     *         {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
     *         to provide a notification when the width is available.
     */
    public int getVideoWidth() {
        onNativeMsgSend(CMD_GETVIDEOWIDTH, 0);

        synchronized (SYNC_VIDEOWIDTH) {
            try {

                SYNC_VIDEOWIDTH.wait(mWAIT1000MS);
                return msiVideoWidth;
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * Returns the height of the video.
     * 
     * @return the height of the video, or 0 if there is no video, no display
     *         surface was set, or the height has not been determined yet. The
     *         OnVideoSizeChangedListener can be registered via
     *         {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
     *         to provide a notification when the height is available.
     */
    public int getVideoHeight() {
        onNativeMsgSend(CMD_GETVIDEOHEIGHT, 0);

        synchronized (SYNC_VIDEOHEIGHT) {
            try {
                SYNC_VIDEOHEIGHT.wait(mWAIT1000MS);
                return msiVideoHeight;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    /**
     * Checks whether the MediaPlayer is playing.
     * 
     * @return true if currently playing, false otherwise
     */
    public boolean isPlaying() {
        onNativeMsgSend(CMD_ISPLAYING, 0);

        synchronized (SYNC_ISPLAYING) {
            try {
                SYNC_ISPLAYING.wait(mWAIT1000MS);
                return msbIsPlaying;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Seeks to specified time position.
     * 
     * @param msec
     *            the offset in milliseconds from the start to seek to
     * @throws IllegalStateException
     *             if the internal player engine has not been initialized
     */
    public void seekTo(int msec) throws IllegalStateException {
        onNativeMsgSend(CMD_SETVIDEOSEEKTO, msec);
    }

    /**
     * Gets the current playback position.
     * 
     * @return the current position in milliseconds
     */
    public int getCurrentPosition() {
        onNativeMsgSend(CMD_GETCURRPOSITION, 0);

        synchronized (SYNC_CURRENTPOSTIION) {
            try {
                SYNC_CURRENTPOSTIION.wait(mWAIT1000MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        siCurrPosition = msiCurrentPosition;
        return msiCurrentPosition;
    }

    /**
     * Gets the duration of the file.
     * 
     * @return the duration in milliseconds
     */
    public int getDuration() {
        onNativeMsgSend(CMD_GETDURATION, 0);

        synchronized (SYNC_Duration) {
            try {

                SYNC_Duration.wait(mWAIT1000MS);
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
        }

        return msiDuration;
    }

    // before call it, we must new Cyberplayer with Context,
    // because this function will call native function for getting the duration
    public static int getDurationForFile(String mediaFile) {
        if (mediaFile != null)
            return nativeGetDuration(mediaFile);
        return -1;
    }

    /**
     * Releases resources associated with this MediaPlayer object. It is
     * considered good practice to call this method when you're done using the
     * MediaPlayer.
     */
    public void release() {
        if (mSDLThread != null) {
            try {
                if (!msbIsStop) {
                    onNativeMsgSend(CMD_PLAYEREXIT, 0);
                }
                mSDLThread.join();
                Log.v(TAG, "SDL thread exit");
            } catch (Exception e) {
                Log.v(TAG, "Problem stopping thread: " + e);
            }

            mSDLThread = null;
            mCPSurface = null;
            //mNativeContext = null;
            mbuf = null;
            mAudioThread = null;
            mAudioTrack = null;
            msiCurrentPosition = 0;
            msiDuration = 0;
            msiVideoWidth = 0;
            msiVideoHeight = 0;
            msiErrorCode = 0;
            msbIsPlaying = false;
            msbIsStart = false;
            msbIsError = false;
            msbIsStop = false;
            //mstrVideoPath = null;
            mstrUA = null;
            mstrReferer = null;
            mSurfaceCreated = false;
            Log.v(TAG, "Finished waiting for SDL thread");
        }
    }

    /**
     * Resets the MediaPlayer to its uninitialized state. After calling this
     * method, you will have to initialize it again by setting the data source
     * and calling prepare().
     */
    public void reset() {
        if (mSDLThread != null) {
            try {
                if (!msbIsStop) {
                    onNativeMsgSend(CMD_PLAYEREXIT, 0);
                }
                mSDLThread.join();
            } catch (Exception e) {
                Log.v(TAG, "Problem stopping thread: " + e);
            }
            mSDLThread = null;
        }

        mbuf = null;
        mAudioThread = null;
        mAudioTrack = null;
        msiCurrentPosition = 0;
        msiDuration = 0;
        msiErrorCode = 0;
        msbIsPlaying = false;
        msbIsStart = false;
        msbIsError = false;
        msbIsStop = false;
        mstrVideoPath = null;
        mSurfaceCreated = false;
        // reset时保留如下信息
        // mNativeContext = null;
        // mstrUA = null;
        // mstrReferer = null;
    }

    /**
     * Interface definition for a callback to be invoked when the media source
     * is ready for playback.
     */
    public interface OnPreparedListener {
        /**
         * Called when the media file is ready for playback.
         * 
         * @param mp
         *            the MediaPlayer that is ready for playback
         */
        void onPrepared(CyberPlayerCore mp);
    }

    /**
     * Register a callback to be invoked when the media source is ready for
     * playback.
     * 
     * @param listener
     *            the callback that will be run
     */
    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    private OnPreparedListener mOnPreparedListener;

    /**
     * Interface definition for a callback to be invoked when playback of a
     * media source has completed.
     */
    public interface OnCompletionListener {
        /**
         * Called when the end of a media source is reached during playback.
         * 
         * @param mp
         *            the MediaPlayer that reached the end of the file
         */
        void onCompletion(CyberPlayerCore mp);
    }

    /**
     * Register a callback to be invoked when the end of a media source has been
     * reached during playback.
     * 
     * @param listener
     *            the callback that will be run
     */
    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    private OnCompletionListener mOnCompletionListener;

    /**
     * Interface definition of a callback to be invoked indicating buffering
     * status of a media resource being streamed over the network.
     */
    public interface OnBufferingUpdateListener {
        /**
         * Called to update status in buffering a media stream.
         * 
         * @param mp
         *            the MediaPlayer the update pertains to
         * @param percent
         *            the percentage (0-100) of the buffer that has been filled
         *            thus far
         */
        void onBufferingUpdate(CyberPlayerCore mp, int percent);
    }

    /**
     * Register a callback to be invoked when the status of a network stream's
     * buffer has changed.
     * 
     * @param listener
     *            the callback that will be run.
     */
    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        mOnBufferingUpdateListener = listener;
    }

    private OnBufferingUpdateListener mOnBufferingUpdateListener;

    /**
     * Interface definition of a callback to be invoked indicating the
     * completion of a seek operation.
     */
    public interface OnSeekCompleteListener {
        /**
         * Called to indicate the completion of a seek operation.
         * 
         * @param mp
         *            the MediaPlayer that issued the seek operation
         */
        public void onSeekComplete(CyberPlayerCore mp);
    }

    /**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     * 
     * @param listener
     *            the callback that will be run
     */
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
        mOnSeekCompleteListener = listener;
    }

    private OnSeekCompleteListener mOnSeekCompleteListener;

    /**
     * Interface definition of a callback to be invoked when the video size is
     * first known or updated
     */
    public interface OnVideoSizeChangedListener {
        /**
         * Called to indicate the video size
         * 
         * @param mp
         *            the MediaPlayer associated with this callback
         * @param width
         *            the width of the video
         * @param height
         *            the height of the video
         */
        public void onVideoSizeChanged(CyberPlayerCore mp, int width, int height);
    }

    /**
     * Register a callback to be invoked when the video size is known or
     * updated.
     * 
     * @param listener
     *            the callback that will be run
     */
    public void setOnVideoSizeChangedListener(
            OnVideoSizeChangedListener listener) {
        mOnVideoSizeChangedListener = listener;
    }

    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;

    /*
     * Do not change these values without updating their counterparts in
     * include/media/mediaplayer.h!
     */
    /**
     * Unspecified media player error.
     * 
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_UNKNOWN = 1;

    /**
     * Media server died. In this case, the application must release the
     * MediaPlayer object and instantiate a new one.
     * 
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_SERVER_DIED = 100;

    /**
     * The video is streamed and its container is not valid for progressive
     * playback i.e the video's index (e.g moov atom) is not at the start of the
     * file.
     * 
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;

    /**
     * Interface definition of a callback to be invoked when there has been an
     * error during an asynchronous operation (other errors will throw
     * exceptions at method call time).
     */
    public interface OnErrorListener {
        /**
         * Called to indicate an error.
         * 
         * @param mp
         *            the MediaPlayer the error pertains to
         * @param what
         *            the type of error that has occurred:
         *            <ul>
         *            <li>{@link #MEDIA_ERROR_UNKNOWN}
         *            <li>{@link #MEDIA_ERROR_SERVER_DIED}
         *            </ul>
         * @param extra
         *            an extra code, specific to the error. Typically
         *            implementation dependant.
         * @return True if the method handled the error, false if it didn't.
         *         Returning false, or not having an OnErrorListener at all,
         *         will cause the OnCompletionListener to be called.
         */
        boolean onError(CyberPlayerCore mp, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an error has happened during an
     * asynchronous operation.
     * 
     * @param listener
     *            the callback that will be run
     */
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    private OnErrorListener mOnErrorListener;

    /*
     * Do not change these values without updating their counterparts in
     * include/media/mediaplayer.h!
     */
    /**
     * Unspecified media player info.
     * 
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_UNKNOWN = 1;

    /**
     * The video is too complex for the decoder: it can't decode frames fast
     * enough. Possibly only the audio plays fine at this stage.
     * 
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;

    /**
     * MediaPlayer is temporarily pausing playback internally in order to buffer
     * more data.
     */
    public static final int MEDIA_INFO_BUFFERING_START = 701;

    /**
     * MediaPlayer is resuming playback after filling buffers.
     */
    public static final int MEDIA_INFO_BUFFERING_END = 702;

    /**
     * Bad interleaving means that a media has been improperly interleaved or
     * not interleaved at all, e.g has all the video samples first then all the
     * audio ones. Video is playing but a lot of disk seeks may be happening.
     * 
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;

    /**
     * The media cannot be seeked (e.g live stream)
     * 
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;

    /**
     * A new set of metadata is available.
     * 
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;

    /**
     * Interface definition of a callback to be invoked to communicate some info
     * and/or warning about the media or its playback.
     */
    public interface OnInfoListener {
        /**
         * Called to indicate an info or a warning.
         * 
         * @param mp
         *            the MediaPlayer the info pertains to.
         * @param what
         *            the type of info or warning.
         *            <ul>
         *            <li>{@link #MEDIA_INFO_UNKNOWN}
         *            <li>{@link #MEDIA_INFO_VIDEO_TRACK_LAGGING}
         *            <li>{@link #MEDIA_INFO_BAD_INTERLEAVING}
         *            <li>{@link #MEDIA_INFO_NOT_SEEKABLE}
         *            <li>{@link #MEDIA_INFO_METADATA_UPDATE}
         *            </ul>
         * @param extra
         *            an extra code, specific to the info. Typically
         *            implementation dependant.
         * @return True if the method handled the info, false if it didn't.
         *         Returning false, or not having an OnErrorListener at all,
         *         will cause the info to be discarded.
         */
        boolean onInfo(CyberPlayerCore mp, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an info/warning is available.
     * 
     * @param listener
     *            the callback that will be run
     */
    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    private OnInfoListener mOnInfoListener;

    /*
     * Do not change these values without updating their counterparts in
     * include/media/mediaplayer.h!
     */
    private static final int MEDIA_NOP = 0; // interface test message
    private static final int MEDIA_PREPARED = 1;
    private static final int MEDIA_PLAYBACK_COMPLETE = 2;
    private static final int MEDIA_BUFFERING_UPDATE = 3;
    private static final int MEDIA_SEEK_COMPLETE = 4;
    private static final int MEDIA_SET_VIDEO_SIZE = 5;
    private static final int MEDIA_ERROR = 100;
    private static final int MEDIA_INFO = 200;

    private class EventHandler extends Handler {
        private CyberPlayerCore mCyberPlayer;

        public EventHandler(CyberPlayerCore cp, Looper looper) {
            super(looper);
            mCyberPlayer = cp;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MEDIA_PREPARED:
                Log.v(TAG, "hanlde: MEDIA_PREPARED start");
                synchronized (SYNC_PREPARE) {
                    SYNC_PREPARE.notify();
                }

                if (mOnPreparedListener != null)
                    mOnPreparedListener.onPrepared(mCyberPlayer);
                stayAwake(true);
                getPreviousPackage(mNativeContext);
                Log.v(TAG, "hanlde: MEDIA_PREPARED end");
                return;
            case MEDIA_PLAYBACK_COMPLETE:
                Log.v(TAG, "hanlde: MEDIA_PLAYBACK_COMPLETE start");
                Log.v(TAG, "handle msg:MEDIA_PLAYBACK_COMPLETE");
                if (mOnCompletionListener != null)
                    Log.v(TAG, "CyberPlayer on complete->listener oncomplete");
                mOnCompletionListener.onCompletion(mCyberPlayer);
                stayAwake(false);
                Log.v(TAG, "hanlde: MEDIA_PLAYBACK_COMPLETE end");
                return;

            case MEDIA_BUFFERING_UPDATE:
                if (mOnBufferingUpdateListener != null)
                    mOnBufferingUpdateListener.onBufferingUpdate(mCyberPlayer,msg.arg1);
                return;

            case MEDIA_SEEK_COMPLETE:
                if (mOnSeekCompleteListener != null)
                    mOnSeekCompleteListener.onSeekComplete(mCyberPlayer);
                return;

            case MEDIA_SET_VIDEO_SIZE:
                if (mOnVideoSizeChangedListener != null)
                    mOnVideoSizeChangedListener.onVideoSizeChanged(mCyberPlayer, msg.arg1, msg.arg2);
                return;

            case MEDIA_ERROR:
                // For PV specific error values (msg.arg2) look in
                // opencore/pvmi/pvmf/include/pvmf_return_codes.h
                Log.e(TAG, "Error (" + msg.arg1 + "," + msg.arg2 + ")");
                boolean error_was_handled = false;
                if (mOnErrorListener != null) {
                    error_was_handled = mOnErrorListener.onError(mCyberPlayer,
                            msg.arg1, msg.arg2);
                }
                if (mOnCompletionListener != null && !error_was_handled) {
                    mOnCompletionListener.onCompletion(mCyberPlayer);
                }
                stayAwake(false);
                return;

            case MEDIA_INFO:
                // For PV specific code values (msg.arg2) look in
                // opencore/pvmi/pvmf/include/pvmf_return_codes.h
                Log.i(TAG, "Info (" + msg.arg1 + "," + msg.arg2 + ")");
                if (mOnInfoListener != null) {
                    mOnInfoListener.onInfo(mCyberPlayer, msg.arg1, msg.arg2);
                }
                // No real default action so far.
                return;

            case MEDIA_NOP: // interface test message - ignore
                break;

            default:
                Log.e(TAG, "Unknown message type " + msg.what);
                return;
            }
        }
    }

    private void stayAwake(boolean awake) {
        if (mWakeLock != null) {
            if (awake && !mWakeLock.isHeld()) {
                mWakeLock.acquire();
            } else if (!awake && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        }
        mStayAwake = awake;
        updateSurfaceScreenOn();
    }

    private void updateSurfaceScreenOn() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
        }
    }

    private SurfaceHolder.Callback msfCallback = new SurfaceHolder.Callback() {

        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                int height) {
            Log.v(TAG, "surfaceChanged() width:" + width + " height:" + height);

            int sdlFormat = 0x85151002; // SDL_PIXELFORMAT_RGB565 by default
            switch (format) {
            case PixelFormat.A_8:
                Log.v(TAG, "pixel format A_8");
                break;
            case PixelFormat.LA_88:
                Log.v(TAG, "pixel format LA_88");
                break;
            case PixelFormat.L_8:
                Log.v(TAG, "pixel format L_8");
                break;
            case PixelFormat.RGBA_4444:
                Log.v(TAG, "pixel format RGBA_4444");
                sdlFormat = 0x85421002; // SDL_PIXELFORMAT_RGBA4444
                break;
            case PixelFormat.RGBA_5551:
                Log.v(TAG, "pixel format RGBA_5551");
                sdlFormat = 0x85441002; // SDL_PIXELFORMAT_RGBA5551
                break;
            case PixelFormat.RGBA_8888:
                Log.v(TAG, "pixel format RGBA_8888");
                sdlFormat = 0x86462004; // SDL_PIXELFORMAT_RGBA8888
                break;
            case PixelFormat.RGBX_8888:
                Log.v(TAG, "pixel format RGBX_8888");
                sdlFormat = 0x86262004; // SDL_PIXELFORMAT_RGBX8888
                break;
            case PixelFormat.RGB_332:
                Log.v(TAG, "pixel format RGB_332");
                sdlFormat = 0x84110801; // SDL_PIXELFORMAT_RGB332
                break;
            case PixelFormat.RGB_565:
                Log.v(TAG, "pixel format RGB_565");
                sdlFormat = 0x85151002; // SDL_PIXELFORMAT_RGB565
                break;
            case PixelFormat.RGB_888:
                Log.v(TAG, "pixel format RGB_888");
                // Not sure this is right, maybe SDL_PIXELFORMAT_RGB24 instead?
                sdlFormat = 0x86161804; // SDL_PIXELFORMAT_RGB888
                break;
            default:
                Log.v(TAG, "pixel format unknown " + format);
                break;
            }

            videoPara.put(VIDEO_WIDTH, width);
            videoPara.put(VIDEO_HEIGHT, height);
            videoPara.put(VIDEO_FORMAT, sdlFormat);

            Log.v(TAG, "videoPara.put value");

            holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);

            synchronized (SYNC_SURFACE_RESIZE) {
                SYNC_SURFACE_RESIZE.notify();
            }
            mSurfaceCreated = true;
        }

        public void surfaceCreated(SurfaceHolder holder) {
            Log.v(TAG, "surfaceCreated");

        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            if (null != mCPSurface) {
                mCPSurface.release();
            }
            
            if (isOverlayedByOther(mNativeContext)) {
                initHandleThread();
            } else {
                exitProxyJudge();
            }
            // BaiduMediaPlayer.this.release();
            Log.v(TAG, "surfaceDestoryed");
        }
    };

    private boolean isOverlayedByOther(Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasks = activityManager.getRunningTasks(3);
        if (null == tasks.get(0) || null == tasks.get(0).baseActivity) { //nothing
            return false;
        }
        
        if (tasks.get(0).baseActivity.flattenToString().contains(context.getPackageName())) { //the play package owner, so return.
            return false;
        }
        
        boolean bFlag = false;
        for (int i=0; i<tasks.size(); i++) { //find play package
            if (tasks.get(i).baseActivity.flattenToString().contains(context.getPackageName())) {
                bFlag = true;
            }
        }
        
        synchronized (this) {
            if (!bFlag) {
                if (tasks.get(0).baseActivity.flattenToString().contains(mstrPath)) { //come back the previous stack
                    return false;
                } else {
                    return true;
                }
            }
        }
        
       return true;  //the stack has play activity
    }
    
    private synchronized void getPreviousPackage(final Context context) {
        mstrPath = null;
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasks = activityManager.getRunningTasks(2);
        if (null == tasks.get(0) || null == tasks.get(0).baseActivity) {
            return;
        }
        
        if (tasks.get(0).baseActivity.flattenToString().contains(context.getPackageName())) {
            mstrPath = tasks.get(1).baseActivity.flattenToString();
        }else {
            mstrPath = tasks.get(0).baseActivity.flattenToString();
        }
        //Log.e(TAG, "tasks.get(0).baseActivity: " + mstrPath);
    }
    
    private boolean doNeedReplay(Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasks = activityManager.getRunningTasks(3); //only read 3 package
        
        if (null == tasks.get(0) || null == tasks.get(0).baseActivity) {
            return false;
        }
        
        if (tasks.get(0).baseActivity.flattenToString().contains(context.getPackageName())) {
            return true;
        }
        
        boolean bFlag = false;
        for (int i=0; i<tasks.size(); i++) {
            if (tasks.get(i).baseActivity.flattenToString().contains(context.getPackageName())) {
                bFlag = true;
            }
        }
        
        synchronized (this) {
            if (!bFlag) {
                if (tasks.get(0).baseActivity.flattenToString().contains(mstrPath)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void initHandleThread() {
        if (null != sProxyThread) {
            return;
        }
        
        sProxyThread = new HandlerThread("startproxyplayer");
        sProxyThread.start();
        sHandler = new Handler(sProxyThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case START_PLAY_FOR_NEXT_TIME:
                    if (!doNeedReplay(mNativeContext)) {
                        sHandler.sendEmptyMessageDelayed(START_PLAY_FOR_NEXT_TIME, 100);
                    } else {
                        sHandler.removeMessages(START_PLAY_FOR_NEXT_TIME);
                        sProxyThread.quit();
                        sProxyThread = null;
                        sHandler = null;
                        
                        Intent intent = new Intent(mNativeContext, VideoPlayActivity.class);
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(mstrVideoPath));
                        intent.putExtra(REPLAY_POSITON, siCurrPosition);
                        mNativeContext.startActivity(intent);
                    }
                    break;
                default:
                    break;
                }
            }
        };
        sHandler.sendEmptyMessage(START_PLAY_FOR_NEXT_TIME);
    }
    
    public static void exitProxyJudge() {
        if (null != sHandler) {
            sHandler.removeMessages(START_PLAY_FOR_NEXT_TIME);
        }

        if (null != sProxyThread) {
            sProxyThread.quit();
            sProxyThread = null;
            sHandler = null;
        }
    }
    
    // called by native
    public static Object audioInit(int sampleRate, boolean is16Bit,
            boolean isStereo, int desiredFrames) {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_CONFIGURATION_STEREO
                : AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT
                : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);

        if (is16Bit) {
            mbuf = new short[desiredFrames * (isStereo ? 2 : 1)];
        } else {
            mbuf = new byte[desiredFrames * (isStereo ? 2 : 1)];
        }

        Log.v(TAG, "SDL audio: wanted " + (isStereo ? "stereo" : "mono") + " "
                + (is16Bit ? "16-bit" : "8-bit") + " "
                + ((float) sampleRate / 1000f) + "kHz, " + desiredFrames
                + " frames buffer");

        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math.max(
                desiredFrames,
                (AudioTrack.getMinBufferSize(sampleRate, channelConfig,
                        audioFormat) + frameSize - 1)
                        / frameSize);

        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                channelConfig, audioFormat, desiredFrames * frameSize,
                AudioTrack.MODE_STREAM);

        audioStartThread();

        Log.v(TAG,
                "SDL audio: got "
                        + ((mAudioTrack.getChannelCount() >= 2) ? "stereo"
                                : "mono")
                        + " "
                        + ((mAudioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit"
                                : "8-bit") + " "
                        + ((float) mAudioTrack.getSampleRate() / 1000f)
                        + "kHz, " + desiredFrames + " frames buffer");

        return mbuf;
    }

    public static void audioStartThread() {
        mAudioThread = new Thread(new Runnable() {
            public void run() {
                mAudioTrack.play();
                nativeRunAudioThread();
            }
        });

        // I'd take REALTIME if I could get it!
        mAudioThread.setPriority(Thread.MAX_PRIORITY);
        mAudioThread.start();
    }

    // native function
    private native void onNativeKeyDown(int keycode);

    private native void onNativeKeyUp(int keycode);

    private native void onNativeTouch(int touchDevId, int pointerFingerId,
            int action, float x, float y, float p);

    private native void onNativeAccel(float x, float y, float z);

    private native void onNativeMsgSend(int iMsgID, int iParam);

    private native void onNativeResize(int x, int y, int format);

    private native void nativeInitpath(int iStartPos, String strPath,
            String strUA, String strReferer);

    private native static void nativeRunAudioThread();

    private native static int nativeGetDuration(String mediaFile);

    // jni call for giving value which you need
    static int ReceiverValue_callback(int iGet, int iIndex) {
        switch (iIndex) {
        case CURRPOSITON_0:
            synchronized (SYNC_CURRENTPOSTIION) {
                msiCurrentPosition = iGet;
                SYNC_CURRENTPOSTIION.notify();
            }
            break;
        case DURATION_1:
            synchronized (SYNC_Duration) {
                msiDuration = iGet;
                SYNC_Duration.notify();
                Log.v(TAG, "DURATION_1, msiDuration: " + msiDuration);
            }
            break;
        case VIDEOWIDTH_2:
            synchronized (SYNC_VIDEOWIDTH) {
                msiVideoWidth = iGet;
                SYNC_VIDEOWIDTH.notify();
            }
            Log.v(TAG, "VIDEOWIDTH_2, msiVideoWidth: " + msiVideoWidth);
            break;
        case VIDEOHEIGH_3:
            synchronized (SYNC_VIDEOHEIGHT) {
                msiVideoHeight = iGet;
                SYNC_VIDEOHEIGHT.notify();
            }
            Log.v(TAG, "VIDEOHEIGH_3, msiVideoHeight: " + msiVideoHeight);
            break;
        case ISPLAYING_4:
            synchronized (SYNC_ISPLAYING) {
                msbIsPlaying = (iGet == 0x00) ? true : false;
                SYNC_ISPLAYING.notify();
            }
            // Log.v(TAG, "ISPLAYING_4, msbIsPlaying: " + msbIsPlaying);
            break;
        case START_5:
            msbIsStart = true;
            msbIsStop = false;
            msbIsError = false;
            if (mEventHandler != null) {
                Message msg = new Message();
                msg.what = MEDIA_PREPARED;
                mEventHandler.sendMessage(msg);
            }
            break;
        case ERROR_6:
            msbIsError = true;
            msbIsStart = false;
            msbIsStop = true;
            msiErrorCode = iGet;
            if (mEventHandler != null) {
                Message msg = new Message();
                msg.what = MEDIA_ERROR;
                msg.arg1 = msiErrorCode;
                mEventHandler.sendMessage(msg);
            }
            break;
        case STOP_7:
            msbIsStart = false;
            msbIsStop = true;
            msbIsError = false;
            if (mEventHandler != null) {
                Log.v(TAG,
                        "CyberPlayerCore on complete, send msg:MEDIA_PLAYBACK_COMPLETE");
                Message msg = new Message();
                msg.what = MEDIA_PLAYBACK_COMPLETE;
                mEventHandler.sendMessage(msg);
            }
            break;
        case CACHE_8:
            msbIsCache = (iGet == 0x01) ? true : false;
            if (mEventHandler != null) {
                Message msg = new Message();
                msg.what = MEDIA_INFO;
                msg.arg1 = msbIsCache ? MEDIA_INFO_BUFFERING_START
                        : MEDIA_INFO_BUFFERING_END;
                mEventHandler.sendMessage(msg);
            }
            break;
        case CACHE_PERCENT_9:
            if (mEventHandler != null) {
                Message msg = new Message();
                msg.what = MEDIA_BUFFERING_UPDATE;
                msg.arg1 = iGet;
                mEventHandler.sendMessage(msg);
            }
            break;
        case ONSEEK_10:
            if (mEventHandler != null) {
                Message msg = new Message();
                msg.what = MEDIA_SEEK_COMPLETE;
                mEventHandler.sendMessage(msg);
            }
            break;
        case ONEVENTLOOPPREPARED_11:
            // if(mOnEventLoopPreparedListener != null){
            // mOnEventLoopPreparedListener.onEventLoopPrepared();
            // }
            break;
        default:
            break;
        }
        return 0;
    }

    // called by native
    public static void audioWriteShortBuffer(short[] buffer) {
        if (buffer == null || mAudioTrack == null)
            return;
        for (int i = 0; i < buffer.length;) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(short)");
                return;
            }
        }
    }

    // called by native
    public static void audioWriteByteBuffer(byte[] buffer) {
        if (buffer == null || mAudioTrack == null)
            return;
        for (int i = 0; i < buffer.length;) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(short)");
                return;
            }
        }
    }

    // called by native
    public static synchronized void audioQuit() {
        if (mAudioThread != null) {
            try {
                mAudioThread.join();
                Log.v(TAG, "audio thread exit");
            } catch (Exception e) {
                Log.v(TAG, "Problem stopping audio thread: " + e);
            }
            mAudioThread = null;
        }

        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }

    // video; called by native
    public static boolean createGLContext(int majorVersion, int minorVersion) {

        if (mCPSurface != null) {
            return mCPSurface.initEGL(majorVersion, minorVersion);
        }
        return false;
    }

    // called by native
    public static void flipBuffers() {
        if (mCPSurface != null) {
            mCPSurface.flipEGL();
        }
    }

    /**
     * Simple nativeInit() runnable
     */
    private class SDLMainThread implements Runnable {
        private ContentValues mVideoPara = null;

        public SDLMainThread(ContentValues videoPara) {
            mVideoPara = videoPara;
        }

        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

            // Runs SDL_main()
            Log.v(TAG, "sdl main thread 1");
            if (null != mVideoPara) {
                if (!mSurfaceCreated) {
                    // wait the surface create finish and start this thread
                    if (CyberPlayerCore.this.mCPSurface != null) {
                        synchronized (SYNC_SURFACE_RESIZE) {
                            try {
                                if (!mHasSurface) {
                                    SYNC_SURFACE_RESIZE.wait();
                                }
                                mHasSurface = false;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                Log.v(TAG, "sdl main thread 2");
                int iStartPos = mVideoPara.getAsInteger(VIDEO_POSITON);
                int iWidth = mVideoPara.getAsInteger(VIDEO_WIDTH);
                int iHeight = mVideoPara.getAsInteger(VIDEO_HEIGHT);
                int iFormat = mVideoPara.getAsInteger(VIDEO_FORMAT);
                String strPath = mVideoPara.getAsString(VIDEO_PATH);
                String strUA = mVideoPara.getAsString(UA);
                String strReferer = mVideoPara.getAsString(RERERER);

                CyberPlayerCore.this.onNativeResize(iWidth, iHeight, iFormat);
                Log.v(TAG, "SDL thread nativeInitpath: " + strPath);
                synchronized (CyberPlayerSurface.SYNCOBJECT) {
                    CyberPlayerSurface.sbIsStarting = false;
                }

                if (null != strPath) {
                    CyberPlayerCore.this.nativeInitpath(iStartPos, strPath,
                            strUA, strReferer);
                } else {
                    if (mEventHandler != null) {
                        Message msg = new Message();
                        msg.what = MEDIA_ERROR;
                        msg.arg1 = ERROR_NO_INPUTFILE;
                        mEventHandler.sendMessage(msg);
                    }
                }
                Log.v(TAG, "sdl main thread 3");
            }
            // release CyberPlayerSurface.mEGLContext from
            // CyberPlayerSurface.mEGLDisplay at current thread
            CyberPlayerCore.this.mCPSurface.releaseEGLContextFromThread();
            Log.v(TAG, "SDLMainThread exit");
        }
    }

    public static void setWebReferer_UserAgent(String strKey, String strValue) {
        if (strKey.equals(RERERER)) {
            mstrReferer = strValue;
        } else if (strKey.equals(UA)) {
            mstrUA = strValue;
        }
    }

    public void setVideoSize(int iWidth, int iHeight) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) mNativeContext
                .getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        d.getMetrics(metrics);
        int videoWidth = iWidth;
        int videoHeight = iHeight;
        int width = 0;
        int height = 0;

        width = videoWidth;
        height = videoHeight;

        int screenSize = (height << 16) | width;
        onNativeMsgSend(CMD_SETVIDEOSIZE, screenSize);
    }

    public void hasSurface() {
        mHasSurface = true;
    }
}
