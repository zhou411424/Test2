package com.leven.videoplayer.subtitle.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

/*
 * The srt format like this:
 *    1027
 *    01:54:13,332 --> 01:54:15,163
 *    content
 *    
 */

public class SrtParser extends Parser {
    private static final String TAG = "SrtParser";
    //the regex for the time area ------ 01:54:13,332 --> 01:54:15,163
    private static final String REGEX_TIME = "\\d\\d:\\d\\d:\\d\\d,\\d\\d\\d"; //--> \\d\\d:\\d\\d:\\d\\d,\\d\\d\\d";

    @Override
    public void parse(String uri) {
        File file = new File(uri);
        parse(file);
    }

    @Override
    public void parse(File file) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "gbk"));
            String line = null;
            Pattern pattern = Pattern.compile(REGEX_TIME);
            Matcher matcher;
            while ((line = reader.readLine()) != null) {
                matcher = pattern.matcher(line);
                if (!matcher.find())
                    continue;
                Item item = parseTime(line);

                //read the content of the time area.
                line = reader.readLine();

                StringBuffer result = new StringBuffer(line);
                line = reader.readLine();
                while (!"".equals(line)) {
                    if (line == null)
                        break;
                    result.append("\n").append(line);
                    line = reader.readLine();

                }
                item.setContent(result.toString());
                mCallbacks.content(item);
            }
        } catch (Exception e) {
            mCallbacks.error(e);
        } 
    }

    @Override
    public void parseFromString(String string) {
        // TODO Auto-generated method stub

    }

    private Item parseTime(String time) {
        Item item = new Item();
        int hour = Integer.valueOf(time.substring(0, 2));
        int min = Integer.valueOf(time.substring(3, 5));
        int sec = Integer.valueOf(time.substring(6, 8));
        int msec = Integer.valueOf(time.substring(9, 12));
        int from = msec + sec * 1000 + min * 60 * 1000 + hour * 60 * 60 * 1000;
        item.setStart(from);

        hour = Integer.valueOf(time.substring(17, 19));
        min = Integer.valueOf(time.substring(20, 22));
        sec = Integer.valueOf(time.substring(23, 25));
        msec = Integer.valueOf(time.substring(26));
        int to = msec + sec * 1000 + min * 60 * 1000 + hour * 60 * 60 * 1000;
        item.setEnd(to);
        return item;
    }

}
