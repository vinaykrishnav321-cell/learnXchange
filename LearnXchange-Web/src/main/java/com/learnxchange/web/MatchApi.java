package com.learnxchange.web;

import com.learnxchange.dao.UserDAO;
import com.learnxchange.model.MatchFilter;
import com.learnxchange.model.User;
import com.learnxchange.service.AdminService;
import com.learnxchange.service.MatchingService;
import com.learnxchange.service.ProfileService;
import com.learnxchange.service.RatingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MatchApi {
    private final MatchingService matching = new MatchingService();
    private final ProfileService profiles = new ProfileService();
    private final RatingService ratings = new RatingService();
    private final AdminService moderation = new AdminService();
    private final UserDAO userDAO = new UserDAO();
    private final SessionGuard guard;

    public MatchApi(SessionGuard guard) { this.guard = guard; }

    @GetMapping("/matches")
    public List<Dto.MatchView> matches(@RequestParam(required = false) String skill,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String slot,
                                       HttpSession session) {
        User me = guard.require(session);
        return matching.findMatches(me, new MatchFilter(skill, category, slot))
                .stream().map(Dto::match).toList();
    }

    @GetMapping("/users/{id}")
    public Dto.ProfileView profile(@PathVariable int id, HttpSession session) {
        guard.require(session);
        User u = userDAO.findById(id).orElse(null);
        if (u == null || u.isBlocked() || u.isAdmin())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        return new Dto.ProfileView(Dto.pub(u), profiles.skillsOf(id), ratings.reviewsFor(id));
    }

    @PostMapping("/reports")
    public Map<String, Boolean> report(@RequestBody Dto.ReportBody b, HttpSession session) {
        User me = guard.require(session);
        moderation.reportUser(me, b.userId(), b.reason());
        return Map.of("ok", true);
    }
}
