package com.communex.groups.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.communex.community.CommunityGroup;
import com.communex.community.CommunityGroupRepository;
import com.communex.groups.dto.GroupMessageRequest;
import com.communex.groups.dto.GroupMessageResponse;
import com.communex.groups.dto.GroupMessageResponse.ReactionSummary;
import com.communex.groups.dto.GroupResponse;
import com.communex.groups.dto.MembershipRequestResponse;
import com.communex.groups.model.GroupMembership;
import com.communex.groups.model.GroupMessage;
import com.communex.groups.model.MessageReaction;
import com.communex.groups.repository.GroupMembershipRepository;
import com.communex.groups.repository.GroupMessageRepository;
import com.communex.groups.repository.MessageReactionRepository;
import com.communex.users.Role;
import com.communex.users.User;
import com.communex.users.UserRepository;

@Service
public class GroupService {
    private static final int MAX_MESSAGE_PAGE_SIZE = 100;

    private final GroupMessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final GroupMembershipRepository membershipRepository;
    private final CommunityGroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupMessageRepository messageRepository,
                        MessageReactionRepository reactionRepository,
                        GroupMembershipRepository membershipRepository,
                        CommunityGroupRepository groupRepository,
                        UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.membershipRepository = membershipRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getVisibleGroups(String email) {
        User viewer = getUserByEmail(email);
        List<CommunityGroup> groups = groupRepository.findAllForDiscovery();
        if (groups.isEmpty()) return List.of();

        List<Long> groupIds = groups.stream().map(CommunityGroup::getId).toList();
        Map<Long, List<GroupMembership>> membershipsByGroup = membershipRepository.findByGroupIdIn(groupIds)
                .stream().collect(Collectors.groupingBy(GroupMembership::getGroupId));

        return groups.stream()
                .map(group -> toGroupResponse(group, viewer,
                        membershipsByGroup.getOrDefault(group.getId(), List.of())))
                .toList();
    }

    @Transactional
    public GroupResponse createGroup(String email, String name, String description, String visibility) {
        User owner = getUserByEmail(email);
        String normalizedVisibility = visibility == null || visibility.isBlank()
                ? "PUBLIC" : visibility.trim().toUpperCase();
        if (!normalizedVisibility.equals("PUBLIC") && !normalizedVisibility.equals("PRIVATE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Visibility must be PUBLIC or PRIVATE.");
        }

        CommunityGroup group = new CommunityGroup();
        group.setName(name.trim());
        group.setDescription(description.trim());
        group.setVisibility(normalizedVisibility);
        group.setOwner(owner);
        CommunityGroup saved = groupRepository.saveAndFlush(group);
        return toGroupResponse(saved, owner, List.of());
    }

    private GroupResponse toGroupResponse(CommunityGroup group, User viewer, List<GroupMembership> memberships) {
        GroupMembership currentMembership = memberships.stream()
                .filter(m -> Objects.equals(m.getUserId(), viewer.getId()))
                .findFirst().orElse(null);
        boolean owner = Objects.equals(group.getOwner().getId(), viewer.getId());
        long approvedMembers = memberships.stream()
                .filter(m -> "APPROVED".equalsIgnoreCase(m.getStatus())).count();
        boolean ownerHasApprovedRow = memberships.stream().anyMatch(m ->
                Objects.equals(m.getUserId(), group.getOwner().getId())
                        && "APPROVED".equalsIgnoreCase(m.getStatus()));
        long memberCount = approvedMembers + (ownerHasApprovedRow ? 0 : 1);
        long pendingRequests = memberships.stream()
                .filter(m -> "PENDING".equalsIgnoreCase(m.getStatus())).count();
        boolean approved = currentMembership != null
                && "APPROVED".equalsIgnoreCase(currentMembership.getStatus());

        return new GroupResponse(
                group.getId(), group.getName(), group.getDescription(), group.getVisibility(),
                group.getOwner().getId(), group.getOwner().getName(), group.getCreatedAt(),
                memberCount, pendingRequests, owner, owner || approved,
                currentMembership == null ? null : currentMembership.getStatus());
    }

