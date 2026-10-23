
package com.communex.events;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityEventRepository
        extends JpaRepository<CommunityEvent, Long> {

    // Existing methods retained.
    @EntityGraph(attributePaths = "organizer")
    List<CommunityEvent> findByStatusOrderByStartsAtAsc(
            String status
    );


    @Query(
        value = """
            SELECT e.*
            FROM community_events e
            WHERE e.status = 'PUBLISHED'
              AND (
                  e.title ILIKE '%' || :q || '%'
                  OR COALESCE(e.description, '') ILIKE '%' || :q || '%'
                  OR COALESCE(e.location, '') ILIKE '%' || :q || '%'
              )
            ORDER BY e.starts_at ASC, e.id ASC
            """,
        nativeQuery = true
    )
    List<CommunityEvent> searchPublishedForGlobal(
            @Param("q") String q,
            Pageable pageable
    );

    long countByStatus(String status);

    // Fetch only published events within the map's time window.
    @EntityGraph(attributePaths = "organizer")
    @Query("""
        select e
        from CommunityEvent e
        where e.status = :status
          and e.startsAt >= :cutoff
        order by e.startsAt asc
        """)
    List<CommunityEvent> findMapEventsSince(
            @Param("status") String status,
            @Param("cutoff") LocalDateTime cutoff
    );
}
