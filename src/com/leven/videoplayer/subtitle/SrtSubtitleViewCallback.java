package com.leven.videoplayer.subtitle;

import com.leven.videoplayer.subtitle.parser.Item;
import com.leven.videoplayer.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.SubMenu;

public class SrtSubtitleViewCallback implements SubtitleView.Callback {

    private Paint paint;

    private float x;
    private float gap;
    private float bottom;

    public SrtSubtitleViewCallback(Context context) {
        paint = new Paint();
        paint.setColor(Color.WHITE);

        float fontSize = context.getResources().getDimension(R.dimen.subtitle_font_size);
        paint.setTextSize(fontSize);

        x = 25.0f;
        gap = 10.0f;
        bottom = 40.0f;
    }

    public void onDrawSubtitles(Canvas canvas, Item[] items) {
        if (items == null || items.length == 0)
            return;

        float offsetX;
        float offsetY;
        float width;
        String text;

        offsetX = x;
        offsetY = canvas.getHeight() - bottom + gap;
        width = canvas.getWidth() - x * 2;

        for (int i = 0; i < items.length; i++) {
            text = items[i].getContent();
            offsetY -= SubtitleView.getTextHeight(text, width, paint) + gap;
            SubtitleView.drawText(canvas, text, offsetX, offsetY, width, paint);
        }

    }

}
