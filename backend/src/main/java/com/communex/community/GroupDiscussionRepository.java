package com.communex.community;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupDiscussionRepository extends JpaRepository<GroupDiscussion, Long> {
    List<GroupDiscussion> findTop100ByGroupIdOrderByCreatedAtDesc(Long groupId);
}
