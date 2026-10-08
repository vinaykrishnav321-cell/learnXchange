package com.learnxchange.service;

import com.learnxchange.dao.*;
import com.learnxchange.model.*;
import com.learnxchange.util.ServiceException;
import com.learnxchange.util.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Admin operations plus user-facing reporting. Admin methods verify the caller's role. */
public class AdminService {
    private final UserDAO userDAO = new UserDAO();
    private final SkillDAO skillDAO = new SkillDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final SwapRequestDAO requestDAO = new SwapRequestDAO();
    private final SessionDAO sessionDAO = new SessionDAO();

    public List<User> users(User admin) {
        requireAdmin(admin);
        return userDAO.findAll();
    }

    public void setBlocked(User admin, int userId, boolean blocked) {
        requireAdmin(admin);
        User target = userDAO.findById(userId).orElseThrow(() -> new ServiceException("User not found."));
        if (target.isAdmin()) throw new ServiceException("Administrator accounts cannot be blocked.");
        userDAO.setBlocked(userId, blocked);
    }

    public List<Skill> skills() {
        return skillDAO.findAll();
    }

    public Skill addSkill(User admin, String name, String category) {
        requireAdmin(admin);
        String n = Validator.require(name, "Skill name", 80);
        String c = Validator.optional(category, "Category", 60);
        if (c.isEmpty()) c = "General";
        if (skillDAO.findByName(n).isPresent()) throw new ServiceException("That skill already exists.");
        int id = skillDAO.insert(n, c);
        return new Skill(id, n, c);
    }

    public void deleteSkill(User admin, int skillId) {
        requireAdmin(admin);
        skillDAO.delete(skillId);
    }

    public List<Report> reports(User admin) {
        requireAdmin(admin);
        return reportDAO.findAll();
    }

    public void resolveReport(User admin, int reportId) {
        requireAdmin(admin);
        reportDAO.resolve(reportId);
    }

    public Map<String, Long> stats(User admin) {
        requireAdmin(admin);
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("Users", userDAO.countByRole(User.Role.USER));
        m.put("Skills in catalog", skillDAO.count());
        m.put("Pending requests", requestDAO.countByStatus(SwapRequest.Status.PENDING));
        m.put("Accepted swaps", requestDAO.countByStatus(SwapRequest.Status.ACCEPTED));
        m.put("Scheduled sessions", sessionDAO.countByStatus(LearningSession.Status.SCHEDULED));
        m.put("Completed sessions", sessionDAO.countByStatus(LearningSession.Status.COMPLETED));
        m.put("Open reports", reportDAO.countOpen());
        return m;
    }

    /** Any logged-in user can report another user. */
    public void reportUser(User reporter, int reportedId, String reason) {
        String r = Validator.require(reason, "Reason", 500);
        if (reporter.getId() == reportedId) throw new ServiceException("You cannot report yourself.");
        userDAO.findById(reportedId).orElseThrow(() -> new ServiceException("User not found."));
        reportDAO.insert(reporter.getId(), reportedId, r);
    }

    private void requireAdmin(User u) {
        if (u == null || !u.isAdmin()) throw new ServiceException("Administrator access required.");
    }
}
