package com.leven.videoplayer.subtitle.parser;

import java.util.ArrayList;

public interface ISubtitle {
    public interface Callback {
        public void onSubtitleChanged(Item[] items);
    }

    public int getSubtitleType();
    public void release();
    public void seekTo(int mesc);
    public void registerCallback(Callback callback);
    public void start();

}
