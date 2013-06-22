package com.leven.videoplayer.persistance;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.DataSetObserver;
import android.util.Log;

public class SortCursor extends CursorWrapper {
   private static final String       TAG = "SortCursor";
   private Cursor                    mCursor;
   private String                    mColumnName = null;
   ArrayList<SortEntry>              mSortList = new ArrayList<SortEntry>();
   int                               mPos = 0;
   public boolean mbDecreace = false;
   @SuppressWarnings("rawtypes")
   private Comparator               cmp = Collator.getInstance(java.util.Locale.CHINA);
   
   public static class SortEntry {
      public String key;
      public int order;
     
   }

    @SuppressWarnings("unchecked")
    public Comparator<SortEntry> comparator = new Comparator<SortEntry>() {
        @Override
        public int compare(SortEntry entry1, SortEntry entry2) {
        	if(mbDecreace)
        	{
        		return -cmp.compare(entry2.key, entry1.key);
        	}
            return cmp.compare(entry2.key, entry1.key);
        }
    };

    private void doSortCursor() {
        long beforeSort = System.currentTimeMillis();

        mSortList.clear();

        if (mCursor != null && mCursor.getCount() > 0) {
            int i = 0;
            SortEntry sortKey = null;
            int column = mCursor.getColumnIndexOrThrow(mColumnName);
            for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext(), i++) {
                sortKey = new SortEntry();
                sortKey.key = mCursor.getString(column);
                sortKey.order = i;
                mSortList.add(sortKey);
            }
        }

        Collections.sort(mSortList, comparator);

        long afterSort = System.currentTimeMillis();
        Log.d(TAG, "Sort cursor cost " + (afterSort - beforeSort));
    }

    private DataSetObserver mDataSetObserver = new DataSetObserver() {

        @Override
        public void onChanged() {
            // TODO Auto-generated method stub
            super.onChanged();
            Log.d(TAG, "onChanged()");
            doSortCursor();
        }

        @Override
        public void onInvalidated() {
            // TODO Auto-generated method stub
            super.onInvalidated();
            Log.d(TAG, "onInvalidated()");
            SortCursor.this.mSortList.clear();
        }

    };

    public SortCursor(Cursor cursor, String columnName,boolean bDecrese) {
        super(cursor);

        mCursor = cursor;
        mColumnName = columnName;
        mbDecreace =bDecrese;
        if (null == mCursor) {
            return;
        }

        doSortCursor();

        if (null != mDataSetObserver) {
            registerDataSetObserver(mDataSetObserver);
        }
    }
    
    public SortCursor(Cursor cursor, String columnName) {
        super(cursor);

        mCursor = cursor;
        mColumnName = columnName;

        if (null == mCursor) {
            return;
        }

        doSortCursor();

        if (null != mDataSetObserver) {
            registerDataSetObserver(mDataSetObserver);
        }
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        super.close();
        if (null != mDataSetObserver) {
            this.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }
    }

    public boolean moveToPosition(int position) {
        if (position >= 0 && position < mSortList.size()) {
            mPos = position;
            int order = mSortList.get(position).order;
            return mCursor.moveToPosition(order);
        }
        if (position < 0) {
            mPos = -1;
        }
        if (position >= mSortList.size()) {
            mPos = mSortList.size();
        }
        return mCursor.moveToPosition(position);
    }

    public boolean isLast()
    {
    	return mPos == mSortList.size();
    }
    
    public boolean moveToFirst() {
        return moveToPosition(0);
    }

    public boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }

    public boolean moveToNext() {
        return moveToPosition(mPos + 1);
    }

    public boolean moveToPrevious() {
        return moveToPosition(mPos - 1);
    }

    public boolean move(int offset) {
        return moveToPosition(mPos + offset);
    }

    public int getPosition() {
        return mPos;
    }
    
    public Cursor getcursor()
    {
    	return mCursor;
    }
}
