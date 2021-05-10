package com.ybeltagy.breathe.data;

/**
 * Categories of InhalerUsageEvent's, as annotated by the user
 */
public enum Tag {
    NULL, // tag that has not been initialized
    PREVENTATIVE, // tag used proactively before any symptoms are experienced
    RESCUE  // tag used in response to an asthma attack or problematic symptoms
}
