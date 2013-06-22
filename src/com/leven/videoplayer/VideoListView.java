package com.leven.videoplayer;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

public class VideoListView extends ListView {
	protected static final String TAG = "VideoListView";
	protected static final int MSG_REFRESH_VIEW = 1;
	private int mListPos;
	private VideoListAdapter mVideoListAdapter;
	private Handler mRefreshHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.what) {
			case MSG_REFRESH_VIEW:
				invalidate();
				break;
			}
		}
		
	};

	public VideoListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		setOnScrollListener(mOnScrollListener);
	}

	public VideoListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		setOnScrollListener(mOnScrollListener);
	}

	public VideoListView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		setOnScrollListener(mOnScrollListener);
	}

	public void setAdapter(VideoListAdapter adapter) {
		// TODO Auto-generated method stub
		super.setAdapter(adapter);
		mVideoListAdapter = adapter;
	}

    private OnScrollListener mOnScrollListener = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub
			if(scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
				mListPos = getFirstVisiblePosition();
				Log.d(TAG, "onScrollStateChanged mListPos = " + mListPos);
			}
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			// TODO Auto-generated method stub
			
		}
	};
	
	public FlingSpeedCallback mFlingSpeedCallback = new FlingSpeedCallback() {
		
		long timeToWait = 0;
		@Override
		public void onFlingSpeedListener(float velocityY) {
			if(VideoListView.this.mVideoListAdapter == null) {
				return;
			}
			
			timeToWait = Math.abs(new Float(velocityY).longValue());
			
			synchronized(mVideoListAdapter.mSyncObject) {
				mVideoListAdapter.mEnableUpdateThumbnails = false;
				
				mThumbnailsUpdateTimer.cancel();
				mThumbnailsUpdateTimer.start();
				Log.d(TAG, "thumbnails update wait ......");
			}
		}
		
	};
	
	//for countdown function
	private CountDownTimer mThumbnailsUpdateTimer = new CountDownTimer(1500, 2000) {

		@Override
		public void onTick(long millisUntilFinished) {
			Log.d(TAG, "thumbnails update tick ......");
		}

		@Override
		public void onFinish() {
			if(mVideoListAdapter == null) {
				return;
			}
			
			synchronized(mVideoListAdapter.mSyncObject) {
				Log.d(TAG, "thumbnails update finish ......");
				mVideoListAdapter.mEnableUpdateThumbnails = true;
			}
			
			mVideoListAdapter.mEnableUpdateThumbnails = true;
			mVideoListAdapter.notifyDataSetChanged();
			mRefreshHandler.sendMessage(Message.obtain(mRefreshHandler, MSG_REFRESH_VIEW));
		}
		
	};

}
