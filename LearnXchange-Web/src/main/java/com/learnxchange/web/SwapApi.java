package com.learnxchange.web;

import com.learnxchange.model.SwapRequest;
import com.learnxchange.model.User;
import com.learnxchange.service.SwapService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class SwapApi {
    private final SwapService swaps = new SwapService();
    private final SessionGuard guard;

    public SwapApi(SessionGuard guard) { this.guard = guard; }

    @PostMapping
    public Map<String, Boolean> send(@RequestBody Dto.SwapBody b, HttpSession session) {
        User me = guard.require(session);
        swaps.send(me, b.receiverId(), b.offeredSkillId(), b.requestedSkillId(), b.message());
        return Map.of("ok", true);
    }

    @GetMapping("/incoming")
    public List<SwapRequest> incoming(HttpSession session) {
        return swaps.incoming(guard.require(session));
    }

    @GetMapping("/outgoing")
    public List<SwapRequest> outgoing(HttpSession session) {
        return swaps.outgoing(guard.require(session));
    }

    @GetMapping("/pending-count")
    public Map<String, Long> pending(HttpSession session) {
        return Map.of("count", swaps.pendingIncomingCount(guard.require(session)));
    }

    @PostMapping("/{id}/accept")
    public Map<String, Boolean> accept(@PathVariable int id, HttpSession session) {
        swaps.accept(guard.require(session), id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/reject")
    public Map<String, Boolean> reject(@PathVariable int id, HttpSession session) {
        swaps.reject(guard.require(session), id);
        return Map.of("ok", true);
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Boolean> cancel(@PathVariable int id, HttpSession session) {
        swaps.cancel(guard.require(session), id);
        return Map.of("ok", true);
    }
}
