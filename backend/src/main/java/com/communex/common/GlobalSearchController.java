package com.communex.common;

import com.communex.events.CommunityEventRepository;
import com.communex.facilities.Facility;
import com.communex.facilities.FacilityRepository;
import com.communex.resources.ResourceItem;
import com.communex.resources.ResourceRepository;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@Validated
public class GlobalSearchController {
    private static final int RESULTS_PER_MODULE = 8;

    private final FacilityRepository facilities;
    private final ResourceRepository resources;
    private final CommunityEventRepository events;
    private final JdbcTemplate jdbc;

    public GlobalSearchController(
            FacilityRepository facilities,
            ResourceRepository resources,
            CommunityEventRepository events,
            JdbcTemplate jdbc) {
        this.facilities = facilities;
        this.resources = resources;
        this.events = events;
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<Map<String, Object>> search(
            @RequestParam @Size(min = 1, max = 100) String q) {
        String query = q == null ? "" : q.trim();
        if (query.length() < 2) return List.of();

        var firstPage = PageRequest.of(0, RESULTS_PER_MODULE);
        List<Map<String, Object>> results = new ArrayList<>(32);

        for (Facility facility : facilities.searchForGlobal(query, firstPage)) {
            results.add(item(
                    "FACILITY", facility.getId(), facility.getName(),
                    facility.getDescription() == null ? facility.getAddress() : facility.getDescription(),
                    "/facilities"));
        }

        for (ResourceItem resource : resources.searchForGlobal(query, firstPage)) {
            results.add(item(
                    "RESOURCE", resource.getId(), resource.getName(),
                    resource.getDescription() == null ? resource.getLocation() : resource.getDescription(),
                    "/resources"));
        }

        for (var event : events.searchPublishedForGlobal(query, firstPage)) {
            results.add(item("EVENT", event.getId(), event.getTitle(), event.getDescription(), "/events"));
        }

        try {
            List<Map<String, Object>> posts = jdbc.query(
                    "SELECT p.id, LEFT(p.content, 250) AS description, u.name AS author " +
                    "FROM community_posts p JOIN users u ON u.id = p.author_id " +
                    "WHERE p.hidden = FALSE AND p.content ILIKE ? " +
                    "ORDER BY p.created_at DESC LIMIT ?",
                    (rs, row) -> item(
                            "POST", rs.getLong("id"),
                            "Community post by " + rs.getString("author"),
                            rs.getString("description"), "/community"),
                    "%" + query + "%", RESULTS_PER_MODULE);
            results.addAll(posts);
        } catch (Exception ignored) {
            // A fresh database may not have installed the community-post module yet.
        }

        return results.stream().limit(30).toList();
    }

    private Map<String, Object> item(
            String type, Long id, String title, String description, String href) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("id", id);
        result.put("title", title);
        result.put("description", description == null ? "" : description);
        result.put("href", href);
        return result;
    }
}
