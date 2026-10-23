package com.communex.resources;

import com.communex.resources.dto.QuantityAdjustmentRequest;
import com.communex.resources.dto.ResourceRequest;
import com.communex.resources.dto.ResourceResponse;
import com.communex.users.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService service;

    // Constructor
    public ResourceController(ResourceService service) {
        this.service = service;
    }

    // Retrieve the authenticated user's email
    private String email(Authentication authentication) {

        if (authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }

        return authentication.getName();
    }

    // Get resources with optional search and low-stock filters
    @GetMapping
    public List<ResourceResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean lowStock
    ) {
        return service.search(q, lowStock);
    }

    // Create a new resource
    @PostMapping
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'VOLUNTEER', 'FACILITY_MANAGER', "
                    + "'ORGANIZER', 'ORGANIZATION_ADMIN')"
    )
    public ResourceResponse create(
            Authentication authentication,
            @Valid @RequestBody ResourceRequest request
    ) {
        return service.create(
                email(authentication),
                request
        );
    }

    // Adjust the quantity of an existing resource
    @PatchMapping("/{id}/quantity")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'VOLUNTEER', 'FACILITY_MANAGER', "
                    + "'ORGANIZER', 'ORGANIZATION_ADMIN')"
    )
    public ResourceResponse adjust(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody QuantityAdjustmentRequest request
    ) {
        return service.adjust(
                id,
                email(authentication),
                request
        );
    }

    // Retrieve the transaction history of a resource
    @GetMapping("/{id}/history")
    public List<Map<String, Object>> history(
            @PathVariable Long id
    ) {
        return service.history(id);
    }
}