
package com.communex.events;

import com.communex.events.dto.EventRequest;
import com.communex.events.dto.EventResponse;
import com.communex.users.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    private String email(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }

        return authentication.getName();
    }

    @GetMapping
    public List<EventResponse> list(Authentication authentication) {
        return service.list(email(authentication));
    }

    @PostMapping
    public EventResponse create(
            Authentication authentication,
            @Valid @RequestBody EventRequest request
    ) {
        return service.create(
                email(authentication),
                request
        );
    }

    @PostMapping("/{id}/register")
    public EventResponse register(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.register(id, email(authentication));
    }

    @DeleteMapping("/{id}/registration")
    public Map<String, String> unregister(
            @PathVariable Long id,
            Authentication authentication
    ) {
        service.unregister(id, email(authentication));

        return Map.of(
                "message",
                "Registration cancelled"
        );
    }
}
