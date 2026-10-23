package com.communex.map;

import com.communex.map.MapDtos.LiveLocationUpdate;
import com.communex.map.MapDtos.LocationUpdate;
import com.communex.map.MapDtos.Overview;
import com.communex.users.User;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/map")
@PreAuthorize("isAuthenticated()")
public class MapController {

    private final MapService service;
    private final DsaMapRoutingService routingService;

    public MapController(
            MapService service,
            DsaMapRoutingService routingService
    ) {
        this.service = service;
        this.routingService = routingService;
    }

    private String email(Authentication authentication) {
        return authentication.getPrincipal() instanceof User user
                ? user.getEmail()
                : authentication.getName();
    }

    @GetMapping("/overview")
    public Overview overview(Authentication authentication) {
        return service.overview(email(authentication));
    }

    /** Road-network data is queried from OSM; shortest-path calculation is local DSA. */
    @GetMapping("/route")
    public DsaMapRoutingService.RouteResponse route(
            @RequestParam double fromLat,
            @RequestParam double fromLng,
            @RequestParam double toLat,
            @RequestParam double toLng
    ) {
        return routingService.route(fromLat, fromLng, toLat, toLng);
    }

    /** Server-side proxy for bounded POI/building queries. */
    @PostMapping("/overpass")
    public ResponseEntity<?> overpass(
            @RequestBody MapOverpassRequest request
    ) {
        try {
            JsonNode result = routingService.fetchMapData(request);
            return ResponseEntity.ok(result);
        } catch (ResponseStatusException exception) {
            String message = exception.getReason() == null
                    ? "Map data request failed."
                    : exception.getReason();

            return ResponseEntity.status(exception.getStatusCode())
                    .body(Map.of("message", message));
        }
    }

    @PutMapping("/locations/{type}/{id}")
    public Map<String, String> pin(
            Authentication authentication,
            @PathVariable String type,
            @PathVariable Long id,
            @Valid @RequestBody LocationUpdate body
    ) {
        service.pin(email(authentication), type, id, body);
        return Map.of("message", "Location saved");
    }

    @DeleteMapping("/locations/{type}/{id}")
    public Map<String, String> unpin(
            Authentication authentication,
            @PathVariable String type,
            @PathVariable Long id
    ) {
        service.unpin(email(authentication), type, id);
        return Map.of("message", "Location removed");
    }

    @PutMapping("/me/location")
    public Map<String, String> share(
            Authentication authentication,
            @Valid @RequestBody LiveLocationUpdate body
    ) {
        service.shareLiveLocation(email(authentication), body);
        return Map.of("message", "Location shared");
    }

    @DeleteMapping("/me/location")
    public Map<String, String> stop(Authentication authentication) {
        service.stopSharing(email(authentication));
        return Map.of("message", "Location sharing stopped");
    }
}
