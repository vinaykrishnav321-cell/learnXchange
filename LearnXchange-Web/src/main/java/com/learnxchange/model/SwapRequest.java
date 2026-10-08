package com.learnxchange.model;

import java.time.LocalDateTime;

public record SwapRequest(int id, int senderId, int receiverId, String senderName, String receiverName,
                          int offeredSkillId, String offeredSkillName,
                          int requestedSkillId, String requestedSkillName,
                          String message, Status status, LocalDateTime createdAt) {
    public enum Status { PENDING, ACCEPTED, REJECTED, CANCELLED }
}
