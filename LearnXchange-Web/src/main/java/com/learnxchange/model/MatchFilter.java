package com.learnxchange.model;

/** Optional filters for the match search. Null/blank values mean "no filter". */
public record MatchFilter(String skillText, String category, String availabilitySlot) {
    public static final MatchFilter NONE = new MatchFilter(null, null, null);
}
