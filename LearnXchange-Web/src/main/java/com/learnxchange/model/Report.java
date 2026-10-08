package com.learnxchange.model;

import java.time.LocalDateTime;

public record Report(int id, int reporterId, String reporterName, int reportedId, String reportedName,
                     String reason, Status status, LocalDateTime createdAt) {
    public enum Status { OPEN, RESOLVED }
}
