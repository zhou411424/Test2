package com.leven.videoplayer.subtitle.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.StringBuilder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

/*
 * The ass/ssa format like this:
 *    Dialogue: 0,0:54:12.33,0:54:15.16,*Default,NTP,0000,0000,0000,,content
 *    
 */

public class AssParser extends Parser { 
    private static final String TAG = "AssParser";
    //the regex for the time area ------ 0:54:12.33,0:54:15.16
    private static final String REGEX_TIME = "\\d:\\d\\d:\\d\\d.\\d\\d,\\d:\\d\\d:\\d\\d.\\d\\d";
    private static final String REGEX_CONTENT_START = ",{8}(\\{.+\\})*";
    private static final String REGEX_CONTENT_MIDDLE = "\\{.+\\}";

    public class AssItem extends Item {

    }

    @Override
    public void parse(String uri) {
        File file = new File(uri);
        parse(file);
    }

    @Override
    public void parse(File file) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "UTF-16"));
            String line = null;
            Pattern timePattern = Pattern.compile(REGEX_TIME);
            Pattern contentStartPattern = Pattern.compile(REGEX_CONTENT_START);
            Matcher matcher;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("Dialogue:")) {
                    continue;
                }
                matcher = timePattern.matcher(line);
                if (!matcher.find())
                    continue;

                Item item = new AssItem();

                parseTime(matcher.group(), item);
                matcher = contentStartPattern.matcher(line);
                parseContent(matcher.group(), item);
                
                mCallbacks.content(item);
            }
        } catch (Exception e) {
            mCallbacks.error(e);
        } 

    }

    @Override
    public void parseFromString(String string) {

    }

    private void parseTime(String time, Item item) {
        int hour = Integer.valueOf(time.substring(0, 1));
        int min = Integer.valueOf(time.substring(2, 4));
        int sec = Integer.valueOf(time.substring(5, 7));
        //0:54:12.33---> mesc = 0.33 * 1000 / 100
        int msec = Integer.valueOf(time.substring(8, 10));
        int from = msec * 10 + sec * 1000 + min * 60 * 1000 + hour * 60 * 60 * 1000;
        item.setStart(from);

        hour = Integer.valueOf(time.substring(11, 12));
        min = Integer.valueOf(time.substring(13, 15));
        sec = Integer.valueOf(time.substring(16, 18));
        msec = Integer.valueOf(time.substring(19));
        int to = msec * 10 + sec * 1000 + min * 60 * 1000 + hour * 60 * 60 * 1000;
        item.setEnd(to);
    }

    private void parseContent(String content, Item item) {
        Pattern contentMiddlePattern = Pattern.compile(REGEX_CONTENT_MIDDLE);
        Matcher matcher = contentMiddlePattern.matcher(content);
        if (!matcher.find()) {
            item.setContent(content);
            return;
        }

        StringBuilder useText = new StringBuilder("");
        int startIndex = 0;
        int endIndex = 0;
        while (matcher.find()) {
            endIndex = matcher.start();
            useText.append(content.substring(startIndex, endIndex));
            startIndex = endIndex + 1;
        }

        item.setContent(useText.toString());
    }

}
