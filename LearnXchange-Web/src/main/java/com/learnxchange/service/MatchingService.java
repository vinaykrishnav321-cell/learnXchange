package com.learnxchange.service;

import com.learnxchange.dao.UserDAO;
import com.learnxchange.dao.UserSkillDAO;
import com.learnxchange.model.MatchFilter;
import com.learnxchange.model.MatchResult;
import com.learnxchange.model.User;
import com.learnxchange.model.UserSkill;

import java.util.*;

/**
 * Weighted matching engine.
 *
 *   score = 0.35 * skill compatibility      (how much of what I want to learn they can teach)
 *         + 0.25 * reverse compatibility    (how much of what they want to learn I can teach)
 *         + 0.20 * proficiency              (avg proficiency of the skills involved in the swap)
 *         + 0.12 * availability overlap     (shared free slots / smaller slot set)
 *         + 0.08 * rating                   (avg rating / 5; neutral 0.5 for users with no ratings yet)
 */
public class MatchingService {
    public static final double W_SKILL = 0.35;
    public static final double W_REVERSE = 0.25;
    public static final double W_PROFICIENCY = 0.20;
    public static final double W_AVAILABILITY = 0.12;
    public static final double W_RATING = 0.08;
    private static final double NEUTRAL_RATING = 0.5;

    private final UserDAO userDAO = new UserDAO();
    private final UserSkillDAO userSkillDAO = new UserSkillDAO();

    public List<MatchResult> findMatches(User me, MatchFilter filter) {
        MatchFilter f = filter == null ? MatchFilter.NONE : filter;
        Map<Integer, List<UserSkill>> all = userSkillDAO.findAllGrouped();
        List<UserSkill> mine = all.getOrDefault(me.getId(), List.of());
        Map<Integer, UserSkill> myTeach = bySkill(mine, UserSkill.Type.TEACH);
        Map<Integer, UserSkill> myLearn = bySkill(mine, UserSkill.Type.LEARN);

        List<MatchResult> results = new ArrayList<>();
        for (User candidate : userDAO.findCandidates(me.getId())) {
            List<UserSkill> theirs = all.getOrDefault(candidate.getId(), List.of());
            Map<Integer, UserSkill> theirTeach = bySkill(theirs, UserSkill.Type.TEACH);
            Map<Integer, UserSkill> theirLearn = bySkill(theirs, UserSkill.Type.LEARN);

            if (!passesFilter(f, candidate, theirTeach.values())) continue;
            MatchResult r = score(me, candidate, myTeach, myLearn, theirTeach, theirLearn);
            if (r != null) results.add(r);
        }
        results.sort(Comparator.comparingDouble(MatchResult::percentage).reversed()
                .thenComparing(r -> r.user().getName()));
        return results;
    }

    private MatchResult score(User me, User other,
                              Map<Integer, UserSkill> myTeach, Map<Integer, UserSkill> myLearn,
                              Map<Integer, UserSkill> theirTeach, Map<Integer, UserSkill> theirLearn) {
        // Their teaching skills that I want to learn
        List<UserSkill> forward = new ArrayList<>();
        for (UserSkill want : myLearn.values()) {
            UserSkill t = theirTeach.get(want.skillId());
            if (t != null) forward.add(t);
        }
        // My teaching skills that they want to learn
        List<UserSkill> reverse = new ArrayList<>();
        for (UserSkill want : theirLearn.values()) {
            UserSkill t = myTeach.get(want.skillId());
            if (t != null) reverse.add(t);
        }
        if (forward.isEmpty() && reverse.isEmpty()) return null;

        double skill = myLearn.isEmpty() ? 0 : (double) forward.size() / myLearn.size();
        double rev = theirLearn.isEmpty() ? 0 : (double) reverse.size() / theirLearn.size();

        double profSum = 0;
        int n = 0;
        for (UserSkill s : forward) { profSum += s.proficiency() / 5.0; n++; }
        for (UserSkill s : reverse) { profSum += s.proficiency() / 5.0; n++; }
        double proficiency = n == 0 ? 0 : profSum / n;

        Set<String> overlap = new HashSet<>(me.getAvailability());
        overlap.retainAll(other.getAvailability());
        int denom = Math.min(me.getAvailability().size(), other.getAvailability().size());
        double availability = denom == 0 ? 0 : (double) overlap.size() / denom;

        double rating = other.getRatingCount() == 0 ? NEUTRAL_RATING : other.getAvgRating() / 5.0;

        double total = W_SKILL * skill + W_REVERSE * rev + W_PROFICIENCY * proficiency
                + W_AVAILABILITY * availability + W_RATING * rating;
        double pct = Math.round(total * 1000.0) / 10.0;

        List<String> theyTeachMe = forward.stream().map(UserSkill::skillName).sorted().toList();
        List<String> iTeachThem = reverse.stream().map(UserSkill::skillName).sorted().toList();
        return new MatchResult(other, pct, skill, rev, proficiency, availability, rating, theyTeachMe, iTeachThem);
    }

    private boolean passesFilter(MatchFilter f, User candidate, Collection<UserSkill> theirTeach) {
        if (f.skillText() != null && !f.skillText().isBlank()) {
            String q = f.skillText().trim().toLowerCase();
            boolean any = theirTeach.stream().anyMatch(s -> s.skillName().toLowerCase().contains(q));
            if (!any) return false;
        }
        if (f.category() != null && !f.category().isBlank()) {
            boolean any = theirTeach.stream().anyMatch(s -> s.category().equalsIgnoreCase(f.category()));
            if (!any) return false;
        }
        if (f.availabilitySlot() != null && !f.availabilitySlot().isBlank()) {
            if (!candidate.getAvailability().contains(f.availabilitySlot())) return false;
        }
        return true;
    }

    private Map<Integer, UserSkill> bySkill(List<UserSkill> skills, UserSkill.Type type) {
        Map<Integer, UserSkill> map = new LinkedHashMap<>();
        for (UserSkill s : skills) {
            if (s.type() == type) map.put(s.skillId(), s);
        }
        return map;
    }
}
