package com.communex.facilities;

import com.communex.facilities.dto.FacilityRequest;
import com.communex.facilities.dto.FacilityResponse;
import com.communex.users.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {

    private final FacilityService service;

    // Constructor
    public FacilityController(FacilityService service) {
        this.service = service;
    }

    // Retrieve the authenticated user's email
    private String email(Authentication authentication) {

        if (authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }

        return authentication.getName();
    }

    // Get facilities with optional search and category filters
    @GetMapping
    public List<FacilityResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category
    ) {
        return service.search(q, category);
    }

    // Create a new facility
    @PostMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN','FACILITY_MANAGER','ORGANIZATION_ADMIN')"
    )
    public FacilityResponse create(
            Authentication authentication,
            @Valid @RequestBody FacilityRequest request
    ) {
        return service.create(
                email(authentication),
                request
        );
    }

    // Update an existing facility
    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN','FACILITY_MANAGER','ORGANIZATION_ADMIN')"
    )
    public FacilityResponse update(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody FacilityRequest request
    ) {
        return service.update(
                id,
                email(authentication),
                request
        );
    }

    // Archive a facility
    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN','FACILITY_MANAGER','ORGANIZATION_ADMIN')"
    )
    public Map<String, String> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        service.deactivate(
                id,
                email(authentication)
        );

        return Map.of("message", "Facility archived");
    }
}
