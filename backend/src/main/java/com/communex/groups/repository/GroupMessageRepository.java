package com.communex.groups.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.communex.groups.model.GroupMessage;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    List<GroupMessage> findByGroupIdOrderByCreatedAtAsc(Long groupId);

    List<GroupMessage> findByGroupIdOrderByIdDesc(Long groupId, Pageable pageable);

    List<GroupMessage> findByGroupIdAndIdGreaterThanOrderByIdAsc(Long groupId, Long afterId, Pageable pageable);

    List<GroupMessage> findByGroupIdAndIdLessThanOrderByIdDesc(Long groupId, Long beforeId, Pageable pageable);
}
