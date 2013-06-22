package com.leven.videoplayer.subtitle;

import com.leven.videoplayer.subtitle.parser.Item;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;
public class SubtitleView extends ImageView {
    private static final String TAG = "SubtitleView";

    private Callback callback;
    private Item[] items;

    public interface Callback {
        void onDrawSubtitles(Canvas canvas, Item[] items);
    }

    public SubtitleView(Context context) {
        super(context);
    }

    public SubtitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SubtitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void registCallback(Callback callback) {
        this.callback = callback;
    }

    public void drawSubtitle(Item[] items) {
        this.items = items;
        postInvalidate();
        //invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (callback != null)
            callback.onDrawSubtitles(canvas, items);
    }

    public static float getTextHeight(String text, float width, Paint paint) {
        if (text == null || paint == null)
            return 0;

        int sum = text.length();
        Rect bounds = new Rect();
        int begin = 0;
        int n = 0;
        float h = 0;

        while (begin < sum) {
            n = paint.breakText(text.toCharArray(), begin, sum - begin, width,
                null);
            paint.getTextBounds(text, begin, begin + n, bounds);

            begin += n;
            h += bounds.height();
        }

        return h;
    }

    public static float drawText(Canvas canvas, String text, float x, float y,
            float width, Paint paint) {
        if (canvas == null || text == null || paint == null)
            return 0;

        int sum = text.length();
        Rect bounds = new Rect();
        int begin = 0;
        int n = 0;
        float h = 0;

        while (begin < sum) {
            n = paint.breakText(text.toCharArray(), begin, sum - begin, width,
                    null);
            paint.getTextBounds(text, begin, begin + n, bounds);
            canvas.drawText(text.subSequence(begin, begin + n).toString(), x
                    + Math.max((width - bounds.width()) / 2, 0), y + h, paint);

            begin += n;
            h += bounds.height();
        }

        return h;
    }
    
}
