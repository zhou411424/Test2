package com.leven.videoplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.GridView;

public class VideoGridView extends GridView {

	private int mListPos;
	
	public VideoGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.setOnScrollListener(mOnScrollListener);
	}

	public VideoGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setOnScrollListener(mOnScrollListener);
	}

	public VideoGridView(Context context) {
		super(context);
		this.setOnScrollListener(mOnScrollListener);
	}

	private OnScrollListener mOnScrollListener = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if(scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
				mListPos = getFirstVisiblePosition();
			}
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			
		}
	};
	
	public void restorePosition() {
		if(mListPos != -1) {
			setSelection(mListPos);
		}
	}
}
