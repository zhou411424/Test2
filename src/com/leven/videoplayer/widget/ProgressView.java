package com.leven.videoplayer.widget;

import android.content.Context;
import android.view.View;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;


public class ProgressView extends View{
	private static final String         TAG = "Progress";
    private int mMax;
    private int mProgress;
    private Paint mPaint = null;
    
    public ProgressView(Context context) {
        super(context);
        mMax = 100;
        mProgress = 0;
        
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMax = 100;
        mProgress = 0;
    }
    

    public void setProgress(int progress) {
        if (progress < 0) {
            mProgress = 0;
        }

        if (progress > mMax) {
            mProgress = mMax;
        }

        mProgress = progress;
        invalidate();
    }


    @Override
    protected synchronized void onDraw(Canvas canvas) {
        // Draw the background for this view
        super.onDraw(canvas);
        
        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setColor(0xffffffff);
            mPaint.setStyle(Paint.Style.FILL);
        }

        canvas.drawRect(0, 5, canvas.getWidth() * mProgress / mMax, 7, mPaint);
    }
    
}