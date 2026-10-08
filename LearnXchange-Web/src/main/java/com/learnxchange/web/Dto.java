package com.learnxchange.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.learnxchange.model.*;

import java.util.List;

/** Request/response shapes. Responses never include password hashes. */
public final class Dto {
    private Dto() { }

    // ---------- responses ----------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserView(int id, String name, String email, String department, String bio,
                           List<String> availability, String role, boolean blocked,
                           double avgRating, int ratingCount) { }

    public record MatchView(UserView user, double percentage, double skillScore, double reverseScore,
                            double proficiencyScore, double availabilityScore, double ratingScore,
                            List<String> theyTeachMe, List<String> iTeachThem) { }

    public record ProfileView(UserView user, List<UserSkill> skills, List<Rating> reviews) { }

    /** Includes the email: only for the user themself and for admins. */
    public static UserView full(User u) {
        return new UserView(u.getId(), u.getName(), u.getEmail(), u.getDepartment(), u.getBio(),
                List.copyOf(u.getAvailability()), u.getRole().name(), u.isBlocked(),
                u.getAvgRating(), u.getRatingCount());
    }

    /** What other members get to see: no email. */
    public static UserView pub(User u) {
        return new UserView(u.getId(), u.getName(), null, u.getDepartment(), u.getBio(),
                List.copyOf(u.getAvailability()), u.getRole().name(), false,
                u.getAvgRating(), u.getRatingCount());
    }

    public static MatchView match(MatchResult m) {
        return new MatchView(pub(m.user()), m.percentage(), m.skillScore(), m.reverseScore(),
                m.proficiencyScore(), m.availabilityScore(), m.ratingScore(), m.theyTeachMe(), m.iTeachThem());
    }

    // ---------- request bodies ----------
    public record RegisterBody(String name, String email, String password, String department) { }
    public record LoginBody(String email, String password) { }
    public record ProfileBody(String name, String department, String bio, List<String> availability) { }
    public record SkillBody(int skillId, String type, int proficiency) { }
    public record SwapBody(int receiverId, int offeredSkillId, int requestedSkillId, String message) { }
    public record SessionBody(int requestId, String date, String time, String mode, String where) { }
    public record RateBody(int score, String feedback) { }
    public record ReportBody(int userId, String reason) { }
    public record CatalogSkillBody(String name, String category) { }
    public record BlockBody(boolean blocked) { }
}
