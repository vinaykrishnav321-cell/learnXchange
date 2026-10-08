package com.learnxchange.model;

/** A skill on a user's profile: either something they TEACH or something they want to LEARN. */
public record UserSkill(int id, int userId, int skillId, String skillName, String category,
                        Type type, int proficiency) {
    public enum Type { TEACH, LEARN }

    @Override public String toString() { return skillName + " (level " + proficiency + ")"; }
}
