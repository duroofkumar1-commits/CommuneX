package com.communex.groups.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.communex.groups.model.GroupMembership;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    List<GroupMembership> findByGroupId(Long groupId);
    List<GroupMembership> findByGroupIdIn(Collection<Long> groupIds);
    List<GroupMembership> findByGroupIdAndStatus(Long groupId, String status);
    Optional<GroupMembership> findByGroupIdAndUserId(Long groupId, Long userId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
}
