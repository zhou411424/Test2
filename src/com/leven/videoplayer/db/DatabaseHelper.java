package com.leven.videoplayer.db;

import com.leven.videoplayer.utils.VideoStatDao;

import android.content.Context;
//import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	
	private static final String TAG = "DatabaseHelper";
	private static final String PLAYSTAT_TABLE_NAME = "playstat";
	private static final String THUMBMISS_TABLE_NAME = "thumbmissing";

	/*public DatabaseHelper(Context context, String name, CursorFactory factory,
			int version, DatabaseErrorHandler errorHandler) {
		super(context, name, factory, version, errorHandler);
	}*/

	public DatabaseHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "database created");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + PLAYSTAT_TABLE_NAME
				+ " (" + VideoStatDao._ID + " INTEGER PRIMARY KEY,"
				+ VideoStatDao.PLAY_POSITION + " INTEGER,"
				+ VideoStatDao.VIDEO_ID + " INTEGER,"
				+ VideoStatDao.FILE_PATH + " TEXT,"
				+ VideoStatDao.LAST_PLAYED + " TEXT"
				+ ");");
		
		db.execSQL("CREATE TABLE IF NOT EXISTS " + THUMBMISS_TABLE_NAME
				+" (" + VideoStatDao._ID + " INTEGER PRIMARY KEY,"
				+ VideoStatDao.VIDEO_ID + " INTEGER,"
				+ VideoStatDao.FILE_PATH + " TEXT"
				+");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "upgrade database form version " + oldVersion + " to " 
				+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + PLAYSTAT_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + THUMBMISS_TABLE_NAME);
		onCreate(db);
	}

}
