package com.communex.documents;

import com.communex.documents.dto.DocumentRequest;
import com.communex.documents.dto.DocumentResponse;
import com.communex.users.User;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    // Constructor
    public DocumentController(DocumentService service) {
        this.service = service;
    }

    // Retrieve the authenticated user's email
    private String email(Authentication authentication) {

        if (authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }

        return authentication.getName();
    }

    // Retrieve documents belonging to the current user
    @GetMapping
    public List<DocumentResponse> list(
            Authentication authentication
    ) {
        return service.list(
                email(authentication)
        );
    }

    // Create a new document
    @PostMapping
    public DocumentResponse create(
            Authentication authentication,
            @Valid @RequestBody DocumentRequest request
    ) {
        return service.create(
                email(authentication),
                request
        );
    }

    // Update an existing document
    @PutMapping("/{id}")
    public DocumentResponse update(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody DocumentRequest request
    ) {
        return service.update(
                id,
                email(authentication),
                request
        );
    }

    // Delete a document
    @DeleteMapping("/{id}")
    public Map<String, String> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        service.delete(
                id,
                email(authentication)
        );

        return Map.of(
                "message",
                "Document deleted"
        );
    }
}