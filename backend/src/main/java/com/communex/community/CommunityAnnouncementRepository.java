package com.communex.community;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityAnnouncementRepository extends JpaRepository<CommunityAnnouncement,Long> {
    @EntityGraph(attributePaths = "createdBy")
    List<CommunityAnnouncement> findTop100ByPublishedTrueOrderByCreatedAtDesc();
}
