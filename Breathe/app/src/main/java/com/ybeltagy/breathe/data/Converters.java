package com.ybeltagy.breathe.data;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.room.TypeConverter;

import com.ybeltagy.breathe.data.Level;
import com.ybeltagy.breathe.data.Tag;

import java.time.Instant;

/**
 * Class for converting more complicated POJOs into types that can be stored in Room
 */
public class Converters {

    /**
     * Converts database stored timeStamp string into relevant Instant (UTC)
     *
     * @param timeStamp
     * @return parsed Instant
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @TypeConverter
    public static Instant fromTimeStampString(String timeStamp) {
            return timeStamp == null ? null : Instant.parse(timeStamp);
    }

    /**
     * Converts Instant timeStamp to string to be stored in database
     *
     * @param timeStamp
     * @return timestamp as a string
     */
    @TypeConverter
    public static String toTimeStampString(Instant timeStamp) {
        return timeStamp == null ? null : timeStamp.toString();
    }

    /**
     * Converts Tag int stored in database to relevant Tag enum
     * @param tagValue (ordinal value of tag)
     * @return Enum Tag (NULL, PREVENTATIVE, or RESCUE)
     */
    @TypeConverter
    public static Tag fromTagIntValue(int tagValue) {
        return Tag.values()[tagValue];
    }

    /**
     * Converts Tag into int to be stored in database
     * @param tag
     * @return ordinal value of enum TAG (NULL = 0, PREVENTATIVE = 1, or RESCUE = 2)
     */
    @TypeConverter
    public static int toTagIntValue(Tag tag) {
        return tag != null ? tag.ordinal() : Tag.NULL.ordinal();
    }

    /**
     * Converts Level int stored in database to relevant Level enum
     * @param level
     * @return enum Level (NULL, LOW, MEDIUM, or HIGH)
     */
    @TypeConverter
    public static Level fromLevelIntValue(int level) {
        return Level.values()[level];
    }

    /**
     * Converts Level into int to be stored in database
     * @param level
     * @return ordinal value of level (NULL = 0, LOW = 1, MEDIUM = 2, or HIGH = 3)
     */
    @TypeConverter
    public static int toLevelIntValue(Level level) {
        return level != null ? level.ordinal() : Level.NULL.ordinal();
    }
}
