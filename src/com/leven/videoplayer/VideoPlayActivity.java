package com.leven.videoplayer;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.leven.videoplayer.fragment.LocalVideoFragment;
import com.leven.videoplayer.utils.DownloadSoUtils;
import com.leven.videoplayer.utils.LogUtil;
import com.leven.videoplayer.utils.VideoObservable;
import com.leven.videoplayer.utils.VideoStatDao;
import com.leven.videoplayer.video.CyberPlayerCore;
import com.leven.videoplayer.video.MoviePlayer;

public class VideoPlayActivity extends Activity {

    private static final String VIDEO_PATH = "VIDEO_PATH";
    private static final String VIDEO_ID = "VIDEO_ID";
    private static final String VIDEO_BUCKET_ID = "BUCKET_ID";
    private static final String VIDEO_POS = "VIDEO_POS";
    private static final String VIDEO_ORDER = "VIDEO_ORDER";
    private static final String USER_GUIDE = "USER_GUIDE";
    private static final String TAG = "VideoPlayActivity";
    private static final String LAST_PLUGIN = "last_one_is_plug_in";
    private static final String LAST_COMPONENT = "last_one_is_local_component";
    public static final String FORMAT = "rmvb rm";
    private static final int NUM_AMIMATE = 4;
    private static final String VIDEO_FORMAT = "rmvb rm";
    private ArrayList<FileEntry> mFileList;
    private Intent mIntent;
    private boolean mNeedSyncWithDB;
    private Uri mUri;
    private int mVideoId;
    private boolean mbUserGuide;
    private VideoObservable mVideoObservable;
    private VideoStatDao mVideoStatDao;
    private int mPlayPos;
    private String mUriString;
    private boolean mbInFilePath;
    private String mFolderBucketId;
    private int mPosInList;
    private boolean mbOrderInAddedTime;
    private boolean mbPlugIn;
    private boolean mbSupportRotate;
    private boolean mbNeedSavePos;
    private int mBrightnessDefault;
    private boolean mFinishOnCompletion;
    private int mLastPos;
    private boolean mbIsPlayingFromPhone;
    private boolean mbIsPausedFromRestart;
    private String mCurrStatus;
    private MoviePlayer mPlayer;
    private String mNextVideoPath;
    private KeyguardManager mKeyguardManager;
    private OnePhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;
    private WindowManager.LayoutParams mWinParams;
    private int mTotalNum = 0;
    private boolean mbIsOrderinAddedtime;
	private boolean mbChangeEndable;
	private boolean mbIsActivie = false;
	private boolean mbScanner_status = false;
	private int mPauseCount = 0;
	private BroadcastReceiver mKeyguardEventReceiver = null;
	private boolean mReceiverRegisted = false;
	private static final String SCREEN_CAPTRUE = "android.intent.action.video.screencapture.start";
	private static final String CAPTRUE_SAVE = "android.intent.action.video.screencapture.save";
	private static final String CAPTRUE_SHARE = "android.intent.action.video.screencapture.share";
	private static final String CAPTRUE_CANCEL = "android.intent.action.video.screencapture.cancel";
	private BroadcastReceiver mScreenCaptureReceiver;
	private final static int PLAYER_PAUSE_FLAG = 2004;
	private final static int PLAYER_WAIT_PAUSE = 2005;
	private final static int PLAYER_WAIT_RESUME = 2006;
	private final static int PLAYER_REFRESH_UI = 2007;
	private final static int SCREEN_CAPTURE = 2008;
	private final static int CAPTURE_SAVE = 2009;
	private final static int CAPTURE_SHARE = 2010;
	private final static int CAPTURE_CANCEL = 2011;
	private IntentFilter mScreenCaptureFilter;
	private int mRefreshCount = 0;
    
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case PLAYER_PAUSE_FLAG:
                if (mPlayer.isPlaying()) {
                    mPlayer.onPause();
                    mPlayer.hideUI();
                    mHandler.removeMessages(PLAYER_PAUSE_FLAG);
                    mRefreshCount = 0;
                    if (!mHandler.hasMessages(PLAYER_REFRESH_UI)) {
                        mHandler.sendMessageDelayed(Message.obtain(
                                mHandler, PLAYER_REFRESH_UI), 500);
                    }
                } else {
                    mRefreshCount = 0;
                    if (!mHandler.hasMessages(PLAYER_REFRESH_UI)) {
                        mHandler.sendMessageDelayed(Message.obtain(
                                mHandler, PLAYER_REFRESH_UI), 500);
                    }

                    mHandler.sendMessageDelayed(Message.obtain(
                            mHandler, PLAYER_PAUSE_FLAG), 200);
                }
                setVolumeMute(false);
                break;
            case PLAYER_WAIT_PAUSE:
                if (mPlayer.isVideoSurfaceCreated()) {
                    mPlayer.onPause();
                    mHandler.removeMessages(PLAYER_WAIT_PAUSE);
                } else {
                    mHandler.sendMessageDelayed(Message.obtain(
                            mHandler, PLAYER_WAIT_PAUSE), 200);
                }
                break;
            case PLAYER_REFRESH_UI:
                mPlayer.refreshUI();
                mHandler.sendMessageDelayed(
                        Message.obtain(mHandler, PLAYER_REFRESH_UI),
                        200);
                if (++mRefreshCount % 10 == 0) {
                    mHandler.removeMessages(PLAYER_REFRESH_UI);
                }
                break;

