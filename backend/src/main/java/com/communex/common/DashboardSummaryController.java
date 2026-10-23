package com.communex.common;

import com.communex.events.CommunityEventRepository;
import com.communex.facilities.FacilityRepository;
import com.communex.requests.HelpRequestRepository;
import com.communex.users.User;
import com.communex.volunteers.VolunteerRepository;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("isAuthenticated()")
public class DashboardSummaryController {
    private final HelpRequestRepository requests;
    private final VolunteerRepository volunteers;
    private final FacilityRepository facilities;
    private final CommunityEventRepository events;

    public DashboardSummaryController(
            HelpRequestRepository requests,
            VolunteerRepository volunteers,
            FacilityRepository facilities,
            CommunityEventRepository events) {
        this.requests = requests;
        this.volunteers = volunteers;
        this.facilities = facilities;
        this.events = events;
    }

    @GetMapping("/summary")
    public Map<String, Long> summary(Authentication authentication) {
        String email = authentication.getPrincipal() instanceof User user
                ? user.getEmail()
                : authentication.getName();

        return Map.of(
                "requests", requests.countForUser(email),
                "volunteers", volunteers.countByAvailableTrue(),
                "facilities", facilities.countByActiveTrue(),
                "events", events.countByStatus("PUBLISHED"));
    }
}
