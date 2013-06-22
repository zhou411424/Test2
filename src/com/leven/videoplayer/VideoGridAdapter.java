package com.leven.videoplayer;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.leven.videoplayer.image.utils.ImageCache;
import com.leven.videoplayer.image.utils.ImageFetcher;
import com.leven.videoplayer.utils.LogUtil;
import com.leven.videoplayer.utils.Utils;
import com.leven.videoplayer.utils.VideoObservable;
import com.leven.videoplayer.utils.VideoStatDao;

public class VideoGridAdapter extends CursorAdapter {
	
	private int mGridViewResId;
	private int mVideoIdColumn;
	private int mDisplayNameColumn;
	private int mDataColumn;
	private int mDurationColumn;
	private int mMimeTypeColumn;
	private int mSizeColumn;
	private int mDateModifiedColumn;
	private Context mContext;
	private VideoStatDao mVideoStatDao;
	private VideoObservable mVideoObservable;
	private static final int IMAGE_WIDTH_MAX = (int) (144 * 1.5);
    private static final int IMAGE_HEIGHT_MAX = (int) (108 * 1.5);
    private static final int IMAGE_WIDTH_MAX_BIG = 172 * 2;
    private static final int IMAGE_HEIGHT_MAX_BIG = 130 * 2;
    private static final String IMAGE_CACHE_DIR = "images";
	private FragmentManager mFm;
	private static final String TAG = "VideoGridAdapter";

	public VideoGridAdapter(Context context, Cursor c, 
			VideoObservable videoObservable, VideoStatDao videoStatDao, FragmentManager fm) {
		super(context, c);
		mGridViewResId = R.layout.video_grid_item;
		mContext = context;
		mFm = fm;
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
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = null;
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewHolder holder = null;
        if(holder == null) {
        	holder = new ViewHolder();
            LogUtil.d(TAG, "==>newView() : grid mode");
            view = inflater.inflate(mGridViewResId, parent, false);
            holder.mGridVideoThumb = (ImageView) view.findViewById(R.id.iv_grid_video_thumb);
        	holder.mGridVideoNewVideoIcon = (ImageView) view.findViewById(R.id.iv_grid_new_video_icon);
        	holder.mGridVideoDuration = (TextView) view.findViewById(R.id.tv_grid_video_duration);
        	holder.mGridVideoName = (TextView) view.findViewById(R.id.tv_grid_video_name);
        	view.setTag(holder);
        }
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder = (ViewHolder) view.getTag();
		//get video duration is millionTime
		long millionTime = cursor.getLong(mDurationColumn);
		String videoDuration = Utils.getVideoDuration(millionTime);
        holder.mGridVideoDuration.setText(videoDuration);
        // get video name
        holder.mGridVideoName.setText(cursor.getString(mDisplayNameColumn));
       // get video thumbnails
        int videoId = cursor.getInt(mVideoIdColumn);
        String videoPath = cursor.getString(mDataColumn);
        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(mContext, IMAGE_CACHE_DIR);
        // set memory cache to 25% of app memory
        cacheParams.setMemCacheSizePercent(0.25f);
        
        ImageCache imageCache = ImageCache.getInstance(mFm, cacheParams);
        //get bitmapDrawable from memory cache
        BitmapDrawable bitmapDrawable = imageCache.getBitmapFromMemCache(videoPath);
        Bitmap bitmap = null;
        if(bitmapDrawable == null) {
        	//if bitmap in cache is null, get video thumb
        	bitmap = getVideoThumbnails(videoId, imageCache);
        	if(bitmap != null) {
        		bitmapDrawable = new BitmapDrawable(bitmap);
        		imageCache.addBitmapToCache(videoPath, bitmapDrawable);
        		holder.mGridVideoThumb.setImageBitmap(bitmap);
        		LogUtil.d(TAG, "get video thumb");
//        		return;
        	}
        }
        if(bitmapDrawable != null) {
        	holder.mGridVideoThumb.setImageDrawable(bitmapDrawable);
        	LogUtil.d(TAG, "from memory cache");
        }
	}
	
	private Bitmap getVideoThumbnails(int videoId, ImageCache imageCache) {
    	String mThumbFilePath = null;
    	Cursor thumbCursor = mVideoObservable.queryThumbnails(videoId);
        if(thumbCursor != null && thumbCursor.moveToFirst()) {
            mThumbFilePath = thumbCursor.getString(thumbCursor.getColumnIndex(MediaStore.Video.Thumbnails.DATA));
        }
        if(thumbCursor != null) {
            thumbCursor.close();
        }
        
        Bitmap bitmap = null;
        if(mThumbFilePath != null) {
            
            float density = mContext.getResources().getDisplayMetrics().density;//(0.5, 1, 1.5, 2)
            int width = 0;
            int height = 0;
            if(density < 2) {
                width = IMAGE_WIDTH_MAX;
                height = IMAGE_HEIGHT_MAX;
            } else {
                width = IMAGE_WIDTH_MAX_BIG;
                height = IMAGE_HEIGHT_MAX_BIG;
            }
            bitmap = ImageFetcher.decodeSampledBitmapFromFile(mThumbFilePath, width, height, imageCache);
        }
        if(bitmap == null) {
        	return null;
        }
        return bitmap;
    }
	
	private static class ViewHolder {
		//grid
		public ImageView mGridVideoThumb;
		public ImageView mGridVideoNewVideoIcon;
		public TextView mGridVideoDuration;
		public TextView mGridVideoName;
	}

}