    @Transactional
    public GroupMembership joinGroup(Long groupId, String email) {
        CommunityGroup group = getGroup(groupId);
        User user = getUserByEmail(email);
        if (Objects.equals(group.getOwner().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already own this group.");
        }

        var existing = membershipRepository.findByGroupIdAndUserId(groupId, user.getId());
        if (existing.isPresent()) return existing.get();

        String status = "PUBLIC".equalsIgnoreCase(group.getVisibility()) ? "APPROVED" : "PENDING";
        return membershipRepository.save(new GroupMembership(groupId, user.getId(), "MEMBER", status));
    }

    @Transactional
    public void leaveGroup(Long groupId, String email) {
        CommunityGroup group = getGroup(groupId);
        User user = getUserByEmail(email);
        if (Objects.equals(group.getOwner().getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group owner cannot leave their own group.");
        }
        GroupMembership membership = membershipRepository.findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));
        membershipRepository.delete(membership);
    }

    @Transactional
    public GroupMessageResponse sendMessage(Long groupId, String email, GroupMessageRequest request) {
        User user = getUserByEmail(email);
        CommunityGroup group = getGroup(groupId);
        requireCanChat(group, user);

        String content = request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required.");
        }

        if (request.parentMessageId() != null) {
            GroupMessage parent = messageRepository.findById(request.parentMessageId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reply target not found"));
            if (!Objects.equals(parent.getGroupId(), groupId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reply target belongs to a different group.");
            }
        }

        GroupMessage message = new GroupMessage();
        message.setGroupId(groupId);
        message.setSenderId(user.getId());
        message.setContent(content);
        message.setParentMessageId(request.parentMessageId());
        GroupMessage saved = messageRepository.saveAndFlush(message);
        return new GroupMessageResponse(saved.getId(), saved.getGroupId(), saved.getSenderId(), user.getName(),
                saved.getContent(), saved.getParentMessageId(), saved.getCreatedAt(), List.of());
    }

    @Transactional(readOnly = true)
    public List<GroupMessageResponse> getGroupMessages(Long groupId, String email,
                                                        Long afterId, Long beforeId, int requestedLimit) {
        if (afterId != null && beforeId != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use afterId or beforeId, not both.");
        }
        User viewer = getUserByEmail(email);
        requireCanChat(getGroup(groupId), viewer);
        int limit = Math.max(1, Math.min(requestedLimit, MAX_MESSAGE_PAGE_SIZE));
        List<GroupMessage> found;

        if (afterId != null) {
            found = messageRepository.findByGroupIdAndIdGreaterThanOrderByIdAsc(
                    groupId, afterId, PageRequest.of(0, limit));
        } else if (beforeId != null) {
            found = new ArrayList<>(messageRepository.findByGroupIdAndIdLessThanOrderByIdDesc(
                    groupId, beforeId, PageRequest.of(0, limit)));
            found.sort(Comparator.comparing(GroupMessage::getId));
        } else {
            found = new ArrayList<>(messageRepository.findByGroupIdOrderByIdDesc(
                    groupId, PageRequest.of(0, limit)));
            found.sort(Comparator.comparing(GroupMessage::getId));
        }
        return toMessageResponses(found, viewer.getId());
    }

    @Transactional
    public MessageReaction addReaction(Long messageId, String email, String emoji) {
        User user = getUserByEmail(email);
        GroupMessage message = getMessage(messageId);
        requireCanChat(getGroup(message.getGroupId()), user);
        String normalizedEmoji = normalizeEmoji(emoji);
        return reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, user.getId(), normalizedEmoji)
                .orElseGet(() -> reactionRepository.save(new MessageReaction(messageId, user.getId(), normalizedEmoji)));
    }

