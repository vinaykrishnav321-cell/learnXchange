package com.learnxchange.web;

import com.learnxchange.model.LearningSession;
import com.learnxchange.model.User;
import com.learnxchange.service.RatingService;
import com.learnxchange.service.SessionService;
import com.learnxchange.util.ServiceException;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionApi {
    private final SessionService sessions = new SessionService();
    private final RatingService ratings = new RatingService();
    private final SessionGuard guard;

    public SessionApi(SessionGuard guard) { this.guard = guard; }

    @GetMapping
    public List<LearningSession> list(HttpSession session) {
        return sessions.sessionsOf(guard.require(session));
    }

    @PostMapping
    public Map<String, Boolean> schedule(@RequestBody Dto.SessionBody b, HttpSession session) {
        User me = guard.require(session);
        LocalDate date;
        LocalTime time;
        LearningSession.Mode mode;
        try {
            date = LocalDate.parse(String.valueOf(b.date()));
        } catch (DateTimeParseException e) {
            throw new ServiceException("Please pick a valid date.");
        }
        try {
            time = LocalTime.parse(String.valueOf(b.time()));
        } catch (DateTimeParseException e) {
            throw new ServiceException("Please enter a valid time.");
        }
        try {
            mode = LearningSession.Mode.valueOf(String.valueOf(b.mode()));
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Choose online or in person.");
        }
        sessions.schedule(me, b.requestId(), date, time, mode, b.where());
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/complete")
    public Map<String, Boolean> complete(@PathVariable int id, HttpSession session) {
        sessions.markCompleted(guard.require(session), id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Boolean> cancel(@PathVariable int id, HttpSession session) {
        sessions.cancel(guard.require(session), id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/rate")
    public Map<String, Boolean> rate(@PathVariable int id, @RequestBody Dto.RateBody b, HttpSession session) {
        ratings.rate(guard.require(session), id, b.score(), b.feedback());
        return Map.of("ok", true);
    }
}
