package com.learnxchange.web;

import com.learnxchange.model.Report;
import com.learnxchange.model.Skill;
import com.learnxchange.model.User;
import com.learnxchange.service.AdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminApi {
    private final AdminService admin = new AdminService();
    private final SessionGuard guard;

    public AdminApi(SessionGuard guard) { this.guard = guard; }

    @GetMapping("/users")
    public List<Dto.UserView> users(HttpSession session) {
        User me = guard.requireAdmin(session);
        return admin.users(me).stream().map(Dto::full).toList();
    }

    @PostMapping("/users/{id}/block")
    public Map<String, Boolean> block(@PathVariable int id, @RequestBody Dto.BlockBody b, HttpSession session) {
        admin.setBlocked(guard.requireAdmin(session), id, b.blocked());
        return Map.of("ok", true);
    }

    @PostMapping("/skills")
    public Skill addSkill(@RequestBody Dto.CatalogSkillBody b, HttpSession session) {
        return admin.addSkill(guard.requireAdmin(session), b.name(), b.category());
    }

    @DeleteMapping("/skills/{id}")
    public Map<String, Boolean> deleteSkill(@PathVariable int id, HttpSession session) {
        admin.deleteSkill(guard.requireAdmin(session), id);
        return Map.of("ok", true);
    }

    @GetMapping("/reports")
    public List<Report> reports(HttpSession session) {
        return admin.reports(guard.requireAdmin(session));
    }

    @PostMapping("/reports/{id}/resolve")
    public Map<String, Boolean> resolve(@PathVariable int id, HttpSession session) {
        admin.resolveReport(guard.requireAdmin(session), id);
        return Map.of("ok", true);
    }

    @GetMapping("/stats")
    public Map<String, Long> stats(HttpSession session) {
        return admin.stats(guard.requireAdmin(session));
    }
}
