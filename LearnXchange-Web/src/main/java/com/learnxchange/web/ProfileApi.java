package com.learnxchange.web;

import com.learnxchange.model.Skill;
import com.learnxchange.model.User;
import com.learnxchange.model.UserSkill;
import com.learnxchange.service.ProfileService;
import com.learnxchange.util.ServiceException;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProfileApi {
    private final ProfileService profiles = new ProfileService();
    private final SessionGuard guard;

    public ProfileApi(SessionGuard guard) { this.guard = guard; }

    @GetMapping("/profile")
    public Map<String, Object> profile(HttpSession session) {
        User me = guard.require(session);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("user", Dto.full(me));
        out.put("skills", profiles.skillsOf(me.getId()));
        return out;
    }

    @PutMapping("/profile")
    public Dto.UserView update(@RequestBody Dto.ProfileBody b, HttpSession session) {
        User me = guard.require(session);
        profiles.updateProfile(me, b.name(), b.department(), b.bio(),
                new LinkedHashSet<>(b.availability() == null ? List.of() : b.availability()));
        return Dto.full(me);
    }

    @GetMapping("/skills")
    public List<Skill> catalog(HttpSession session) {
        guard.require(session);
        return profiles.allSkills();
    }

    @PostMapping("/profile/skills")
    public List<UserSkill> addSkill(@RequestBody Dto.SkillBody b, HttpSession session) {
        User me = guard.require(session);
        Skill skill = profiles.allSkills().stream().filter(s -> s.id() == b.skillId()).findFirst()
                .orElseThrow(() -> new ServiceException("Please choose a skill from the list."));
        UserSkill.Type type;
        try {
            type = UserSkill.Type.valueOf(String.valueOf(b.type()));
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Choose whether you teach this skill or want to learn it.");
        }
        profiles.saveSkill(me.getId(), skill, type, b.proficiency());
        return profiles.skillsOf(me.getId());
    }

    @DeleteMapping("/profile/skills/{id}")
    public List<UserSkill> removeSkill(@PathVariable int id, HttpSession session) {
        User me = guard.require(session);
        profiles.removeSkill(id, me.getId());
        return profiles.skillsOf(me.getId());
    }
}
