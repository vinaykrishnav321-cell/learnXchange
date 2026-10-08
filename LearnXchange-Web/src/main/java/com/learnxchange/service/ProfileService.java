package com.learnxchange.service;

import com.learnxchange.dao.SkillDAO;
import com.learnxchange.dao.UserDAO;
import com.learnxchange.dao.UserSkillDAO;
import com.learnxchange.model.Availability;
import com.learnxchange.model.Skill;
import com.learnxchange.model.User;
import com.learnxchange.model.UserSkill;
import com.learnxchange.util.ServiceException;
import com.learnxchange.util.Validator;

import java.util.List;
import java.util.Set;

public class ProfileService {
    private final UserDAO userDAO = new UserDAO();
    private final SkillDAO skillDAO = new SkillDAO();
    private final UserSkillDAO userSkillDAO = new UserSkillDAO();

    public User getUser(int id) {
        return userDAO.findById(id).orElseThrow(() -> new ServiceException("User not found."));
    }

    public void updateProfile(User user, String name, String department, String bio, Set<String> availability) {
        user.setName(Validator.require(name, "Name", 100));
        user.setDepartment(Validator.optional(department, "Department", 100));
        user.setBio(Validator.optional(bio, "Bio", 500));
        for (String slot : availability) {
            if (!Availability.SLOTS.contains(slot)) throw new ServiceException("Unknown availability slot: " + slot);
        }
        user.setAvailability(availability);
        userDAO.updateProfile(user);
    }

    public List<Skill> allSkills() {
        return skillDAO.findAll();
    }

    public List<UserSkill> skillsOf(int userId) {
        return userSkillDAO.findByUser(userId);
    }

    public void saveSkill(int userId, Skill skill, UserSkill.Type type, int proficiency) {
        if (skill == null) throw new ServiceException("Please choose a skill.");
        if (type == null) throw new ServiceException("Please choose whether you teach or want to learn it.");
        Validator.range(proficiency, 1, 5, "Proficiency");

        UserSkill.Type opposite = type == UserSkill.Type.TEACH ? UserSkill.Type.LEARN : UserSkill.Type.TEACH;
        boolean clash = userSkillDAO.findByUser(userId).stream()
                .anyMatch(s -> s.skillId() == skill.id() && s.type() == opposite);
        if (clash) {
            throw new ServiceException("You already listed " + skill.name() + " as a skill you "
                    + (opposite == UserSkill.Type.TEACH ? "teach" : "want to learn")
                    + ". Remove it first if you want to change that.");
        }
        userSkillDAO.upsert(userId, skill.id(), type, proficiency);
    }

    public void removeSkill(int userSkillId, int userId) {
        userSkillDAO.delete(userSkillId, userId);
    }
}
