package com.leven.videoplayer.persistance;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.os.Environment;


import android.util.Log;

public class SecondaryStorageReflect {
    protected static final String TAG = "VideoPlayerSecondaryStorageReflect";

    private static Method mGetSecondaryExternalStorageDirectory;
    private static Method mGetSecondaryExternalStorageState;

    static{
        initCompatibility();
    }
    private static void initCompatibility(){
        try{
            mGetSecondaryExternalStorageDirectory = Environment.class.getMethod(
                 "getSecondaryExternalStorageDirectory", new Class[]{});
            mGetSecondaryExternalStorageState = Environment.class.getMethod(
                 "getSecondaryExternalStorageState", new Class[]{});
        }catch(NoSuchMethodException e){
            Log.e(TAG, "NoSuchMethodException");
        }
    }
    
    public static String getSecondaryExternalStorageState() throws IOException{
        Object resultObj = null;
        try{
            resultObj = mGetSecondaryExternalStorageState.invoke(null);
        }catch(InvocationTargetException ite){
            Throwable cause = ite.getCause();
            if(cause instanceof IOException){
                throw (IOException) cause;
            } else if(cause instanceof RuntimeException){
                throw (RuntimeException) cause;
            } else if(cause instanceof Error){
                throw (Error) cause;
            } else{
                throw new RuntimeException(ite);
            }
        } catch(IllegalAccessException e){
            Log.e(TAG, "unexpected");
        }
        
        return (String)resultObj;
    }
    
    public static File getSecondaryExternalStorageDirectory() throws IOException{
        Object resultObj = null;
        try{
            resultObj = mGetSecondaryExternalStorageDirectory.invoke(null);
        }catch(InvocationTargetException ite){
            Throwable cause = ite.getCause();
            if(cause instanceof IOException){
                throw (IOException) cause;
            } else if(cause instanceof RuntimeException){
                throw (RuntimeException) cause;
            } else if(cause instanceof Error){
                throw (Error) cause;
            } else{
                throw new RuntimeException(ite);
            }
        } catch(IllegalAccessException e){
            Log.e(TAG, "unexpected");
        }
        
        return (File)resultObj;
    }
}

