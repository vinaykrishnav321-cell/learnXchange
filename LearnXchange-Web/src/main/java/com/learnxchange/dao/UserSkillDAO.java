package com.learnxchange.dao;

import com.learnxchange.model.UserSkill;
import com.learnxchange.util.DBConnection;
import com.learnxchange.util.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserSkillDAO {
    private static final String SELECT =
            "SELECT us.id, us.user_id, us.skill_id, s.name, s.category, us.skill_type, us.proficiency "
                    + "FROM user_skills us JOIN skills s ON s.id = us.skill_id ";

    public List<UserSkill> findByUser(int userId) {
        return query(SELECT + "WHERE us.user_id = ? ORDER BY us.skill_type, s.name", userId);
    }

    /** All skills of all users, grouped by user id (used by the matching engine to avoid N+1 queries). */
    public Map<Integer, List<UserSkill>> findAllGrouped() {
        Map<Integer, List<UserSkill>> map = new LinkedHashMap<>();
        for (UserSkill us : query(SELECT + "ORDER BY us.user_id")) {
            map.computeIfAbsent(us.userId(), k -> new ArrayList<>()).add(us);
        }
        return map;
    }

    /** Adds the skill, or updates the proficiency if the user already has it with the same type. */
    public void upsert(int userId, int skillId, UserSkill.Type type, int proficiency) {
        String sql = "INSERT INTO user_skills (user_id, skill_id, skill_type, proficiency) VALUES (?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE proficiency = VALUES(proficiency)";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, skillId);
            ps.setString(3, type.name());
            ps.setInt(4, proficiency);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save skill", e);
        }
    }

    public void delete(int id, int userId) {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM user_skills WHERE id = ? AND user_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not remove skill", e);
        }
    }

    private List<UserSkill> query(String sql, Object... params) {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<UserSkill> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new UserSkill(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4),
                            rs.getString(5), UserSkill.Type.valueOf(rs.getString(6)), rs.getInt(7)));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load user skills", e);
        }
    }
}
