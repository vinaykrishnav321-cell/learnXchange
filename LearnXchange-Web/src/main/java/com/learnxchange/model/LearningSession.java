package com.learnxchange.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** A scheduled teaching session belonging to an accepted swap request. User A = request sender, B = receiver. */
public record LearningSession(int id, int requestId, int userAId, int userBId,
                              String userAName, String userBName, String swapSummary,
                              LocalDate date, LocalTime time, Mode mode, String locationOrLink, Status status) {
    public enum Mode { ONLINE, OFFLINE }
    public enum Status { SCHEDULED, COMPLETED, CANCELLED }

    public boolean involves(int userId) { return userAId == userId || userBId == userId; }
    public int otherUserId(int userId) { return userAId == userId ? userBId : userAId; }
    public String otherUserName(int userId) { return userAId == userId ? userBName : userAName; }
}
