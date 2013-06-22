package com.leven.videoplayer.subtitle;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.leven.videoplayer.subtitle.parser.ISubtitle;
import com.leven.videoplayer.subtitle.parser.Item;
import com.leven.videoplayer.subtitle.parser.Parser;
import com.leven.videoplayer.subtitle.parser.ParserFactory;
import com.leven.videoplayer.subtitle.parser.Parser.ParserCallbacks;

import android.media.MediaPlayer;
import android.os.Process;
import android.util.Log;

public class Subtitle implements ISubtitle, Runnable {

    private static final String TAG = "Subtitle";
    private static final String REGEX_SUFFIX = /*".*(srt|ass|ssa)"*/".*(srt)";
    private static final int REFRESH_CAPACITY = 10;
    private static final int MAX_CHACHE_NUM = 10;
   
    private String mUri;
    private ISubtitle.Callback mCallback;
    private Thread mSubtitleThread;
    private LinkedList<Item> mUnPlayedData = new LinkedList<Item>();
    private LinkedList<Item> mPlayedData = new LinkedList<Item>();
    private ArrayList<Item> mRefreshList;
    private ArrayList<RefreshEntry> mCache;
    
    private Parser mParser;
    private MediaPlayer mPlayer;
    private int mCurrentPosition;
    private int mType;
    private boolean mActive;

    private final class RefreshEntry {
        public static final int FLAG_START = 0;
        public static final int FLAG_END = 1;
        
        public int time;
        public int flag;
        public Item item;
    }

    public static Subtitle createSubtitle(String videoPath) {
        String path = getSubtitlePath(videoPath);
        Log.d(TAG, "subtitle path: " + path);
        if (path != null)
            return new Subtitle(path);

        if (hasInternalSubtitle()) 
            return new Subtitle();

        return null;
    }

    private static String getSubtitlePath(String videoPath) {
        Log.d(TAG, "video path: " + videoPath);
        if (videoPath == null || videoPath.equals(""))
            return null;

        int lastPotIndex = videoPath.lastIndexOf(".");
        int lastPatIndex = videoPath.lastIndexOf("/");
        if (lastPotIndex <= 0 || lastPatIndex <= 0)
            return null;

        String nameWidthoutSuffix = videoPath.substring(lastPatIndex + 1, lastPotIndex);
        File parent = new File(videoPath).getParentFile();
        if (parent == null || parent.list() == null)
            return null;
            
        Pattern pattern;
        Matcher matcher;
        for (String name : parent.list()) {
            if (!name.startsWith(nameWidthoutSuffix))
                continue;

            pattern = Pattern.compile(REGEX_SUFFIX);
            matcher = pattern.matcher(name);
            if (matcher.find())
                return parent.getAbsolutePath() + "/" + name;
        }

        return null;
    }

    private static boolean hasInternalSubtitle() {
        return false;
    }

    private Subtitle() {
        mRefreshList = new ArrayList<Item>(REFRESH_CAPACITY);
        setDataSource();
    }

    private Subtitle(String subtitlePath) {
        mRefreshList = new ArrayList<Item>(REFRESH_CAPACITY);
        setDataSource(subtitlePath);
    }

    public void bindPlayer(MediaPlayer player) {
        mPlayer = player;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mCache = new ArrayList<Subtitle.RefreshEntry>(MAX_CHACHE_NUM);
        parse();
        while (true) {
            try {
                if (!mActive)
                    break;

                if (!mPlayer.isPlaying()) {
                    continue;
                }
                synchronized (mUnPlayedData) {
                    mCurrentPosition = mPlayer.getCurrentPosition();

                    if (mCache.size() <= 0) {
                        syncCache();
                    }

                    updateDisplay();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "IllegalStateException occured...");
                release();
            }
        }
    }

    private String getInternalSubtitle() {
        return null;
    }

    private void addToCache(Item item) {
        RefreshEntry entry = new RefreshEntry();
        entry.time = item.getStart();
        entry.flag = RefreshEntry.FLAG_START;
        entry.item = item;
        mCache.add(entry);

        entry = new RefreshEntry();
        entry.time = item.getEnd();
        entry.flag = RefreshEntry.FLAG_END;
        entry.item = item;
        mCache.add(entry);
    }

    private boolean shouldAddToCache(Item item) {
        if (mCache.size() == 0 || mCache.isEmpty())
            return true;
    
        for (RefreshEntry refreshEntry : mCache) {
            if (item.getStart() < refreshEntry.time)
                return true;
        }
        return false;
    }

    private boolean isItemPlayed(Item item) {
        if (item != null && mCurrentPosition > item.getEnd())
            return true;

        return false;
    }

