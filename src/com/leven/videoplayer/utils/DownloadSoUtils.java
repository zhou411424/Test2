package com.leven.videoplayer.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.leven.videoplayer.persistance.SecondaryStorageReflect;

//download so file for play video. e.g., libffmpeg.so
public class DownloadSoUtils {
	
	private static final String STANDARD_PLUG_IN_DELETE = "libffmpeg.txt";
	private static final String STANDARD_PLUG_IN_ZIP = "libffmpeg.zip";
	public static final String STANDARD_PLUG_IN_LIB = "libffmpeg.so";
	private static final int UNZIP_SUCCESS = 0;
	private static final int UNZIP_ERROR = 1;
	private static boolean mbBreakCircle = false;
	private static int miDownloadLenth;
	private static ConnectivityManager mConnectivityManager;
	private static final int NO_DATA = 0;
	private static final int GPRS_3G_DATA = 1;
	private static final int WIFI_DATA = 2;
	private static final String TAG = "DownloadSOUtils";
	
	// check network type
	public static int checkNetworkType(Context context) {
		int netType = NO_DATA;
		mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		State mobileState = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();
		State wifiState = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
		if(mobileState == State.CONNECTED || mobileState == State.CONNECTING) {
			netType = GPRS_3G_DATA;
			Log.d(TAG, "mobile connect");
		}
		
		if(wifiState == State.CONNECTED || wifiState == State.CONNECTING) {
			netType = WIFI_DATA;
			Log.d(TAG, "wifi connect");
		}
		return netType;
	}

	//download libffmpeg.so
	public static boolean hasDownloaded(Context context, String strSaveFile) {
		String strFilePath = context.getFilesDir() + "/" + strSaveFile;
		File file = new File(strFilePath);
		if(file.exists()) {
			return true;
		}
		return false;
	}
	
