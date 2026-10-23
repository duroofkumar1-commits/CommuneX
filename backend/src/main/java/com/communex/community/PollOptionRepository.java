
package com.communex.community;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollOptionRepository
        extends JpaRepository<PollOption, Long> {

    List<PollOption> findByPollIdOrderByIdAsc(Long pollId);

    Optional<PollOption> findByIdAndPollId(
            Long id,
            Long pollId
    );

    // Fetch options for all requested polls in one query.
    @Query("""
        select o
        from PollOption o
        where o.poll.id in :pollIds
        order by o.poll.id asc, o.id asc
        """)
    List<PollOption> findAllForPollIds(
            @Param("pollIds") Collection<Long> pollIds
    );
}
