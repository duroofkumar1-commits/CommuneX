package com.communex.community;

import com.communex.common.ActivityLogService;
import com.communex.community.dto.GroupDiscussionRequest;
import com.communex.community.dto.GroupDiscussionResponse;
import com.communex.groups.repository.GroupMembershipRepository; // <-- Yeh import add karna zaroori hai
import com.communex.users.Role;
import com.communex.users.User;
import com.communex.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Objects;

@Service
public class GroupDiscussionService {
    private final GroupDiscussionRepository discussions;
    private final CommunityGroupRepository groups;
    private final GroupMembershipRepository memberships;
    private final UserRepository users;
    private final ActivityLogService activity;

    public GroupDiscussionService(GroupDiscussionRepository discussions, CommunityGroupRepository groups, GroupMembershipRepository memberships, UserRepository users, ActivityLogService activity) {
        this.discussions = discussions;
        this.groups = groups;
        this.memberships = memberships;
        this.users = users;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<GroupDiscussionResponse> list(Long groupId, String email) {
        CommunityGroup group = getGroup(groupId);
        User actor = getUser(email);
        requireView(group, actor);
        return discussions.findTop100ByGroupIdOrderByCreatedAtDesc(groupId).stream().map(GroupDiscussionResponse::from).toList();
    }

    @Transactional
    public GroupDiscussionResponse create(Long groupId, String email, GroupDiscussionRequest request) {
        CommunityGroup group = getGroup(groupId);
        User actor = getUser(email);
        requireMember(group, actor);
        GroupDiscussion d = new GroupDiscussion();
        d.setGroup(group);
        d.setAuthor(actor);
        d.setContent(request.content().trim());
        d = discussions.save(d);
        activity.record(email, "GROUP_DISCUSSION_CREATED", "GROUP", groupId, "Created group discussion");
        return GroupDiscussionResponse.from(d);
    }

    @Transactional
    public void delete(Long id, String email) {
        GroupDiscussion d = discussions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discussion not found"));
        User actor = getUser(email);
        if (!d.getAuthor().getId().equals(actor.getId()) && !Objects.equals(d.getGroup().getOwner().getId(), actor.getId()) && actor.getRole() != Role.ADMIN && actor.getRole() != Role.MODERATOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this discussion");
        }
        discussions.delete(d);
        activity.record(email, "GROUP_DISCUSSION_DELETED", "GROUP_DISCUSSION", id, "Deleted discussion");
    }

    private void requireView(CommunityGroup g, User u) {
        if ("PRIVATE".equals(g.getVisibility()) && !g.getOwner().getId().equals(u.getId()) && !memberships.existsByGroupIdAndUserId(g.getId(), u.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This group is private");
        }
    }

    private void requireMember(CommunityGroup g, User u) {
        if (!g.getOwner().getId().equals(u.getId()) && !memberships.existsByGroupIdAndUserId(g.getId(), u.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Join the group before posting");
        }
    }

    private CommunityGroup getGroup(Long id) {
        return groups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private User getUser(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}