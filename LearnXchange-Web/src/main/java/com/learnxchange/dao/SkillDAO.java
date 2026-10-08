package com.learnxchange.dao;

import com.learnxchange.model.Skill;
import com.learnxchange.util.DBConnection;
import com.learnxchange.util.DataAccessException;
import com.learnxchange.util.ServiceException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SkillDAO {

    public List<Skill> findAll() {
        String sql = "SELECT id, name, category FROM skills ORDER BY category, name";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Skill> out = new ArrayList<>();
            while (rs.next()) out.add(new Skill(rs.getInt(1), rs.getString(2), rs.getString(3)));
            return out;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load skills", e);
        }
    }

    public Optional<Skill> findByName(String name) {
        String sql = "SELECT id, name, category FROM skills WHERE LOWER(name) = LOWER(?)";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(new Skill(rs.getInt(1), rs.getString(2), rs.getString(3)))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Skill lookup failed", e);
        }
    }

    public int insert(String name, String category) {
        String sql = "INSERT INTO skills (name, category) VALUES (?, ?)";
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            if (isConstraintViolation(e)) throw new ServiceException("That skill already exists.");
            throw new DataAccessException("Could not add skill", e);
        }
    }

    public void delete(int id) {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("DELETE FROM skills WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (isConstraintViolation(e))
                throw new ServiceException("This skill is referenced by existing swap requests and cannot be removed.");
            throw new DataAccessException("Could not delete skill", e);
        }
    }

    public long count() {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM skills");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new DataAccessException("Count failed", e);
        }
    }

    private static boolean isConstraintViolation(SQLException e) {
        return e.getSQLState() != null && e.getSQLState().startsWith("23");
    }
}
