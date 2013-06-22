package com.leven.videoplayer.utils;

import android.net.Uri;
import android.provider.BaseColumns;

public final class Constants {
	public static final Object SYNC_Duration = new Object();
	public static final Object SYNC_CURRENTPOSTIION = new Object();
	public static final Object SYNC_VIDEOWIDTH = new Object();
	public static final Object SYNC_VIDEOHEIGHT = new Object();
	public static final Object SYNC_ISPLAYING = new Object();
	public static final Object SYNC_SURFACE_RESIZE = new Object();

	public static final int CURRPOSITON_0 = 0;
	public static final int DURATION_1 = 1;
	public static final int VIDEOWIDTH_2 = 2;
	public static final int VIDEOHEIGH_3 = 3;
	public static final int ISPLAYING_4 = 4;
	public static final int START_5 = 5;
	public static final int ERROR_6 = 6;
	public static final int STOP_7 = 7;
	public static final int CACHE_8 = 8;
	public static final int CACHE_PERCENT_9 = 9;

	public static final int SDL_USEREVENT = 0x8000;
	public static final int CMD_GETCURRPOSITION = (SDL_USEREVENT + 3);
	public static final int CMD_GETDURATION = (SDL_USEREVENT + 4);
	public static final int CMD_GETVIDEOHEIGHT = (SDL_USEREVENT + 5);
	public static final int CMD_GETVIDEOWIDTH = (SDL_USEREVENT + 6);
	public static final int CMD_SETVIDEOSEEKTO = (SDL_USEREVENT + 7);
	public static final int CMD_ISPLAYING = (SDL_USEREVENT + 8);
	public static final int CMD_SETVIDEOSIZE = (SDL_USEREVENT + 9);
	public static final int CMD_PLAYERPAUSE = (SDL_USEREVENT + 10);
	public static final int CMD_PLAYEREXIT = (SDL_USEREVENT + 11);
	public static final int CMD_VIDEO_FORWARD15s = 22;
	public static final int CMD_VIDEO_BACK5s = 21;
	public static final int mWAIT1000MS = 1000;
	public static final int mREFRESH = 200;
	public static final int mREFRESHCOUNT = 30;

	public static final int CMD_TITLE = 0;
	public static final int CMD_CURRPOSITION = 1;
	public static final int CMD_EXIT = 2;
	public static final int CMD_SPEED = 3;

	public static final String VIDEO_POSITON = "start-positon";
	public static final String VIDEO_WIDTH = "width";
	public static final String VIDEO_HEIGHT = "height";
	public static final String VIDEO_FORMAT = "format";
	public static final String VIDEO_PATH = "path";
	
	public static final String UA = "User-Agent";
	public static final String RERERER = "Referer";
	
	public static final String LIBAVFORMAT_IDENT = "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_2_1 like Mac OS X; zh-cn) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8C148 Safari/6533.18.5";
	public static final String AUTHORITY = "com.baidu.video";
	public static final String CONFIG_PREFERENCES = "config_pref";

    private Constants() {}
    
    /**
     * Notes table
     */
    public static final class VideoColumns implements BaseColumns {
        // This class cannot be instantiated
        private VideoColumns() {}

        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/netvideos");


        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.baidu.netvideo";


        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.baidu.netvideo";

        public static final String PATH = "path";

        public static final String POSTION = "position";     
    }
    
	public static int getResourceIdByName(String packageName, String className,
			String name) {
		Class r = null;
		int id = 0;
		try {
			r = Class.forName(packageName + ".R");

			Class[] classes = r.getClasses();
			Class desireClass = null;

			for (int i = 0; i < classes.length; i++) {
				if (classes[i].getName().split("\\$")[1].equals(className)) {
					desireClass = classes[i];

					break;
				}
			}
			if (desireClass != null)
				id = desireClass.getField(name).getInt(desireClass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}

		return id;
	}
}
