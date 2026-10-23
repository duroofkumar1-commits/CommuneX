
package com.communex.community;

import com.communex.common.ActivityLogService;
import com.communex.community.dto.PollRequest;
import com.communex.community.dto.PollVoteRequest;
import com.communex.users.User;
import com.communex.users.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PollService {

    private final CommunityPollRepository polls;
    private final PollOptionRepository options;
    private final PollVoteRepository votes;
    private final UserRepository users;
    private final ActivityLogService activity;

    public PollService(
            CommunityPollRepository polls,
            PollOptionRepository options,
            PollVoteRepository votes,
            UserRepository users,
            ActivityLogService activity
    ) {
        this.polls = polls;
        this.options = options;
        this.votes = votes;
        this.users = users;
        this.activity = activity;
    }

    /**
     * Loads all polls and their related information in batches.
     * Avoids issuing separate count queries for every poll and option.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String email) {

        User currentUser = user(email);

        List<CommunityPoll> pollList =
                polls.findAllWithCreatorOrderByCreatedAtDesc();

        if (pollList.isEmpty()) {
            return List.of();
        }

        List<Long> pollIds = pollList.stream()
                .map(CommunityPoll::getId)
                .toList();

        // Load options for all polls in one query.
        List<PollOption> allOptions =
                options.findAllForPollIds(pollIds);

        Map<Long, List<PollOption>> optionsByPollId =
                new HashMap<>();

        for (PollOption option : allOptions) {
            Long pollId = option.getPoll().getId();

            optionsByPollId
                    .computeIfAbsent(pollId, id -> new ArrayList<>())
                    .add(option);
        }

        List<Long> optionIds = allOptions.stream()
                .map(PollOption::getId)
                .toList();

        // Fetch all option vote counts in one grouped query.
        Map<Long, Long> optionVoteCounts =
                loadOptionVoteCounts(optionIds);

        // Fetch all poll vote totals in one grouped query.
        Map<Long, Long> pollVoteCounts =
                loadPollVoteCounts(pollIds);

        // Find which polls the current user has already voted in.
        Set<Long> votedPollIds = new HashSet<>(
                votes.findVotedPollIdsByUserIdAndPollIds(
                        currentUser.getId(),
                        pollIds
                )
        );

        return pollList.stream()
                .map(poll -> toResponse(
                        poll,
                        optionsByPollId.getOrDefault(
                                poll.getId(),
                                List.of()
                        ),
                        optionVoteCounts,
                        pollVoteCounts.getOrDefault(
                                poll.getId(),
                                0L
                        ),
                        votedPollIds.contains(poll.getId())
                ))
                .toList();
    }

    @Transactional
    public Map<String, Object> create(
            String email,
            PollRequest request
    ) {
        User currentUser = user(email);

        CommunityPoll poll = new CommunityPoll();
        poll.setQuestion(request.question().trim());
        poll.setCreatedBy(currentUser);

        poll = polls.save(poll);

        List<PollOption> newOptions = new ArrayList<>();

        for (String text : request.options()) {
            PollOption option = new PollOption();
            option.setPoll(poll);
            option.setText(text.trim());
            newOptions.add(option);
        }

        options.saveAll(newOptions);

        activity.record(
                email,
                "POLL_CREATED",
                "POLL",
                poll.getId(),
                poll.getQuestion()
        );

        return singlePollResponse(poll, currentUser);
    }

    @Transactional
    public Map<String, Object> vote(
            Long id,
            String email,
            PollVoteRequest request
    ) {
        CommunityPoll poll = poll(id);
        User currentUser = user(email);

        if (poll.isClosed()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Poll is closed"
            );
        }

        if (votes.existsByPollIdAndUserId(
                id,
                currentUser.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already voted"
            );
        }

        PollOption option = options
                .findByIdAndPollId(request.optionId(), id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Option does not belong to this poll"
                        )
                );

        PollVote vote = new PollVote();
        vote.setPoll(poll);
        vote.setOption(option);
        vote.setUser(currentUser);

        votes.save(vote);

        activity.record(
                email,
                "POLL_VOTED",
                "POLL",
                id,
                "Voted in poll"
        );

        return singlePollResponse(poll, currentUser);
    }

    /**
     * Builds a response for one poll after create/vote.
     * The list endpoint uses batched data instead.
     */
    private Map<String, Object> singlePollResponse(
            CommunityPoll poll,
            User currentUser
    ) {
        List<PollOption> pollOptions =
                options.findByPollIdOrderByIdAsc(poll.getId());

        List<Long> optionIds = pollOptions.stream()
                .map(PollOption::getId)
                .toList();

        Map<Long, Long> optionVoteCounts =
                loadOptionVoteCounts(optionIds);

        long totalVotes = votes.countByPollId(poll.getId());

        boolean votedByMe = votes.existsByPollIdAndUserId(
                poll.getId(),
                currentUser.getId()
        );

        return toResponse(
                poll,
                pollOptions,
                optionVoteCounts,
                totalVotes,
                votedByMe
        );
    }

    private Map<Long, Long> loadOptionVoteCounts(
            List<Long> optionIds
    ) {
        if (optionIds.isEmpty()) {
            return Map.of();
        }

        return votes.countVotesByOptionIds(optionIds)
                .stream()
                .collect(Collectors.toMap(
                        OptionVoteCountProjection::getOptionId,
                        OptionVoteCountProjection::getVoteCount
                ));
    }

    private Map<Long, Long> loadPollVoteCounts(
            List<Long> pollIds
    ) {
        if (pollIds.isEmpty()) {
            return Map.of();
        }

        return votes.countVotesByPollIds(pollIds)
                .stream()
                .collect(Collectors.toMap(
                        PollVoteCountProjection::getPollId,
                        PollVoteCountProjection::getVoteCount
                ));
    }

    private Map<String, Object> toResponse(
            CommunityPoll poll,
            List<PollOption> pollOptions,
            Map<Long, Long> optionVoteCounts,
            long totalVotes,
            boolean votedByMe
    ) {
        List<Map<String, Object>> optionResponses =
                pollOptions.stream()
                        .map(option -> Map.<String, Object>of(
                                "id", option.getId(),
                                "text", option.getText(),
                                "voteCount", optionVoteCounts.getOrDefault(
                                        option.getId(),
                                        0L
                                )
                        ))
                        .toList();

        return Map.of(
                "id", poll.getId(),
                "question", poll.getQuestion(),
                "options", optionResponses,
                "totalVotes", totalVotes,
                "votedByMe", votedByMe,
                "closed", poll.isClosed(),
                "createdAt", poll.getCreatedAt(),
                "createdBy", poll.getCreatedBy().getName()
        );
    }

    private User user(String email) {
        return users.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );
    }

    private CommunityPoll poll(Long id) {
        return polls.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Poll not found"
                        )
                );
    }
}