    private void updateDisplay() {
        if (mCache.size() <= 0)
            return;

        RefreshEntry entry = mCache.get(0);
        if (mCurrentPosition < entry.time)
            return;

        mCache.remove(0);
        Item i = entry.item;
        if (entry.flag == RefreshEntry.FLAG_START) {
            mRefreshList.add(i);
        } else {
            mRefreshList.remove(i);
        }

        mCallback.onSubtitleChanged(mRefreshList.toArray(new Item[mRefreshList.size()]));

    }

    private void setDataSource(String uri) {
        if (uri == null || "".equals(uri)) {
            Log.e(TAG, "illegal uri when setDataSource...");
            return;
        }
        mUri = uri;

        if (mUri.endsWith(".ass") || mUri.endsWith(".ssa")) {
            mType = Parser.TYPE_ASS;
        } else if (mUri.endsWith(".srt")) {
            mType = Parser.TYPE_SRT;
        }
        if (mType != Parser.TYPE_ASS && mType != Parser.TYPE_SRT) {
            Log.e(TAG, "Unknown type of the file");
            return;
        }
        mParser = ParserFactory.newInstance().createParser(mType);
    }

    private void setDataSource() {
        mType = Parser.TYPE_ASS;
        mUri = null;
        mParser = ParserFactory.newInstance().createParser(mType);
    }

    @Override
    public void release() {
        Log.d(TAG, "The Subtitle is released.");
        if (mSubtitleThread != null) {
            mSubtitleThread.interrupt();
            mSubtitleThread = null;
        }
        mUri = null;
        mCallback = null;
        mParser = null;
        mCache = null;
        mRefreshList = null;
        mActive = false;
    }

    @Override
    public void seekTo(int mesc) {
        if (!isAlive())
            return;

        syncPosition(mesc);
        mCallback.onSubtitleChanged(null);

    }

    private void syncPosition(int mesc) {
        Log.e(TAG, "mesc = " + mesc + " currentposion = " + mCurrentPosition);
        synchronized (mUnPlayedData) {
            Item item;

            if (mesc > mCurrentPosition) {
                while ((item = mUnPlayedData.peekFirst()) != null) {
                    if (mesc > item.getStart()) {
                        mPlayedData.addLast(mUnPlayedData.pollFirst());
                    } else {
                        break;
                    }

                }
            } else if (mesc < mCurrentPosition) {
                while ((item = mPlayedData.peekLast()) != null) {
                    if (mesc < item.getStart()) {
                        mUnPlayedData.addFirst(mPlayedData.pollLast());
                    } else {
                        break;
                    }

                }
            }

            if (mCache != null)
                mCache.clear();
            mRefreshList.clear();
        }

    }

    public void syncCache() {
        Item lastItem;
        while ((lastItem = mUnPlayedData.peekFirst()) != null) {
            if (isItemPlayed(lastItem)) {
                mPlayedData.addLast(mUnPlayedData.pollFirst());
                continue;
            }

            if (!shouldAddToCache(lastItem))
                break;
            lastItem = mUnPlayedData.pollFirst();
            mPlayedData.addLast(lastItem);
            addToCache(lastItem);

        }
        if (mCache.size() > 2) {
            Collections.sort(mCache, new Comparator<RefreshEntry>() {

                @Override
                public int compare(RefreshEntry object1,
                        RefreshEntry object2) {
                    if (object1.time > object2.time)
                        return 1;
                    else if (object1.time < object2.time)
                        return -1;
                    return 0;
                }
            });
        }
    }

    @Override
    public void registerCallback(Callback callback) {
        mCallback = callback;
    }
    
    private void parse() {
        synchronized (mUnPlayedData) {
            mParser.registerCallbacks(new ParserCallbacks() {

                @Override
                public void content(Item item) {
                    synchronized (mUnPlayedData) {
                        mUnPlayedData.add(item);
                    }
                }

                @Override
                public void start() { 
                    //Log.e(TAG, "----> start to parse title." ); 

                }

                @Override
                public void end() {
                    //Log.e(TAG, "----> parse title finished.");

                }

                @Override
                public void error(Exception e) {
                    Log.e(TAG, e.toString());
                    release();
                }

            });
            if (mUri == null) {
                mParser.parseFromString(getInternalSubtitle());
            } else {
                mParser.parse(mUri);
            }
            Collections.sort(mUnPlayedData, new MyComparator());
        }
    }

    @Override
    public void start() {
        if (mSubtitleThread == null) {
            mSubtitleThread = new Thread(this);
            mSubtitleThread.setName("Subtitle thread");
        } 
        mActive = true;

        mSubtitleThread.start();
    }

    @Override
    public int getSubtitleType() {
        return mType;
    }

    public boolean isAlive() {
        return mActive;
    }

    private class MyComparator implements Comparator<Item> {

        @Override
        public int compare(Item object1, Item object2) {
            if (object1.getStart() > object2.getStart())
                return 1;
            else if (object1.getStart() < object2.getStart())
                return -1;

            return 0;
        }

    }

}