    @Transactional
    public void removeReaction(Long messageId, String email, String emoji) {
        User user = getUserByEmail(email);
        GroupMessage message = getMessage(messageId);
        requireCanChat(getGroup(message.getGroupId()), user);
        String normalizedEmoji = normalizeEmoji(emoji);
        reactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, user.getId(), normalizedEmoji)
                .ifPresent(reactionRepository::delete);
    }

    private String normalizeEmoji(String emoji) {
        String value = emoji == null ? "" : emoji.trim();
        if (value.isEmpty() || value.length() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reaction emoji is invalid.");
        }
        return value;
    }

    @Transactional
    public GroupMembership requestToJoin(Long groupId, String email) {
        return joinGroup(groupId, email);
    }

    @Transactional(readOnly = true)
    public List<MembershipRequestResponse> getPendingJoinRequests(Long groupId, String email) {
        User actor = getUserByEmail(email);
        CommunityGroup group = getGroup(groupId);
        requireOwnerOrAdmin(group, actor);
        return membershipRepository.findByGroupIdAndStatus(groupId, "PENDING").stream()
                .map(membership -> {
                    User requester = userRepository.findById(membership.getUserId()).orElse(null);
                    return new MembershipRequestResponse(membership.getId(), membership.getGroupId(),
                            membership.getUserId(), requester == null ? "Community member" : requester.getName(),
                            requester == null ? "" : requester.getEmail(), membership.getCreatedAt(), membership.getStatus());
                })
                .toList();
    }

    @Transactional
    public GroupMembership approveMembership(Long membershipId, User actor) {
        GroupMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership request not found"));
        CommunityGroup group = getGroup(membership.getGroupId());
        requireOwnerOrAdmin(group, actor);
        if (!"PENDING".equalsIgnoreCase(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is no longer pending.");
        }
        membership.setStatus("APPROVED");
        return membershipRepository.save(membership);
    }

    private void requireOwnerOrAdmin(CommunityGroup group, User actor) {
        boolean isOwner = Objects.equals(group.getOwner().getId(), actor.getId());
        boolean isAdmin = actor.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the group owner or an administrator can perform this action.");
        }
    }

    private void requireCanChat(CommunityGroup group, User user) {
        if (Objects.equals(group.getOwner().getId(), user.getId())) return;
        GroupMembership membership = membershipRepository.findByGroupIdAndUserId(group.getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Join this group before opening its chat."));
        if (!"APPROVED".equalsIgnoreCase(membership.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your group join request is still pending approval.");
        }
    }

    private CommunityGroup getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private GroupMessage getMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    }

    private List<GroupMessageResponse> toMessageResponses(List<GroupMessage> messages, Long currentUserId) {
        if (messages.isEmpty()) return List.of();
        List<Long> messageIds = messages.stream().map(GroupMessage::getId).toList();
        List<Long> senderIds = messages.stream().map(GroupMessage::getSenderId).distinct().toList();
        Map<Long, String> namesByUser = new HashMap<>();
        userRepository.findAllById(senderIds).forEach(user -> namesByUser.put(user.getId(), user.getName()));

        Map<Long, Map<String, List<MessageReaction>>> reactionsByMessage = new HashMap<>();
        reactionRepository.findByMessageIdIn(messageIds).forEach(reaction ->
                reactionsByMessage.computeIfAbsent(reaction.getMessageId(), ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(reaction.getEmoji(), ignored -> new ArrayList<>()).add(reaction));

        return messages.stream().map(message -> {
            Map<String, List<MessageReaction>> byEmoji = reactionsByMessage.getOrDefault(message.getId(), Map.of());
            List<ReactionSummary> summaries = byEmoji.entrySet().stream()
                    .map(entry -> new ReactionSummary(entry.getKey(), entry.getValue().size(),
                            entry.getValue().stream().anyMatch(r -> Objects.equals(r.getUserId(), currentUserId))))
                    .toList();
            return new GroupMessageResponse(message.getId(), message.getGroupId(), message.getSenderId(),
                    namesByUser.getOrDefault(message.getSenderId(), "Community member"), message.getContent(),
                    message.getParentMessageId(), message.getCreatedAt(), summaries);
        }).toList();
    }
}
