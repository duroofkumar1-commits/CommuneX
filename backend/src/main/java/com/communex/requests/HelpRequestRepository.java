
package com.communex.requests;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HelpRequestRepository
        extends JpaRepository<HelpRequest, Long> {

    // Existing methods retained.
    List<HelpRequest> findByStatusOrderByCreatedAtDesc(
            HelpRequestStatus status
    );

    List<HelpRequest> findByCreatedByEmailOrderByCreatedAtDesc(
            String email
    );


    // ---- Fast list queries -------------------------------------------------
    // The entity graph tells Hibernate to JOIN the requester and the volunteer in the
    // SAME query. Without it, every card in the list triggered extra SELECTs (the
    // "N+1 problem"), and each SELECT is a full network round trip to Neon.
    // Pageable keeps the result size bounded.
    @EntityGraph(attributePaths = {"createdBy", "assignedVolunteer"})
    List<HelpRequest> findByStatusOrderByCreatedAtDesc(
            HelpRequestStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"createdBy", "assignedVolunteer"})
    List<HelpRequest> findByCreatedByEmailOrderByCreatedAtDesc(
            String email,
            Pageable pageable
    );

    // Single request with both users loaded (used by accept/start/complete/cancel/history).
    @EntityGraph(attributePaths = {"createdBy", "assignedVolunteer"})
    Optional<HelpRequest> findWithUsersById(Long id);

    @Query("select count(r) from HelpRequest r where r.createdBy.email = :email")
    long countForUser(@Param("email") String email);

    long countByStatus(HelpRequestStatus status);

    // Load map requests and related users together.
    @EntityGraph(attributePaths = {
            "createdBy",
            "assignedVolunteer"
    })
    @Query("""
        select r
        from HelpRequest r
        where r.status in :statuses
        """)
    List<HelpRequest> findForMapByStatuses(
            @Param("statuses")
            Collection<HelpRequestStatus> statuses
    );
}
