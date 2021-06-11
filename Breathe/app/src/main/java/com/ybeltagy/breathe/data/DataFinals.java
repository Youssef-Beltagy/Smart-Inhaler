package com.ybeltagy.breathe.data;

import android.annotation.SuppressLint;

import java.time.Instant;

public class DataFinals {
    public static final Level DEFAULT_LEVEL = Level.NULL; // todo: use that everywhere Tag.Null is used directly
    public static final Tag DEFAULT_TAG = Tag.NULL; // todo: use that everywhere Tag.Null is used directly
    public static final float DEFAULT_FLOAT = Float.MIN_VALUE;
    public static final int DEFAULT_INTEGER = Integer.MIN_VALUE;
    public static final char DEFAULT_CHAR = '-';
    public static final String DEFAULT_STRING = "";
    // this is to satisfy the @NonNull compiler warning for the InhalerUsageEvent timestamp
    @SuppressLint("NewApi")
    public static final Instant DEFAULT_INSTANT = Instant.MIN;
}
