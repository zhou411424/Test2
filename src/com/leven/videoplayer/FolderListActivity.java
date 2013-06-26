package com.leven.videoplayer;

import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.leven.videoplayer.fragment.LocalVideoFragment;
import com.leven.videoplayer.utils.Constants;
import com.leven.videoplayer.utils.VideoObservable;
import com.leven.videoplayer.utils.VideoStatDao;

public class FolderListActivity extends FragmentActivity implements Observer {
    private VideoObservable mVideoObservable;
    private VideoStatDao mVideoStatDao;
    private FragmentManager mFm;
    private VideoListAdapter mVideoListAdapter;
    private Cursor mVideoListCursor;
    private ImageButton mBackBtn;
    private TextView mFolderPathTxt;
    private ImageButton mScanBtn;
    private ImageButton mSortBtn;
    private ImageButton mEditBtn;
    private VideoListView mVideoListView;
    private SharedPreferences prefs;
    private boolean mbOrderInAddedTime;
    private int mLastPlayedFile;
    private Intent mIntent;
    private String mBucketPath;
    private String mBucketId;
    private TextView mFolderNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.folder_list_activity);
        
        initView();
        initPreferences();
    }

    private void initPreferences() {
        prefs = getSharedPreferences(Constants.CONFIG_PREFERENCES, Context.MODE_PRIVATE);
        mbOrderInAddedTime = prefs.getBoolean(LocalVideoFragment.IS_ORDER_IN_ADDEDTIME, false);
        mLastPlayedFile = prefs.getInt(LocalVideoFragment.LAST_PLAYED_FILE, -1);
    }
    
    private void initView() {
        mBackBtn = (ImageButton) findViewById(R.id.title_bar_left);
        mFolderPathTxt = (TextView) findViewById(R.id.title_bar_title);
        mScanBtn = (ImageButton) findViewById(R.id.title_bar_scan);
        mSortBtn = (ImageButton) findViewById(R.id.title_bar_sort);
        mEditBtn = (ImageButton) findViewById(R.id.title_bar_edit);
        mVideoListView = (VideoListView) findViewById(R.id.videoList);
        mFolderNum = (TextView) View.inflate(this, R.layout.footer_view, null);
        mVideoListView.addFooterView(mFolderNum);
        
        //get Intent
        mIntent = getIntent();
        mBucketPath = mIntent.getStringExtra("bucket_path");
        mBucketId = mIntent.getStringExtra("bucket_id");
        
        mFm = getSupportFragmentManager();
        mVideoObservable = new VideoObservable(this);
        mVideoStatDao = VideoStatDao.open(this);
        if(mbOrderInAddedTime) {
            mVideoListCursor = mVideoObservable.queryFolderVideoInfo_InAddedDate(mBucketId);
        } else {// by name
            mVideoListCursor = mVideoObservable.queryFolderVideoInfo_InNameOrder(mBucketId);
        }
        mVideoListAdapter = new VideoListAdapter(this, mVideoListCursor, 
                mVideoObservable, mVideoStatDao, mFm);
        mVideoListView.setAdapter(mVideoListAdapter);
        mVideoListView.setOnItemClickListener(mVideoItemClickListener);
        registerForContextMenu(mVideoListView);
        mFolderNum.setText((mVideoListView.getCount() - 1) + getString(R.string.video_num));
        mFolderPathTxt.setText(mBucketPath);
        
        mVideoObservable.addObserver(this);
    }

    private OnItemClickListener mVideoItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            
        }
        
    };

    @Override
    public void update(Observable observable, Object data) {
        
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mVideoStatDao != null) {
            mVideoStatDao.close();
        }
        if(mVideoListCursor != null) {
            mVideoListCursor.close();
            mVideoListCursor = null;
        }
    }
}
