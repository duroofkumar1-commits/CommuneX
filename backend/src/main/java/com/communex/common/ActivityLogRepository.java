package com.communex.common;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findTop100ByUserEmailOrderByCreatedAtDesc(String email);
    List<ActivityLog> findTop200ByOrderByCreatedAtDesc();
}
