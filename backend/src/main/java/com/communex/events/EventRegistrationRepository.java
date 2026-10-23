
package com.communex.events;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository
        extends JpaRepository<EventRegistration, Long> {

    Optional<EventRegistration> findByEventIdAndUserId(
            Long eventId,
            Long userId
    );

    long countByEventId(Long eventId);

    void deleteByEventIdAndUserId(
            Long eventId,
            Long userId
    );

    boolean existsByEventIdAndUserId(
            Long eventId,
            Long userId
    );

    @Query("""
        select r.event.id as eventId,
               count(r.id) as registrationCount
        from EventRegistration r
        where r.event.id in :eventIds
        group by r.event.id
        """)
    List<RegistrationCountProjection> countRegistrationsByEventIds(
            @Param("eventIds") Collection<Long> eventIds
    );

    @Query("""
        select r.event.id
        from EventRegistration r
        where r.user.id = :userId
          and r.event.id in :eventIds
        """)
    List<Long> findRegisteredEventIdsByUserIdAndEventIds(
            @Param("userId") Long userId,
            @Param("eventIds") Collection<Long> eventIds
    );
}
