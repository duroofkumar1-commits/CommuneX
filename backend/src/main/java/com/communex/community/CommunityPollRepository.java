
package com.communex.community;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommunityPollRepository
        extends JpaRepository<CommunityPoll, Long> {

    // Existing method retained for compatibility with other code.
    List<CommunityPoll> findAllByOrderByCreatedAtDesc();

    // Load poll creators together with the polls.
    @Query("""
        select p
        from CommunityPoll p
        join fetch p.createdBy
        order by p.createdAt desc
        """)
    List<CommunityPoll> findAllWithCreatorOrderByCreatedAtDesc();
}
