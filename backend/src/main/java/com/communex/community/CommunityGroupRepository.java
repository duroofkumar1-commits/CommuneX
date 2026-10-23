package com.communex.community;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommunityGroupRepository extends JpaRepository<CommunityGroup, Long> {

    /**
     * Lists groups that users can discover. Private groups are discoverable by name
     * and description, but membership approval is still required to read/send chat
     * messages; that access is enforced in GroupService.requireCanChat().
     */
    @EntityGraph(attributePaths = "owner")
    @Query("select g from CommunityGroup g order by g.createdAt desc")
    List<CommunityGroup> findAllForDiscovery();
}
