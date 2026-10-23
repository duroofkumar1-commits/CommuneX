package com.communex.volunteers;

import com.communex.users.User;
import com.communex.volunteers.dto.VolunteerRequest;
import com.communex.volunteers.dto.VolunteerResponse;
import com.communex.volunteers.dto.VolunteerUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/volunteers")
public class VolunteerController {
    private final VolunteerService volunteerService;

    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('USER', 'VOLUNTEER')")
    public VolunteerResponse register(
            Authentication authentication,
            @Valid @RequestBody VolunteerRequest request) {
        User user = (User) authentication.getPrincipal();
        return volunteerService.createVolunteer(user.getEmail(), request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public VolunteerResponse me(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return volunteerService.getMyProfile(user.getEmail());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public VolunteerResponse update(
            Authentication authentication,
            @Valid @RequestBody VolunteerUpdateRequest request) {
        User user = (User) authentication.getPrincipal();
        return volunteerService.updateMyProfile(user.getEmail(), request);
    }

    @GetMapping("/available")
    @PreAuthorize("isAuthenticated()")
    public List<VolunteerResponse> available(
            @RequestParam(required = false) String skills,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        return volunteerService.searchAvailableVolunteers(skills, page, size);
    }
}
