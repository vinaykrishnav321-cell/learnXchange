package com.learnxchange.dao;

import com.learnxchange.model.LearningSession;
import com.learnxchange.util.DBConnection;
import com.learnxchange.util.DataAccessException;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SessionDAO {
    private static final String SELECT =
            "SELECT se.id, se.request_id, r.sender_id, r.receiver_id, a.name AS a_name, b.name AS b_name, "
                    + "os.name AS offered_name, rs.name AS requested_name, "
                    + "se.session_date, se.session_time, se.session_mode, se.location_or_link, se.status "
                    + "FROM sessions se "
                    + "JOIN swap_requests r ON r.id = se.request_id "
                    + "JOIN users a ON a.id = r.sender_id "
                    + "JOIN users b ON b.id = r.receiver_id "
                    + "JOIN skills os ON os.id = r.offered_skill_id "
                    + "JOIN skills rs ON rs.id = r.requested_skill_id ";

    public int insert(int requestId, LocalDate date, LocalTime time, LearningSession.Mode mode, String where) {
        String sql = "INSERT INTO sessions (request_id, session_date, session_time, session_mode, location_or_link) "
                + "VALUES (?,?,?,?,?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, requestId);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(time));
            ps.setString(4, mode.name());
            ps.setString(5, where);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not schedule session", e);
        }
    }

    public Optional<LearningSession> findById(int id) {
        List<LearningSession> l = query(SELECT + "WHERE se.id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public List<LearningSession> findByUser(int userId) {
        return query(SELECT + "WHERE r.sender_id = ? OR r.receiver_id = ? "
                + "ORDER BY se.session_date DESC, se.session_time DESC", userId, userId);
    }

    public void updateStatus(int id, LearningSession.Status status) {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("UPDATE sessions SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update session", e);
        }
    }

    public long countByStatus(LearningSession.Status status) {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM sessions WHERE status = ?")) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Count failed", e);
        }
    }

    private List<LearningSession> query(String sql, Object... params) {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<LearningSession> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new LearningSession(
                            rs.getInt("id"), rs.getInt("request_id"),
                            rs.getInt("sender_id"), rs.getInt("receiver_id"),
                            rs.getString("a_name"), rs.getString("b_name"),
                            rs.getString("offered_name") + " <-> " + rs.getString("requested_name"),
                            rs.getDate("session_date").toLocalDate(), rs.getTime("session_time").toLocalTime(),
                            LearningSession.Mode.valueOf(rs.getString("session_mode")),
                            rs.getString("location_or_link"),
                            LearningSession.Status.valueOf(rs.getString("status"))));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load sessions", e);
        }
    }
}
