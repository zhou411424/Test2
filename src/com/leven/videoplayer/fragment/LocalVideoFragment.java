package com.leven.videoplayer.fragment;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
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
import android.widget.AdapterView;
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
import com.leven.videoplayer.persistance.LocalVideo;
import com.leven.videoplayer.utils.DownloadSoUtils;
import com.leven.videoplayer.utils.LogUtil;
import com.leven.videoplayer.utils.VideoObservable;
//import com.leven.videoplayer.VideoListAdapter;
import com.leven.videoplayer.utils.VideoStatDao;
import com.leven.videoplayer.BaseActivity;
import com.leven.videoplayer.DownloadSoActivity;
import com.leven.videoplayer.GestureSpeedListener;
import com.leven.videoplayer.VideoFolderAdapter;
import com.leven.videoplayer.VideoGridAdapter;
import com.leven.videoplayer.VideoGridView;
import com.leven.videoplayer.VideoListAdapter;
import com.leven.videoplayer.VideoListAdapter2;
import com.leven.videoplayer.VideoListView;
import com.leven.videoplayer.VideoPlayActivity;
import com.slidingmenu.lib.SlidingMenu;

public class LocalVideoFragment extends SherlockFragment {
	private static final String SHOW_MODE = "show_mode";
	public static final String LAST_SHOWMODE = "last_show_mode";
	public static final String LAST_PLAYED_FILE = "last_played_file";
	private int mShowMode = -1;
	public static final int LIST_MODE = 0;
	public static final int GRID_MODE = 1;
	public static final int FOLDER_MODE = 2;
	private static final String IS_ORDER_IN_ADDEDTIME = "is_order_inaddedtime";
    private static final String TAG = "LocalVideoFragment";
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

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	view = inflater.inflate(R.layout.local_video_fragment, container, false);
    	return view;
    }
    
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initView();
		initPreferences();
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
	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    video_list = inflater.inflate(R.layout.video_list, null);
	    video_grid = inflater.inflate(R.layout.video_grid, null);
	    video_folder = inflater.inflate(R.layout.video_folder, null);
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
			LogUtil.d(TAG, "video num = " + mVideoNum);
			mListVideoNum.setText((mVideoNum - 1) + getString(R.string.video_num));
		} else if(mShowMode == GRID_MODE){
		    LogUtil.d(TAG, "current mode is grid mode");
		    mVideoGridAdapter = new VideoGridAdapter(getActivity(), mVideoListCursor, mVideoObservable, mVideoStatDao, mFm);
		    mVideoGridView.setAdapter(mVideoGridAdapter);
            mVideoGridView.setOnItemClickListener(mVideoItemClickListener);
            registerForContextMenu(mVideoGridView);
            mVideoNum = mVideoGridView.getCount();
		} else if(mShowMode == FOLDER_MODE) {
		    LogUtil.d(TAG, "current mode is folder mode");
		    mVideoFolderAdapter = new VideoFolderAdapter(getActivity(), mVideoListCursor, mVideoObservable, mVideoStatDao);
		    mVideoFolderView.setAdapter(mVideoFolderAdapter);
		    mVideoFolderView.setOnItemClickListener(mVideoItemClickListener);
		    registerForContextMenu(mVideoFolderView);
            mVideoNum = mVideoFolderView.getCount();
		}
	}

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
		        showToast(R.string.sd_card_unmounted);
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
    private ArrayList<LocalVideo> mLocalVideos;
    private VideoListView mVideoFolderView;
	private VideoGridAdapter mVideoGridAdapter;
	private VideoFolderAdapter mVideoFolderAdapter;
	private FragmentManager mFm;
	
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

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences prefs = getActivity().getSharedPreferences(Constants.CONTENT_DIRECTORY, Context.MODE_WORLD_WRITEABLE);
        Editor editor = prefs.edit();
        editor.putInt(SHOW_MODE, mShowMode);
        editor.putBoolean(IS_ORDER_IN_ADDEDTIME, mbOrderInAddedTime);
        editor.commit();
    }
	
}
