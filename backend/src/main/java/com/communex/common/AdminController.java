package com.communex.common;

import com.communex.events.CommunityEventRepository;
import com.communex.facilities.FacilityRepository;
import com.communex.requests.HelpRequestRepository;
import com.communex.requests.HelpRequestStatus;
import com.communex.resources.ResourceRepository;
import com.communex.users.Role;
import com.communex.users.User;
import com.communex.users.UserRepository;
import com.communex.users.dto.UserResponse;
import com.communex.volunteers.VolunteerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository users;
    private final VolunteerRepository volunteers;
    private final HelpRequestRepository requests;
    private final FacilityRepository facilities;
    private final ResourceRepository resources;
    private final CommunityEventRepository events;
    private final ActivityLogRepository logs;
    private final ActivityLogService activity;

    // Constructor
    public AdminController(
            UserRepository users,
            VolunteerRepository volunteers,
            HelpRequestRepository requests,
            FacilityRepository facilities,
            ResourceRepository resources,
            CommunityEventRepository events,
            ActivityLogRepository logs,
            ActivityLogService activity
    ) {
        this.users = users;
        this.volunteers = volunteers;
        this.requests = requests;
        this.facilities = facilities;
        this.resources = resources;
        this.events = events;
        this.logs = logs;
        this.activity = activity;
    }

    // Get administrative dashboard statistics
    @GetMapping("/overview")
    public Map<String, Object> overview() {

        return Map.of(
                "users", users.count(),
                "volunteers", volunteers.count(),
                "openRequests", requests.countByStatus(
                        HelpRequestStatus.OPEN
                ),
                "facilities", facilities.countByActiveTrue(),
                "resources", resources.countByActiveTrue(),
                "events", events.countByStatus("PUBLISHED"),
                "activityEvents", logs.count()
        );
    }

    // Retrieve all users
    @GetMapping("/users")
    public List<UserResponse> userList() {

        return users.findAll()
                .stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getProvider(),
                        user.getProfileImageUrl()
                ))
                .toList();
    }

    // Change a user's role
    @PatchMapping("/users/{id}/role")
    public UserResponse changeRole(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody RoleChangeRequest body
    ) {

        User actor = (User) authentication.getPrincipal();

        // Prevent an administrator from removing their own admin access
        if (actor.getId().equals(id) && body.role() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You cannot remove your own admin access"
            );
        }

        // Find the target user
        User target = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Save the role change
        Role previousRole = target.getRole();

        target.setRole(body.role());
        target = users.save(target);

        // Record the administrative action
        activity.record(
                actor.getEmail(),
                "ROLE_CHANGED",
                "USER",
                id,
                previousRole + " → " + body.role()
        );

        // Return the updated user information
        return new UserResponse(
                target.getId(),
                target.getName(),
                target.getEmail(),
                target.getRole(),
                target.getProvider(),
                target.getProfileImageUrl()
        );
    }

    // Request DTO for changing a user's role
    public record RoleChangeRequest(
            @NotNull
            Role role
    ) {
    }
}