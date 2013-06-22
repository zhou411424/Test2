
package com.leven.videoplayer.utils;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;

import android.R;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

public class ImageMemoryCache2 {
    private static final int SOFT_CACHE_SIZE = 15;
    private static final int DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB 
    private static LruCache<String, Bitmap> mLruCache;
    private static LinkedHashMap<String, SoftReference<Bitmap>> mSoftCache;

    public ImageMemoryCache2(Context context) {

        int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                .getMemoryClass();
        int hardCacheSize = 1024 * 1024 * memClass / 4; // 硬引用缓存容量，为系统可用内存的1/4
        // int hardCacheSize = 1024 * 1024 * 8;//8M
        mLruCache = new LruCache<String, Bitmap>(hardCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                if (value != null)
                    return value.getRowBytes() * value.getHeight();
                else
                    return 0;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue,
                    Bitmap newValue) {

                if (oldValue != null)
                    // 硬引用缓存容量满的时候，会根据LRU算法把最近一个没有被使用的图片转入此软引用缓存
                    mSoftCache.put(key, new SoftReference<Bitmap>(oldValue));
            }

        };

        mSoftCache = new LinkedHashMap<String, SoftReference<Bitmap>>(SOFT_CACHE_SIZE, 0.75f, true) {
            private static final long serialVersionUID = 6040103833179403725L;

            @Override
            public SoftReference<Bitmap> put(String key, SoftReference<Bitmap> value) {
                return super.put(key, value);
            }

            @Override
            protected boolean removeEldestEntry(Entry<String, SoftReference<Bitmap>> eldest) {
                if (size() > SOFT_CACHE_SIZE) {
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * 添加图片到缓存
     */
    public void putBitmapToCache(String url, Bitmap bitmap) {
        if (bitmap != null) {
            synchronized (mLruCache) {
                mLruCache.put(url, bitmap);
            }
            
            
        }
    }

    /**
     * 从缓存中获取图片
     */
    public Bitmap getBitmapFromCache(String url) {
        Bitmap bitmap;
        // 先从硬引用缓存中获取
        synchronized (mLruCache) {
            bitmap = mLruCache.get(url);
            if (bitmap != null) {
                // 如果找到的话，把元素移到LinkedHashMap的最前面，从而保证在LRU算法中是最后被删除
                mLruCache.remove(url);
                mLruCache.put(url, bitmap);
                return bitmap;
            }
        }

        // 如果硬引用缓存中找不到，到软引用缓存中找
        synchronized (mSoftCache) {
            SoftReference<Bitmap> bitmapReference = mSoftCache.get(url);
            if (bitmapReference != null) {
                bitmap = bitmapReference.get();
                if (bitmap != null) {
                    // 将图片移回硬缓存
                    mLruCache.put(url, bitmap);
                    mSoftCache.remove(url);
                    return bitmap;
                } else {
                    mSoftCache.remove(url);
                }
            }
        }
        return null;
    }

    public void loadBitmap(int resId, ImageView imageView) {
        Bitmap bitmap = getBitmapFromCache(resId+"");
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(imageView);
            bitmapWorkerTask.execute(resId);
        }
    }

    public void clearCache() {
        mSoftCache.clear();
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> taskReference;

        public AsyncDrawable(Resources res,Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            taskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }
        
        public BitmapWorkerTask getBitmapWorkerTask() {
            return taskReference.get();
        }
    }
    
    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {

        private final WeakReference<ImageView> imageViewReference;
        private int data = 0;

        public BitmapWorkerTask(ImageView imageView) {
            this.imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            data = params[0];
            Bitmap bitmap = null;
//            return decodeSampledBitmapFromResource(0, data, 100, 100);
            putBitmapToCache("", bitmap);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            if(imageViewReference != null && result != null) {
                ImageView imageView = imageViewReference.get();
                if(imageView != null) {
                    imageView.setImageBitmap(result);
                }
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }
}
