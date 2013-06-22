package com.leven.videoplayer;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.leven.videoplayer.fragment.LocalVideoFragment;
import com.leven.videoplayer.persistance.LocalVideo;
import com.leven.videoplayer.utils.ImageMemoryCache;
import com.leven.videoplayer.utils.LogUtil;
import com.leven.videoplayer.utils.VideoObservable;
import com.leven.videoplayer.widget.CustomProgressDialog;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ThumbnailUtils;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class VideoListAdapter2 extends BaseAdapter {

    private static final String TAG = "VideoListAdapter";
    private static final long B = 1024;
    private static final long KB = 1024 * 1024;
    private static final long MB = 1024 * 1024 * 1024;
    private static final int IMAGE_WIDTH_MAX = (int) (144 * 1.5);
    private static final int IMAGE_HEIGHT_MAX = (int) (108 * 1.5);
    private static final int IMAGE_WIDTH_MAX_BIG = 172 * 2;
    private static final int IMAGE_HEIGHT_MAX_BIG = 130 * 2;
    private int mShowMode = -1;
    private Context mContext;
    private ImageMemoryCache mThumbCache;
    private ContentResolver mContentResolver;
    private CustomProgressDialog progressDialog;
    private Cursor mVideoListCursor;
    private VideoObservable mVideoObservable;
    private ArrayList<LocalVideo> mLocalVideos;
    private int mVideoIdColumn;
    private int mDisplayNameColumn;
    private int mDataColumn;
    private int mDurationColumn;
    private int mMimeTypeColumn;
    private int mSizeColumn;
    private int mDateModifiedColumn;
    private int mBucketIdColumn;
    private int mBucketDisplayNameColumn;
    
    public VideoListAdapter2(Context context, Cursor videoListCursor, VideoObservable videoObservable, int showMode) {
        this.mContext = context;
        this.mVideoListCursor = videoListCursor;
        this.mVideoObservable = videoObservable;
        this.mShowMode = showMode;
        mThumbCache = new ImageMemoryCache(context);
        mContentResolver = context.getContentResolver();
        
        if(mVideoListCursor != null) {
	        mVideoIdColumn = mVideoListCursor.getColumnIndex(MediaStore.Video.Media._ID);
	        mDisplayNameColumn = mVideoListCursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
	        mDataColumn = mVideoListCursor.getColumnIndex(MediaStore.Video.Media.DATA);
	        mDurationColumn = mVideoListCursor.getColumnIndex(MediaStore.Video.Media.DURATION);
	        mMimeTypeColumn = mVideoListCursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE);
	        mSizeColumn = mVideoListCursor.getColumnIndex(MediaStore.Video.Media.SIZE);
	        mDateModifiedColumn = mVideoListCursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED);
	        mBucketIdColumn = mVideoListCursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID);
	        mBucketDisplayNameColumn = mVideoListCursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
	        
	        mLocalVideos = getVideoList(mVideoListCursor);
	    }
    }
    
	private ArrayList<LocalVideo> getVideoList(Cursor videoListCursor) {
		ArrayList<LocalVideo> localVideos = new ArrayList<LocalVideo>();
		LocalVideo localVideo = null;
		while (videoListCursor.moveToNext()) {
			localVideo = new LocalVideo();
			String videoId = videoListCursor.getString(mVideoIdColumn);
			String displayName = videoListCursor.getString(mDisplayNameColumn);
			String data = videoListCursor.getString(mDataColumn);
			String duration = videoListCursor.getString(mDurationColumn);
			String mimeType = videoListCursor.getString(mMimeTypeColumn);
			String size = videoListCursor.getString(mSizeColumn);
			String dateModified = videoListCursor
					.getString(mDateModifiedColumn);
//			String bucketId = videoListCursor.getString(mBucketIdColumn);
//			String bucketDisplayName = videoListCursor
//					.getString(mBucketDisplayNameColumn);
			localVideo.setVideoId(videoId);
			localVideo.setDisplayName(displayName);
			localVideo.setData(data);
			localVideo.setDuration(duration);
			localVideo.setMimeType(mimeType);
			localVideo.setSize(size);
			localVideo.setDateModified(dateModified);
//			localVideo.setBucketId(bucketId);
//			localVideo.setBucketDisplayName(bucketDisplayName);
			localVideos.add(localVideo);
		}
		return localVideos;
	}

    @Override
    public int getCount() {
        return mLocalVideos.size();
    }

    @Override
    public Object getItem(int position) {
        return mLocalVideos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewHolder holder = null;
        if(convertView == null) {
            holder = new ViewHolder();
            if(mShowMode == LocalVideoFragment.LIST_MODE) {
                LogUtil.d(TAG, "list mode");
                convertView = inflater.inflate(R.layout.video_list_item, parent, false);
                holder.mListVideoThumb = (ImageView) convertView.findViewById(R.id.iv_video_thumb);
                holder.mListNewVideoIcon = (ImageView) convertView.findViewById(R.id.iv_new_video_icon);
                holder.mListVideoDuration = (TextView) convertView.findViewById(R.id.tv_video_duration);
                holder.mListVideoName = (TextView) convertView.findViewById(R.id.tv_video_name);
                holder.mListVideoDate = (TextView) convertView.findViewById(R.id.tv_video_date);
                holder.mListVideoSize = (TextView) convertView.findViewById(R.id.tv_video_size);
                holder.mListPlayVideo = (ImageView) convertView.findViewById(R.id.iv_play);
            } else if(mShowMode == LocalVideoFragment.GRID_MODE) {
                LogUtil.d(TAG, "grid mode");
                convertView = inflater.inflate(R.layout.video_grid_item, parent, false);
                holder.mGridVideoThumb = (ImageView) convertView.findViewById(R.id.iv_grid_video_thumb);
                holder.mGridVideoNewVideoIcon = (ImageView) convertView.findViewById(R.id.iv_grid_new_video_icon);
                holder.mGridVideoDuration = (TextView) convertView.findViewById(R.id.tv_grid_video_duration);
                holder.mGridVideoName = (TextView) convertView.findViewById(R.id.tv_grid_video_name);
            } else if(mShowMode == LocalVideoFragment.FOLDER_MODE) {
                LogUtil.d(TAG, "folder mode");
                convertView = inflater.inflate(R.layout.video_folder_item, parent, false);
                holder.mFolderName = (TextView) convertView.findViewById(R.id.tv_folder_name);
                holder.mFolderVideoNum = (TextView) convertView.findViewById(R.id.tv_video_num);
                holder.mFolderPath = (TextView) convertView.findViewById(R.id.tv_folder_path);
                holder.mFolderOperation = (ImageView) convertView.findViewById(R.id.iv_folder_operation);
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        LocalVideo localVideo = mLocalVideos.get(position);
        if(mShowMode == LocalVideoFragment.LIST_MODE) {
            holder.mListVideoName.setText(localVideo.getDisplayName());
            String videoDuration = getVideoDuration(localVideo.getDuration());
            holder.mListVideoDuration.setText(videoDuration);
            //get video date is milliseconds
            String videoDate = getVideoDate(localVideo.getDateModified());
            holder.mListVideoDate.setText(videoDate);
            //get video size is bytes
            long bytes = Long.parseLong(localVideo.getSize());
            holder.mListVideoSize.setText(formatFileSize(bytes));
            //get video thumbnails and put it in Cache
            String videoId = localVideo.getVideoId();
            String videoThumbPath = localVideo.getData();
            
            putBitmapToCache(videoId, videoThumbPath);
            
            // get bitmap from cache
            Bitmap bm = mThumbCache.getBitmapFromCache(videoThumbPath);
            if(bm != null) {
                holder.mListVideoThumb.setImageBitmap(bm);
            } else {
                holder.mListVideoThumb.setImageResource(R.drawable.video_icon_list_pic);
            }
        } else if(mShowMode == LocalVideoFragment.GRID_MODE) {
            LogUtil.d(TAG, "adapter grid mode");
            String videoDuration = getVideoDuration(localVideo.getDuration());
            holder.mGridVideoDuration.setText(videoDuration);
            holder.mGridVideoName.setText(localVideo.getDisplayName());
          //get video thumbnails and put it in Cache
            String videoId = localVideo.getVideoId();
            String videoThumbPath = localVideo.getData();
            
            putBitmapToCache(videoId, videoThumbPath);
            
            // get bitmap from cache
            Bitmap bm = mThumbCache.getBitmapFromCache(videoThumbPath);
            if(bm != null) {
                holder.mGridVideoThumb.setImageBitmap(bm);
            } else {
                holder.mGridVideoThumb.setImageResource(R.drawable.video_icon_list_pic);
            }
        } else if(mShowMode == LocalVideoFragment.FOLDER_MODE) {
//            holder.mFolderName
//            holder.mFolderVideoNum
//            holder.mFolderPath
//            holder.mFolderOperation
        }
        return convertView;
    }

    private void putBitmapToCache(String videoId, String videoThumbPath) {
        Bitmap bitmap = null;
        if(videoThumbPath != null) { 
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(videoThumbPath, options);// return null(no bitmap),but save in options
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
                bitmap = BitmapFactory.decodeFile(videoThumbPath, options);
            } catch (Exception e) {
                LogUtil.d(TAG, "out of memory error");
                e.printStackTrace();
            }
        }
        if(bitmap == null) {
            try {
                bitmap = MediaStore.Video.Thumbnails.getThumbnail(mContentResolver, Long.parseLong(videoId), 
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
        
        if(mThumbCache != null) {
            synchronized (mThumbCache) {
                mThumbCache.putBitmapToCache(videoThumbPath, bitmap);
            }
        }
    }

    //show progressDialog
    private void startProgressDialog() {
        if(progressDialog == null) {
            progressDialog = CustomProgressDialog.createDialog(mContext);
            progressDialog.setMessage(R.string.general_loading);
        }
        progressDialog.show();
    }
    
    //dismiss progressDialog
    private void stopProgressDialog() {
        if(progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
    
    // calculate thumbnail scaling
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
    
    private String formatFileSize(long bytes) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeStr = "";
        if(bytes < B) {//Bytes
            fileSizeStr = df.format((double)bytes) + "B";
        } else if(bytes < KB) {//KB
            fileSizeStr = df.format((double)bytes / B) + "K";
        } else if(bytes < MB) {//MB
            fileSizeStr = df.format((double)bytes / KB) + "M";
        } else {//GB
            fileSizeStr = df.format((double)bytes / MB) + "G";
        }
        return fileSizeStr;
    }
    private String getVideoFolderName(String videoPath) {
//        sdcard/videos/baiduvideo/1.mp3
//        videoPath.lastIndexOf("/")
        return null;
    }
    
    private String getVideoDate(String videoDate) {
        long milliseconds = Long.parseLong(videoDate);
        return formatDate(milliseconds);
    }

    private String formatDate(long milliseconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(new Date(milliseconds));
        return dateStr;
    }
    
    private String getVideoDuration(String videoDuration) {
        long millionTime = Long.parseLong(videoDuration);
        if (millionTime > 0 && millionTime < 1000) {
            millionTime = 1000;// ms
        }
        long duration = millionTime / 1000;// ms tranfer to s
        return formatDuration(duration);
    }

    private String formatDuration(long duration) {
        if (mContext == null) {
            return String.format("%2d", 0, mContext.getString(R.string.second));
        }
        long second = duration % 60;
        long minute = (duration / 60) % 60;
        long hour = duration / 3600;
        if (hour > 0) {
            return String.format("%02d:%02d:%02d", hour, minute, second);
        } else if (minute > 0) {
            return String.format("%02d:%02d", minute, second);
        } else {
            return String.format("00:%02d", second);
        }
    }

    private static class ViewHolder {
        public ImageView mListVideoThumb;
        public ImageView mListNewVideoIcon;
        public TextView mListVideoDuration;
        public TextView mListVideoName;
        public TextView mListVideoDate;
        public TextView mListVideoSize;
        public ImageView mListPlayVideo;
        public ImageView mGridVideoThumb;
        public ImageView mGridVideoNewVideoIcon;
        public TextView mGridVideoDuration;
        public TextView mGridVideoName;
        public TextView mFolderName;
        public TextView mFolderVideoNum;
        public TextView mFolderPath;
        public ImageView mFolderOperation;
    }
}
