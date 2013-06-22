package com.leven.videoplayer.subtitle.parser;

import java.io.File;

public abstract class Parser {
    public static final int TYPE_SRT = 0;
    public static final int TYPE_ASS = 1;

    public interface ParserCallbacks {
        public void start();
        public void end();
        public void content(Item item);
        public void error(Exception e);
    }

    protected ParserCallbacks mCallbacks;

    public void registerCallbacks(ParserCallbacks callbaks) {
        mCallbacks = callbaks;
    }

    public abstract void parse(String uri);

    public abstract void parse(File file);

    public abstract void parseFromString(String string);

}
