package com.communex.requests;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HelpRequestUpdateRepository extends JpaRepository<HelpRequestUpdate, Long> {
    // Load the actor (the user who made the change) in the same query to avoid N+1 selects.
    @EntityGraph(attributePaths = {"actor"})
    List<HelpRequestUpdate> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}
