
package com.communex.community;

import com.communex.community.dto.GroupDiscussionRequest;
import com.communex.community.dto.GroupDiscussionResponse;
import com.communex.users.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing discussions within community groups.
 *
 * Provides endpoints for retrieving, creating, and deleting
 * group discussions.
 */
@RestController
@RequestMapping("/api/groups/{groupId}/discussions")
public class GroupDiscussionController {

    private final GroupDiscussionService service;

    /**
     * Creates the controller with its required service.
     *
     * @param service service responsible for discussion operations
     */
    public GroupDiscussionController(GroupDiscussionService service) {
        this.service = service;
    }

    /**
     * Resolves the email address of the authenticated user.
     *
     * If the authentication principal is a User entity, its email
     * is returned. Otherwise, the authentication name is used.
     *
     * @param authentication current authentication information
     * @return authenticated user's email or authentication name
     */
    private String email(Authentication authentication) {
        if (authentication.getPrincipal() instanceof User user) {
            return user.getEmail();
        }

        return authentication.getName();
    }

    /**
     * Retrieves discussions for the specified group.
     *
     * GET /api/groups/{groupId}/discussions
     *
     * @param groupId group identifier
     * @param authentication current authenticated user
     * @return list of discussion responses
     */
    @GetMapping
    public List<GroupDiscussionResponse> list(
        @PathVariable Long groupId,
        Authentication authentication
    ) {
        return service.list(groupId, email(authentication));
    }

    /**
     * Creates a discussion in the specified group.
     *
     * POST /api/groups/{groupId}/discussions
     *
     * @param groupId group identifier
     * @param authentication current authenticated user
     * @param request validated discussion creation request
     * @return created discussion response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDiscussionResponse create(
        @PathVariable Long groupId,
        Authentication authentication,
        @Valid @RequestBody GroupDiscussionRequest request
    ) {
        return service.create(
            groupId,
            email(authentication),
            request
        );
    }

    /**
     * Deletes the specified discussion.
     *
     * DELETE /api/groups/{groupId}/discussions/{discussionId}
     *
     * @param groupId group identifier from the request path
     * @param discussionId discussion identifier
     * @param authentication current authenticated user
     * @return confirmation message
     */
    @DeleteMapping("/{discussionId}")
    public Map<String, String> delete(
        @PathVariable Long groupId,
        @PathVariable Long discussionId,
        Authentication authentication
    ) {
        service.delete(discussionId, email(authentication));

        return Map.of("message", "Discussion deleted");
    }
}
