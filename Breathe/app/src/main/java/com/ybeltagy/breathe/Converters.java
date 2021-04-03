package com.ybeltagy.breathe;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.room.TypeConverter;

import java.time.OffsetDateTime;

public class Converters {

    // Converts database stored timeStamp string into relevant OffsetDateTime (UTC)
    @TypeConverter
    public static OffsetDateTime fromTimeStampString(String timeStamp) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return timeStamp == null ? null : OffsetDateTime.parse(timeStamp);
        }
        else {
            // TODO: implement date time formatting from string to OffsetDateTime
            //  for API < 26 (Oreo)
            return null;
        }
    }

    // Converts OffsetDateTime timeStamp to string to be stored in database
    @TypeConverter
    public static String toTimeStampString(OffsetDateTime timeStamp) {
        return timeStamp == null ? null : timeStamp.toString();
    }

    //  Converts Tag string stored in database to relevant Tag enum
    @TypeConverter
    public static Tag fromTagString(String tag){
        return tag == null ? null : Tag.valueOf(tag.toUpperCase());
    }

    // Converts Tag into string to be stored in database
    @TypeConverter
    public static String toTagString(Tag tag){
        return tag == null ? null : tag.toString();
    }

    // Converts Level string stored in database to relevant Level enum
    @TypeConverter
    public static Level fromLevelString(String level) {
        return level == null ? null : Level.valueOf(level.toUpperCase());
    }

    // Converts Level into string to be stored in database
    @TypeConverter
    public static String toLevelString(Level level) {
        return level == null ? null : level.toString();
    }
}