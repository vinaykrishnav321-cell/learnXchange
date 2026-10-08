package com.learnxchange.model;

import java.util.List;

/** The fixed set of availability slots a user can pick from. */
public final class Availability {
    public static final List<String> SLOTS = List.of(
            "Weekday morning", "Weekday afternoon", "Weekday evening",
            "Weekend morning", "Weekend afternoon", "Weekend evening");

    private Availability() { }
}
