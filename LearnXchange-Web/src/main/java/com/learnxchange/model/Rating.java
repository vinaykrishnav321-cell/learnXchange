package com.learnxchange.model;

import java.time.LocalDateTime;

public record Rating(int id, int sessionId, int raterId, String raterName, int rateeId,
                     int score, String feedback, LocalDateTime createdAt) { }
