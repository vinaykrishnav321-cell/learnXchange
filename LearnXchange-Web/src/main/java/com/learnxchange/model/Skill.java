package com.learnxchange.model;

public record Skill(int id, String name, String category) {
    @Override public String toString() { return name + " [" + category + "]"; }
}
