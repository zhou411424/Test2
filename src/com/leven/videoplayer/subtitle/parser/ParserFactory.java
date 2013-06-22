package com.leven.videoplayer.subtitle.parser;

public class ParserFactory {

    private static ParserFactory mInstance;

    public static ParserFactory newInstance() {
        if (mInstance == null)
            mInstance = new ParserFactory();

        return mInstance;
    }

    public Parser createParser(int type) {
        switch (type) {
        case Parser.TYPE_ASS:
            return new AssParser();
        case Parser.TYPE_SRT:
            return new SrtParser();
        default:
            break;
        }
        return null;
    }

}
