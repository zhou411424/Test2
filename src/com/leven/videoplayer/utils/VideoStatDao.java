package com.leven.videoplayer.utils;

import java.io.File;
import java.util.ArrayList;

import com.leven.videoplayer.db.DatabaseHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.MediaStore;
import android.util.Log;

public class VideoStatDao {
	public static final String _ID = "stat_id";
    public static final String PLAY_POSITION = "play_position";
    public static final String VIDEO_ID = "video_id";
    public static final String FILE_PATH = "path";
    public static final String LAST_PLAYED = "last_played";
	private static final Integer SYNCHRONIZE = 10;
	private static final String DATABASE_NAME = "playstat.db";
	private static final int DATABASE_VERSION = 2;
	private static final String THUMBMISS_TABLE_NAME = "thumbmiss";
	private static final String[] THUMB_COLUMNS = new String[] {_ID, VIDEO_ID, FILE_PATH};
	private static final String VIDEO_ID_SELECTION = VIDEO_ID + " = ?";
	private static final String PLAYSTAT_TABLE_NAME = "playstat";
	private static final String VIDEO_ORDER = LAST_PLAYED + " desc";
	private static final String TAG = "VideoStatDao";
	private final String[] STAT_COLUMNS = new String[] { _ID,
			PLAY_POSITION, VIDEO_ID,FILE_PATH, LAST_PLAYED };
	private DatabaseHelper mDatabaseHelper;
	private SQLiteDatabase mDB;
	private static volatile Integer mUsers = 0;
	private Context mContext;
	private boolean mbNeedSync = false;
	@SuppressWarnings("unused")
	private static class PlayFileInfo {
		public int mVideoId;
		public int mPlayPos;
		public boolean mbLastPlayed;
		public String mPath;
		
		public PlayFileInfo(int mVideoId, int mPlayPos, boolean mbLastPlayed,
				String mPath) {
			super();
			this.mVideoId = mVideoId;
			this.mPlayPos = mPlayPos;
			this.mbLastPlayed = mbLastPlayed;
			this.mPath = mPath;
		}
		
	}
    
    public VideoStatDao(Context context) {
		super();
		synchronized (SYNCHRONIZE) {
			if(mDatabaseHelper == null) {
				mContext = context.getApplicationContext();
				mDatabaseHelper = new DatabaseHelper(context, 
						DATABASE_NAME, null, DATABASE_VERSION);
				mDB = mDatabaseHelper.getWritableDatabase();
			}
			mUsers ++;
		}
	}

	public static VideoStatDao open(Context context) {
    	return new VideoStatDao(context);
    }
	
	public void close() {
	    synchronized (SYNCHRONIZE) {
            if(mDB != null) {
                mDB.close();
                mDB = null;
            }
            
            if(mDatabaseHelper != null) {
                mDatabaseHelper.close();
                mDatabaseHelper = null;
            }
        }
	}
	
	public Cursor queryPlayedInfoByVideoId(int videoId) {
		Cursor cursor = null;
		if(mDB == null) {
			return cursor;
		}
		String[] selectionArgs = { String.valueOf(videoId) };
		cursor = mDB.query(PLAYSTAT_TABLE_NAME, STAT_COLUMNS, 
				VIDEO_ID_SELECTION, selectionArgs, null, null, VIDEO_ORDER);
		return cursor;
	}
	
	public Cursor queryAllPlayedInfo() {
		Cursor cursor = null;
		if(mDB == null) {
			return cursor;
		}
		cursor = mDB.query(PLAYSTAT_TABLE_NAME, STAT_COLUMNS, 
				VIDEO_ID_SELECTION, null, null, null, VIDEO_ORDER);
		return cursor;
	}
	
	public void updateThumbInfo(int videoId, String path) {
		synchronized (SYNCHRONIZE) {
			if(mDB == null) {
				return;
			}
			String[] selectionArgs = { String.valueOf(videoId) };
			Cursor cursor = mDB.query(THUMBMISS_TABLE_NAME, THUMB_COLUMNS, 
					VIDEO_ID_SELECTION, selectionArgs, null, null, null);
			if(cursor != null && cursor.moveToFirst()) {
				ContentValues values = new ContentValues();
				values.put(FILE_PATH, path);
				String[] whereArgs = { String.valueOf(videoId) };
				mDB.update(THUMBMISS_TABLE_NAME, values, VIDEO_ID_SELECTION, whereArgs);
			} else {
				ContentValues values = new ContentValues();
				values.put(VIDEO_ID, videoId);
				values.put(FILE_PATH, path);
				mDB.insert(THUMBMISS_TABLE_NAME, null, values);
			}
			
			if(cursor != null) {
				cursor.close();
			}
		}
	}
	
