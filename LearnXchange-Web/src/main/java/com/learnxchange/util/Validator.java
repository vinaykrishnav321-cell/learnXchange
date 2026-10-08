package com.learnxchange.util;

import java.util.regex.Pattern;

/** Central input validation. All methods throw ServiceException with a user-friendly message. */
public final class Validator {
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validator() { }

    public static String require(String value, String field, int maxLen) {
        if (value == null || value.isBlank()) throw new ServiceException(field + " is required.");
        String v = value.trim();
        if (v.length() > maxLen) throw new ServiceException(field + " must be at most " + maxLen + " characters.");
        return v;
    }

    public static String optional(String value, String field, int maxLen) {
        if (value == null) return "";
        String v = value.trim();
        if (v.length() > maxLen) throw new ServiceException(field + " must be at most " + maxLen + " characters.");
        return v;
    }

    public static String email(String value) {
        String v = require(value, "Email", 150).toLowerCase();
        if (!EMAIL.matcher(v).matches()) throw new ServiceException("Please enter a valid email address.");
        return v;
    }

    public static void password(String value) {
        if (value == null || value.length() < 8)
            throw new ServiceException("Password must be at least 8 characters.");
        boolean letter = value.chars().anyMatch(Character::isLetter);
        boolean digit = value.chars().anyMatch(Character::isDigit);
        if (!letter || !digit)
            throw new ServiceException("Password must contain at least one letter and one digit.");
    }

    public static int range(int value, int min, int max, String field) {
        if (value < min || value > max)
            throw new ServiceException(field + " must be between " + min + " and " + max + ".");
        return value;
    }
}
