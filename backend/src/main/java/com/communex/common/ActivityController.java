package com.communex.common;

import com.communex.users.User;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityLogRepository logs;

    // Constructor
    public ActivityController(ActivityLogRepository logs) {
        this.logs = logs;
    }

    // Get the current user's recent activity
    @GetMapping
    public List<Map<String, Object>> mine(Authentication authentication) {

        String email;

        // Identify the authenticated user
        if (authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            email = user.getEmail();
        } else {
            email = authentication.getName();
        }

        // Retrieve and format the latest 100 activity records
        return logs.findTop100ByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(log -> {

                    Long entityId = log.getEntityId();
                    if (entityId == null) {
                        entityId = 0L;
                    }

                    String details = log.getDetails();
                    if (details == null) {
                        details = "";
                    }

                    return Map.<String, Object>of(
                            "id", log.getId(),
                            "action", log.getAction(),
                            "entityType", log.getEntityType(),
                            "entityId", entityId,
                            "details", details,
                            "createdAt", log.getCreatedAt()
                    );
                })
                .toList();
    }
}