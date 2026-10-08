package com.learnxchange.dao;

import com.learnxchange.model.Rating;
import com.learnxchange.util.DBConnection;
import com.learnxchange.util.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RatingDAO {

    public void insert(int sessionId, int raterId, int rateeId, int score, String feedback) {
        String sql = "INSERT INTO ratings (session_id, rater_id, ratee_id, score, feedback) VALUES (?,?,?,?,?)";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, raterId);
            ps.setInt(3, rateeId);
            ps.setInt(4, score);
            ps.setString(5, feedback);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save rating", e);
        }
    }

    public boolean exists(int sessionId, int raterId) {
        String sql = "SELECT COUNT(*) FROM ratings WHERE session_id = ? AND rater_id = ?";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, raterId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Rating check failed", e);
        }
    }

    public List<Rating> findByRatee(int rateeId) {
        String sql = "SELECT r.id, r.session_id, r.rater_id, u.name, r.ratee_id, r.score, r.feedback, r.created_at "
                + "FROM ratings r JOIN users u ON u.id = r.rater_id WHERE r.ratee_id = ? "
                + "ORDER BY r.created_at DESC, r.id DESC";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rateeId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Rating> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Rating(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getInt(5),
                            rs.getInt(6), rs.getString(7), rs.getTimestamp(8).toLocalDateTime()));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load ratings", e);
        }
    }
}
