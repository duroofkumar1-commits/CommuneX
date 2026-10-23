package com.communex.messaging;

import com.communex.common.ActivityLogService;
import com.communex.messaging.dto.ConversationResponse;
import com.communex.messaging.dto.MessageRequest;
import com.communex.messaging.dto.MessageResponse;
import com.communex.notifications.NotificationService;
import com.communex.users.User;
import com.communex.users.UserBlockRepository;
import com.communex.users.UserRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageService {
    private static final int CONVERSATION_PAGE_SIZE = 200;

    private final DirectMessageRepository messages;
    private final UserRepository users;
    private final UserBlockRepository blocks;
    private final ActivityLogService activity;
    private final NotificationService notifications;

    public MessageService(
            DirectMessageRepository messages,
            UserRepository users,
            UserBlockRepository blocks,
            ActivityLogService activity,
            NotificationService notifications) {
        this.messages = messages;
        this.users = users;
        this.blocks = blocks;
        this.activity = activity;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> conversation(String email, String with) {
        return conversation(email, with, 0);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> conversation(String email, String with, int page) {
        String currentEmail = normalizeEmail(email);
        String otherEmail = normalizeEmail(with);
        if (currentEmail.equals(otherEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot message yourself");
        }

        User sender = user(currentEmail);
        User recipient = user(otherEmail);
        checkBlocked(sender, recipient);

        // Query only the latest messages; sort them for display from oldest to newest.
        return messages.latestConversation(
                        currentEmail, otherEmail,
                        PageRequest.of(Math.max(0, page), CONVERSATION_PAGE_SIZE))
                .stream()
                .sorted(Comparator.comparing(DirectMessage::getSentAt)
                        .thenComparing(DirectMessage::getId))
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse send(String email, MessageRequest request) {
        User sender = user(email);
        User recipient = user(request.recipientEmail().trim().toLowerCase(Locale.ROOT));

        if (sender.getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot message yourself");
        }
        checkBlocked(sender, recipient);

        DirectMessage message = new DirectMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(request.content().trim());
        message = messages.save(message);

        notifications.createForUser(recipient, "New private message from " + sender.getName() + ".");
        activity.record(email, "MESSAGE_SENT", "MESSAGE", message.getId(), "Sent a private message");
        return MessageResponse.from(message);
    }

    @Transactional
    public void markRead(String email, String with) {
        if (with == null || with.isBlank()) return;

        messages.markConversationRead(
                normalizeEmail(email), normalizeEmail(with), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> conversations(String email) {
        return messages.findConversationSummaries(normalizeEmail(email)).stream()
                .map(row -> new ConversationResponse(
                        row.getEmail(),
                        row.getName(),
                        row.getLastMessage(),
                        row.getLastMessageAt(),
                        row.getUnreadCount() == null ? 0L : row.getUnreadCount()))
                .toList();
    }

    private void checkBlocked(User first, User second) {
        boolean eitherBlocked = blocks.existsByBlockerIdAndBlockedId(first.getId(), second.getId())
                || blocks.existsByBlockerIdAndBlockedId(second.getId(), first.getId());
        if (eitherBlocked) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Messaging is unavailable because one account has blocked the other");
        }
    }

    private User user(String email) {
        return users.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
