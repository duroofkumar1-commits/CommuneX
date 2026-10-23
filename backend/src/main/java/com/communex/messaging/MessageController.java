package com.communex.messaging;

import com.communex.messaging.dto.ConversationResponse;
import com.communex.messaging.dto.MessageReadRequest;
import com.communex.messaging.dto.MessageRequest;
import com.communex.messaging.dto.MessageResponse;
import com.communex.users.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    private String email(Authentication authentication) {
        return authentication.getPrincipal() instanceof User user
                ? user.getEmail()
                : authentication.getName();
    }

    @GetMapping
    public List<MessageResponse> conversation(
            Authentication authentication,
            @RequestParam("with") String with,
            @RequestParam(defaultValue = "0") int page) {
        return service.conversation(email(authentication), with, Math.max(0, page));
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> conversations(Authentication authentication) {
        return service.conversations(email(authentication));
    }

    @PostMapping
    public MessageResponse send(
            Authentication authentication,
            @Valid @RequestBody MessageRequest request) {
        return service.send(email(authentication), request);
    }

    @PostMapping("/read")
    public Map<String, String> read(
            Authentication authentication,
            @Valid @RequestBody MessageReadRequest request) {
        service.markRead(email(authentication), request.withEmail());
        return Map.of("message", "Messages marked read");
    }
}