                default:
                    break;
            }
        }
        
    };
    
    @SuppressWarnings("unused")
    private class FileEntry {
        public String id;
        public String name;
        public String data;
        public String addedDate;
        public FileEntry(String id, String name, String data, String addedDate) {
            super();
            this.id = id;
            this.name = name;
            this.data = data;
            this.addedDate = addedDate;
        }
        
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFileList = new ArrayList<FileEntry>();
        mIntent = getIntent();
        if(!intentProcess(mIntent)) {
            finish();
            return;
        }
        LogUtil.d(TAG, "intentProcess()==true");
        
        mbNeedSavePos = true;
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mVideoObservable = new VideoObservable(this);
        //listen the screen on or off
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
//        KeyguardLock mKeyguardLock = mKeyguardManager.newKeyguardLock("Lock");
//        mKeyguardLock.disableKeyguard();
        
        if(mNeedSyncWithDB) {
            if(mVideoStatDao == null) {
                mVideoStatDao = VideoStatDao.open(this);
            }
            mVideoStatDao.isSyncWithVideoDB();
        }
        try {
            mBrightnessDefault = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        setContentView(R.layout.video_play_activity);
        
        FrameLayout rootView = (FrameLayout) findViewById(R.id.videoLayout);
        
        Intent intent = new Intent();
        mFinishOnCompletion = intent.getBooleanExtra(MediaStore.EXTRA_FINISH_ON_COMPLETION, true);
        mPlayer = new MoviePlayer(rootView, this, mUri, savedInstanceState, 
                !mFinishOnCompletion, mHandler, mbPlugIn) {

            @Override
            public void onCompletion() {
                super.onCompletion();
                LogUtil.d(TAG, "current video is completion");
                mLastPos = 0;
                if(mVideoId != -1) {
                    updateStatInfoToDB(mVideoId, mLastPos, mUri.getPath());
                }
                if(hasNextVideo()) {
                    LogUtil.d(TAG, "has next video");
                    startPlayerByPath(mNextVideoPath);
                } else {
                    if(mFinishOnCompletion) {
                        finish();
                    }
                }
            }

        };
        
        groupVideos();
        resetNextPreButtons();
        mPlayer.setUriString(mUriString);
        /*if(!mbUserGuide && isNeedAnimate()) {
            mbUserGuide = true;
        }
        if(mbUserGuide) {
            goToAnimate();
        }*/
        
        registerPhoneListener();
        setAutoScreenLockFlag();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    private void setAutoScreenLockFlag() {
        if("Incredible S".equals(android.os.Build.MODEL)) {
            return;
        }
        Window window = getWindow();
        WindowManager.LayoutParams winParams = window.getAttributes();
        mWinParams = winParams;
        winParams.buttonBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
        winParams.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        window.setAttributes(winParams);
    }

    private void resumeAutoScreenLockFlag() {
        if ("Incredible S".equals(android.os.Build.MODEL)) {
           return;
        }
        
        Window win = getWindow();
        win.setAttributes(mWinParams);
    }
    
    private void registerPhoneListener() {
        if(mPhoneStateListener == null) {
            mPhoneStateListener = new OnePhoneStateListener();
        }
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
    
    private void unRegisterPhoneListener() {
        if(mPhoneStateListener == null) {
            mPhoneStateListener = new OnePhoneStateListener();
        }
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mPhoneStateListener = null;
    }

    private class OnePhoneStateListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                Log.i(TAG, "[Listener]ringing" + incomingNumber);

                if (null != mPlayer && mPlayer.isPlaying()) {
                    mbIsPlayingFromPhone = true;
                    mbIsPausedFromRestart = false;
                } else {
                    if (null != mKeyguardManager && !mKeyguardManager.inKeyguardRestrictedInputMode()) {
                        mbIsPausedFromRestart = true;
                    }
                }

                if (!mbIsPlayingFromPhone) {
                    Log.i(TAG, "[Listener] mPlayer.isPlaying():" + false);
                    if (mbIsPausedFromRestart) {
                        mbIsPlayingFromPhone = false;
                    } else {
                        mbIsPlayingFromPhone = true;
                    }
                }
                break;
            default:
                break;
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    }
    
   /* private void goToAnimate() {
        Intent intent = new Intent();
        intent.setClass(this, AnimateActivity.class);
        startActivity(intent);
    }*/

    /*private boolean isNeedAnimate() {
        int iRet = -2;
        Boolean bRet = false;
        SharedPreferences sharedPreference = getSharedPreferences(
                VideoPickerActivity.ANIMATE_NAME, Context.MODE_WORLD_WRITEABLE);
        iRet = sharedPreference.getInt(VideoPickerActivity.ANIMATE_ORDER, -1);
        if (-1 == iRet) {
            Editor editor = sharedPreference.edit();
            editor.putInt(VideoPickerActivity.ANIMATE_ORDER, 0);
            editor.commit();
            bRet = true;
        } else if (iRet >= NUM_AMIMATE) {
            iRet = -2;
            bRet = false;
        }

        return bRet;
    }*/

    private void resetNextPreButtons() {
        if(mFileList != null && mFileList.size() > 1) {
            mPlayer.setNextEnable(true);
            mPlayer.setPrevEnable(true);
        } else {
            mPlayer.setNextEnable(false);
            mPlayer.setPrevEnable(false);
        }
    }

    private void groupVideos() {
        if(mFileList.size() > 0) {
            return;
        }
        
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA, 
                MediaStore.MediaColumns.DATE_ADDED };
        Cursor cur = null;
        try{
            if(mbInFilePath) {
                String selection = MediaStore.Video.VideoColumns.BUCKET_ID + " = ?";
                String folder = getFolderBucketId();
                cur = getContentResolver().query(uri, projection, selection, new String[] { folder }, null);
            } else {
                cur = getContentResolver().query(uri, projection, null, null, null);
            }
            LogUtil.d(TAG, "mbInFilePath="+mbInFilePath);
            if(cur != null && cur.moveToFirst()) {
                String name = null;
                String data = null;
                String fileId = null;
                String dataAdded = null;
                mFileList.clear();
                while(cur.moveToNext()) {
                    name = cur.getString(cur.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                    data = cur.getString(cur.getColumnIndex(MediaStore.MediaColumns.DATA));
                    fileId = cur.getString(cur.getColumnIndex(MediaStore.MediaColumns._ID));
                    dataAdded = cur.getString(cur.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED));
                    FileEntry fileEntry = new FileEntry(fileId, name, data, dataAdded);
                    mFileList.add(fileEntry);
                }
                LogUtil.d(TAG, "mFileList size="+mFileList.size());
                FileComparator fileComparator = new FileComparator();
                Collections.sort(mFileList, fileComparator);
            }
        } finally {
            if(cur != null) {
                cur.close();
            }
        }
    }

    private class FileComparator implements Comparator<FileEntry> {

        @Override
        public int compare(FileEntry lhs, FileEntry rhs) {
            String leftstr = lhs.data.substring(0, lhs.data.lastIndexOf("."));
            String rightstr = rhs.data.substring(0, rhs.data.lastIndexOf("."));
            int numidexleft = hasDigital(leftstr);
            int numidexright = hasDigital(rightstr);
            String strLeft = leftstr;
            String strRight = rightstr;
            if(numidexleft == -1||numidexright == -1){
                return leftstr.compareTo(rightstr);
            }

            if(numidexleft > leftstr.length()-1){
                numidexleft = leftstr.length()-1;
            }
            strLeft = leftstr.substring(0, numidexleft);

            if(numidexright > rightstr.length()-1){
                numidexright = rightstr.length()-1;
            }
            strRight = rightstr.substring(0, numidexright);

            // such as "AA01"
            //left
            int char_index_left = 0;
            for (int i =  strLeft.length()-1; i >= 0; i--) {
                if(!Character.isDigit(strLeft.charAt(i)))
                {
                    char_index_left = i+1;
                    break;
                }
            }
            String name_left = leftstr.substring(0, char_index_left); // to get "AA"

            //right
            int char_index_right = 0;
            for (int i =  strRight.length()-1; i >= 0; i--) {
                if(!Character.isDigit(strRight.charAt(i))){
                    char_index_right = i+1;
                    break;
                }
            }
            String name_right = rightstr.substring(0, char_index_right); // to get "AA"
            NumberFormat format = NumberFormat.getInstance(Locale.CHINA);
            
            if(name_left.contentEquals(name_right)){
                int num_left = 0;
                if(char_index_left < leftstr.length()){
                    int lengthofnum = leftstr.length() - char_index_left;
                    if(lengthofnum > 8){
                        return leftstr.substring(char_index_left).compareTo(rightstr.substring(char_index_right));
                    }
                    else{
                        try {
                            num_left = format.parse(leftstr.substring(char_index_left)).intValue();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        //num_left  =  Integer.valueOf(leftstr.substring(char_index_left)).intValue();
                    }
                }

                int num_right = 0;
                if(char_index_right < rightstr.length()){
                    int lengthofnum = rightstr.length() - char_index_right;
                    if(lengthofnum > 8){
                        return leftstr.substring(char_index_left).compareTo(rightstr.substring(char_index_right));
                    }
                    else{
                        try {
                            num_right = format.parse(rightstr.substring(char_index_right)).intValue();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        //num_right  =  Integer.valueOf(rightstr.substring(char_index_right)).intValue(); 
                    }
                }
                return  num_left - num_right;
            } else{
                return name_left.compareTo(name_right);
            }
        }
        
    }
    
    private String getFolderBucketId() {
        Bundle bundle = getIntent().getExtras();
        String bucket_id = null;
        if(bundle != null) {
            bucket_id = bundle.getString("bucket_id");
        }
        if(bucket_id == null) {
            finish();
            return null;
        }
        return bucket_id;
    }

    private void startPlayerByPath(String videoPath) {
		LogUtil.d(TAG, "play video by path");
        if(videoPath == null) {
            return;
        }
        if(mVideoStatDao == null) {
            mVideoStatDao = VideoStatDao.open(this);
        }
        int videoId = 0;
        if(mVideoObservable != null) {
            videoId = (int) mVideoObservable.queryVideoIdByPath(videoPath);
        }
        
        if(mPlayer != null && mPlayer.mVideoView != null) {
            if(mPlayer.mVideoView.isPlaying()) {
                mPlayer.mVideoView.stopPlayback();
            }
        }

        if(LAST_COMPONENT.equals(mCurrStatus)) {
            mVideoId = videoId;
            mUri = Uri.parse("file://" + videoPath);
            mPlayPos = getStatInfoFromDB(mVideoId);
            LogUtil.d(TAG, "mPlayPos="+mPlayPos);
            mPlayer.restartplay(mUri, mLastPos);
        } else {
            mbNeedSavePos = false;
            finish();
            
            Intent intent = new Intent(this, VideoPlayActivity.class);
            intent.putExtra(VIDEO_PATH, videoPath);
            intent.putExtra(VIDEO_ID, videoId + "");
            intent.putExtra(VIDEO_POS, mPosInList + "");
            if(mbInFilePath) {
                intent.putExtra(VIDEO_BUCKET_ID, getFolderBucketId());
            }
            intent.putExtra(VIDEO_ORDER, mbIsOrderinAddedtime + "");
            startActivity(intent);
        }
    }
    
    private boolean hasNextVideo() {
		// eg. /mnt/sdcard/external_sd/Movies/甄妮84年演唱会/1.mp4
        String pathSeg = mUri.getLastPathSegment();
        if(pathSeg == null) {
            return false;
        }
//        String mCurVideoPath = mUri.getLastPathSegment().toString();
        String mCurVideoPath = mUri.getPath();
//        LogUtil.d(TAG, "hasNextVideo()==>pathSeg="+pathSeg + ", mCurVideoPath="+mCurVideoPath);
        if(mFileList == null) {
            return false;
        }
        for(int i = 0; i < mFileList.size(); i++) {
            if(mFileList.get(i).data == null) {
                continue;
            }
            if(mFileList.get(i).data.equals(mCurVideoPath) 
                    || getLastPathSegment(mFileList.get(i).data).equals(mCurVideoPath)) {
                if(i == mFileList.size() - 1) {
                    return false;
                }
                mNextVideoPath = mFileList.get(i + 1).data;
                LogUtil.d(TAG, "mCurVideoPath="+mCurVideoPath+", mNextVideoPath="+mNextVideoPath);
                LogUtil.d(TAG, "isSamePath="+isSamePath(mCurVideoPath, mNextVideoPath));
                return isSamePath(mCurVideoPath, mNextVideoPath);// same name,but diff
            }
        }
        return false;
    }
    
    public boolean isSamePath(String path1, String path2) {
		// path1=/mnt/sdcard/external_sd/Movies/Jackson演唱会/1.mp4
		// path2=/mnt/sdcard/external_sd/Movies/Jackson演唱会/2.mp4
        //left
        String leftStr = path1.substring(0, path1.lastIndexOf("."));
        String nameLeft = substringFromNum(leftStr);// to get "AA" for AA01
        
        //right
        String rightStr = path2.substring(0, path2.lastIndexOf("."));
        String nameRight = substringFromNum(rightStr);

		LogUtil.d(TAG, "leftStr=" + leftStr + ", nameLeft=" + nameLeft);
		LogUtil.d(TAG, "rightStr=" + rightStr + ", nameRight=" + nameRight);
        if(nameLeft != null && nameRight != null) {
            return nameLeft.contentEquals(nameRight);
        } else if(nameLeft == null && nameRight == null) {
            return true;
        } else {
            return false;
        }
    }
    
    private String substringFromNum(String videoname) {
		// /mnt/sdcard/external_sd/Movies/Jackson演唱会/1
        String name = videoname;
        LogUtil.d(TAG, "hasDigital(videoname)="+hasDigital(videoname));
        if(hasDigital(videoname) == -1){
            return name;
        }
        for(int i = videoname.length()-1;i >= 0;i-- ){
              if(Character.isDigit(videoname.charAt(i))){
                  if(i !=  videoname.length()-1){
                      i++;
                  }
                  name = videoname.substring(0,i);
                  break;
              }
        }

        for(int i = name.length()-1;i >= 0;i-- ){
              if(!Character.isDigit(videoname.charAt(i))){
                  if(i !=  videoname.length()-1){
                      i++;
                      name = videoname.substring(0,i);
                      break;
                  }
              }
              else if(i == 0){
                  name = null;
              }
        }

        return name;
    }
    
    private int hasDigital(String str)
    {
        int result = -1;
        for(int i = str.length()-1;i > 0 ;i--)
        {
           char charinstr = str.charAt(i);
           if(Character.isDigit(charinstr))
           {
              result = i+1;
              return result;
           }
        }

        return result;
    }
    
    private String getLastPathSegment(String str) {
        int len = str.length();
        for(int i = len - 1; i > 0; i--) {
            char c = str.charAt(i);
            if(c == '/') {
                if(i < len -1) {
                    return str.substring(i + 1);
                }
            }
        }
        return str;
    }
    
    private boolean updateStatInfoToDB(int videoId, int playPos,
            String path) {
        if(mVideoStatDao != null) {
//        	LogUtil.d(TAG, "path="+path + ", videoId="+videoId);
            if(path != null && path.startsWith("//")) {
                path = path.substring(1);
//              LogUtil.d(TAG, "path="+path);
            }
            boolean err = mVideoStatDao.update(videoId, playPos, path, true);
            if(err == false) {
                LogUtil.d(TAG, "failed to update current played file's stat to db.");
            }
            return err;
        }
        return false;
    }
    
    private boolean intentProcess(Intent intent) {
        if(Intent.ACTION_VIEW.equals(intent.getAction())) {
            mNeedSyncWithDB = false;
            mUri = intent.getData();
            /*Bundle bundle = intent.getExtras();
            if(bundle != null) {
                mUriString = bundle.getString(VIDEO_PATH);
                Uri.Builder builder = new Uri.Builder();
                builder.appendPath(mUriString);
                mUri = builder.build();
            }*/
            LogUtil.d(TAG, "mUri=" + mUri.toString());
            String uriPrefix = (MediaStore.Video.Media.EXTERNAL_CONTENT_URI).toString();
            //      content://media/external/video/media
            if(mUri != null && mUri.toString().startsWith(uriPrefix)) {
                String strVideoPath = getVideoPathByUri(mUri);
                if(strVideoPath != null) {
                    String strRowId = mUri.getLastPathSegment();
                    if(strRowId != null) {
                        NumberFormat numberFormat = NumberFormat.getInstance(Locale.CHINA);
                        try {
                            mVideoId = numberFormat.parse(strRowId).intValue();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    // e.g., file:///sdcard/videos/123.mp4
                    mUri = Uri.parse("file://" + strVideoPath);
                }
            }
            
            mbUserGuide = false;
            
            String strPath = mUri.toString();
            if(strPath != null && strPath.lastIndexOf(".m3u8") != -1) {
                int netType = DownloadSoUtils.checkNetworkType(this);
                if(netType == 0) {
                    showToast(R.string.net_disconnect);
                }
            }
            if(mVideoObservable == null) {
                mVideoObservable = new VideoObservable(this);
            }
            if(mVideoStatDao == null) {
                mVideoStatDao = VideoStatDao.open(this);
            }
            mVideoId = (int) mVideoObservable.queryVideoIdByPath(mUri.getPath());
            if(intent.getFlags() != 0) {
                if(mVideoId == -1) {
                    mPlayPos = intent.getIntExtra(CyberPlayerCore.REPLAY_POSITON, 0);
                } else {
                    mPlayPos = getStatInfoFromDB(mVideoId);
                }
            } else {
                mPlayPos = 0;
            }
        } else {
            LogUtil.d(TAG, "! ACTION_VIEW");
            Bundle bundle = intent.getExtras();
            if(bundle != null) {
                mUriString = bundle.getString(VIDEO_PATH);
                Uri.Builder builder = new Uri.Builder();
                builder.appendPath(mUriString);
                mUri = builder.build();
                LogUtil.d(TAG, "mUriString="+mUriString);
                LogUtil.d(TAG, "mUri="+mUri);
                //video_id
                String video_id = bundle.getString(VIDEO_ID);
                if(video_id != null) {
                    mVideoId = Integer.parseInt(video_id);
                    mPlayPos = getStatInfoFromDB(mVideoId);
                }
                
                //bucket_id
                String bucket_id = bundle.getString(VIDEO_BUCKET_ID);
                if(bucket_id != null) {
                    mbInFilePath = true;
                    mFolderBucketId = bucket_id;
                }
                
                //video_pos
                String video_pos = bundle.getString(VIDEO_POS);
                if(video_pos != null) {
                    mPosInList = Integer.parseInt(video_pos);
                }
                
                //video_order
                mbOrderInAddedTime = bundle.getBoolean(VIDEO_ORDER);
                
                //user_guide
                String user_guide = bundle.getString(USER_GUIDE);
                if(user_guide != null) {
                    mbUserGuide = true;
                } else {
                    mbUserGuide = false;
                }
            }
        }
        
        //finally
        if(mUri != null) {
            if(mUri != null && mUri.getPath() != null) {
                int iM3U8 = mUri.getPath().toLowerCase().lastIndexOf(".m3u8");
                int iRM = mUri.getPath().toLowerCase().lastIndexOf(".rm");
                int iRMVB = mUri.getPath().toLowerCase().lastIndexOf(".rmvb");
                if(iM3U8 != -1 || iRM != -1 || iRMVB != -1) {
                    mbPlugIn = true;
                } else {
                    mbPlugIn = false;
                }
            } else {
                mbPlugIn = false;
            }
            LogUtil.d(TAG, "mbPlugIn="+mbPlugIn);
            return true;
        }
        return false;
    }

    //get play position
    private int getStatInfoFromDB(int videoId) {
        int playPos = 0;
        if(mVideoStatDao != null) {
            Cursor cs = null;
            try {
                cs = mVideoStatDao.queryPlayedInfoByVideoId(videoId);
                if(cs != null && cs.moveToFirst()) {
                    String recordedPath = cs.getString(cs.getColumnIndex(VideoStatDao.FILE_PATH));
                    String uriPath = mUri.getPath();
                    LogUtil.d(TAG, "recordedPath="+recordedPath+", uriPath="+uriPath);
                    if(recordedPath != null && uriPath.startsWith("//") 
                            && recordedPath.compareTo(uriPath.substring(1)) == 0 
                            || recordedPath.compareTo(uriPath) == 0) {
                        playPos = cs.getInt(cs.getColumnIndex(VideoStatDao.PLAY_POSITION));
                    }
                }
            } finally {
                if(cs != null) {
                    cs.close();
                }
            }
        }
        return playPos;
    }
    
    private void showToast(int textId) {
        Toast.makeText(this, textId, Toast.LENGTH_LONG).show();
    }

    // get video path by uri
    private String getVideoPathByUri(Uri uri) {
        if(uri == null) {
            return null;
        }
        String videoPath = null;
        String[] VIDEO_COLUMNS = { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA };
        if("content".equalsIgnoreCase(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, VIDEO_COLUMNS, null, null, null);
                if(cursor != null && cursor.moveToFirst()) {
                    videoPath = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                }
                
            } finally {
                if(cursor != null) {
                    cursor.close();
                }
            }
        }
        return videoPath;
    }
    
    public void lockOrientation(boolean bLock){
        /*if(mbSupportRotate){
            if(bLock){
                int ore = this.getResources().getConfiguration().orientation;
                if(ore == Configuration.ORIENTATION_LANDSCAPE){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                else{
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
            else{
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
        }*/
    }

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
	    if(mPlayer != null){
            mPlayer.resetBtRect(newConfig.orientation);
            mPlayer.mVideoView.configurationChanged();
           // mPlayer.mVideoView.setVideoSize(true);
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
	protected void onStart() {
		((AudioManager) getSystemService(AUDIO_SERVICE)).requestAudioFocus(
				null, AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		super.onStart();
	}

	@Override
	public void onPause() {
		mbIsActivie = false;
		mbScanner_status = false;
		mPauseCount++;
		unRegisterKeyguardEventListener();

		// mHandler.removeMessages(MSG_GET_MEDIASCANNER_STATUS);

		int iDuration = 0;
		if (!isPlugIN(mUri)) {
			if (mPauseCount <= 1) {
				iDuration = mPlayer.getDuration() / 1000;
				mLastPos = mPlayer.getCurrentPosition() / 1000;
				if (mLastPos >= iDuration - 2) {
					mLastPos = 0;
				}
			}
		} else {
			if (mPauseCount <= 1) {
				iDuration = mPlayer.getDuration();
				mLastPos = mPlayer.getCurrentPosition() / 1000;
				if (mLastPos >= iDuration - 2) {
					mLastPos = 0;
				}
			}
		}

		if (mVideoId != -1) {
			SharedPreferences sharedPreference = getSharedPreferences(
					LocalVideoFragment.LAST_SHOWMODE,
					Context.MODE_WORLD_WRITEABLE);
			Editor editor = sharedPreference.edit();
			editor.putInt(LocalVideoFragment.LAST_PLAYED_FILE, mVideoId);
			editor.commit();

			if (mbNeedSavePos) {
				updateStatInfoToDB(mVideoId, mLastPos, mUri.getPath());
			}
		}

		if (mReceiverRegisted) {
			mReceiverRegisted = false;
			unregisterReceiver(mBatInfoReceiver);
			unregisterReceiver(mScreenCaptureReceiver);
		}

		if (mPlayer.isPaused()) {
			mbIsPausedFromRestart = true;
		} else {
			mbIsPausedFromRestart = false;
		}

		mHandler.sendMessageDelayed(
				Message.obtain(mHandler, PLAYER_WAIT_PAUSE), 30);

		/*
		 * if (mbIsUserGuide) { mbIsUserGuide = false; mbIsPausedFromRestart =
		 * false; // resetUserGuide(); }
		 */
		if (mPlayer != null && mPlayer.mVideoView != null) {
			if (!isPlugIN(mUri)) {
				mPlayer.mVideoView.setViewVisible(View.INVISIBLE);
			}

		}

		super.onPause();
	}

	@Override
	public void onResume() {
		mbIsActivie = true;

		if (!mReceiverRegisted) {
			mReceiverRegisted = true;
			IntentFilter iFilter = new IntentFilter(
					Intent.ACTION_BATTERY_CHANGED);
			iFilter.addAction(Intent.ACTION_SCREEN_OFF);
			this.registerReceiver(mBatInfoReceiver, iFilter);
		}

		if (mKeyguardManager != null) {
			if (!mKeyguardManager.inKeyguardRestrictedInputMode()) {
				LogUtil.d(TAG, "screen unlock, Launch video");
				resumeVideo();
			} else {
				LogUtil.d(TAG, "onResume(), screen lock, don't play video");
				registerKeyguardEventListener();
			}
		}

		mScreenCaptureFilter = new IntentFilter(SCREEN_CAPTRUE);
		mScreenCaptureFilter.addAction(CAPTRUE_SAVE);
		mScreenCaptureFilter.addAction(CAPTRUE_SHARE);
		mScreenCaptureFilter.addAction(CAPTRUE_CANCEL);
		mScreenCaptureReceiver = new ScreenCaptureReceiver();
		registerReceiver(mScreenCaptureReceiver, mScreenCaptureFilter);

		if (mPlayer != null && mPlayer.mVideoView != null) {
			if (!isPlugIN(mUri)) {
				mPlayer.mVideoView.setViewVisible(View.VISIBLE);
			}

		}
		mPlayer.mVideoView.configurationChanged();
		mPlayer.configTime();
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		unRegisterPhoneListener();
		((AudioManager) getSystemService(AUDIO_SERVICE))
				.abandonAudioFocus(null);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (null != mPlayer) {
            mPlayer.onDestroy();
        }
        
        if (mVideoStatDao != null) {
            mVideoStatDao.close();
            mVideoStatDao = null;
        }
        
        resumeAutoScreenLockFlag();
	}

	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
		public void onReceive(Context arg0, Intent intent) {
			int iCharging = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
					BatteryManager.BATTERY_STATUS_UNKNOWN);
			if (BatteryManager.BATTERY_STATUS_CHARGING == iCharging) {
				mPlayer.updateChargingStatus(true);
			} else {
				mPlayer.updateChargingStatus(false);
			}
			// get current battery level
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
			mPlayer.updateBatteryInfo(level);
		}
	};

	public class ScreenCaptureReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (SCREEN_CAPTRUE.equals(action)) {
				mHandler.sendEmptyMessage(SCREEN_CAPTURE);
			} else if (CAPTRUE_SAVE.equals(action)) {
				mHandler.sendEmptyMessage(CAPTURE_SAVE);
			} else if (CAPTRUE_SHARE.equals(action)) {
				mHandler.sendEmptyMessage(CAPTURE_SHARE);
			} else if (CAPTRUE_CANCEL.equals(action)) {
				mHandler.sendEmptyMessage(CAPTURE_CANCEL);
			}
		}
	}

	private void resumeVideo() {
		mPauseCount = 0;
		if (mVideoId != -1) {
			if (mLastPos > 0) {
				mPlayPos = mLastPos;
			} else if (mNeedSyncWithDB) {
				mPlayPos = getStatInfoFromDB(mVideoId);

			}
		}

		if (mbIsPlayingFromPhone) {
			mbIsPlayingFromPhone = false;
			mPlayer.onResume(mPlayPos, mPlayPos > 0);
			mRefreshCount = 0;
			mHandler.sendMessageDelayed(
					Message.obtain(mHandler, PLAYER_REFRESH_UI), 1000);
		} else {
			if (mbIsPausedFromRestart) {
				setVolumeMute(true);
				mPlayer.onResume(mPlayPos, false);
				mHandler.sendMessageDelayed(
						Message.obtain(mHandler, PLAYER_PAUSE_FLAG), 500);
			} else {
				mPlayer.onResume(mPlayPos, mPlayPos > 0);
				mRefreshCount = 0;
				mHandler.sendMessageDelayed(
						Message.obtain(mHandler, PLAYER_REFRESH_UI), 1000);
			}
		}

		mbIsPausedFromRestart = false;
		mbScanner_status = true;

		// mHandler.sendMessage(Message.obtain(mHandler,
		// MSG_GET_MEDIASCANNER_STATUS));
	}

	private void setVolumeMute(boolean bmute) {
		AudioManager audioManager;
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioManager.setStreamMute(AudioManager.STREAM_MUSIC, bmute);
	}

	public void registerKeyguardEventListener() {
		if (mKeyguardEventReceiver == null) {
			mKeyguardEventReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					resumeVideo();
				}
			};

			IntentFilter iFilter = new IntentFilter();
			iFilter.addAction(Intent.ACTION_USER_PRESENT);
			registerReceiver(mKeyguardEventReceiver, iFilter);
		}
	}

	public void unRegisterKeyguardEventListener() {
		if (mKeyguardEventReceiver != null) {
			unregisterReceiver(mKeyguardEventReceiver);
			mKeyguardEventReceiver = null;
		}
	}

    public void playNextVideo() {
        if (0 >= mTotalNum) {
            mTotalNum = getVideoCount(this);
            if (-1 == mTotalNum) {
                return;
            }
			LogUtil.d(TAG, "mTotalNum=" + mTotalNum);
        }

        if (++mPosInList >= mTotalNum) {
            mPosInList = 0;
        }

        if (isPlugIN(mUri)) {
            mCurrStatus = LAST_PLUGIN;
        } else {
			LogUtil.d(TAG, "mCurrStatus is no plugin");
            mCurrStatus = LAST_COMPONENT;
        }

        mLastPos = mPlayer.getCurrentPosition() / 1000;
		LogUtil.d(TAG, "mPosInList=" + mPosInList + ", mLastPos=" + mLastPos);
        if (mVideoId != -1) {
            updateStatInfoToDB(mVideoId, mLastPos, mUri.getPath());
        }

        String videoPath = null;
        if (mbIsOrderinAddedtime) {
            if (mbInFilePath) {
				LogUtil.d(TAG, "order in added time and in file path");
                videoPath = mVideoObservable
                        .queryNextVideoName_orderinAddedDate_InFolderByID(
                                mPosInList, getFolderBucketId());
            } else {
				LogUtil.d(TAG, "order in added time but not in file path");
                videoPath = mVideoObservable.queryNextVideoNameByID(mPosInList);
            }

        } else {
            if (mbInFilePath) {
				LogUtil.d(TAG, "order in name and in file path");
                videoPath = mVideoObservable
                        .queryNextVideoNameByID_InNameOrder_InFilePath(
                                mPosInList, getFolderBucketId());
            } else {
				LogUtil.d(TAG, "order in name but not in file path");
                videoPath = mVideoObservable
                        .queryNextVideoNameByID_InNameOrder(mPosInList);
            }

        }
		LogUtil.d(TAG, "videoPath=" + videoPath);
        Uri uri = Uri.parse("file://" + videoPath);
        if (isPlugIN(uri)) {
            boolean bIsDownloaded = DownloadSoUtils.hasDownloaded(this,
                    DownloadSoActivity.LIBFFMPEG_NAME);
            if (bIsDownloaded) {
                startPlayerByRowIDWithPlugIN(this, mPosInList);
            } else {

                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mbChangeEndable = true;
                        playNextVideo();
                    }
                }, 500);
            }

        } else {
            startPlayerByPath(videoPath);
        }
    }
    
    public void playPreviousVideo() {
        if (0 >= mTotalNum) {
            mTotalNum = getVideoCount(this);
            if (-1 == mTotalNum) {
                return;
            }
        }

        if (0 >= mPosInList) {
            mPosInList = mTotalNum - 1;
        } else {
            --mPosInList;
        }

        if (isPlugIN(mUri)) {
            mCurrStatus = LAST_PLUGIN;
        } else {
            mCurrStatus = LAST_COMPONENT;
        }

        mLastPos = mPlayer.getCurrentPosition() / 1000;
        if (mVideoId != -1) {
            updateStatInfoToDB(mVideoId, mLastPos, mUri.getPath());
        }

        // startPlayerByRowID(this, mPos);
        String videoPath = null;
        if (mbIsOrderinAddedtime) {
            if (mbInFilePath) {
                videoPath = mVideoObservable
                        .queryNextVideoName_orderinAddedDate_InFolderByID(
                                mPosInList, getFolderBucketId());
            } else {
                videoPath = mVideoObservable
                        .queryNextVideoNameByID(mPosInList);
            }

        } else {
            if (mbInFilePath) {
                videoPath = mVideoObservable
                        .queryNextVideoNameByID_InNameOrder_InFilePath(
                                mPosInList, getFolderBucketId());
            } else {
                videoPath = mVideoObservable
                        .queryNextVideoNameByID_InNameOrder(mPosInList);
            }
        }
        Uri uri = Uri.parse("file://" + videoPath);
        if (isPlugIN(uri)) {
            boolean bIsDownloaded = DownloadSoUtils.hasDownloaded(
                    this, DownloadSoActivity.LIBFFMPEG_NAME);
            if (bIsDownloaded) {
                startPlayerByRowIDWithPlugIN(this, mPosInList);
            } else {

                mHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mbChangeEndable = true;
                        playPreviousVideo();
                    }
                }, 500);
            }
        } else {
            startPlayerByPath(videoPath);
        }
    }

    // protocol plugin
    private void startPlayerByRowIDWithPlugIN(Context context, int rowId) {
        String videoPath = null;
        if(mPlayer.mVideoView.isPreparing()) {
            mPlayer.mVideoView.stopPlayback();
            return;
        }
        if(mbIsOrderinAddedtime) {
            videoPath = mVideoObservable.queryNextVideoNameByID(rowId);
        } else {
            videoPath = mVideoObservable.queryNextVideoNameByID_InNameOrder(rowId);
        }
        
        if(videoPath == null) {
            return;
        }
        
        int tmpVideoId = 0;
        if(mVideoStatDao != null) {
            tmpVideoId = (int) mVideoObservable.queryVideoIdByPath(videoPath);
        }
        if(mPlayer != null) {
            if(mPlayer.mVideoView.isPreparing()) {
                mPlayer.mVideoView.stopPlayback();
            }
        }
        
        if(LAST_PLUGIN.equals(mCurrStatus)) {
            mVideoId = tmpVideoId;
            mbChangeEndable = true;
            mPlayer.setSomeVideo(videoPath);
        } else {
            mbNeedSavePos = false;
            finish();
            
            Intent intent = new Intent(context, VideoPlayActivity.class);
            intent.putExtra(VIDEO_PATH, videoPath);
            intent.putExtra(VIDEO_ID, tmpVideoId + "");
            intent.putExtra(VIDEO_POS, rowId + "");
            if(mbInFilePath) {
                intent.putExtra(VIDEO_BUCKET_ID, getFolderBucketId());
            }
            intent.putExtra(VIDEO_ORDER, mbIsOrderinAddedtime + "");
            startActivity(intent);
        }
        
    }

    private boolean isPlugIN(Uri uri) {
        if(uri == null) {
            LogUtil.d(TAG, "uri is null");
        } else {
            String strPath = null;
            String scheme = uri.getScheme();
            LogUtil.d(TAG, "uri="+uri+" ,uri scheme="+scheme);
            if(scheme != null && (scheme.equals("http") 
                    || scheme.equals("https") || scheme.equals("rtsp"))) {
                strPath = uri.toString();
            } else {
                strPath = uri.getPath();
                LogUtil.d(TAG, "strPath="+strPath);
            }
            int iDot = strPath.lastIndexOf(".");
            if(iDot != -1) {
                strPath = strPath.substring(iDot + 1);
                if(VIDEO_FORMAT.indexOf(strPath) != -1) {
                    return true;
                }
            }
        }
        return false;
    }

    private int getVideoCount(Context context) {
        if(mVideoObservable != null) {
            if(mbInFilePath) {
                return mVideoObservable.queryVideoCountInFolder(mFolderBucketId);
            } else {
                return mVideoObservable.queryVideoCount();
            }
        }
        return -1;
    }

}
