package com.communex.groups.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.communex.community.dto.GroupRequest;
import com.communex.groups.dto.GroupMessageRequest;
import com.communex.groups.dto.GroupMessageResponse;
import com.communex.groups.dto.GroupResponse;
import com.communex.groups.dto.MembershipRequestResponse;
import com.communex.groups.model.GroupMembership;
import com.communex.groups.model.MessageReaction;
import com.communex.groups.service.GroupService;
import com.communex.users.User;
import com.communex.users.UserRepository;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groupService;
    private final UserRepository userRepository;

    public GroupController(GroupService groupService, UserRepository userRepository) {
        this.groupService = groupService;
        this.userRepository = userRepository;
    }

    private String getEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authentication.getPrincipal() instanceof User user ? user.getEmail() : authentication.getName();
    }

    private User getUser(Authentication authentication) {
        String email = getEmail(authentication);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getAllGroups(Authentication authentication) {
        return ResponseEntity.ok(groupService.getVisibleGroups(getEmail(authentication)));
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request,
                                                      Authentication authentication) {
        String visibility = request.visibility() == null ? "PUBLIC" : request.visibility();
        GroupResponse saved = groupService.createGroup(getEmail(authentication),
                request.name(), request.description(), visibility);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<GroupMembership> joinGroup(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(groupService.joinGroup(groupId, getEmail(authentication)));
    }

    @DeleteMapping("/{groupId}/membership")
    public ResponseEntity<Map<String, String>> leaveGroup(@PathVariable Long groupId, Authentication authentication) {
        groupService.leaveGroup(groupId, getEmail(authentication));
        return ResponseEntity.ok(Map.of("message", "Successfully left group"));
    }

    @PostMapping("/{groupId}/messages")
    public ResponseEntity<GroupMessageResponse> sendMessage(@PathVariable Long groupId,
                                                             @Valid @RequestBody GroupMessageRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.sendMessage(groupId, getEmail(authentication), request));
    }

    @GetMapping("/{groupId}/messages")
    public ResponseEntity<List<GroupMessageResponse>> getGroupMessages(
            @PathVariable Long groupId,
            @RequestParam(required = false) Long afterId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "60") int limit,
            Authentication authentication) {
        return ResponseEntity.ok(groupService.getGroupMessages(
                groupId, getEmail(authentication), afterId, beforeId, limit));
    }

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<MessageReaction> addReaction(@PathVariable Long messageId,
                                                        @RequestParam String emoji,
                                                        Authentication authentication) {
        return ResponseEntity.ok(groupService.addReaction(messageId, getEmail(authentication), emoji));
    }

    @DeleteMapping("/messages/{messageId}/reactions")
    public ResponseEntity<Map<String, String>> removeReaction(@PathVariable Long messageId,
                                                               @RequestParam String emoji,
                                                               Authentication authentication) {
        groupService.removeReaction(messageId, getEmail(authentication), emoji);
        return ResponseEntity.ok(Map.of("message", "Reaction removed"));
    }

    @PostMapping("/{groupId}/join-request")
    public ResponseEntity<GroupMembership> requestToJoin(@PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(groupService.requestToJoin(groupId, getEmail(authentication)));
    }

    @GetMapping("/{groupId}/join-requests")
    public ResponseEntity<List<MembershipRequestResponse>> getPendingJoinRequests(
            @PathVariable Long groupId, Authentication authentication) {
        return ResponseEntity.ok(groupService.getPendingJoinRequests(groupId, getEmail(authentication)));
    }

    @PutMapping("/memberships/{membershipId}/approve")
    public ResponseEntity<GroupMembership> approveMembership(@PathVariable Long membershipId,
                                                              Authentication authentication) {
        return ResponseEntity.ok(groupService.approveMembership(membershipId, getUser(authentication)));
    }
}
