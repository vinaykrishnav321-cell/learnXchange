package com.learnxchange.model;

import java.util.LinkedHashSet;
import java.util.Set;

public class User {
    public enum Role { USER, ADMIN }

    private int id;
    private String name = "";
    private String email = "";
    private String passwordHash = "";
    private String department = "";
    private String bio = "";
    private Set<String> availability = new LinkedHashSet<>();
    private Role role = Role.USER;
    private boolean blocked;
    private double avgRating;
    private int ratingCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public Set<String> getAvailability() { return availability; }
    public void setAvailability(Set<String> availability) { this.availability = availability; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public double getAvgRating() { return avgRating; }
    public void setAvgRating(double avgRating) { this.avgRating = avgRating; }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public String getAvailabilityCsv() { return String.join(",", availability); }

    public void setAvailabilityCsv(String csv) {
        Set<String> set = new LinkedHashSet<>();
        if (csv != null && !csv.isBlank()) {
            for (String s : csv.split(",")) {
                if (!s.isBlank()) set.add(s.trim());
            }
        }
        this.availability = set;
    }

    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override public String toString() { return name; }
}
