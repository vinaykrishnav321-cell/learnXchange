package com.learnxchange.service;

import com.learnxchange.dao.RatingDAO;
import com.learnxchange.dao.SessionDAO;
import com.learnxchange.dao.UserDAO;
import com.learnxchange.model.LearningSession;
import com.learnxchange.model.Rating;
import com.learnxchange.model.User;
import com.learnxchange.util.ServiceException;
import com.learnxchange.util.Validator;

import java.util.List;

public class RatingService {
    private final RatingDAO ratingDAO = new RatingDAO();
    private final SessionDAO sessionDAO = new SessionDAO();
    private final UserDAO userDAO = new UserDAO();

    /** Rates the other participant of a completed session. The ratee's cached average is refreshed immediately. */
    public void rate(User me, int sessionId, int score, String feedback) {
        Validator.range(score, 1, 5, "Rating");
        String text = Validator.optional(feedback, "Feedback", 500);

        LearningSession s = sessionDAO.findById(sessionId).orElseThrow(() -> new ServiceException("Session not found."));
        if (!s.involves(me.getId())) throw new ServiceException("You are not part of this session.");
        if (s.status() != LearningSession.Status.COMPLETED)
            throw new ServiceException("You can rate your partner once the session is marked completed.");
        if (ratingDAO.exists(sessionId, me.getId()))
            throw new ServiceException("You have already rated this session.");

        int rateeId = s.otherUserId(me.getId());
        ratingDAO.insert(sessionId, me.getId(), rateeId, score, text);
        userDAO.refreshRating(rateeId);
    }

    public List<Rating> reviewsFor(int userId) {
        return ratingDAO.findByRatee(userId);
    }
}
