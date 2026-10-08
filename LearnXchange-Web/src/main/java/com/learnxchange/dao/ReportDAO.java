package com.learnxchange.dao;

import com.learnxchange.model.Report;
import com.learnxchange.util.DBConnection;
import com.learnxchange.util.DataAccessException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public void insert(int reporterId, int reportedId, String reason) {
        String sql = "INSERT INTO reports (reporter_id, reported_id, reason) VALUES (?,?,?)";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reporterId);
            ps.setInt(2, reportedId);
            ps.setString(3, reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not file report", e);
        }
    }

    public List<Report> findAll() {
        String sql = "SELECT rp.id, rp.reporter_id, a.name, rp.reported_id, b.name, rp.reason, rp.status, rp.created_at "
                + "FROM reports rp JOIN users a ON a.id = rp.reporter_id JOIN users b ON b.id = rp.reported_id "
                + "ORDER BY (rp.status = 'OPEN') DESC, rp.created_at DESC";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Report> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Report(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getString(5),
                        rs.getString(6), Report.Status.valueOf(rs.getString(7)), rs.getTimestamp(8).toLocalDateTime()));
            }
            return out;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load reports", e);
        }
    }

    public void resolve(int id) {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("UPDATE reports SET status = 'RESOLVED' WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not resolve report", e);
        }
    }

    public long countOpen() {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM reports WHERE status = 'OPEN'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DataAccessException("Count failed", e);
        }
    }
}
