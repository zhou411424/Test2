package com.leven.videoplayer.video;

import java.io.IOException;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import com.leven.videoplayer.utils.Utils;
import com.leven.videoplayer.video.VideoInterface;
import com.leven.videoplayer.R;
import com.leven.videoplayer.DownloadSoActivity;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;

public class CyberPlayerSurface extends SurfaceView implements
                                                CyberPlayerCore.OnCompletionListener, 
                                                CyberPlayerCore.OnPreparedListener,
                                                CyberPlayerCore.OnErrorListener, 
                                                CyberPlayerCore.OnBufferingUpdateListener,
                                                CyberPlayerCore.OnSeekCompleteListener,
                                                CyberPlayerCore.OnInfoListener,
                                                CyberPlayerCore.OnVideoSizeChangedListener,
                                                Callback,
                                                VideoInterface{
    public static final String TAG = "CyberPlayerSurface";
    public static final String DECODER_LIB = DownloadSoActivity.LIBFFMPEG_NAME;
    public static final Object SYNCOBJECT = new Object();
    // EGL private objects
    private static EGLContext  mEGLContext;
    private static EGLSurface  mEGLSurface;
    private static EGLDisplay  mEGLDisplay;
    private static EGLConfig   mEGLConfig;
    private static Context     mContext;
    private static int mGLMajor, mGLMinor;
    private CyberPlayerCore corePlayer = null;

    public  final int DECODE_HW = 0;
    public  final int DECODE_SW = 1;
    public  final int DECODE_AUTO = 2;
    public  final int SEEK_PLUG = 2;
    public  final int SEEK_MINUS = 1;
    private int mDecodeMode = DECODE_SW; 
    private boolean mManMadeSet  = false;
    private String mstrVideoPath = null;
    //private Uri mVideoPath = null;
    private static boolean sbIsPrepare = true;
    public static boolean sbIsStarting = false;
    private static int siDuration = 0;
    private int miIsPlus = 0;
    private int miSeek = 0;
    private int miLastSeek = 0;
    private int miCount = 0;
   
    public static final float            DETLA_VALUE = 0.5f; 
    private int                         mWidth;
    private int                         mHeight;   
    private int                         mSurfaceWidth;
    private int                         mSurfaceHeight;
    private int                         mScreenWidth;
    private int                         mScreenHeight;
    private int                         mVideoWidth = 800;
    private int                         mVideoHeight = 480;
    private float                       mfCurrFactor = 1.0f;   
    private float                       mfScaleMax = 1.0f;
    //private float                       mfScaleMin = 0.5f;
   // private float                       FACTOR = 0.017f; 
    private float                       mfAccel_Factor = 0.034f; 
    
    // Startup    
    public CyberPlayerSurface(Context context) {
        super(context);
        init(context);
    }
    
    public CyberPlayerSurface(Context context, AttributeSet Attris) {
        super(context, Attris);
        init(context);
    }

    public CyberPlayerSurface(Context context, AttributeSet Attris, int defStyle) {
        super(context, Attris, defStyle);
        init(context);
    }
    
    private void init(Context context) {
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        initCyberPlayer(context);
        mContext = context;
    }

    // EGL functions
    public boolean initEGL(int majorVersion, int minorVersion) {
        if (CyberPlayerSurface.mEGLDisplay == null) {
            try { 
                        
                EGL10   egl = (EGL10) EGLContext.getEGL();
                EGLDisplay  dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        
                int[] version = new int[2];
                egl.eglInitialize(dpy, version);
        
                int EGL_OPENGL_ES_BIT = 1;
                int EGL_OPENGL_ES2_BIT = 4;
                int renderableType = 0;
                if (majorVersion == 2) {
                    renderableType = EGL_OPENGL_ES2_BIT;
                } else if (majorVersion == 1) {
                    renderableType = EGL_OPENGL_ES_BIT;
                }
                int[] configSpec = {
                        // EGL10.EGL_DEPTH_SIZE, 16,
                        EGL10.EGL_RENDERABLE_TYPE, renderableType,
                        EGL10.EGL_NONE };
                EGLConfig[] configs = new EGLConfig[1]; 
                int[] num_config = new int[1];
                if (!egl.eglChooseConfig(dpy, configSpec, configs, 1, num_config)
                        || num_config[0] == 0) {
                    Log.e(TAG, "No EGL config available");
                    return false;
                }
                    
                EGLConfig config = configs[0];

                CyberPlayerSurface.mEGLDisplay = dpy;
                CyberPlayerSurface.mEGLConfig = config;
                CyberPlayerSurface.mGLMajor = majorVersion;
                CyberPlayerSurface.mGLMinor = minorVersion;

                return createEGLSurface();
                
            } catch (Exception e) {
                Log.v(TAG, e + "");
                for (StackTraceElement s : e.getStackTrace()) {
                    Log.v(TAG, s.toString());
                }
            }
        }else{
            return createEGLSurface();
        }
        return true;
    }

    // EGL buffer flip
    public void flipEGL() {
        try {
            EGL10 egl = (EGL10)EGLContext.getEGL();
            egl.eglWaitNative(EGL10.EGL_CORE_NATIVE_ENGINE, null);
            // drawing here
            egl.eglWaitGL();
            egl.eglSwapBuffers(mEGLDisplay, mEGLSurface);           
        } catch(Exception e) {
            Log.v(TAG, "flipEGL(): " + e);
            for (StackTraceElement s : e.getStackTrace()) {
                Log.v(TAG, s.toString());
            }
        }
    }

    public  boolean createEGLContext() {
        EGL10 egl = (EGL10)EGLContext.getEGL();
        int EGL_CONTEXT_CLIENT_VERSION=0x3098;
        int contextAttrs[] = new int[] { EGL_CONTEXT_CLIENT_VERSION, CyberPlayerSurface.mGLMajor, EGL10.EGL_NONE };
        CyberPlayerSurface.mEGLContext = egl.eglCreateContext(CyberPlayerSurface.mEGLDisplay, CyberPlayerSurface.mEGLConfig, EGL10.EGL_NO_CONTEXT, contextAttrs);
        if (CyberPlayerSurface.mEGLContext == EGL10.EGL_NO_CONTEXT) {
            Log.e("SDL", "Couldn't create context");
            return false;
        }
        return true;
    }

    public boolean createEGLSurface() {
        if (CyberPlayerSurface.mEGLDisplay != null && CyberPlayerSurface.mEGLConfig != null) {
            EGL10 egl = (EGL10)EGLContext.getEGL();
            //if (CyberPlayerSurface.mEGLContext == null) 
            createEGLContext();

            Log.v("SDL", "Creating new EGL Surface");
            
            EGLSurface surface = null;
            try{
                surface = egl.eglCreateWindowSurface(CyberPlayerSurface.mEGLDisplay, CyberPlayerSurface.mEGLConfig, this, null);
            }catch(java.lang.IllegalArgumentException e){
                Log.w("SDL", "get the java.lang.IllegalArgumentException");
                return false;
            }
            
            if (surface == EGL10.EGL_NO_SURFACE) {
                Log.e("SDL", "Couldn't create surface");
                return false;
            }
                
            if (!egl.eglMakeCurrent(CyberPlayerSurface.mEGLDisplay, surface, surface, CyberPlayerSurface.mEGLContext)) {
                Log.e("SDL", "Old EGL Context doesnt work, trying with a new one");
                Log.e("SDL", "eglmakecurrent error,error code=" + egl.eglGetError());
                createEGLContext();
                if (!egl.eglMakeCurrent(CyberPlayerSurface.mEGLDisplay, surface, surface, CyberPlayerSurface.mEGLContext)) {
                    Log.e("SDL", "Failed making EGL Context current");
                    return false;
                }
            }
            CyberPlayerSurface.mEGLSurface = surface;
            return true;
        }
        return false;
    }
    
    public void releaseEGLContextFromThread(){
        if (CyberPlayerSurface.mEGLDisplay != null && CyberPlayerSurface.mEGLConfig != null) {
            Log.v(TAG, "releaseEGLContextFromThread");
            EGL10 egl = (EGL10)EGLContext.getEGL();
            egl.eglMakeCurrent(CyberPlayerSurface.mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            egl.eglDestroyContext(CyberPlayerSurface.mEGLDisplay, CyberPlayerSurface.mEGLContext);
            egl.eglDestroySurface(CyberPlayerSurface.mEGLDisplay, CyberPlayerSurface.mEGLSurface);
        }
        
        mEGLContext = null;
        mEGLSurface = null;
        mEGLDisplay = null;
        mEGLConfig = null;
        mGLMajor = 0;
        mGLMinor = 0;
        corePlayer = null;
        mDecodeMode = DECODE_SW; 
        mManMadeSet  = false;
        //mstrVideoPath = null;
       //mVideoPath = null;
        synchronized (CyberPlayerSurface.SYNCOBJECT) {
            sbIsPrepare = true;
        }
        
        siDuration = 0;
        miIsPlus = 0;
        miLastSeek = 0;
        miCount = 0;
        miSeek = 0;
    }
    
    public void initCyberPlayer(Context context){
        if(corePlayer == null){
            corePlayer = new CyberPlayerCore(context);
            corePlayer.setOnBufferingUpdateListener(this);
            corePlayer.setOnCompletionListener(this);
            corePlayer.setOnErrorListener(this);
            corePlayer.setOnInfoListener(this);
            corePlayer.setOnPreparedListener(this);
            corePlayer.setOnSeekCompleteListener(this);
            corePlayer.setOnVideoSizeChangedListener(this);
        }
    }

    /**
     * Sets the SurfaceHolder to use for displaying the video portion of the media.
     * This call is optional. Not calling it when playing back a video will
     * result in only the audio track being played.
     *
     * @param sh the SurfaceHolder to use for video display
     */
    public void setDisplay(CyberPlayerSurface sv){
        if(corePlayer != null)
            corePlayer.setDisplay(sv);
    }
    /**
     * get current decode mode
     * @return
     */
    public int getCurrentDecodeMode(){
        return mDecodeMode;
    }
    
    /**
     * you must call setDecodeMode before calling setDisplay() and prepare()/prepareAsync()
     * @param mode
     * @return setDecodeMode success return true,otherwise return false
     */
    public boolean setDecodeMode(int mode){
        if(mode == DECODE_HW || mode == DECODE_SW){
            mDecodeMode = mode;
            mManMadeSet = true;
            return true;
        }
        else{
            return false;
        }
    }
    
    /**
     * Sets the data source (file-path or http/rtsp URL) to use.
     *
     * @param path the path of the file, or the http/rtsp URL of the stream you want to play
     * @throws IOException 
     * @throws IllegalArgumentException 
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void setDataSource(String path) throws IllegalArgumentException, IllegalStateException, IOException {
         if(mManMadeSet == false){
             int lastDot = path.lastIndexOf(".");
             if (lastDot < 0){
                 mDecodeMode = DECODE_HW;
             }else{
                 String ext = path.substring(lastDot + 1).toUpperCase();
                 if(ext.equalsIgnoreCase("mp4") || ext.equalsIgnoreCase("3gp"))
                     mDecodeMode = DECODE_HW;
                 else
                     mDecodeMode = DECODE_SW;
             }
         }
         
         mstrVideoPath = path;
         
         if(corePlayer != null)
             corePlayer.setDataSource(path);
    }
    
    /**
     * Prepares the player for playback, synchronously.
     *
     * After setting the datasource and the display surface, you need to either
     * call prepare() or prepareAsync(). For files, it is OK to call prepare(),
     * which blocks until MediaPlayer is ready for playback.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void prepare() throws IOException, IllegalStateException{
        if(corePlayer != null)
            corePlayer.prepare();
    }

    /**
     * Prepares the player for playback, asynchronously.
     *
     * After setting the datasource and the display surface, you need to either
     * call prepare() or prepareAsync(). For streams, you should call prepareAsync(),
     * which returns immediately, rather than blocking until enough data has been
     * buffered.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    public void prepareAsync() throws IllegalStateException{
        if(corePlayer != null)
            corePlayer.prepareAsync();
    }
    
    /**
     * Starts or resumes playback. If playback had previously been paused,
     * playback will continue from where it was paused. If playback had
     * been stopped, or never started before, playback will start at the
     * beginning.
     *
     * @throws IllegalStateException if it is called in an invalid state
     */
    public  void start() throws IllegalStateException {
        if(corePlayer != null)
            corePlayer.start();
    }
    
    /**
     * Stops playback after playback has been stopped or paused.
     *
     * @throws IllegalStateException if the internal player engine has not been
     * initialized.
     */
    public void stop() throws IllegalStateException {
        if(corePlayer != null)
            corePlayer.stop();
    }
    
    
    /**
     * Pauses playback. Call start() to resume.
     *
     * @throws IllegalStateException if the internal player engine has not been
     * initialized.
     */
    public void pause() throws IllegalStateException {
        if(corePlayer != null)
            corePlayer.pause();
    }
    
    /**
     * Returns the width of the video.
     *
     * @return the width of the video, or 0 if there is no video,
     * no display surface was set, or the width has not been determined
     * yet. The OnVideoSizeChangedListener can be registered via
     * {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
     * to provide a notification when the width is available.
     */
    public int getVideoWidth(){
        if (sbIsPrepare) {
            return 800;
        }
        
        if(corePlayer != null)
            return corePlayer.getVideoWidth();
        return 0;
    }
    
    /**
     * Returns the height of the video.
     *
     * @return the height of the video, or 0 if there is no video,
     * no display surface was set, or the height has not been determined
     * yet. The OnVideoSizeChangedListener can be registered via
     * {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
     * to provide a notification when the height is available.
     */
    public int getVideoHeight(){
        if (sbIsPrepare) {
            return 480;
        }
        
        if(corePlayer != null)
            return corePlayer.getVideoHeight();
        return 0;
    }
    
    /**
     * Checks whether the MediaPlayer is playing.
     *
     * @return true if currently playing, false otherwise
     */
    public boolean isPlaying(){
        if (sbIsPrepare) {
            return false;
        }
        
        if(corePlayer != null) {
            return corePlayer.isPlaying();
        }
           
        return false;
    }
    
    /**
     * Seeks to specified time position.
     *
     * @param msec the offset in milliseconds from the start to seek to
     * @throws IllegalStateException if the internal player engine has not been
     * initialized
     */
    public void seekTo(int msec,boolean flag) throws IllegalStateException{
        if (msec == miLastSeek) {
            return;
        }
        
        if (msec > miLastSeek) {
            miIsPlus = SEEK_PLUG;
            miLastSeek = msec;
            miCount = 0;
        } else {
            miIsPlus = SEEK_MINUS;
            miLastSeek = msec;
            miCount = 0;
        } 

        if(corePlayer != null) {
            corePlayer.seekTo(msec/1000);
        }
    }
    
    /**
     * Gets the current playback position.
     *
     * @return the current position in milliseconds
     */
    public int getCurrentPosition(){
        if (sbIsPrepare) {
            return 0;
        }
        
        int iCurr = 0;

        if(corePlayer != null) {
            iCurr = corePlayer.getCurrentPosition()*1000;
        }

        if (miLastSeek != 0 && (SEEK_MINUS == miIsPlus && miLastSeek < iCurr || SEEK_PLUG == miIsPlus && miLastSeek > iCurr)) {
            if (SEEK_MINUS == miIsPlus && miLastSeek < iCurr && miCount > 5) {
                miLastSeek = 0;
                miCount = 0;
                miIsPlus = 0;
            } else {
                iCurr = miLastSeek; 
            }

            miCount++;
        }  

        return iCurr;
    }
    
    /**
     * Gets the duration of the file.
     *
     * @return the duration in milliseconds
     */
    public int getDuration(){
        if (sbIsPrepare) {
            return 1000;
        }
        
        if(corePlayer != null)
            siDuration = corePlayer.getDuration()*1000;
        return siDuration;
    }
    
    
    /**
     * Releases resources associated with this MediaPlayer object.
     * It is considered good practice to call this method when you're
     * done using the MediaPlayer.
     */
    public void release() {
        if (Utils.isM3U8Path(mstrVideoPath)) {
            if ((getCurrentPosition() + 1000) < getDuration()) {
                Utils.recordM3U8History(mContext, mstrVideoPath.hashCode()+"", getCurrentPosition());
            } else {
                Utils.recordM3U8History(mContext, mstrVideoPath.hashCode()+"", 0);
            }
        }
        
        if(corePlayer != null)
            corePlayer.release();
    }
    
    /**
     * Resets the MediaPlayer to its uninitialized state. After calling
     * this method, you will have to initialize it again by setting the
     * data source and calling prepare().
     */
    public void reset() {
        if(corePlayer != null)
            corePlayer.reset();
    }
    
    /**
     * Interface definition for a callback to be invoked when the media
     * source is ready for playback.
     */
    public interface OnCyberPreparedListener
    {
        /**
         * Called when the media file is ready for playback.
         *
         * @param mp the MediaPlayer that is ready for playback
         */
        void onPrepared(CyberPlayerSurface mp);
    }

    /**
     * Register a callback to be invoked when the media source is ready
     * for playback.
     *
     * @param listener the callback that will be run
     */
    public void setOnCyberPreparedListener(OnCyberPreparedListener listener)
    {
        mOnPreparedListener = listener;
    }

    private OnCyberPreparedListener mOnPreparedListener;
    
    
    
    /**
     * Interface definition for a callback to be invoked when playback of
     * a media source has completed.
     */
    public interface OnCyberCompletionListener
    {
        /**
         * Called when the end of a media source is reached during playback.
         *
         * @param mp the MediaPlayer that reached the end of the file
         */
        void onCompletion(CyberPlayerSurface mp);
    }

    /**
     * Register a callback to be invoked when the end of a media source
     * has been reached during playback.
     *
     * @param listener the callback that will be run
     */
    public void setOnCyberCompletionListener(OnCyberCompletionListener listener)
    {
        mOnCompletionListener = listener;
    }

    private OnCyberCompletionListener mOnCompletionListener;
    
    
    /**
     * Interface definition of a callback to be invoked indicating buffering
     * status of a media resource being streamed over the network.
     */
    public interface OnCyberBufferingUpdateListener
    {
        /**
         * Called to update status in buffering a media stream.
         *
         * @param mp      the MediaPlayer the update pertains to
         * @param percent the percentage (0-100) of the buffer
         *                that has been filled thus far
         */
        void onBufferingUpdate(CyberPlayerSurface mp, int percent);
    }

    /**
     * Register a callback to be invoked when the status of a network
     * stream's buffer has changed.
     *
     * @param listener the callback that will be run.
     */
    public void setOnCyberBufferingUpdateListener(OnCyberBufferingUpdateListener listener)
    {
        mOnBufferingUpdateListener = listener;
    }

    private OnCyberBufferingUpdateListener mOnBufferingUpdateListener;
    
    
    /**
     * Interface definition of a callback to be invoked indicating
     * the completion of a seek operation.
     */
    public interface OnCyberSeekCompleteListener
    {
        /**
         * Called to indicate the completion of a seek operation.
         *
         * @param mp the MediaPlayer that issued the seek operation
         */
        public void onSeekComplete(CyberPlayerSurface mp);
    }

    /**
     * Register a callback to be invoked when a seek operation has been
     * completed.
     *
     * @param listener the callback that will be run
     */
    public void setOnCyberSeekCompleteListener(OnCyberSeekCompleteListener listener)
    {
        mOnSeekCompleteListener = listener;
    }

    private OnCyberSeekCompleteListener mOnSeekCompleteListener;
    
    
    /**
     * Interface definition of a callback to be invoked when the
     * video size is first known or updated
     */
    public interface OnCyberVideoSizeChangedListener
    {
        /**
         * Called to indicate the video size
         *
         * @param mp        the MediaPlayer associated with this callback
         * @param width     the width of the video
         * @param height    the height of the video
         */
         public void onVideoSizeChanged(CyberPlayerSurface mp, int width, int height);
    }

    /**
     * Register a callback to be invoked when the video size is
     * known or updated.
     *
     * @param listener the callback that will be run
     */
    public void setOnCyberVideoSizeChangedListener(OnCyberVideoSizeChangedListener listener)
    {
        mOnVideoSizeChangedListener = listener;
    }

    private OnCyberVideoSizeChangedListener mOnVideoSizeChangedListener;
    
    
    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
    /** Unspecified media player error.
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_UNKNOWN = 1;

    /** Media server died. In this case, the application must release the
     * MediaPlayer object and instantiate a new one.
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_SERVER_DIED = 100;

    /** The video is streamed and its container is not valid for progressive
     * playback i.e the video's index (e.g moov atom) is not at the start of the
     * file.
     * @see android.media.MediaPlayer.OnErrorListener
     */
    public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
    
    public static final int MEDIA_ERROR_NO_INPUTFILE            = 301;
    public static final int MEDIA_ERROR_INVALID_INPUTFILE   = 302;
    public static final int MEDIA_ERROR_NO_SUPPORTED_CODEC  = 303;
    public static final int MEDIA_ERROR_SET_VIDEOMODE           = 304;

    /**
     * Interface definition of a callback to be invoked when there
     * has been an error during an asynchronous operation (other errors
     * will throw exceptions at method call time).
     */
    public interface OnCyberErrorListener
    {
        /**
         * Called to indicate an error.
         *
         * @param mp      the MediaPlayer the error pertains to
         * @param what    the type of error that has occurred:
         * <ul>
         * <li>{@link #MEDIA_ERROR_UNKNOWN}
         * <li>{@link #MEDIA_ERROR_SERVER_DIED}
         * </ul>
         * @param extra an extra code, specific to the error. Typically
         * implementation dependant.
         * @return True if the method handled the error, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the OnCompletionListener to be called.
         */
        boolean onError(CyberPlayerSurface mp, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an error has happened
     * during an asynchronous operation.
     *
     * @param listener the callback that will be run
     */
    public void setOnCyberErrorListener(OnCyberErrorListener listener)
    {
        mOnErrorListener = listener;
    }

    private OnCyberErrorListener mOnErrorListener;
    
    
    /* Do not change these values without updating their counterparts
     * in include/media/mediaplayer.h!
     */
    /** Unspecified media player info.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_UNKNOWN = 1;

    /** The video is too complex for the decoder: it can't decode frames fast
     *  enough. Possibly only the audio plays fine at this stage.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;

    /** MediaPlayer is temporarily pausing playback internally in order to
     * buffer more data.
     */
    public static final int MEDIA_INFO_BUFFERING_START = 701;

    /** MediaPlayer is resuming playback after filling buffers.
     */
    public static final int MEDIA_INFO_BUFFERING_END = 702;

    /** Bad interleaving means that a media has been improperly interleaved or
     * not interleaved at all, e.g has all the video samples first then all the
     * audio ones. Video is playing but a lot of disk seeks may be happening.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_BAD_INTERLEAVING = 800;

    /** The media cannot be seeked (e.g live stream)
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_NOT_SEEKABLE = 801;

    /** A new set of metadata is available.
     * @see android.media.MediaPlayer.OnInfoListener
     */
    public static final int MEDIA_INFO_METADATA_UPDATE = 802;

    /**
     * Interface definition of a callback to be invoked to communicate some
     * info and/or warning about the media or its playback.
     */
    public interface OnCyberInfoListener
    {
        /**
         * Called to indicate an info or a warning.
         *
         * @param mp      the MediaPlayer the info pertains to.
         * @param what    the type of info or warning.
         * <ul>
         * <li>{@link #MEDIA_INFO_UNKNOWN}
         * <li>{@link #MEDIA_INFO_VIDEO_TRACK_LAGGING}
         * <li>{@link #MEDIA_INFO_BAD_INTERLEAVING}
         * <li>{@link #MEDIA_INFO_NOT_SEEKABLE}
         * <li>{@link #MEDIA_INFO_METADATA_UPDATE}
         * </ul>
         * @param extra an extra code, specific to the info. Typically
         * implementation dependant.
         * @return True if the method handled the info, false if it didn't.
         * Returning false, or not having an OnErrorListener at all, will
         * cause the info to be discarded.
         */
        boolean onInfo(CyberPlayerSurface mp, int what, int extra);
    }

    /**
     * Register a callback to be invoked when an info/warning is available.
     *
     * @param listener the callback that will be run
     */
    public void setOnCyberInfoListener(OnCyberInfoListener listener)
    {
        mOnInfoListener = listener;
    }

    private OnCyberInfoListener mOnInfoListener;


    public void onSeekComplete(CyberPlayerCore mp) {
        if(mOnSeekCompleteListener != null)
            mOnSeekCompleteListener.onSeekComplete(this);
    }


    public void onBufferingUpdate(CyberPlayerCore mp, int percent) {
        if(mOnBufferingUpdateListener != null)
            mOnBufferingUpdateListener.onBufferingUpdate(this, percent);
    }


    public boolean onError(CyberPlayerCore mp, int what, int extra) {
        if(mOnErrorListener != null)
            return mOnErrorListener.onError(this, what, extra);
        return false;
    }


    public void onPrepared(CyberPlayerCore mp) {
        if(mOnPreparedListener != null)
            mOnPreparedListener.onPrepared(this);
        synchronized (CyberPlayerSurface.SYNCOBJECT) {
            sbIsPrepare = false;
        }

        start();
        
        if (Utils.isM3U8Path(mstrVideoPath)) {
            miSeek = (int)(Utils.getM3U8History(mContext, mstrVideoPath.hashCode()+"")/1000);
        }
        
        if (0 != miSeek) {
            seekTo(miSeek*1000,true);
        }
    }

    public void onCompletion(CyberPlayerCore mp) {
        if(mOnCompletionListener != null)
            mOnCompletionListener.onCompletion(this);
    }
    
    public void onVideoSizeChanged(CyberPlayerCore mp, int width, int height) {
        if(mOnVideoSizeChangedListener != null)
            mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height);
    }


    public boolean onInfo(CyberPlayerCore mp, int what, int extra) {
        Log.v(TAG, "onInfo");
        if(mOnInfoListener != null)
            return mOnInfoListener.onInfo(this, what, extra);
        return false;
    }

    public void onVideoSizeChanged(MediaPlayer arg0, int arg1, int arg2) {
        if(mOnVideoSizeChangedListener != null)
            mOnVideoSizeChangedListener.onVideoSizeChanged(this, arg1, arg2);
    }


    public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
        if(mOnInfoListener != null)
            return mOnInfoListener.onInfo(this, arg1, arg2);
        return false;
    }


    public void onSeekComplete(MediaPlayer arg0) {
        if(mOnSeekCompleteListener != null)
            mOnSeekCompleteListener.onSeekComplete(this);
    }


    public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
        if(mOnBufferingUpdateListener != null)
            mOnBufferingUpdateListener.onBufferingUpdate(this, arg1);
    }


    public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
        // TODO Auto-generated method stub
        if(mOnErrorListener != null)
            return mOnErrorListener.onError(this, arg1, arg2);
        return false;
    }


    public void onPrepared(MediaPlayer arg0) {
        if(mOnPreparedListener != null)
            mOnPreparedListener.onPrepared(this);
    }


    public void onCompletion(MediaPlayer arg0) {
        if(mOnCompletionListener != null)
            mOnCompletionListener.onCompletion(this);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "CyberPlayer: surfaceCreated : setdisplayer");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        
    }

    public void setOnPreparedListener(android.media.MediaPlayer.OnPreparedListener l) {
        
    }

    public void setOnCompletionListener(android.media.MediaPlayer.OnCompletionListener l) {
        
    }

    public void setOnErrorListener(android.media.MediaPlayer.OnErrorListener l) {
        
    }

    public void setOnSeekCompleteListener(android.media.MediaPlayer.OnSeekCompleteListener l) {
        
    }

    public void start(final Context context, final Uri uri) {
        synchronized (SYNCOBJECT) {
            sbIsStarting = true;
        }
        
        reset();
        initCyberPlayer(context);
        try {
            if (null == uri) {
                
                return;
            }
            
            String strPath;
            String scheme = uri.getScheme();
            if (null != scheme && (scheme.equals("http") || scheme.equals("rtsp"))) {
                strPath = uri.toString();
            } else {
                strPath = uri.getPath();
                if (null != strPath && strPath.startsWith("//")) {
                    strPath = strPath.substring(1);
                }
            }
            setDataSource(strPath);
        } catch (IllegalArgumentException e1) {
            e1.printStackTrace();
        } catch (IllegalStateException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        setDisplay(this);
        corePlayer.hasSurface();
        prepareAsync();
    }
    
    public void setVideoPath(final Uri uri) {
        //reset();  
        try {
            if (null == uri) {
                return;
            }
            
            String strPath;
            String scheme = uri.getScheme();
            if (null != scheme && (scheme.equals("http") || scheme.equals("rtsp"))) {
                strPath = uri.toString();
            } else {
                strPath = uri.getPath();
                if (null != strPath && strPath.startsWith("//")) {
                    strPath = strPath.substring(1);
                }
            }
            setDataSource(strPath);
        } catch (IllegalArgumentException e1) {
            e1.printStackTrace();
        } catch (IllegalStateException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        setDisplay(this);
        
        prepareAsync();
    }

    public boolean isSurfaceCreated() {
        return true;
    }

    public boolean isPaused() {
        return !isPlaying();
    }

    public boolean startFromPos(int playPosition, boolean showContinueToast) {
        if (playPosition > 0) {
            miSeek = playPosition;
        }

        if (!sbIsPrepare) {
            if (isPaused()) {
                start();
            }
        }
        return true;
    }

    public void stopPlayback() {
        stop();
    }

    public int getSeekPos(boolean flag) {
        int idur = getDuration();
        int ipos = getCurrentPosition();
        if(idur != 0)
        {
            return ipos*100/idur;
        }
        return 0;
    }

    public int getPausePosition() {
        return getCurrentPosition();
    }

    public String getVideoFactor() {
        return mContext.getResources().getString(R.string.fullscreen);
    }

    public void setViewVisible() {
        setVisibility(View.VISIBLE);
    }

    @Override
    public int getCurState() {
        return 0;
    }

    @Override
   /* protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = (int) (mVideoWidth * mfCurrFactor);
        mHeight = (int) (mVideoHeight * mfCurrFactor);
        int width = getDefaultSize(mWidth, widthMeasureSpec);
        int height = getDefaultSize(mHeight, heightMeasureSpec);

        if (corePlayer != null) {
            setMeasuredDimension(mWidth, mHeight);
        } else {
            setMeasuredDimension(width, height);
        }
    }*/
    public void configurationChanged(){
        
    }
    public void setVideoSize(boolean addFlag) {/*
        if (addFlag) {
            mfCurrFactor += mfAccel_Factor;
            if (mfCurrFactor >= mfScaleMax) {
                mfCurrFactor = mfScaleMax;
            }
        } else {
            mfCurrFactor -= mfAccel_Factor;
            
            if (mfCurrFactor < 0.5f) {
                mfCurrFactor = 0.5f;
            }
            if (mfCurrFactor <= mfScaleMin) {
                mfCurrFactor = mfScaleMin;
            }
        }

        //mWidth = (int) (mVideoWidth * mfCurrFactor);
        //mHeight = (int) (mVideoHeight * mfCurrFactor);

        requestLayout();
        return;
    
    */}
    
    private void getMaxScale() {
        int iScreen =  (mScreenWidth*100)/mScreenHeight;
        int iVideo =  (mVideoWidth*100)/mVideoHeight;

        if (iScreen > iVideo)  {
            if (mVideoHeight > mScreenHeight) {
                mfScaleMax = 1.0f;
            } else {
                mfScaleMax = (float) (mScreenHeight)/mVideoHeight;
                mfScaleMax += DETLA_VALUE;
            }
        } else {
            if (mVideoWidth > mScreenWidth) {
                mfScaleMax = 1.0f;
            } else {
                mfScaleMax = (float)(mScreenWidth)/mVideoWidth;
                mfScaleMax += DETLA_VALUE;
            }
        }
    }

	@Override
	public boolean isPreparing() {
		return sbIsStarting;
	}
	
	@Override
    public void setViewVisible(int visible) {
        setVisibility(visible);
        
    }

    @Override
    public void resetScreenSize(boolean Ori_Hor) {
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

	@Override
	public void setSystemUiVisibility(int visibility) {
		// TODO Auto-generated method stub
		
	}
}

