
package com.communex.map;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MapLocationRepository
        extends JpaRepository<MapLocation, Long> {

    // Fetch all pins for one entity type.
    List<MapLocation> findByEntityType(String entityType);

    // Fetch pins for multiple entity types in one query.
    List<MapLocation> findByEntityTypeIn(
            Collection<String> entityTypes
    );

    // Find a specific pin for an entity.
    Optional<MapLocation> findByEntityTypeAndEntityId(
            String entityType,
            Long entityId
    );
}
