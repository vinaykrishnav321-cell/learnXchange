package com.learnxchange.web;

import com.learnxchange.dao.UserDAO;
import com.learnxchange.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Resolves the logged-in user from the HTTP session on every request (so blocks/role changes apply instantly). */
@Component
public class SessionGuard {
    public static final String KEY = "uid";
    private final UserDAO userDAO = new UserDAO();

    public User require(HttpSession session) {
        Object id = session.getAttribute(KEY);
        if (!(id instanceof Integer uid)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        User u = userDAO.findById(uid).orElse(null);
        if (u == null || u.isBlocked()) {
            session.invalidate();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        return u;
    }

    public User requireAdmin(HttpSession session) {
        User u = require(session);
        if (!u.isAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required.");
        return u;
    }
}
