package com.leven.videoplayer;

import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

interface FlingSpeedCallback {
	public void onFlingSpeedListener(float velocityY);
}

public class GestureSpeedListener implements OnGestureListener {

	private static final String TAG = "GestureSpeedListener";
	private FlingSpeedCallback mFlingSpeedCallback;
	private static final float MIN_THRESHOLD_VELOCITY_Y = 1500.0f;
	
	public GestureSpeedListener(FlingSpeedCallback flingSpeedCallback) {
		this.mFlingSpeedCallback = flingSpeedCallback;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		Log.d(TAG, "velocityX = " + velocityX + ", velocityY = " + velocityY);
		if(Math.abs(velocityY) >= MIN_THRESHOLD_VELOCITY_Y) {
			mFlingSpeedCallback.onFlingSpeedListener(velocityY);
		}
		return false;
	}

}
