package com.learnxchange.web;

import com.learnxchange.model.User;
import com.learnxchange.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthApi {
    private static final int MAX_FAILS = 5;
    private static final long WINDOW_MS = 5 * 60_000L;

    private final AuthService auth = new AuthService();
    private final SessionGuard guard;
    /** email -> {failure count, time of first failure}. Simple in-memory brute-force protection. */
    private final Map<String, long[]> fails = new ConcurrentHashMap<>();

    public AuthApi(SessionGuard guard) { this.guard = guard; }

    @PostMapping("/register")
    public Dto.UserView register(@RequestBody Dto.RegisterBody b, HttpServletRequest req) {
        User u = auth.register(b.name(), b.email(), b.password(), b.department());
        startSession(req, u);
        return Dto.full(u);
    }

    @PostMapping("/login")
    public Dto.UserView login(@RequestBody Dto.LoginBody b, HttpServletRequest req) {
        String key = b.email() == null ? "" : b.email().trim().toLowerCase();
        checkThrottle(key);
        User u;
        try {
            u = auth.login(b.email(), b.password());
        } catch (RuntimeException e) {
            recordFailure(key);
            throw e;
        }
        fails.remove(key);
        startSession(req, u);
        return Dto.full(u);
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(HttpSession session) {
        session.invalidate();
        return Map.of("ok", true);
    }

    @GetMapping("/me")
    public Dto.UserView me(HttpSession session) {
        return Dto.full(guard.require(session));
    }

    private void startSession(HttpServletRequest req, User u) {
        HttpSession s = req.getSession(true);
        req.changeSessionId(); // prevents session fixation
        s.setAttribute(SessionGuard.KEY, u.getId());
    }

    private void checkThrottle(String key) {
        long[] f = fails.get(key);
        if (f == null) return;
        if (System.currentTimeMillis() - f[1] > WINDOW_MS) {
            fails.remove(key);
        } else if (f[0] >= MAX_FAILS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed sign-in attempts. Try again in a few minutes.");
        }
    }

    private void recordFailure(String key) {
        long now = System.currentTimeMillis();
        fails.compute(key, (k, f) -> (f == null || now - f[1] > WINDOW_MS)
                ? new long[]{1, now} : new long[]{f[0] + 1, f[1]});
    }
}
