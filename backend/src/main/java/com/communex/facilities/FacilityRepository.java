
package com.communex.facilities;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacilityRepository
        extends JpaRepository<Facility, Long> {

    @Query(
        value = """
            SELECT *
            FROM facilities f
            WHERE f.active = true
              AND (
                  :q IS NULL
                  OR f.name ILIKE '%' || :q || '%'
                  OR COALESCE(f.address, '') ILIKE '%' || :q || '%'
                  OR COALESCE(f.description, '') ILIKE '%' || :q || '%'
              )
              AND (
                  :category IS NULL
                  OR f.category ILIKE :category
              )
            ORDER BY f.verified DESC, f.name ASC
            """,
        nativeQuery = true
    )
    List<Facility> search(
            @Param("q") String q,
            @Param("category") String category
    );


    @Query(
        value = """
            SELECT *
            FROM facilities f
            WHERE f.active = TRUE
              AND (
                  f.name ILIKE '%' || :q || '%'
                  OR COALESCE(f.address, '') ILIKE '%' || :q || '%'
                  OR COALESCE(f.description, '') ILIKE '%' || :q || '%'
              )
            ORDER BY f.verified DESC, f.name ASC
            """,
        nativeQuery = true
    )
    List<Facility> searchForGlobal(
            @Param("q") String q,
            Pageable pageable
    );

    long countByActiveTrue();

    // Fetch only active facilities that can be plotted on the map.
    @Query("""
        select f
        from Facility f
        where f.active = true
          and f.latitude is not null
          and f.longitude is not null
        """)
    List<Facility> findActiveForMap();
}
