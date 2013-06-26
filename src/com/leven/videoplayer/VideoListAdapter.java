package com.leven.videoplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.leven.videoplayer.image.utils.ImageCache;
import com.leven.videoplayer.utils.LogUtil;
import com.leven.videoplayer.utils.Utils;
import com.leven.videoplayer.utils.VideoObservable;
import com.leven.videoplayer.utils.VideoStatDao;

public class VideoListAdapter extends CursorAdapter {
    private static final String TAG = "VideoListAdapter";
	private int mListViewResId;
	
	private Context mContext;
	private VideoStatDao mVideoStatDao;
	private VideoObservable mVideoObservable;
	private int mVideoIdColumn;
	private int mDisplayNameColumn;
	private int mDataColumn;
	private int mDurationColumn;
	private int mMimeTypeColumn;
	private int mSizeColumn;
	private int mDateAddedColumn;
	private int mShowMode = -1;
	public static final Object mSyncObject = new Object();// for syncronized
	public boolean mEnableUpdateThumbnails = true;
	private static final int IMAGE_WIDTH_MAX = (int) (144 * 1.5);
    private static final int IMAGE_HEIGHT_MAX = (int) (108 * 1.5);
    private static final int IMAGE_WIDTH_MAX_BIG = 172 * 2;
    private static final int IMAGE_HEIGHT_MAX_BIG = 130 * 2;
	private static final String IMAGE_CACHE_DIR = "images";
	private FragmentManager mFm;
	private ContentResolver mContentResolver;
	
	@SuppressWarnings("deprecation")
	public VideoListAdapter(Context context, Cursor c, 
			VideoObservable videoObservable, VideoStatDao videoStatDao, FragmentManager fm) {
		super(context, c);
		mListViewResId = R.layout.video_list_item;
		mContentResolver = context.getContentResolver();
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
			mDateAddedColumn = c.getColumnIndex(MediaStore.Video.VideoColumns.DATE_ADDED);
		}

		// allow debug
		LogUtil.DEBUG = true;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = null;
        ViewHolder holder = new ViewHolder();
        view = inflater.inflate(mListViewResId, parent, false);
        holder.mListVideoThumb = (ImageView) view.findViewById(R.id.iv_video_thumb);
    	holder.mListNewVideoIcon = (ImageView) view.findViewById(R.id.iv_new_video_icon);
    	holder.mListVideoDuration = (TextView) view.findViewById(R.id.tv_video_duration);
    	holder.mListVideoName = (TextView) view.findViewById(R.id.tv_video_name);
    	holder.mListVideoSize = (TextView) view.findViewById(R.id.tv_video_size);
    	holder.mListPlayVideo = (ImageView) view.findViewById(R.id.iv_play);
        view.setTag(holder);
	    return view;
	}

    @SuppressWarnings("deprecation")
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
    	//get video name
    	holder.mListVideoName.setText(cursor.getString(mDisplayNameColumn));
    	//get video duration is millionTime
		long millionTime = cursor.getLong(mDurationColumn);
		String videoDuration = Utils.getVideoDuration(millionTime);
        holder.mListVideoDuration.setText(videoDuration);
        //get video size is bytes
        long bytes = cursor.getLong(mSizeColumn);
        holder.mListVideoSize.setText(Utils.formatFileSize(bytes));
        
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
        	//get bitmap from disk cache
        	/*bitmap = imageCache.getBitmapFromDiskCache(videoPath);
        	if(bitmap != null) {
        		bitmapDrawable = new BitmapDrawable(bitmap);
        		imageCache.addBitmapToCache(videoPath, bitmapDrawable);
        		holder.mListVideoThumb.setImageBitmap(bitmap);
        		LogUtil.d(TAG, "from disk cache");
//        		return;
        	}*/
        	//if bitmap in cache is null, get video thumb
        	bitmap = getVideoThumbnails(videoId);
        	if(bitmap != null) {
        		bitmapDrawable = new BitmapDrawable(bitmap);
        		imageCache.addBitmapToCache(videoPath, bitmapDrawable);
        		holder.mListVideoThumb.setImageBitmap(bitmap);
        		LogUtil.d(TAG, "get video thumb");
//        		return;
        	} else {
        		
        		LogUtil.d(TAG, "bitmap is null");
        	}
        }
        if(bitmapDrawable != null) {
        	holder.mListVideoThumb.setImageDrawable(bitmapDrawable);
        	LogUtil.d(TAG, "from memory cache");
        }
	}
    
    private Bitmap getVideoThumbnails(int videoId) {
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
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(mThumbFilePath, options);// return null(no bitmap),but save in options
                options.inJustDecodeBounds = false;
                
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
                options.inSampleSize = reckonThumbnail(options.outWidth,
                        options.outHeight, width, height);
                bitmap = BitmapFactory.decodeFile(mThumbFilePath, options);
            } catch (Exception e) {
                LogUtil.d(TAG, "out of memory error");
                e.printStackTrace();
            }
        }
        if(bitmap == null) {
            try {
                bitmap = MediaStore.Video.Thumbnails.getThumbnail(mContentResolver, videoId, 
                        MediaStore.Video.Thumbnails.MINI_KIND, null);
            } catch (Exception e) {
                LogUtil.e(TAG, "thumbnails image file is not avialable, videoId=" + videoId);
            }
        }
        
        //create video thumbnail by fixed size
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
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height);
        if(bitmap == null) {
            LogUtil.d(TAG, "Can not create scaled thumbnails image, video id is "
                    + videoId);
        }
        return bitmap;
    }
    
    private int reckonThumbnail(int oldWidth, int oldHeight, int newWidth,
            int newHeight) {
        if ((oldHeight > newHeight && oldWidth > newWidth)
                || (oldHeight <= newHeight && oldWidth > newWidth)) {// width
                                                                     // reduce
            int be = (int) (oldWidth / (float) newWidth);
            if (be <= 1)
                be = 1;
            return be;
        } else if (oldHeight > newHeight && oldWidth <= newWidth) {// height
                                                                   // reduce
            int be = (int) (oldHeight / (float) newHeight);
            if (be <= 1)
                be = 1;
            return be;
        }
        return 1;
    }
    
    public void switchMode(int showMode) {
		mShowMode = showMode;
	}
	
	private static class ViewHolder {
		//list
		public ImageView mListVideoThumb;
		public ImageView mListNewVideoIcon;
		public TextView mListVideoDuration;
		public TextView mListVideoName;
		public TextView mListVideoSize;
		public ImageView mListPlayVideo;
	}
	
}
