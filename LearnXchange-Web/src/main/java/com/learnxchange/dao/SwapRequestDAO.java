package com.learnxchange.dao;

import com.learnxchange.model.SwapRequest;
import com.learnxchange.util.DBConnection;
import com.learnxchange.util.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SwapRequestDAO {
    private static final String SELECT =
            "SELECT r.id, r.sender_id, r.receiver_id, su.name AS sender_name, ru.name AS receiver_name, "
                    + "r.offered_skill_id, os.name AS offered_name, r.requested_skill_id, rs.name AS requested_name, "
                    + "r.message, r.status, r.created_at "
                    + "FROM swap_requests r "
                    + "JOIN users su ON su.id = r.sender_id "
                    + "JOIN users ru ON ru.id = r.receiver_id "
                    + "JOIN skills os ON os.id = r.offered_skill_id "
                    + "JOIN skills rs ON rs.id = r.requested_skill_id ";

    public int insert(int senderId, int receiverId, int offeredSkillId, int requestedSkillId, String message) {
        String sql = "INSERT INTO swap_requests (sender_id, receiver_id, offered_skill_id, requested_skill_id, message) "
                + "VALUES (?,?,?,?,?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setInt(3, offeredSkillId);
            ps.setInt(4, requestedSkillId);
            ps.setString(5, message);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not send request", e);
        }
    }

    public Optional<SwapRequest> findById(int id) {
        List<SwapRequest> l = query(SELECT + "WHERE r.id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public List<SwapRequest> findIncoming(int userId) {
        return query(SELECT + "WHERE r.receiver_id = ? ORDER BY r.created_at DESC, r.id DESC", userId);
    }

    public List<SwapRequest> findOutgoing(int userId) {
        return query(SELECT + "WHERE r.sender_id = ? ORDER BY r.created_at DESC, r.id DESC", userId);
    }

    public boolean existsPending(int senderId, int receiverId, int offeredSkillId, int requestedSkillId) {
        String sql = "SELECT COUNT(*) FROM swap_requests WHERE sender_id=? AND receiver_id=? "
                + "AND offered_skill_id=? AND requested_skill_id=? AND status='PENDING'";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setInt(3, offeredSkillId);
            ps.setInt(4, requestedSkillId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Duplicate check failed", e);
        }
    }

    public void updateStatus(int id, SwapRequest.Status status) {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("UPDATE swap_requests SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update request", e);
        }
    }

    public long countByStatus(SwapRequest.Status status) {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM swap_requests WHERE status = ?")) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Count failed", e);
        }
    }

    private List<SwapRequest> query(String sql, Object... params) {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                List<SwapRequest> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new SwapRequest(
                            rs.getInt("id"), rs.getInt("sender_id"), rs.getInt("receiver_id"),
                            rs.getString("sender_name"), rs.getString("receiver_name"),
                            rs.getInt("offered_skill_id"), rs.getString("offered_name"),
                            rs.getInt("requested_skill_id"), rs.getString("requested_name"),
                            rs.getString("message"), SwapRequest.Status.valueOf(rs.getString("status")),
                            rs.getTimestamp("created_at").toLocalDateTime()));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load requests", e);
        }
    }
}
