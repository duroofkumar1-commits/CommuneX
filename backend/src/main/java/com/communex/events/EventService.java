
package com.communex.events;

import com.communex.common.ActivityLogService;
import com.communex.events.dto.EventRequest;
import com.communex.events.dto.EventResponse;
import com.communex.notifications.NotificationService;
import com.communex.users.User;
import com.communex.users.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final CommunityEventRepository events;
    private final EventRegistrationRepository registrations;
    private final UserRepository users;
    private final ActivityLogService activity;
    private final NotificationService notifications;

    public EventService(
            CommunityEventRepository events,
            EventRegistrationRepository registrations,
            UserRepository users,
            ActivityLogService activity,
            NotificationService notifications
    ) {
        this.events = events;
        this.registrations = registrations;
        this.users = users;
        this.activity = activity;
        this.notifications = notifications;
    }

    /**
     * Loads published events, organizers and registration information
     * without running separate registration queries for every event.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> list(String email) {

        User currentUser = user(email);

        List<CommunityEvent> eventList =
                events.findByStatusOrderByStartsAtAsc("PUBLISHED");

        if (eventList.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = eventList.stream()
                .map(CommunityEvent::getId)
                .toList();

        // Load registration counts for all events in one query.
        Map<Long, Long> registrationCounts =
                registrations.countRegistrationsByEventIds(eventIds)
                        .stream()
                        .collect(Collectors.toMap(
                                RegistrationCountProjection::getEventId,
                                RegistrationCountProjection::getRegistrationCount
                        ));

        // Load the events registered by the current user in one query.
        Set<Long> registeredEventIds = new HashSet<>(
                registrations.findRegisteredEventIdsByUserIdAndEventIds(
                        currentUser.getId(),
                        eventIds
                )
        );

        return eventList.stream()
                .map(event -> EventResponse.from(
                        event,
                        registrationCounts.getOrDefault(
                                event.getId(),
                                0L
                        ),
                        registeredEventIds.contains(event.getId())
                ))
                .toList();
    }

    @Transactional
    public EventResponse create(String email, EventRequest request) {

        User organizer = user(email);

        CommunityEvent event = new CommunityEvent();

        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setLocation(request.location().trim());
        event.setStartsAt(request.startsAt());
        event.setCapacity(request.capacity());
        event.setOrganizer(organizer);

        event = events.save(event);

        activity.record(
                email,
                "EVENT_CREATED",
                "EVENT",
                event.getId(),
                event.getTitle()
        );

        return EventResponse.from(event, 0L, false);
    }

    @Transactional
    public EventResponse register(Long id, String email) {

        CommunityEvent event = event(id);
        User currentUser = user(email);

        if (!event.getStartsAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Registration is closed for past events"
            );
        }

        if (registrations.existsByEventIdAndUserId(
                id,
                currentUser.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Already registered for this event"
            );
        }

        long currentCount = registrations.countByEventId(id);

        if (event.getCapacity() != null
                && currentCount >= event.getCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Event is full"
            );
        }

        EventRegistration registration = new EventRegistration();
        registration.setEvent(event);
        registration.setUser(currentUser);

        registrations.save(registration);

        if (!event.getOrganizer().getId().equals(currentUser.getId())) {
            notifications.createForUser(
                    event.getOrganizer(),
                    currentUser.getName()
                            + " registered for your event: "
                            + event.getTitle()
            );
        }

        activity.record(
                email,
                "EVENT_REGISTERED",
                "EVENT",
                id,
                event.getTitle()
        );

        return EventResponse.from(
                event,
                currentCount + 1,
                true
        );
    }

    @Transactional
    public void unregister(Long id, String email) {

        CommunityEvent event = event(id);
        User currentUser = user(email);

        if (!registrations.existsByEventIdAndUserId(
                id,
                currentUser.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Registration not found"
            );
        }

        registrations.deleteByEventIdAndUserId(
                id,
                currentUser.getId()
        );

        activity.record(
                email,
                "EVENT_UNREGISTERED",
                "EVENT",
                id,
                event.getTitle()
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

    private CommunityEvent event(Long id) {
        return events.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Event not found"
                        )
                );
    }
}