	//if decoder plugin exists,so delete it
	public static boolean hasDecoderPlugInExists() {
		try {
			// sdcard2
			String strPlunIn = null;
			if(SecondaryStorageReflect.getSecondaryExternalStorageDirectory() != null) {
				if(Environment.MEDIA_MOUNTED.equals(SecondaryStorageReflect.getSecondaryExternalStorageState())) {
					strPlunIn = SecondaryStorageReflect.getSecondaryExternalStorageDirectory().getPath();
					if(strPlunIn != null) {
						strPlunIn = strPlunIn + File.separator + STANDARD_PLUG_IN_DELETE;
						File file = new File(strPlunIn);
						if(file.exists()) {
							return true;
						}
					}
				}
			}
			
			//sdcard
			strPlunIn = Environment.getExternalStorageDirectory().getPath();
			if(strPlunIn != null) {
				if(strPlunIn != null) {
					strPlunIn = strPlunIn + File.separator + STANDARD_PLUG_IN_DELETE;
					File file = new File(strPlunIn);
					if(file.exists()) {
						return true;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	//whether the decoder plunin is downloaded
	public static boolean hasDownloadedDecoderPlunIn(Context context) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK 
				| PowerManager.ON_AFTER_RELEASE, "DOWNLOAD_INSTALL_DECODER_PLUGIN");
		if(wakeLock != null && !wakeLock.isHeld()) {
			wakeLock.acquire();//acquire wake lock
		}
		
		// if libffmpeg.zip is exists
		if(DownloadSoUtils.hasDownloadedZipPlunIn()) {
			String strZipPath = getDecoderZipPlunInPath();
			exitDecoderOperation();
			copyLibrary(context, strZipPath, STANDARD_PLUG_IN_ZIP);
			exitDecoderOperation();
			int iUnZipResult = unZip(context, STANDARD_PLUG_IN_LIB, STANDARD_PLUG_IN_LIB, STANDARD_PLUG_IN_ZIP);
			context.deleteFile(STANDARD_PLUG_IN_ZIP);
			boolean bResult = (UNZIP_SUCCESS == iUnZipResult);
			if(!bResult) {//unzip failure
				context.deleteFile(STANDARD_PLUG_IN_LIB);
			}
			return bResult;
		}
		
		if(wakeLock != null && wakeLock.isHeld()) {
			wakeLock.release();
		}
		return false;
	}
	
	//decompress libffmpeg.zip 
	@SuppressWarnings("unchecked")
	public static int unZip(Context context, String strZipName, String strDstName, String strSrcName) {
		boolean bFlag = false;
		InputStream is = null;
		FileOutputStream fos = null;
		String strSrcPath = context.getFilesDir() + "/" + strSrcName;
		String strDstPath = context.getFilesDir() + "/" + strDstName;
		
		File srcFile = new File(strSrcPath);
		if(!srcFile.exists()) {
			return UNZIP_ERROR;
		}
		
		try {
			ZipFile srcZipFile = new ZipFile(strSrcPath);
			Enumeration<ZipEntry> enums = (Enumeration<ZipEntry>) srcZipFile.entries();
			mbBreakCircle = false;
			ZipEntry zipEntry = enums.nextElement();
			String zipName = zipEntry.getName();
			if(strZipName.equals(zipName)) {
				bFlag = true;
				is = srcZipFile.getInputStream(zipEntry);
				fos = new FileOutputStream(strDstPath);
				int length = -1;
				byte[] buffer = new byte[8 * 1024];
				while((length = is.read(buffer)) != -1 && !mbBreakCircle) {
					fos.write(buffer, 0, length);
				}
				fos.flush();
				fos.getFD().sync();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(is != null) {
					is.close();
				}
				if(fos != null) {
					fos.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		mbBreakCircle = false;
		if(bFlag) {
			return UNZIP_SUCCESS;
		} else {
			return UNZIP_ERROR;
		}
	}
	
	//copy zip plungin to a new file
	private static void copyLibrary(Context context, String oldPath, String newName) {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		File oldFile = new File(oldPath);
		if(oldFile.exists()) {
			context.deleteFile(newName);
			try {
				fis = new FileInputStream(oldPath);
				fos = context.openFileOutput(newName, Context.MODE_WORLD_WRITEABLE);
				byte[] buffer = new byte[8 * 1024];
				int length = 0;
				while ((length = fis.read(buffer)) != -1 && !mbBreakCircle) {
					fos.write(buffer, 0, length);
				}
				fos.flush();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if(fis != null) {
						fis.close();
					}
					if(fos != null) {
						fos.close();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public static void exitDecoderOperation() {
	    // break loop
		mbBreakCircle = true;
		miDownloadLenth = 0;
	}
	
	//download zip plungin. e.g., libffmpeg.zip
	public static boolean hasDownloadedZipPlunIn() {
		if(getDecoderZipPlunInFile().exists()) {
			return true;
		}
		return false;
	}
	
	public static String getDecoderZipPlunInPath() {
		return getDecoderZipPlunInFile().getPath();
	}
	
	// get libffmpeg.zip path
	public static File getDecoderZipPlunInFile() {
		try {
			// sdcard2
			String strPlunIn = null;
			if(SecondaryStorageReflect.getSecondaryExternalStorageDirectory() != null) {
				if(Environment.MEDIA_MOUNTED.equals(SecondaryStorageReflect.getSecondaryExternalStorageState())) {
					strPlunIn = SecondaryStorageReflect.getSecondaryExternalStorageDirectory().getPath();
					if(strPlunIn != null) {
						strPlunIn = strPlunIn + File.separator + STANDARD_PLUG_IN_ZIP;
						File file = new File(strPlunIn);
						if(file.exists()) {
							return file;
						}
					}
				}
			}
			
			//sdcard
			strPlunIn = Environment.getExternalStorageDirectory().getPath();
			if(strPlunIn != null) {
				if(strPlunIn != null) {
					strPlunIn = strPlunIn + File.separator + STANDARD_PLUG_IN_ZIP;
					File file = new File(strPlunIn);
					if(file.exists()) {
						return file;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
}
