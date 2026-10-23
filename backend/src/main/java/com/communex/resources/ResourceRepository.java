
package com.communex.resources;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceRepository
        extends JpaRepository<ResourceItem, Long> {

    @Query("""
        select r
        from ResourceItem r
        where r.active = true
          and (
              lower(r.name) like lower(concat('%', :q, '%'))
              or lower(coalesce(r.location, ''))
                    like lower(concat('%', :q, '%'))
              or lower(coalesce(r.description, ''))
                    like lower(concat('%', :q, '%'))
          )
          and (
              :lowOnly = false
              or r.quantity <= r.lowStockThreshold
          )
        order by r.name asc
        """)
    List<ResourceItem> search(
            @Param("q") String q,
            @Param("lowOnly") boolean lowOnly
    );


    @Query(
        value = """
            SELECT r.*
            FROM resource_items r
            WHERE r.active = TRUE
              AND (
                  r.name ILIKE '%' || :q || '%'
                  OR COALESCE(r.location, '') ILIKE '%' || :q || '%'
                  OR COALESCE(r.description, '') ILIKE '%' || :q || '%'
              )
            ORDER BY r.name ASC
            """,
        nativeQuery = true
    )
    List<ResourceItem> searchForGlobal(
            @Param("q") String q,
            Pageable pageable
    );

    long countByActiveTrue();

    // Fetch active resources for the community map.
    List<ResourceItem> findByActiveTrue();
}
