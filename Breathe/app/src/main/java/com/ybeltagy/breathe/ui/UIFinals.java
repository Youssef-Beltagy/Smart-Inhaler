package com.ybeltagy.breathe.ui;

public class UIFinals {
    protected static final int UPDATE_INHALER_USAGE_EVENT_REQUEST_CODE = 1;

    // Intent extra for Diary Entry Activity
    protected static final String EXTRA_DATA_UPDATE_INHALER_USAGE_EVENT_TIMESTAMP_KEY =
            "com.ybeltagy.breathe.ui.extra_inhaler_usage_event_to_be_updated_timestamp";
    protected static final String EXTRA_DATA_UPDATE_INHALER_USAGE_EVENT_EXISTING_MESSAGE =
            "com.ybeltagy.breathe.ui.extra_inhaler_usage_event_to_be_updated_existing_message";
    protected static final String EXTRA_DATA_UPDATE_INHALER_USAGE_EVENT_TAG =
            "com.ybeltagy.breathe.ui.extra_inhaler_usage_event_to_be_updated_existing_tag";

    // fake data - assumed number of doses in a canister
    // TODO: Replace with real number of doses in a canister
    //  or put that in the resources folder if we will continue using it.
    //  Also add a way for the user to reset the size of the canister without deleting what is in the DB.
    protected static int TOTAL_DOSES_IN_CANISTER = 200;
}
