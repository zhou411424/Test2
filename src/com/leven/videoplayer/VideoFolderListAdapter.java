package com.leven.videoplayer;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.leven.videoplayer.utils.LogUtil;
import com.leven.videoplayer.utils.VideoObservable;
import com.leven.videoplayer.utils.VideoStatDao;

public class VideoFolderListAdapter extends CursorAdapter {
	private static final String TAG = "VideoFolderAdapter";
	private int mFolderViewResId;
	private Context mContext;
	private VideoStatDao mVideoStatDao;
	private VideoObservable mVideoObservable;
	private int mVideoIdColumn;
	private int mDisplayNameColumn;
	private int mDataColumn;
	private int mDurationColumn;
	private int mMimeTypeColumn;
	private int mSizeColumn;
	private int mDateModifiedColumn;
	private int mBucketIdColumn;
    private int mBucketDisplayNameColumn;
	
	public VideoFolderListAdapter(Context context, Cursor c,
			VideoObservable videoObservable, VideoStatDao videoStatDao) {
		super(context, c);
		mFolderViewResId = R.layout.video_folder_item;
		mContext = context;
		if(videoStatDao != null) {
			mVideoStatDao = videoStatDao;
		} else {
			mVideoStatDao = new VideoStatDao(context);
		}
		mVideoObservable = videoObservable;
		if(c != null) {
			mVideoIdColumn = c.getColumnIndex(MediaStore.Video.Media._ID);
			mDisplayNameColumn = c.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
			mDataColumn = c.getColumnIndex(MediaStore.Video.Media.DATA);
			mDurationColumn = c.getColumnIndex(MediaStore.Video.Media.DURATION);
			mMimeTypeColumn = c.getColumnIndex(MediaStore.Video.Media.MIME_TYPE);
			mSizeColumn = c.getColumnIndex(MediaStore.Video.Media.SIZE);
			mDateModifiedColumn = c.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED);
			mBucketIdColumn = c.getColumnIndex(MediaStore.Video.Media.BUCKET_ID);
	        mBucketDisplayNameColumn = c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LogUtil.d(TAG, "==>newView() : folder mode");
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = null;
        ViewHolder holder = null;
        if(holder == null) {
        	holder = new ViewHolder();
        	view = inflater.inflate(mFolderViewResId, parent, false);
        	holder.mFolderName = (TextView) view.findViewById(R.id.tv_folder_name);
        	holder.mFolderVideoNum = (TextView) view.findViewById(R.id.tv_video_num);
        	holder.mFolderPath = (TextView) view.findViewById(R.id.tv_folder_path);
        	holder.mFolderOperation = (ImageView) view.findViewById(R.id.iv_folder_operation);
        	view.setTag(holder);
        }
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		LogUtil.d(TAG, "==>bindView() : folder mode");
//    	holder.mFolderName = (TextView) view.findViewById(R.id.tv_folder_name);
//    	holder.mFolderVideoNum = (TextView) view.findViewById(R.id.tv_video_num);
//    	holder.mFolderPath = (TextView) view.findViewById(R.id.tv_folder_path);
//    	holder.mFolderOperation = (ImageView) view.findViewById(R.id.iv_folder_operation);
		ViewHolder holder = (ViewHolder) view.getTag();
//		holder.mFolderName.setText(cursor.getString(mBucketDisplayNameColumn));
		holder.mFolderPath.setText(cursor.getString(mDataColumn));
	}
	
	private static class ViewHolder {
		//folder
		public TextView mFolderName;
		public TextView mFolderVideoNum;
		public TextView mFolderPath;
		public ImageView mFolderOperation;
	}

}