	public synchronized boolean isSyncWithVideoDB() {
		return innerSyncDB();
	}

	private boolean innerSyncDB() {
		synchronized (SYNCHRONIZE) {
			if(mContext == null) {
				return false;
			}
			
			File dbFile = mContext.getDatabasePath(DATABASE_NAME);
			if(dbFile != null && dbFile.length() < 1024 * 1024) {
				Log.d(TAG, "the database size(" + dbFile.length()
                        + ") is not bigger than 1Mb, skip");
                return false;
			}
			
			Cursor cr = queryAllPlayedInfo();
			if(cr == null) {
				return true;
			}
			
			VideoObservable videoObservable = new VideoObservable(mContext);
			ArrayList<Integer> invalidIds = new ArrayList<Integer>();
			invalidIds.clear();
			
			while(continueToStopSync() && cr.moveToNext() != false) {
				int video_id = cr.getInt(cr.getColumnIndex(VIDEO_ID));
				String video_path = cr.getString(cr.getColumnIndex(FILE_PATH));
				Cursor videoCursor = null;
				String path_in_db = null;
				try {
					videoCursor = videoObservable.queryThumbnails(video_id);
					if(videoCursor != null && videoCursor.moveToFirst()) {
						path_in_db = videoCursor.getString(videoCursor.getColumnIndex(MediaStore.Video.Media.DATA));
					}
				} finally {
					if(videoCursor != null) {
						videoCursor.close();
					}
				}
				if(path_in_db == null || path_in_db.equals(video_path)) {
					String[] whereArgs = { String.valueOf(video_id) }; 
					mDB.delete(PLAYSTAT_TABLE_NAME, VIDEO_ID_SELECTION, whereArgs);
				}
				
			}
			if(cr != null) {
				cr.close();
			}
			changeSyncState(false);
			return true;
		}
	}
	
	private synchronized boolean continueToStopSync() {
		synchronized (VideoStatDao.this) {
			return mbNeedSync ;
		}
	}
	
	private void changeSyncState(boolean bNeedSync) {
		synchronized (VideoStatDao.this) {
			mbNeedSync = bNeedSync;
		}
	}
	
	public boolean update(int videoId, int playPos, String path, boolean last_played) {
		PlayFileInfo playFileInfo = new PlayFileInfo(videoId, playPos, last_played, path);
		int result = innerUpdateFile(playFileInfo);
		if(result != 1) {
			// insert operation or return error
			Log.d(TAG, "failed to do inner file info update,with return value " + result);
			return false;
		} else {
			// update operation
			return true;
		}
	}
	
	private int innerUpdateFile(PlayFileInfo playFileInfo) {
		int videoId = playFileInfo.mVideoId;
		int playPos = playFileInfo.mPlayPos;
		String path = playFileInfo.mPath;
		boolean last_played = playFileInfo.mbLastPlayed;
		int count = 0;
		if(mDB == null) {
			return 0;
		}
		
		// update latest played record
		synchronized (SYNCHRONIZE) {
			Cursor cr = null;
			try {
				cr = queryPlayedInfoByVideoId(videoId);
				if(cr != null && cr.moveToFirst()) {
					ContentValues values = new ContentValues();
					String[] whereArgs = { String.valueOf(videoId) };
					values.put(PLAY_POSITION, playPos);
					values.put(FILE_PATH, path);
					values.put(LAST_PLAYED, last_played == true ? "true" : "false");
					count = mDB.update(PLAYSTAT_TABLE_NAME, values, VIDEO_ID_SELECTION, whereArgs);
					LogUtil.d(TAG, "update() ==> count="+count);
				} else {
					count = insert(videoId, playPos, path, last_played);
					LogUtil.d(TAG, "insert() ==> count="+count);
				}
			} finally {
				if(cr != null) {
					cr.close();
				}
			}
			return count;
		}
	}
	
	private int insert(int videoId, int playPos, String path, boolean last_played) {
		int count = 0;
		ContentValues values = new ContentValues();
		values.clear();
		values.put(VIDEO_ID, videoId);
		values.put(PLAY_POSITION, playPos);
		values.put(FILE_PATH, path);
		values.put(LAST_PLAYED, last_played == true ? "true" : "false");
		count = (int) mDB.insert(PLAYSTAT_TABLE_NAME, null, values);
		return count;
	}
}
