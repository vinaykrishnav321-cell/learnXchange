package com.learnxchange.dao;

import com.learnxchange.model.User;
import com.learnxchange.util.DBConnection;
import com.learnxchange.util.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {
    private static final String COLS =
            "id, name, email, password_hash, department, bio, availability, role, blocked, avg_rating, rating_count";

    public int insert(User u) {
        String sql = "INSERT INTO users (name, email, password_hash, department, bio, availability, role) "
                + "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getDepartment());
            ps.setString(5, u.getBio());
            ps.setString(6, u.getAvailabilityCsv());
            ps.setString(7, u.getRole().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not create user", e);
        }
    }

    public void updateProfile(User u) {
        String sql = "UPDATE users SET name=?, department=?, bio=?, availability=? WHERE id=?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getDepartment());
            ps.setString(3, u.getBio());
            ps.setString(4, u.getAvailabilityCsv());
            ps.setInt(5, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update profile", e);
        }
    }

    public void setBlocked(int userId, boolean blocked) {
        execute("UPDATE users SET blocked=? WHERE id=?", blocked, userId);
    }

    /** Recomputes the cached average rating from the ratings table. */
    public void refreshRating(int userId) {
        execute("UPDATE users SET "
                + "avg_rating = (SELECT COALESCE(AVG(score),0) FROM ratings WHERE ratee_id = ?), "
                + "rating_count = (SELECT COUNT(*) FROM ratings WHERE ratee_id = ?) WHERE id = ?",
                userId, userId, userId);
    }

    public Optional<User> findByEmail(String email) {
        return one("SELECT " + COLS + " FROM users WHERE email = ?", email);
    }

    public Optional<User> findById(int id) {
        return one("SELECT " + COLS + " FROM users WHERE id = ?", id);
    }

    public List<User> findAll() {
        return list("SELECT " + COLS + " FROM users ORDER BY id");
    }

    /** Everyone who can appear in match results: not me, not blocked, not an admin. */
    public List<User> findCandidates(int excludeId) {
        return list("SELECT " + COLS + " FROM users WHERE id <> ? AND blocked = FALSE AND role = 'USER'", excludeId);
    }

    public long count() {
        return scalar("SELECT COUNT(*) FROM users");
    }

    public long countByRole(User.Role role) {
        return scalar("SELECT COUNT(*) FROM users WHERE role = ?", role.name());
    }

    // ---- helpers ----
    private Optional<User> one(String sql, Object... params) {
        List<User> l = list(sql, params);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    private List<User> list(String sql, Object... params) {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("User query failed", e);
        }
    }

    private long scalar(String sql, Object... params) {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Count query failed", e);
        }
    }

    private void execute(String sql, Object... params) {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("User update failed", e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setDepartment(nz(rs.getString("department")));
        u.setBio(nz(rs.getString("bio")));
        u.setAvailabilityCsv(rs.getString("availability"));
        u.setRole(User.Role.valueOf(rs.getString("role")));
        u.setBlocked(rs.getBoolean("blocked"));
        u.setAvgRating(rs.getDouble("avg_rating"));
        u.setRatingCount(rs.getInt("rating_count"));
        return u;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
