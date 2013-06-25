package com.leven.videoplayer.fragment;

import java.io.File;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.provider.SyncStateContract.Constants;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.leven.videoplayer.R;
import com.leven.videoplayer.utils.DownloadSoUtils;
import com.leven.videoplayer.utils.LogUtil;
import com.leven.videoplayer.utils.VideoObservable;
import com.leven.videoplayer.utils.VideoStatDao;
import com.leven.videoplayer.BaseActivity;
import com.leven.videoplayer.DownloadSoActivity;
import com.leven.videoplayer.FolderListActivity;
import com.leven.videoplayer.GestureSpeedListener;
import com.leven.videoplayer.VideoGridAdapter;
import com.leven.videoplayer.VideoGridView;
import com.leven.videoplayer.VideoListAdapter;
import com.leven.videoplayer.VideoListView;
import com.leven.videoplayer.VideoPlayActivity;
import com.slidingmenu.lib.SlidingMenu;

public class LocalVideoFragment extends SherlockFragment {
    private static final String TAG = "LocalVideoFragment";
	private static final String SHOW_MODE = "show_mode";
	public static final String LAST_SHOWMODE = "last_show_mode";
	public static final String LAST_PLAYED_FILE = "last_played_file";
	private int mShowMode = -1;
	public static final int LIST_MODE = 0;
	public static final int GRID_MODE = 1;
	public static final int FOLDER_MODE = 2;
	private static final String IS_ORDER_IN_ADDEDTIME = "is_order_inaddedtime";
    protected static final String VIDEO_PATH_KEY = "VIDEO_PATH";
    protected static final String VIDEO_ID_KEY = "VIDEO_ID";
    protected static final String VIDEO_POS_KEY = "VIDEO_POS";
    protected static final String VIDEO_ORDER_KEY = "VIDEO_ORDER";
    private static final String RMVB_RM_FORMAT = "rmvb rm";
	private TextView tvList;
    private TextView tvGrid;
    private TextView tvFolder;
    private ImageView ivBottomLine;
    private ViewPager viewPager;
    private int bottomLineWidth;
    private int offset;
    private int position_one;
    private int position_two;
	private View view;
	private ArrayList<View> listViews;
	private int curIndex = 0;
	private SharedPreferences prefs;
	private VideoObservable mVideoObservable;
	private Cursor mVideoListCursor;
	private LinearLayout mNoVideoLayout;
	private boolean mbOrderInAddedTime = false;
	private VideoStatDao mVideoStatDao;
	private VideoListAdapter mVideoListAdapter;
	private VideoGridView mVideoGridView;
	private int mVideoNum;
    private VideoListView mVideoListView;
	private View video_list;
	private View video_grid;
	private View video_folder;
	private GestureDetector mGestureDetector;
	private TextView mListVideoNum;
	private VideoListView mVideoFolderView;
    private VideoGridAdapter mVideoGridAdapter;
    private VideoFolderAdapter mVideoFolderAdapter;
    private FragmentManager mFm;
    private boolean mSdCardExist = true;
    private int mVideoFolderInListPos;
    private ContentResolver mContentResolver;
    private LayoutInflater mInflater;
    private static ArrayList<FileInfo> mVideoFolderList = new ArrayList<FileInfo>();
    private String mCurPath;
    private String mCurBucketId;
    private boolean mbIsDir;
    private final String DEFAULT_BUCKET_SORT_ORDER = "upper("
            + Video.VideoColumns.BUCKET_DISPLAY_NAME + ") ASC";
    private final String[] BUCKET_PROJECTION_VIDEOS = new String[] {
            Video.VideoColumns.BUCKET_ID,
            Video.VideoColumns.BUCKET_DISPLAY_NAME };
    private final String[] BUCKET_PROJECTION_FILE_VIDEOS = new String[] {
            Video.VideoColumns.DATA, Video.VideoColumns.BUCKET_DISPLAY_NAME };
    private final Uri VIDEOS_URI = Video.Media.EXTERNAL_CONTENT_URI.buildUpon()
            .appendQueryParameter("distinct", "true").build();
    
