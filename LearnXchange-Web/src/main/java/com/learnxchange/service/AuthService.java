package com.learnxchange.service;

import com.learnxchange.dao.UserDAO;
import com.learnxchange.model.User;
import com.learnxchange.util.PasswordUtil;
import com.learnxchange.util.ServiceException;
import com.learnxchange.util.Validator;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public User register(String name, String email, String password, String department) {
        String cleanName = Validator.require(name, "Name", 100);
        String cleanEmail = Validator.email(email);
        Validator.password(password);
        String cleanDept = Validator.optional(department, "Department", 100);

        if (userDAO.findByEmail(cleanEmail).isPresent())
            throw new ServiceException("An account with this email already exists.");

        User u = new User();
        u.setName(cleanName);
        u.setEmail(cleanEmail);
        u.setDepartment(cleanDept);
        u.setPasswordHash(PasswordUtil.hash(password));
        u.setRole(User.Role.USER);
        u.setId(userDAO.insert(u));
        return u;
    }

    public User login(String email, String password) {
        String e = email == null ? "" : email.trim().toLowerCase();
        User u = userDAO.findByEmail(e).orElse(null);
        if (u == null || !PasswordUtil.verify(password == null ? "" : password, u.getPasswordHash()))
            throw new ServiceException("Invalid email or password.");
        if (u.isBlocked())
            throw new ServiceException("This account has been blocked. Please contact an administrator.");
        return u;
    }

    /** Creates the first admin from deployment-supplied credentials. Does nothing if an admin already exists. */
    public boolean ensureAdmin(String name, String email, String password) {
        if (userDAO.countByRole(User.Role.ADMIN) > 0) return false;
        User admin = new User();
        admin.setName(name == null || name.isBlank() ? "Administrator" : name.trim());
        admin.setEmail(Validator.email(email));
        Validator.password(password);
        admin.setPasswordHash(PasswordUtil.hash(password));
        admin.setRole(User.Role.ADMIN);
        userDAO.insert(admin);
        return true;
    }
}
