package com.communex.requests;

import com.communex.common.ActivityLogService;
import com.communex.notifications.NotificationService;
import com.communex.requests.dto.HelpRequestCreateRequest;
import com.communex.requests.dto.HelpRequestResponse;
import com.communex.requests.dto.RequestStatusChangeRequest;
import com.communex.requests.dto.RequestUpdateResponse;
import com.communex.users.Role;
import com.communex.users.User;
import com.communex.users.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HelpRequestService {

    private final HelpRequestRepository repository;
    private final HelpRequestUpdateRepository updates;
    private final UserRepository users;
    private final NotificationService notifications;
    private final ActivityLogService activity;

    // Constructor
    public HelpRequestService(
            HelpRequestRepository repository,
            HelpRequestUpdateRepository updates,
            UserRepository users,
            NotificationService notifications,
            ActivityLogService activity
    ) {
        this.repository = repository;
        this.updates = updates;
        this.users = users;
        this.notifications = notifications;
        this.activity = activity;
    }

    // Create a new help request
    @Transactional
    public HelpRequestResponse create(
            String email,
            HelpRequestCreateRequest input
    ) {
        User creator = getUser(email);

        HelpRequest request = new HelpRequest();

        request.setTitle(input.title().trim());
        request.setDescription(input.description().trim());
        request.setCategory(input.category().trim());
        request.setLocation(input.location().trim());
        request.setCreatedBy(creator);
        request.setStatus(HelpRequestStatus.OPEN);

        request.setPriority(
                classifyPriority(
                        input.category(),
                        input.description()
                )
        );

        request = repository.save(request);

        recordUpdate(
                request,
                null,
                HelpRequestStatus.OPEN,
                "Request created",
                creator
        );

        activity.record(
                email,
                "HELP_REQUEST_CREATED",
                "HELP_REQUEST",
                request.getId(),
                request.getTitle()
        );

        return HelpRequestResponse.from(request);
    }

    // Hard cap on how many OPEN rows are pulled from the database for ranking.
    private static final int MAX_OPEN_CANDIDATES = 500;
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;

    // Retrieve open requests, most urgent first (priority, then oldest first).
    //
    // DSA: a PriorityQueue (binary heap) gives the next most urgent request in O(log n).
    // We only need ONE PAGE of results, so we poll (page + 1) * size items instead of
    // sorting everything: O(n + k log n) instead of O(n log n) for the whole list.
    @Transactional(readOnly = true)
    public List<HelpRequestResponse> getOpenRequests(int page, int size) {

        int safeSize = clampSize(size);
        int safePage = Math.max(page, 0);

        Comparator<HelpRequest> order = Comparator
                .comparingInt(
                        (HelpRequest request) -> request.getPriority().rank()
                )
                .reversed()
                .thenComparing(HelpRequest::getCreatedAt)
                .thenComparing(HelpRequest::getId);

        // One query: requester + volunteer are joined (see the repository entity graph).
        List<HelpRequest> candidates = repository.findByStatusOrderByCreatedAtDesc(
                HelpRequestStatus.OPEN,
                PageRequest.of(0, MAX_OPEN_CANDIDATES)
        );

        // Build the heap in O(n).
        PriorityQueue<HelpRequest> queue = new PriorityQueue<>(
                Math.max(candidates.size(), 1),
                order
        );
        queue.addAll(candidates);

        long skip = (long) safePage * safeSize;
        List<HelpRequestResponse> result = new ArrayList<>(safeSize);

        for (long position = 0;
             !queue.isEmpty() && result.size() < safeSize;
             position++) {

            HelpRequest request = queue.poll();

            if (position >= skip) {
                result.add(HelpRequestResponse.from(request));
            }
        }

        return result;
    }

    // Retrieve requests created by a specific user (newest first, one page at a time)
    @Transactional(readOnly = true)
    public List<HelpRequestResponse> getMyRequests(String email, int page, int size) {

        return repository
                .findByCreatedByEmailOrderByCreatedAtDesc(
                        email,
                        PageRequest.of(Math.max(page, 0), clampSize(size))
                )
                .stream()
                .map(HelpRequestResponse::from)
                .toList();
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    // Accept an open help request
    @Transactional
    public HelpRequestResponse accept(Long id, String email) {

        User volunteer = getUser(email);

        if (volunteer.getRole() != Role.VOLUNTEER
                && volunteer.getRole() != Role.RESPONDER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only an authorized volunteer or responder can accept requests"
            );
        }

        HelpRequest request = getRequest(id);

        if (request.getCreatedBy().getId().equals(volunteer.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot accept your own request"
            );
        }

        if (request.getStatus() != HelpRequestStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Request is no longer open"
            );
        }

        request.setAssignedVolunteer(volunteer);
        request.setStatus(HelpRequestStatus.ACCEPTED);

        request = repository.save(request);

        recordUpdate(
                request,
                HelpRequestStatus.OPEN,
                request.getStatus(),
                "Accepted by " + volunteer.getName(),
                volunteer
        );

        notifications.createForUser(
                request.getCreatedBy(),
                "Your help request was accepted by "
                        + volunteer.getName() + "."
        );

        activity.record(
                email,
                "HELP_REQUEST_ACCEPTED",
                "HELP_REQUEST",
                id,
                request.getTitle()
        );

        return HelpRequestResponse.from(request);
    }

    // Move an accepted request to in progress
    @Transactional
    public HelpRequestResponse startInProgress(Long id, String email) {

        User volunteer = getUser(email);
        HelpRequest request = getRequest(id);

        assertAssigned(request, volunteer);

        if (request.getStatus() != HelpRequestStatus.ACCEPTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only an accepted request can be moved to in progress"
            );
        }

        HelpRequestStatus before = request.getStatus();

        request.setStatus(HelpRequestStatus.IN_PROGRESS);
        request = repository.save(request);

        recordUpdate(
                request,
                before,
                request.getStatus(),
                "Work started by assigned volunteer",
                volunteer
        );

        notifications.createForUser(
                request.getCreatedBy(),
                "Work has started on your help request."
        );

        activity.record(
                email,
                "HELP_REQUEST_STARTED",
                "HELP_REQUEST",
                id,
                request.getTitle()
        );

        return HelpRequestResponse.from(request);
    }

    // Mark an assigned request as completed
    @Transactional
    public HelpRequestResponse complete(Long id, String email) {

        User volunteer = getUser(email);
        HelpRequest request = getRequest(id);

        assertAssigned(request, volunteer);

        if (!Set.of(
                HelpRequestStatus.ACCEPTED,
                HelpRequestStatus.IN_PROGRESS
        ).contains(request.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Request is not in an active assigned state"
            );
        }

        HelpRequestStatus before = request.getStatus();

        request.setStatus(HelpRequestStatus.COMPLETED);
        request.setResolvedAt(LocalDateTime.now());

        request = repository.save(request);

        recordUpdate(
                request,
                before,
                request.getStatus(),
                "Marked complete by assigned volunteer",
                volunteer
        );

        notifications.createForUser(
                request.getCreatedBy(),
                "Your help request has been marked complete."
        );

        activity.record(
                email,
                "HELP_REQUEST_COMPLETED",
                "HELP_REQUEST",
                id,
                request.getTitle()
        );

        return HelpRequestResponse.from(request);
    }

    // Cancel a request created by the current user
    @Transactional
    public HelpRequestResponse cancel(Long id, String email) {

        User actor = getUser(email);
        HelpRequest request = getRequest(id);

        if (!request.getCreatedBy().getId().equals(actor.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the requester can cancel this request"
            );
        }

        if (request.getStatus() != HelpRequestStatus.OPEN
                && request.getStatus() != HelpRequestStatus.ACCEPTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only an open or accepted request can be cancelled"
            );
        }

        HelpRequestStatus before = request.getStatus();

        request.setStatus(HelpRequestStatus.CANCELLED);
        request = repository.save(request);

        recordUpdate(
                request,
                before,
                request.getStatus(),
                "Cancelled by requester",
                actor
        );

        if (request.getAssignedVolunteer() != null) {
            notifications.createForUser(
                    request.getAssignedVolunteer(),
                    "A help request you accepted has been cancelled by its requester."
            );
        }

        activity.record(
                email,
                "HELP_REQUEST_CANCELLED",
                "HELP_REQUEST",
                id,
                request.getTitle()
        );

        return HelpRequestResponse.from(request);
    }

    // Allow an authorized moderator to change request status
    @Transactional
    public HelpRequestResponse moderatorTransition(
            Long id,
            String email,
            RequestStatusChangeRequest input
    ) {
        User actor = getUser(email);

        if (actor.getRole() != Role.ADMIN
                && actor.getRole() != Role.RESPONDER
                && actor.getRole() != Role.MODERATOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Moderator/responder/admin role required"
            );
        }

        HelpRequest request = getRequest(id);
        HelpRequestStatus target = input.status();

        if (!Set.of(
                HelpRequestStatus.RESOLVED,
                HelpRequestStatus.CLOSED,
                HelpRequestStatus.CANCELLED
        ).contains(target)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Moderators may resolve, close or cancel requests only"
            );
        }

        if (request.getStatus() == HelpRequestStatus.COMPLETED
                || request.getStatus() == HelpRequestStatus.CLOSED
                || request.getStatus() == HelpRequestStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This request is already in a terminal state"
            );
        }

        HelpRequestStatus before = request.getStatus();

        request.setStatus(target);

        if (target == HelpRequestStatus.RESOLVED
                || target == HelpRequestStatus.CLOSED) {
            request.setResolvedAt(LocalDateTime.now());
        }

        request = repository.save(request);

        recordUpdate(
                request,
                before,
                target,
                input.note().trim(),
                actor
        );

        notifications.createForUser(
                request.getCreatedBy(),
                "Your help request status changed to "
                        + target.name() + "."
        );

        activity.record(
                email,
                "HELP_REQUEST_STATUS_CHANGED",
                "HELP_REQUEST",
                id,
                before + " -> " + target
        );

        return HelpRequestResponse.from(request);
    }

    // Retrieve the history of a help request
    @Transactional(readOnly = true)
    public List<RequestUpdateResponse> history(Long id, String email) {

        User actor = getUser(email);
        HelpRequest request = getRequest(id);

        boolean isRequester = request.getCreatedBy()
                .getId()
                .equals(actor.getId());

        boolean isAssignedVolunteer =
                request.getAssignedVolunteer() != null
                && request.getAssignedVolunteer()
                        .getId()
                        .equals(actor.getId());

        boolean hasPrivilegedRole =
                actor.getRole() == Role.ADMIN
                || actor.getRole() == Role.MODERATOR
                || actor.getRole() == Role.RESPONDER;

        boolean allowed = isRequester
                || isAssignedVolunteer
                || hasPrivilegedRole;

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot view this request history"
            );
        }

        return updates
                .findByRequestIdOrderByCreatedAtAsc(id)
                .stream()
                .map(RequestUpdateResponse::from)
                .toList();
    }

    // Record a request status update
    private void recordUpdate(
            HelpRequest request,
            HelpRequestStatus from,
            HelpRequestStatus to,
            String note,
            User actor
    ) {
        HelpRequestUpdate update = new HelpRequestUpdate();

        update.setRequest(request);
        update.setFromStatus(from);
        update.setToStatus(to);
        update.setNote(note);
        update.setActor(actor);

        updates.save(update);
    }

    // Ensure that the current volunteer is assigned to the request
    private void assertAssigned(
            HelpRequest request,
            User volunteer
    ) {
        if (request.getAssignedVolunteer() == null
                || !request.getAssignedVolunteer()
                        .getId()
                        .equals(volunteer.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the assigned volunteer can change this request"
            );
        }
    }

    // Classify request priority using its category and description
    private RequestPriority classifyPriority(
            String category,
            String description
    ) {
        String text = (category + " " + description)
                .toLowerCase(Locale.ROOT);

        if (containsAny(
                text,
                "fire",
                "violence",
                "unsafe",
                "injury",
                "medical",
                "urgent",
                "no shelter",
                "stranded",
                "immediate"
        )) {
            return RequestPriority.HIGH;
        }

        if (containsAny(
                text,
                "transport",
                "food",
                "groceries",
                "elderly",
                "accessibility",
                "medicine"
        )) {
            return RequestPriority.NORMAL;
        }

        return RequestPriority.NORMAL;
    }

    // Check whether text contains any specified keyword
    private boolean containsAny(
            String value,
            String... needles
    ) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }

        return false;
    }

    // Find a user by email
    private User getUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    // Find a help request by its ID
    private HelpRequest getRequest(Long id) {
        return repository.findWithUsersById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Help request not found"
                ));
    }
}