    private static class FileInfo {
        String bucketId;
        String displayName;
        String path;
        int num;
        boolean isDir;
    }
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	view = inflater.inflate(R.layout.local_video_fragment, container, false);
    	return view;
    }
    
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mSdCardExist  = isSdCardExists();
		mContentResolver = getActivity().getContentResolver();
		
		initView();
		initPreferences();
	}

    @Override
    public void onStart() {
        super.onStart();
        LogUtil.d(TAG, "OnStart...");
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addDataScheme("file");
        getActivity().registerReceiver(mIntentReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtil.d(TAG, "onResume...");
        
        if(mbIsDir) {
            getVideoFolderFromDB(mVideoFolderList);
        }
        
        restorePosition();
    }

    @Override
    public void onPause() {
        super.onPause();
        LogUtil.d(TAG, "onPause...");
    }

    @Override
    public void onStop() {
        super.onStop();
        LogUtil.d(TAG, "onStop...");
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.CONTENT_DIRECTORY, Context.MODE_WORLD_WRITEABLE);
        Editor editor = prefs.edit();
        editor.putInt(SHOW_MODE, mShowMode);
        editor.putBoolean(IS_ORDER_IN_ADDEDTIME, mbOrderInAddedTime);
        editor.commit();
        
        getActivity().unregisterReceiver(mIntentReceiver);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy...");
    }
    
    //Restore the first visible item position
    private void restorePosition() {
        LogUtil.d(TAG, "mVideoFolderInListPos="+mVideoFolderInListPos);
        if(mVideoFolderInListPos != -1) {
            if(mVideoFolderView != null) {
                mVideoFolderView.setSelection(mVideoFolderInListPos);
            } else {
                LogUtil.d(TAG, "restorePosition() has been called but mVideoFolderView is null");
            }
        }
    }
    
    private boolean isSdCardExists() {
        if (android.os.Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {
            return true;
        } else {
            return false;
        }
    }
    
	private void initPreferences() {
		prefs = getActivity().getSharedPreferences(Constants.CONTENT_DIRECTORY, Context.MODE_PRIVATE);
		mShowMode = prefs.getInt(SHOW_MODE, LIST_MODE);
		mbOrderInAddedTime = prefs.getBoolean(IS_ORDER_IN_ADDEDTIME, false);
	}

	private void initView() {
		tvList = (TextView) view.findViewById(R.id.tv_list);
	    tvGrid = (TextView) view.findViewById(R.id.tv_grid);
	    tvFolder = (TextView) view.findViewById(R.id.tv_folder);
	    
	    tvList.setOnClickListener(new MyOnClickListener(0));
	    tvGrid.setOnClickListener(new MyOnClickListener(1));
	    tvFolder.setOnClickListener(new MyOnClickListener(2));
	    
	    ivBottomLine = (ImageView) view.findViewById(R.id.iv_bottom_line);
	    bottomLineWidth = ivBottomLine.getLayoutParams().width;
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenW = dm.widthPixels;
        offset = (int) ((screenW / 3.0 - bottomLineWidth) / 2);
        position_one = (int) (screenW / 3.0);
        position_two = position_one * 2;
        
	    viewPager = (ViewPager) view.findViewById(R.id.viewPager);
	    
	    listViews = new ArrayList<View>();
	    mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    video_list = mInflater.inflate(R.layout.video_list, null);
	    video_grid = mInflater.inflate(R.layout.video_grid, null);
	    video_folder = mInflater.inflate(R.layout.video_folder, null);
	    listViews.add(video_list);
	    listViews.add(video_grid);
	    listViews.add(video_folder);
	    
	    viewPager.setAdapter(new MyPagerAdapter(listViews));
	    viewPager.setCurrentItem(0);
	    viewPager.setOnPageChangeListener(new MyOnPageChangeListener());
	    
	    mVideoObservable = new VideoObservable(getActivity());
	    mVideoStatDao = VideoStatDao.open(getActivity());
	    if(mbOrderInAddedTime) {
	        mVideoListCursor = mVideoObservable.queryMideaFileInfo();
	    } else {// by name
	        mVideoListCursor = mVideoObservable.queryMideaFileInfoInName();
	    }
	    if(mVideoListCursor == null) {
	        showNoSdCard();
	    }
	    mNoVideoLayout = (LinearLayout) view.findViewById(R.id.no_video_layout);
	    
	    //init show mode is list mode
	    mShowMode = LIST_MODE;
	    mVideoListView = (VideoListView) video_list.findViewById(R.id.videoList);
	    //custom footer view
	    mListVideoNum = (TextView) View.inflate(getActivity(), R.layout.footer_view, null);
	    mVideoListView.addFooterView(mListVideoNum);
	    mVideoGridView = (VideoGridView) video_grid.findViewById(R.id.videoGrid);
	    mVideoFolderView = (VideoListView) video_folder.findViewById(R.id.videoList);
	    mVideoFolderView.setOnScrollListener(mScrollListener);
	    mVideoFolderView.setBackgroundColor(Color.WHITE);
	    if(mVideoFolderList != null) {
	        mVideoFolderList.clear();
	    }
	    refreshVideoList();
	}
	
	@SuppressWarnings("deprecation")
    private void refreshVideoList() {
		if(mShowMode == LIST_MODE) {
		    LogUtil.d(TAG, "current mode is list mode");
		    mFm = getActivity().getSupportFragmentManager();
			if(mVideoListAdapter == null) {
				mVideoListAdapter = new VideoListAdapter(getActivity(), mVideoListCursor, mVideoObservable, mVideoStatDao, mFm);
			}
			
			mVideoListView.setAdapter(mVideoListAdapter);
			mVideoListView.setOnItemClickListener(mVideoItemClickListener);
			registerForContextMenu(mVideoListView);
			
			mGestureDetector = new GestureDetector(
					new GestureSpeedListener(mVideoListView.mFlingSpeedCallback));
			
			mVideoListView.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					mGestureDetector.onTouchEvent(event);
					return false;
				}
				
			});
			mVideoNum = mVideoListView.getCount();
			mListVideoNum.setText((mVideoNum - 1) + getString(R.string.video_num));
			LogUtil.d(TAG, "video num = " + (mVideoNum-1));
		} else if(mShowMode == GRID_MODE){
		    LogUtil.d(TAG, "current mode is grid mode");
		    mVideoGridAdapter = new VideoGridAdapter(getActivity(), mVideoListCursor, mVideoObservable, mVideoStatDao, mFm);
		    mVideoGridView.setAdapter(mVideoGridAdapter);
            mVideoGridView.setOnItemClickListener(mVideoItemClickListener);
            registerForContextMenu(mVideoGridView);
            mVideoNum = mVideoGridView.getCount();
		} else if(mShowMode == FOLDER_MODE) {
		    LogUtil.d(TAG, "current mode is folder mode");
		    mVideoFolderAdapter = new VideoFolderAdapter();
		    mVideoFolderView.setAdapter(mVideoFolderAdapter);
		    mVideoFolderView.setOnItemClickListener(mFolderItemClickListener);
		    registerForContextMenu(mVideoFolderView);
            mVideoNum = mVideoFolderView.getCount();
		}
	}
	
	private class VideoFolderAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mVideoFolderList.size();
        }

        @Override
        public Object getItem(int position) {
            return mVideoFolderList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if(convertView == null) {
                convertView = mInflater.inflate(R.layout.video_folder_item, parent, false);
                holder = new ViewHolder();
                holder.mFileIcon = (ImageView) convertView.findViewById(R.id.iv_file_icon);
                holder.mFolderName = (TextView) convertView.findViewById(R.id.tv_folder_name);
                holder.mFolderVideoNum = (TextView) convertView.findViewById(R.id.tv_video_num);
                holder.mFolderPath = (TextView) convertView.findViewById(R.id.tv_folder_path);
                holder.mFolderOperation = (ImageView) convertView.findViewById(R.id.iv_folder_operation);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            FileInfo fileInfo = mVideoFolderList.get(position);
            if(fileInfo.isDir) {
                holder.mFileIcon.setImageResource(R.drawable.icon_list_folder);
            } else {
                holder.mFileIcon.setImageResource(R.drawable.icon_list_videofile);
            }

            holder.mFolderName.setText(fileInfo.displayName);
            holder.mFolderVideoNum.setText(fileInfo.num + "");
            holder.mFolderPath.setText(fileInfo.path);
            holder.mFolderOperation.setOnClickListener(mOperationClickListener);
            return convertView;
        }
	    
	}
	
	private OnClickListener mOperationClickListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            Toast.makeText(getActivity(), "operation", Toast.LENGTH_LONG).show();
        }
    };
	
	
	private static class ViewHolder {
        //folder
	    public ImageView mFileIcon;
        public TextView mFolderName;
        public TextView mFolderVideoNum;
        public TextView mFolderPath;
        public ImageView mFolderOperation;
    }
	
	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String sdState = Environment.getExternalStorageState();

            boolean sdcard_status_changed = true;
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                mSdCardExist = true;
            } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                /* It is mounting now */
                if (Environment.MEDIA_UNMOUNTED.equals(sdState)
                        || Environment.MEDIA_CHECKING.equals(sdState)) {
                    LogUtil.d(TAG, "ACTION_MEDIA_UNMOUNTED ---- SD 1 state: " + sdState + ", ignored");
                    return;
                }
                if (mSdCardExist) {
                    mSdCardExist = false;
                    showSdCardUnmount();
                }
            } else if (action.equals(Intent.ACTION_MEDIA_REMOVED)) {
                showSdCardUnmount();
            } else if (action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)) {
                showSdCardUnmount();
            } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
                Log.d(TAG, "ACTION_MEDIA_SCANNER_FINISHED");
            } else {
                Log.d(TAG, "Unkown broadcast, action = " + action + ","
                        + "sdcard_status_changed = " + sdcard_status_changed
                        + ", sd card exist = " + mSdCardExist);
                sdcard_status_changed = false;
            }

            if (sdcard_status_changed) {
                getVideoFolderFromDB(mVideoFolderList);
            }
        }

    };
    
    private void getVideoFolderFromDB(ArrayList<FileInfo> videoFolderList) {
        if(videoFolderList == null) {
            return;
        }
        videoFolderList.clear();
        Cursor cursor = mContentResolver.query(VIDEOS_URI, BUCKET_PROJECTION_VIDEOS, 
                null, null, DEFAULT_BUCKET_SORT_ORDER);
        if(cursor == null) {
            showToast(R.string.no_video);
            getActivity().finish();
            return;
        }
        
        while(cursor.moveToNext()) {
            FileInfo fi = new FileInfo();
            fi.bucketId = cursor.getString(
                    cursor.getColumnIndex(MediaStore.Video.VideoColumns.BUCKET_ID));
            fi.displayName = cursor.getString(
                    cursor.getColumnIndex(MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME));
            fi.path = getVideoFolderPathByBucketId(fi.bucketId);
            fi.num = getVideoFolderNumByBucketId(fi.bucketId);
            fi.isDir = true;
            if(fi.path != null) {
                mVideoFolderList.add(fi);
            }
        }
        LogUtil.d(TAG, "mVideoFolderList size="+mVideoFolderList.size());
        mbIsDir = true;
        
        if(cursor != null) {
            cursor.close();
        }
        
        mVideoFolderView.setAdapter(mVideoFolderAdapter);
        return;
    }
	
    private int getVideoFolderNumByBucketId(String bucketId) {
        int videosNum = 0;
        Uri videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        StringBuffer whereString = new StringBuffer(MediaStore.Video.VideoColumns.BUCKET_ID + "=?");
        Cursor cursor = mContentResolver.query(videosUri, BUCKET_PROJECTION_FILE_VIDEOS,
                whereString.toString(), new String[] { bucketId }, null);
        if(cursor.moveToFirst()) {
            videosNum = cursor.getCount();
        }
        
        if(cursor != null) {
            cursor.close();
        }
        return videosNum;
    }
    
    private String getVideoFolderPathByBucketId(String bucketId) {
        String folderPath = null;
        Uri videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        StringBuffer whereString = new StringBuffer(MediaStore.Video.VideoColumns.BUCKET_ID + "=?");
        Cursor cursor = mContentResolver.query(videosUri, BUCKET_PROJECTION_FILE_VIDEOS, 
                whereString.toString(), new String[] { bucketId }, null);
        if(cursor.moveToFirst()) {
            folderPath = cursor.getString(cursor.getColumnIndex(Video.VideoColumns.DATA));
            int iDot = folderPath.lastIndexOf('/');
            if(iDot != -1) {
                folderPath = folderPath.substring(0, iDot);
            }
        }
        
        if(cursor != null) {
            cursor.close();
        }
        return folderPath;
    }
    
    private void showSdCardUnmount() {
        showToast(R.string.sd_card_unmounted);
    }
    
	private OnScrollListener mScrollListener = new OnScrollListener() {
        
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if(scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
                if(mVideoFolderView != null) {
                    mVideoFolderInListPos = mVideoFolderView.getFirstVisiblePosition();
                } else {
                    LogUtil.d(TAG, "onScrollStateChanged has been called but mFileListView is null");
                }
                LogUtil.d(TAG, "mVideoFolderInListPos="+mVideoFolderInListPos);
            }
        }
        
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            
        }
    };
    
    private OnItemClickListener mFolderItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            File chosedFile = new File(mVideoFolderList.get(position).path);
            if(chosedFile.isDirectory()) {
                mCurPath = mVideoFolderList.get(position).displayName;
                mCurBucketId = mVideoFolderList.get(position).bucketId;
                
                Intent intent = new Intent(getActivity(), FolderListActivity.class);
                intent.putExtra("bucket_name", mCurPath);
                intent.putExtra("bucket_id", mCurBucketId);
                startActivity(intent);
            }
        }
        
    };

	private OnItemClickListener mVideoItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
		    Intent intent = new Intent();
		    int videoId = (int) mVideoListAdapter.getItemId(position);
		    String videoPath = mVideoObservable.queryVideoPathById(videoId);
		    int iDot = videoPath.lastIndexOf(".");
		    String videoFormat = videoPath.substring(iDot + 1);
		    if(!(new File(videoPath)).exists()) {
		        showSdCardUnmount();
		        return;
		    }
		    
		    //if video format is rm/rmvb
		    if(-1 != RMVB_RM_FORMAT.indexOf(videoFormat)) {
		        // libffmpeg.so is downloaded
		        boolean mIsDownloaded = DownloadSoUtils.hasDownloaded(getActivity(), DownloadSoActivity.LIBFFMPEG_NAME);
		        // libffmpeg.txt is exists
		        boolean mIsExists = DownloadSoUtils.hasDecoderPlugInExists();
		        if(mIsDownloaded && mIsExists) {
		            getActivity().deleteFile(DownloadSoUtils.STANDARD_PLUG_IN_LIB);
		        }
		        if(!mIsDownloaded && !mIsExists) {
		            if(DownloadSoUtils.hasDownloadedDecoderPlunIn(getActivity())) {
		                intent.setClass(getActivity(), VideoPlayActivity.class);
		                intent.putExtra(VIDEO_ID_KEY, videoId+"");
		                intent.putExtra(VIDEO_PATH_KEY, videoPath);
		                intent.putExtra(VIDEO_POS_KEY, position+"");
		                intent.putExtra(VIDEO_ORDER_KEY, mbOrderInAddedTime);
		                startActivity(intent);
		                return;
		            }
		            //else
		            showToast(R.string.file_damaged);
		            return;
		        } else if(!mIsDownloaded || mIsExists) {
		            showToast(R.string.file_damaged);
		            return;
		        }
		    }
			
			intent.setClass(getActivity(), VideoPlayActivity.class);
            intent.putExtra(VIDEO_ID_KEY, videoId+"");
            intent.putExtra(VIDEO_PATH_KEY, videoPath);
            intent.putExtra(VIDEO_POS_KEY, position+"");
            intent.putExtra(VIDEO_ORDER_KEY, mbOrderInAddedTime);
            startActivity(intent);
		}
		
	};
	
	private void showNoSdCard() {
		Toast.makeText(getActivity(), R.string.no_sd_card_prompt, Toast.LENGTH_LONG).show();		
	}

	public class MyOnPageChangeListener implements OnPageChangeListener {

		@Override
		public void onPageScrollStateChanged(int arg0) {
			
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			
		}

		@Override
		public void onPageSelected(int arg0) {
			Animation animation = null;
            switch (arg0) {
                case 0:
                    if(curIndex == 1) {
                        animation = new TranslateAnimation(position_one, 0, 0, 0);
                        tvGrid.setTextColor(getResources().getColor(R.color.lightwhite));
                    } else if(curIndex == 2) {
                        animation = new TranslateAnimation(position_two, 0, 0, 0);
                        tvFolder.setTextColor(getResources().getColor(R.color.lightwhite));
                    }
                    tvList.setTextColor(getResources().getColor(R.color.white));
                    mShowMode = LIST_MODE;
                    // only on the page 0 can enter left sliding menu
                    ((BaseActivity)getActivity()).getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
                    break;
                case 1:
                    if(curIndex == 0) {
                        animation = new TranslateAnimation(offset, position_one, 0, 0);
                        tvList.setTextColor(getResources().getColor(R.color.lightwhite));
                    } else if(curIndex == 2) {
                        animation = new TranslateAnimation(position_two, position_one, 0, 0);
                        tvFolder.setTextColor(getResources().getColor(R.color.lightwhite));
                    }
                    tvGrid.setTextColor(getResources().getColor(R.color.white));
                    mShowMode = GRID_MODE;
                    ((BaseActivity)getActivity()).getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
                    break;
                case 2:
                    if(curIndex == 0) {
                        animation = new TranslateAnimation(offset, position_two, 0, 0);
                        tvList.setTextColor(getResources().getColor(R.color.lightwhite));
                    } else if(curIndex == 1) {
                        animation = new TranslateAnimation(position_one, position_two, 0, 0);
                        tvGrid.setTextColor(getResources().getColor(R.color.lightwhite));
                    }
                    tvFolder.setTextColor(getResources().getColor(R.color.white));
                    
                    mShowMode = FOLDER_MODE;
                    ((BaseActivity)getActivity()).getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
                    break;
            }
            curIndex = arg0;
            animation.setFillAfter(true);
            animation.setDuration(300);
            ivBottomLine.startAnimation(animation);
            refreshVideoList();
		}
		
	}
	
	public class MyPagerAdapter extends PagerAdapter {
		private ArrayList<View> listViews;
		
		public MyPagerAdapter(ArrayList<View> listViews) {
			this.listViews = listViews;
		}
		
		@Override
		public Object instantiateItem(View container, int position) {
			((ViewPager) container).addView(listViews.get(position), 0);
			return listViews.get(position);
		}

		@Override
		public void destroyItem(View container, int position, Object object) {
			((ViewPager) container).removeView(listViews.get(position));
		}

		@Override
		public int getCount() {
			return listViews.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}
		
	}
	
	public class MyOnClickListener implements OnClickListener {
	    private int index = 0;
	        
        public MyOnClickListener(int index) {
            this.index = index;
        }

        @Override
        public void onClick(View v) {
            viewPager.setCurrentItem(index);
        }
	    
	}

	private void showToast(int resId) {
	    Toast.makeText(getActivity(), resId, Toast.LENGTH_LONG).show();
	}
	
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

}
