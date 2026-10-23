package com.communex.common;

import com.communex.events.CommunityEventRepository;
import com.communex.facilities.FacilityRepository;
import com.communex.requests.HelpRequestRepository;
import com.communex.requests.HelpRequestStatus;
import com.communex.resources.ResourceRepository;
import com.communex.users.UserRepository;
import com.communex.volunteers.VolunteerRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final UserRepository users;
    private final VolunteerRepository volunteers;
    private final HelpRequestRepository requests;
    private final FacilityRepository facilities;
    private final ResourceRepository resources;
    private final CommunityEventRepository events;
    private final JdbcTemplate jdbc;

    // Constructor
    public AnalyticsController(
            UserRepository users,
            VolunteerRepository volunteers,
            HelpRequestRepository requests,
            FacilityRepository facilities,
            ResourceRepository resources,
            CommunityEventRepository events,
            JdbcTemplate jdbc
    ) {
        this.users = users;
        this.volunteers = volunteers;
        this.requests = requests;
        this.facilities = facilities;
        this.resources = resources;
        this.events = events;
        this.jdbc = jdbc;
    }

    // Retrieve application analytics
    @GetMapping("/overview")
    public Map<String, Object> overview() {

        Map<String, Object> analytics = new LinkedHashMap<>();

        // General statistics
        analytics.put("users", users.count());
        analytics.put("volunteers", volunteers.count());

        analytics.put(
                "openRequests",
                requests.countByStatus(HelpRequestStatus.OPEN)
        );

        analytics.put(
                "facilities",
                facilities.countByActiveTrue()
        );

        analytics.put(
                "resources",
                resources.countByActiveTrue()
        );

        analytics.put(
                "events",
                events.countByStatus("PUBLISHED")
        );

        // Retrieve the number of visible community posts
        try {
            Long postCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM community_posts WHERE hidden = FALSE",
                    Long.class
            );

            analytics.put("posts", postCount);

        } catch (Exception exception) {
            analytics.put("posts", null);
        }

        // Record when the analytics response was generated
        analytics.put("generatedAt", LocalDateTime.now());

        return analytics;
    }
}