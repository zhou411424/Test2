package com.leven.videoplayer.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.leven.videoplayer.R;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.provider.MediaStore;
import android.util.Log;

public class Utils {
	public static final String TAG = "Utils";
	public static final String M3U8_HISTORY = "m3u8_history_record";
	private static final long B = 1024;
    private static final long KB = 1024 * 1024;
    private static final long MB = 1024 * 1024 * 1024;
	
    // -----------video info start--------------------
    public static String formatFileSize(long bytes) {
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
    
	public static String formatDate(long milliseconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(new Date(milliseconds));
        return dateStr;
    }

	public static String getVideoDuration(long millionTime) {
	    if(millionTime > 0 && millionTime < 1000) {
	        millionTime = 1000;//ms
	    }
	    long duration = millionTime / 1000;// ms tranfer to s
	    return formatDuration(duration);
    }

	public static String formatDuration(long duration) {
        long second = duration % 60;
        long minute = (duration / 60) % 60;
        long hour = duration / 3600;
        if(hour > 0) {
            return String.format("%02d:%02d:%02d", hour, minute, second);
        } else if(minute > 0) {
            return String.format("%02d:%02d", minute, second);
        } else {
            return String.format("00:%02d", second);
        }
    }
	// -----------video info end--------------------
	
	public static String getFileMD5String(File file) {
		String ret = "";
		FileInputStream in = null;
		FileChannel ch = null;
		try {
			in = new FileInputStream(file);
			ch = in.getChannel();
			ByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0,
					file.length());
		    MessageDigest md = MessageDigest.getInstance("MD5");
		    md.update(byteBuffer);
			ret = new String(md.digest(), "utf-8");
			byteBuffer = null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (ch != null) {
				try {
					ch.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}  
	
	private static boolean download_SO(String strUrl, String StrFilePath) {
		int count;
		InputStream input = null;
		OutputStream output = null;
		URLConnection urlConnection = null;
		Boolean bret = false;
		
		try {			 
			URL url = new URL(strUrl);
			urlConnection = url.openConnection();
			urlConnection.connect();
			int lenghtOfFile = urlConnection.getContentLength();
			/*if (-1 == lenghtOfFile) {
				return false;
			}*/
			
			// download the file
			input = new BufferedInputStream(urlConnection.getInputStream());
			File file = new File(StrFilePath);
			if (!file.exists()) {
				file.createNewFile();
			}
			output = new FileOutputStream(file);
			byte data[] = new byte[8192];
			while ((count = input.read(data)) != -1) {
				output.write(data, 0, count);
			}
			output.flush();
			bret = true;
			data = null;
		} catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
        }
        return bret;
	}
	
	public static String getMD5( String strfilename, Context context) {
		String strFilePath = context.getFilesDir()+ "/" + strfilename;
		return getFileMD5(strFilePath);
	}
	
	public static String getFileMD5(String strfilePath) {
		MessageDigest md = null;
		BufferedInputStream in = null;
		try {
			md = MessageDigest.getInstance("MD5");
			in = new BufferedInputStream(new FileInputStream(strfilePath));
			byte[] bytes = new byte[8192];
			int byteCount;
			while ((byteCount = in.read(bytes)) > 0) {
				md.update(bytes, 0, byteCount);
			}
			bytes = null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return byte2hex(md.digest());
	}
	
	public static int checkNetworkInfo(Context context) {
		int netType = 0;
		ConnectivityManager conMan = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		State mobile = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.getState();
		State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.getState();
		if (mobile == State.CONNECTED || mobile == State.CONNECTING) {
			netType = 1;
			Log.d(TAG, "mobile connect");
		}

		if (wifi == State.CONNECTED || wifi == State.CONNECTING) {
			netType = 2;
			Log.d(TAG, "wifi connect");
		}

		return netType;
	}

	
	public static String byte2hex(byte[] b) {
		String hs = "";
		String stmp = "";
		for (int n = 0; n < b.length; n++) {
			stmp = Integer.toHexString(b[n] & 0XFF);
			if (stmp.length() == 1)
				hs = hs + "0" + stmp;
			else
				hs = hs + stmp;
			if (n < b.length - 1)
				hs = hs + ":";
		}
		return hs.toUpperCase();
	}   

	public static boolean downloadFile(String strURL, String strFilePath) {
		boolean bret = false;
		URL myURL;
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			myURL = new URL(strURL);
			HttpURLConnection conn = (HttpURLConnection) myURL.openConnection();
			conn.setConnectTimeout(6 * 1000);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept-Language", "zh-CN");
			conn.setRequestProperty("Referer", strURL);
			conn.setRequestProperty("Charset", "UTF-8");
			conn.setRequestProperty(
					"User-Agent",
					"Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.connect();

			is = conn.getInputStream();
			File myFileTemp = new File(strFilePath);

			// the file to which this stream writes
			fos = new FileOutputStream(myFileTemp);
			byte buf[] = new byte[8192];
			int numread;
			while ((numread = is.read(buf)) != -1) {
				fos.write(buf, 0, numread);
			}
			fos.flush();
			bret = true;
			buf = null;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != is) {
					is.close();
				}
				if (null != fos) {
					fos.close();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return bret;
	}   
	
	public static boolean copyLibrary(String oldPath, String newName,
			Context context) {
		int byteread = 0;
		boolean bret = false;
		InputStream inStream = null;
		FileOutputStream fos = null;

		try {
			File oldfile = new File(oldPath);
			if (oldfile.exists()) {
				context.deleteFile(newName);
				inStream = new FileInputStream(oldPath);
				fos = context.openFileOutput(newName,
						Context.MODE_WORLD_WRITEABLE);

				byte[] buffer = new byte[8192];
				while ((byteread = inStream.read(buffer)) != -1) {
					fos.write(buffer, 0, byteread);
				}
				fos.flush();
				buffer = null;
				bret = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != inStream) {
					inStream.close();
				}

				if (null != fos) {
					fos.close();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return bret;
	}
	
	public static void insertVideoPos(String strPath, int iPos, Context context) {
		ContentValues values = new ContentValues();
		values.put(Constants.VideoColumns.PATH, strPath);
		values.put(Constants.VideoColumns.POSTION, iPos);
		context.getContentResolver().insert(Constants.VideoColumns.CONTENT_URI, values);
	}

	public static void updateVideoPos(String strPath, ContentValues values, Context context) {
		StringBuffer whereString = new StringBuffer(
				Constants.VideoColumns.PATH + "=" + "?");
		
		context.getContentResolver().update(
				Constants.VideoColumns.CONTENT_URI, 
				values,
				whereString.toString(), 
				new String[] { strPath });
	}
	
	public static void deleteVideoPos(String strPath, Context context) {
		StringBuffer whereString = new StringBuffer(
				Constants.VideoColumns.PATH + "=" + "?");
		context.getContentResolver().delete(
				Constants.VideoColumns.CONTENT_URI, 
				whereString.toString(),
				new String[] { strPath });
	}
	
	public static int getVideoPos(String strPath, Context context) {
		int iRet = -1;
		final String[] VidePath_PROJECTION = new String[] {
				Constants.VideoColumns._ID, 
				Constants.VideoColumns.PATH,
				Constants.VideoColumns.POSTION };

		StringBuffer whereString = new StringBuffer(
				Constants.VideoColumns.PATH + "=" + "?");
		
		Cursor cur = context.getContentResolver().query(
				Constants.VideoColumns.CONTENT_URI, 
				VidePath_PROJECTION,
				whereString.toString(),
				new String[] { strPath }, 
				null);
		
		if (null == cur) {
		    return iRet;
		}
		
		cur.moveToFirst();
		if (cur.getCount() != 0) {
			iRet = cur.getInt(2);
		}

		if (null != cur) {
			cur.close();
		}
		return iRet;
	}
	
	public static int getVideoCounts(Context context) {
		int iCount = 0;
		final String[] VidePath_PROJECTION = new String[] {
				Constants.VideoColumns._ID, Constants.VideoColumns.PATH};

		Cursor cur = context.getContentResolver().query(
				Constants.VideoColumns.CONTENT_URI, VidePath_PROJECTION,
				null, null, null);
		cur.moveToFirst();
		iCount = cur.getCount();
		if (null != cur) {
			cur.close();
		}
		return iCount;
	}
	
	public static int deleteInvalidKey(Context context) {
		int iCount = 0;
		final String[] VidePath_PROJECTION = new String[] {
				Constants.VideoColumns._ID, Constants.VideoColumns.PATH};

		Cursor cur = context.getContentResolver().query(
				Constants.VideoColumns.CONTENT_URI, VidePath_PROJECTION,
				null, null, null);
		cur.moveToFirst();
		String strPath;
		iCount = cur.getCount();
		File file;
		do {
			strPath = cur.getString(1);
			file = new File(strPath);
			if (!file.exists()) {
				deleteVideoPos(strPath, context);
			}	
		}while (cur.moveToNext());
		if (null != cur) {
			cur.close();
		}
		return iCount;
	}
	
	
	public static void recordM3U8History(final Context context, final String strKey, final int iValue) {
	    SharedPreferences prefer = context.getSharedPreferences(M3U8_HISTORY, context.MODE_WORLD_WRITEABLE);
	    Editor editor = prefer.edit();
	    editor.putLong(strKey, iValue);
	    editor.commit();
	}
	
	public static long getM3U8History(final Context context, final String strKey) {
	    SharedPreferences prefer = context.getSharedPreferences(M3U8_HISTORY, context.MODE_WORLD_READABLE);
	    return prefer.getLong(strKey, 0);
	}
	
	public static boolean isM3U8Path(final String strPath) {
	    if (-1 != strPath.lastIndexOf(".m3u8") || strPath.startsWith("http") || strPath.startsWith("https")){
	        return true;
	    }
	    
	    return false;
	}
}
