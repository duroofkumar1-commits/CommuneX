package com.communex.notifications;

import com.communex.users.User;
import com.communex.users.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional
    public void createForUser(User recipient, String message) {
        if (recipient == null || message == null || message.isBlank()) return;
        Notification notification = new Notification();
        notification.setUser(recipient);
        String safeMessage = message.trim();
        notification.setMessage(safeMessage.length() > 200
                ? safeMessage.substring(0, 197) + "..."
                : safeMessage);
        notifications.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAll(String email) {
        User user = getUser(email);
        return toResponseList(notifications.findTop50ByUserOrderByCreatedAtDesc(user));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(String email, int page) {
        User user = getUser(email);
        int safePage = Math.max(0, page);
        List<Map<String, Object>> items = toResponseList(
                notifications.findByUserOrderByCreatedAtDesc(user, PageRequest.of(safePage, 50)));
        long unreadCount = notifications.countByUserAndReadFalse(user);
        return Map.of("items", items, "unreadCount", unreadCount);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadCount(String email) {
        User user = getUser(email);
        return Map.of("count", notifications.countByUserAndReadFalse(user));
    }

    @Transactional
    public void markRead(Long id, String email) {
        User user = getUser(email);
        Notification notification = notifications.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        notification.setRead(true);
    }

    private List<Map<String, Object>> toResponseList(List<Notification> source) {
        return source.stream()
                .map(notification -> Map.<String, Object>of(
                        "id", notification.getId(),
                        "message", notification.getMessage(),
                        "read", notification.isRead(),
                        "createdAt", notification.getCreatedAt().toString()))
                .toList();
    }

    private User getUser(String email) {
        return users.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
