
package com.communex.community;

import com.communex.community.dto.PollRequest;
import com.communex.community.dto.PollVoteRequest;
import com.communex.users.User;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/polls")
public class PollController {

    private final PollService service;

    public PollController(PollService service) {
        this.service = service;
    }

    private String email(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }

        return authentication.getName();
    }

    @GetMapping
    public List<Map<String, Object>> list(Authentication authentication) {
        return service.list(email(authentication));
    }

    @PostMapping
    public Map<String, Object> create(
            Authentication authentication,
            @Valid @RequestBody PollRequest request
    ) {
        return service.create(email(authentication), request);
    }

    @PostMapping("/{id}/vote")
    public Map<String, Object> vote(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody PollVoteRequest request
    ) {
        return service.vote(
                id,
                email(authentication),
                request
        );
    }
}
