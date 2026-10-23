package com.communex.common;

import com.communex.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {

    private final ActivityLogRepository logs;
    private final UserRepository users;

    // Constructor
    public ActivityLogService(
            ActivityLogRepository logs,
            UserRepository users
    ) {
        this.logs = logs;
        this.users = users;
    }

    // Record an activity for the specified user
    @Transactional
    public void record(
            String email,
            String action,
            String type,
            Long entityId,
            String details
    ) {

        // Ignore missing or blank email addresses
        if (email == null || email.isBlank()) {
            return;
        }

        // Find the user and save the activity
        users.findByEmail(email).ifPresent(user -> {

            ActivityLog log = new ActivityLog();

            log.setUser(user);
            log.setAction(action);
            log.setEntityType(type);
            log.setEntityId(entityId);
            log.setDetails(details);

            logs.save(log);
        });
    }
}