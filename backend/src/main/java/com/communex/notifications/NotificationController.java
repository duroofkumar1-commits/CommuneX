package com.communex.notifications;

import com.communex.users.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    private String email(Authentication auth) {
        return ((User) auth.getPrincipal()).getEmail();
    }

    @GetMapping
    public List<Map<String, Object>> getAll(Authentication auth) {
        return service.getAll(email(auth));
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page) {
        return service.getSummary(email(auth), page);
    }

    @GetMapping("/unread-count")
    public Map<String, Object> unreadCount(Authentication auth) {
        return service.getUnreadCount(email(auth));
    }

    @PatchMapping("/{id}/read")
    public Map<String, String> markRead(
            @PathVariable Long id, Authentication auth) {
        service.markRead(id, email(auth));
        return Map.of("message", "Notification marked as read");
    }
}
