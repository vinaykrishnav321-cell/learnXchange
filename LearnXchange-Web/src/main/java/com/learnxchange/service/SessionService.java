package com.learnxchange.service;

import com.learnxchange.dao.SessionDAO;
import com.learnxchange.dao.SwapRequestDAO;
import com.learnxchange.model.LearningSession;
import com.learnxchange.model.SwapRequest;
import com.learnxchange.model.User;
import com.learnxchange.util.ServiceException;
import com.learnxchange.util.Validator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class SessionService {
    private final SessionDAO sessionDAO = new SessionDAO();
    private final SwapRequestDAO requestDAO = new SwapRequestDAO();

    public void schedule(User me, int requestId, LocalDate date, LocalTime time,
                         LearningSession.Mode mode, String locationOrLink) {
        SwapRequest r = requestDAO.findById(requestId).orElseThrow(() -> new ServiceException("Request not found."));
        if (r.senderId() != me.getId() && r.receiverId() != me.getId())
            throw new ServiceException("You are not part of this swap.");
        if (r.status() != SwapRequest.Status.ACCEPTED)
            throw new ServiceException("Sessions can only be scheduled for accepted swaps.");
        if (date == null) throw new ServiceException("Please pick a date.");
        if (time == null) throw new ServiceException("Please enter a valid time (HH:mm).");
        if (mode == null) throw new ServiceException("Please choose online or offline.");
        if (!LocalDateTime.of(date, time).isAfter(LocalDateTime.now()))
            throw new ServiceException("The session must be scheduled in the future.");
        String where = Validator.require(locationOrLink,
                mode == LearningSession.Mode.ONLINE ? "Meeting link" : "Location", 255);
        sessionDAO.insert(requestId, date, time, mode, where);
    }

    public List<LearningSession> sessionsOf(User me) {
        return sessionDAO.findByUser(me.getId());
    }

    public void markCompleted(User me, int sessionId) {
        LearningSession s = loadFor(me, sessionId);
        if (s.status() != LearningSession.Status.SCHEDULED)
            throw new ServiceException("Only scheduled sessions can be marked as completed.");
        sessionDAO.updateStatus(sessionId, LearningSession.Status.COMPLETED);
    }

    public void cancel(User me, int sessionId) {
        LearningSession s = loadFor(me, sessionId);
        if (s.status() != LearningSession.Status.SCHEDULED)
            throw new ServiceException("Only scheduled sessions can be cancelled.");
        sessionDAO.updateStatus(sessionId, LearningSession.Status.CANCELLED);
    }

    private LearningSession loadFor(User me, int sessionId) {
        LearningSession s = sessionDAO.findById(sessionId).orElseThrow(() -> new ServiceException("Session not found."));
        if (!s.involves(me.getId())) throw new ServiceException("You are not part of this session.");
        return s;
    }
}
