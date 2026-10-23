package com.communex.requests;

import com.communex.requests.dto.HelpRequestCreateRequest;
import com.communex.requests.dto.HelpRequestResponse;
import com.communex.requests.dto.RequestStatusChangeRequest;
import com.communex.requests.dto.RequestUpdateResponse;
import com.communex.users.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class HelpRequestController {

    private final HelpRequestService service;

    // Constructor
    public HelpRequestController(HelpRequestService service) {
        this.service = service;
    }

    // Retrieve the authenticated user's email
    private String email(Authentication authentication) {

        if (authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }

        return authentication.getName();
    }

    // Create a new help request
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public HelpRequestResponse create(
            Authentication authentication,
            @Valid @RequestBody HelpRequestCreateRequest request
    ) {
        return service.create(
                email(authentication),
                request
        );
    }

    // Retrieve all open help requests
    @GetMapping("/open")
    @PreAuthorize("isAuthenticated()")
    public List<HelpRequestResponse> open(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return service.getOpenRequests(page, size);
    }

    // Retrieve the authenticated user's help requests
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public List<HelpRequestResponse> mine(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return service.getMyRequests(
                email(authentication),
                page,
                size
        );
    }

    // Accept a help request
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'RESPONDER')")
    public HelpRequestResponse accept(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.accept(
                id,
                email(authentication)
        );
    }

    // Mark a help request as in progress
    @PostMapping("/{id}/in-progress")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'RESPONDER')")
    public HelpRequestResponse inProgress(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.startInProgress(
                id,
                email(authentication)
        );
    }

    // Complete a help request
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'RESPONDER')")
    public HelpRequestResponse complete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.complete(
                id,
                email(authentication)
        );
    }

    // Cancel a help request
    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public HelpRequestResponse cancel(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.cancel(
                id,
                email(authentication)
        );
    }

    // Retrieve the history of a help request
    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public List<RequestUpdateResponse> history(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.history(
                id,
                email(authentication)
        );
    }

    // Allow authorized moderators to change request status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR', 'RESPONDER')")
    public HelpRequestResponse moderatorStatus(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody RequestStatusChangeRequest request
    ) {
        return service.moderatorTransition(
                id,
                email(authentication),
                request
        );
    }
}