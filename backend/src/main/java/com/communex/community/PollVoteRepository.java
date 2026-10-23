
package com.communex.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PollVoteRepository
        extends JpaRepository<PollVote, Long> {

    boolean existsByPollIdAndUserId(
            Long pollId,
            Long userId
    );

    long countByPollId(Long pollId);

    long countByOptionId(Long optionId);

    // Return vote totals for all requested polls in one query.
    @Query("""
        select v.poll.id as pollId, count(v.id) as voteCount
        from PollVote v
        where v.poll.id in :pollIds
        group by v.poll.id
        """)
    List<PollVoteCountProjection> countVotesByPollIds(
            @Param("pollIds") Collection<Long> pollIds
    );

    // Return vote totals for all requested options in one query.
    @Query("""
        select v.option.id as optionId, count(v.id) as voteCount
        from PollVote v
        where v.option.id in :optionIds
        group by v.option.id
        """)
    List<OptionVoteCountProjection> countVotesByOptionIds(
            @Param("optionIds") Collection<Long> optionIds
    );

    // Return IDs of polls the specified user has voted in.
    @Query("""
        select v.poll.id
        from PollVote v
        where v.user.id = :userId
          and v.poll.id in :pollIds
        """)
    List<Long> findVotedPollIdsByUserIdAndPollIds(
            @Param("userId") Long userId,
            @Param("pollIds") Collection<Long> pollIds
    );
}
