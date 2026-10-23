
package com.communex.volunteers;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.communex.users.User;

public interface VolunteerRepository
        extends JpaRepository<Volunteer, Long> {

    // Existing methods retained.
    Optional<Volunteer> findByUser(User user);

    boolean existsByUser(User user);

    List<Volunteer> findByAvailableTrue();

    @EntityGraph(attributePaths = "user")
    List<Volunteer> findByAvailableTrueOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<Volunteer> findByAvailableTrueAndSkillsContainingIgnoreCaseOrderByCreatedAtDesc(
            String skills,
            Pageable pageable
    );

    long countByAvailableTrue();


    List<Volunteer> findByAvailableTrueAndSkillsContainingIgnoreCase(
            String skills
    );

    // Batch-load volunteer profiles with their associated users.
    @Query("""
        select v
        from Volunteer v
        join fetch v.user
        where v.user.id in :userIds
        """)
    List<Volunteer> findAllWithUserByUserIds(
            @Param("userIds")
            Collection<Long> userIds
    );
}
