package com.leven.videoplayer.utils;

import java.util.Observable;

import com.leven.videoplayer.persistance.SortCursor;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;


public class VideoObservable extends Observable {
	private Context mContext;
	private final static Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    private static String[] VIDEO_COLUMNS = new String[] {
            MediaStore.Video.Media.DURATION, MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE, MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED };

    private static String[] VIDEO_BUCKET = new String[] {
            MediaStore.Video.Media.DURATION, MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE, MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATA, MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME };
    private static String VIDEO_ORDER = MediaStore.Video.Media.DATE_ADDED + " desc";
    private static String VIDEO_PATH_SELECTION = MediaStore.Video.Media.DATA + "=?";
    private static String VIDEO_ID_SELECTION = MediaStore.Video.Media._ID + "=?";
    private static String BUCKET_ID_SELECTION = MediaStore.Video.VideoColumns.BUCKET_ID + " = ?";
    //thumbnails query info
    private final static Uri THUMBNAILS_URI = MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI;
    private static String[] THUMBNAILS_COLUMNS = new String[] {
            MediaStore.Video.Thumbnails._ID, MediaStore.Video.Thumbnails.DATA,
            MediaStore.Video.Thumbnails.VIDEO_ID };
    private static String THUMBNAILS_VIDEO_ID_SELECTION = MediaStore.Video.Thumbnails.VIDEO_ID
            + "=?";
    private static String THUMBNAILS_ID_SELECTION = MediaStore.Video.Thumbnails._ID
            + "=?";
    private static String THUMBNAILS_ORDER = MediaStore.Video.Thumbnails.VIDEO_ID
            + " asc";
    
	public VideoObservable(Context mContext) {
		this.mContext = mContext;
	}
	
	// in date added
	public Cursor queryMideaFileInfo() {
		return mContext.getContentResolver().query(VIDEO_URI, 
				VIDEO_COLUMNS, null, null, VIDEO_ORDER);
	}
	
	// in name order
	public Cursor queryMideaFileInfoInName() {
		Cursor c = mContext.getContentResolver().query(VIDEO_URI,
                VIDEO_COLUMNS, null, null, null);
		if(c == null) {
			return null;
		} else {
//			boolean bDesc = true;//descending order
			return c;
		}
	}
	
	// get thumbnails
	public Cursor queryThumbnails(int videoId) {
		String[] selectionArgs = new String[] { videoId + "" };
		Cursor cursor = mContext.getContentResolver().query(THUMBNAILS_URI, 
				THUMBNAILS_COLUMNS, THUMBNAILS_VIDEO_ID_SELECTION, selectionArgs, THUMBNAILS_ORDER);
		return cursor;
	}
	
	public long queryVideoIdByPath(String videoPath) {
		String[] selectionArgs = { videoPath };
		Cursor cr = null;
		long videoId = -1;
		try {
			cr = mContext.getContentResolver().query(VIDEO_URI, VIDEO_COLUMNS, 
					VIDEO_PATH_SELECTION, selectionArgs, null);
			if(cr != null && cr.moveToFirst()) {
				videoId = cr.getLong(cr.getColumnIndex(MediaStore.Video.Media._ID));
			}
		} finally {
			if(cr != null) {
				cr.close();
			}
		}
		return videoId;
	}
	
	public String queryVideoPathById(int videoId) {
		String[] selectionArgs = { videoId+"" };
		Cursor cr = null;
		String videoPath = null;
		try {
			cr = mContext.getContentResolver().query(VIDEO_URI, VIDEO_COLUMNS, 
					VIDEO_ID_SELECTION, selectionArgs, null);
			if(cr != null && cr.moveToFirst()) {
				videoPath = cr.getString(cr.getColumnIndex(MediaStore.Video.Media.DATA));
			}
		} finally {
			if(cr != null) {
				cr.close();
			}
		}
		return videoPath;
	}
	
	public int queryVideoCountInFolder(String folder) {
		Cursor cur = null;
		int count = -1;
		try {
			cur = mContext.getContentResolver().query(
					VIDEO_URI, VIDEO_BUCKET, BUCKET_ID_SELECTION, new String[] { folder }, null);
			if(cur != null && cur.moveToFirst()) {
				count = cur.getCount();
			}
			
		} finally {
			if(cur != null) {
				cur.close();
			}
		}
		return count;
	}
	
	public int queryVideoCount() {
		Cursor cur = null;
		int count = -1;
		try {
			cur = mContext.getContentResolver().query(
					VIDEO_URI, VIDEO_COLUMNS, null, null, VIDEO_ORDER);
			if(cur != null && cur.moveToFirst()) {
				count = cur.getCount();
			}
			
		} finally {
			if(cur != null) {
				cur.close();
			}
		}
		return count;
	}
	
	public String queryNextVideoName_orderinAddedDate_InFolderByID(int id, String path) {
		Cursor cur = null;
		try {
			cur = mContext.getContentResolver().query(VIDEO_URI, VIDEO_BUCKET, 
					BUCKET_ID_SELECTION, new String[] { path }, null);
			if(cur != null) {
				SortCursor sc = new SortCursor(cur, MediaStore.Video.Media.DATE_ADDED);
				sc.moveToFirst();
				if(id <= sc.getCount()) {
					sc.moveToPosition(id);
					return sc.getString(6);
				}
			}
		} finally {
			if(cur != null) {
				cur.close();
			}
		}
		return null;
	}
	
	public String queryNextVideoNameByID(int id) {
		Cursor cur = null;
		try {
			cur = mContext.getContentResolver().query(VIDEO_URI, VIDEO_COLUMNS, 
					null, null, VIDEO_ORDER);
			if(cur != null) {
				SortCursor sc = new SortCursor(cur, MediaStore.Video.Media.DATE_ADDED);
				sc.moveToFirst();
				if(id <= sc.getCount()) {
					sc.moveToPosition(id);
					return sc.getString(6);
				}
			}
		} finally {
			if(cur != null) {
				cur.close();
			}
		}
		return null;
	}
	
	public String queryNextVideoNameByID_InNameOrder_InFilePath(int id, String path) {
		Cursor cur = null;
		try {
			cur = mContext.getContentResolver().query(VIDEO_URI, VIDEO_BUCKET, 
					BUCKET_ID_SELECTION, new String[] { path }, null);
			if(cur != null) {
				SortCursor sc = new SortCursor(cur, MediaStore.Video.Media.DISPLAY_NAME, true);
				sc.moveToFirst();
				if(id <= sc.getCount()) {
					sc.moveToPosition(id);
					return sc.getString(6);
				}
			}
		} finally {
			if(cur != null) {
				cur.close();
			}
		}
		return null;
	}
	
	public String queryNextVideoNameByID_InNameOrder(int id) {
		Cursor cur = null;
		try {
			cur = mContext.getContentResolver().query(VIDEO_URI, VIDEO_BUCKET, 
					null, null, null);
			if(cur != null) {
				SortCursor sc = new SortCursor(cur, MediaStore.Video.Media.DISPLAY_NAME, true);
				sc.moveToFirst();
				if(id <= sc.getCount()) {
					sc.moveToPosition(id);
					return sc.getString(6);
				}
			}
		} finally {
			if(cur != null) {
				cur.close();
			}
		}
		return null;
	}
}
