package com.ybeltagy.breathe.data;

import android.provider.ContactsContract;

import androidx.room.Ignore;

/**
 * Encapsulates user-added information for a given InhalerUsageEvent - comments and tag
 * - One InhalerUsageEvent object contains one DiaryEntry object
 * <p>
 *
 */
public class DiaryEntry {
    private Tag tag = DataFinals.DEFAULT_TAG; // category this InhalerUsageEvent falls into, as defined by the user
    private String message = DataFinals.DEFAULT_STRING; // user entered message describing the InhalerUsageEvent

    @Ignore
    public DiaryEntry(Tag tag, String message) {
        this.tag = tag;
        this.message = message;
    }

    public DiaryEntry() {
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
