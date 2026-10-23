package com.communex.map;

import com.communex.events.CommunityEvent;
import com.communex.events.CommunityEventRepository;
import com.communex.facilities.Facility;
import com.communex.facilities.FacilityRepository;
import com.communex.map.MapDtos.LiveLocationUpdate;
import com.communex.map.MapDtos.LocationUpdate;
import com.communex.map.MapDtos.MapItem;
import com.communex.map.MapDtos.Overview;
import com.communex.map.MapDtos.VolunteerPin;
import com.communex.requests.HelpRequest;
import com.communex.requests.HelpRequestRepository;
import com.communex.requests.HelpRequestStatus;
import com.communex.resources.ResourceItem;
import com.communex.resources.ResourceRepository;
import com.communex.users.Role;
import com.communex.users.User;
import com.communex.users.UserRepository;
import com.communex.volunteers.Volunteer;
import com.communex.volunteers.VolunteerRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MapService {

    public static final String HELP = "help-requests";
    public static final String EVENT = "events";
    public static final String RESOURCE = "resources";

    private static final Set<HelpRequestStatus> ACTIVE_REQUEST_STATUSES = Set.of(
            HelpRequestStatus.OPEN,
            HelpRequestStatus.ACKNOWLEDGED,
            HelpRequestStatus.ASSIGNED,
            HelpRequestStatus.ACCEPTED,
            HelpRequestStatus.IN_PROGRESS
    );

    private static final Set<Role> PIN_ADMIN_ROLES = Set.of(
            Role.ADMIN, Role.MODERATOR
    );
    private static final Set<Role> EVENT_PIN_ROLES = Set.of(
            Role.ADMIN, Role.ORGANIZER, Role.ORGANIZATION_ADMIN
    );
    private static final Set<Role> RESOURCE_PIN_ROLES = Set.of(
            Role.ADMIN, Role.FACILITY_MANAGER, Role.ORGANIZATION_ADMIN
    );
    private static final Set<Role> LIVE_LOCATION_ROLES = Set.of(
            Role.VOLUNTEER, Role.RESPONDER
    );

    private static final long LIVE_WINDOW_HOURS = 6;

    private final HelpRequestRepository requests;
    private final CommunityEventRepository events;
    private final ResourceRepository resources;
    private final FacilityRepository facilities;
    private final VolunteerRepository volunteers;
    private final UserRepository users;
    private final MapLocationRepository locations;
    private final UserLiveLocationRepository live;

    public MapService(
            HelpRequestRepository requests,
            CommunityEventRepository events,
            ResourceRepository resources,
            FacilityRepository facilities,
            VolunteerRepository volunteers,
            UserRepository users,
            MapLocationRepository locations,
            UserLiveLocationRepository live
    ) {
        this.requests = requests;
        this.events = events;
        this.resources = resources;
        this.facilities = facilities;
        this.volunteers = volunteers;
        this.users = users;
        this.locations = locations;
        this.live = live;
    }

    private User user(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private static boolean hasRole(User user, Set<Role> roles) {
        return user != null && roles.contains(user.getRole());
    }

    /**
     * Builds one pin index per entity type. This avoids repeatedly scanning
     * MapLocation rows when enriching map items with coordinates.
     */
    private Map<String, Map<Long, MapLocation>> loadPinIndexes() {
        Map<String, Map<Long, MapLocation>> indexes = new HashMap<>();
        Collection<String> entityTypes = List.of(HELP, EVENT, RESOURCE);

        for (MapLocation location : locations.findByEntityTypeIn(entityTypes)) {
            indexes.computeIfAbsent(location.getEntityType(), ignored -> new HashMap<>())
                    .put(location.getEntityId(), location);
        }

        return indexes;
    }

    private static MapLocation pinFor(
            Map<String, Map<Long, MapLocation>> indexes,
            String type,
            Long id
    ) {
        return indexes.getOrDefault(type, Map.of()).get(id);
    }

    @Transactional(readOnly = true)
    public Overview overview(String email) {
        User me = user(email);
        Map<String, Map<Long, MapLocation>> pinIndexes = loadPinIndexes();

        // Fetch only map-relevant help requests and eagerly load their map relations.
        List<MapItem> helpItems = new ArrayList<>();
        for (HelpRequest request : requests.findForMapByStatuses(ACTIVE_REQUEST_STATUSES)) {
            MapLocation pin = pinFor(pinIndexes, HELP, request.getId());
            boolean mine = request.getCreatedBy() != null
                    && request.getCreatedBy().getId().equals(me.getId());
            boolean canPin = mine || hasRole(me, PIN_ADMIN_ROLES);

            helpItems.add(new MapItem(
                    "help",
                    request.getId(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getCategory(),
                    request.getStatus().name(),
                    request.getPriority().name(),
                    request.getLocation(),
                    pin == null ? null : pin.getLatitude(),
                    pin == null ? null : pin.getLongitude(),
                    pin != null,
                    canPin,
                    mine,
                    request.getCreatedBy() == null ? null : request.getCreatedBy().getName(),
                    request.getAssignedVolunteer() == null ? null : request.getAssignedVolunteer().getName(),
                    request.getCreatedAt()
            ));
        }

        // Only include events starting within the last 6 hours or in the future.
        LocalDateTime cutoff = LocalDateTime.now().minusHours(LIVE_WINDOW_HOURS);
        List<MapItem> eventItems = new ArrayList<>();
        for (CommunityEvent event : events.findMapEventsSince("PUBLISHED", cutoff)) {
            MapLocation pin = pinFor(pinIndexes, EVENT, event.getId());
            boolean mine = event.getOrganizer() != null
                    && event.getOrganizer().getId().equals(me.getId());
            boolean canPin = mine || hasRole(me, EVENT_PIN_ROLES);

            eventItems.add(new MapItem(
                    "event",
                    event.getId(),
                    event.getTitle(),
                    event.getDescription(),
                    "Event",
                    event.getStatus(),
                    null,
                    event.getLocation(),
                    pin == null ? null : pin.getLatitude(),
                    pin == null ? null : pin.getLongitude(),
                    pin != null,
                    canPin,
                    mine,
                    event.getOrganizer() == null ? null : event.getOrganizer().getName(),
                    event.getCapacity() == null ? null : "Capacity " + event.getCapacity(),
                    event.getStartsAt()
            ));
        }

        // Fetch only active resource rows (important as the inventory grows).
        boolean canPinResources = hasRole(me, RESOURCE_PIN_ROLES);
        List<MapItem> resourceItems = new ArrayList<>();
        for (ResourceItem resource : resources.findByActiveTrue()) {
            MapLocation pin = pinFor(pinIndexes, RESOURCE, resource.getId());
            BigDecimal quantity = resource.getQuantity();
            String stockStatus = quantity.signum() <= 0
                    ? "OUT_OF_STOCK"
                    : quantity.compareTo(resource.getLowStockThreshold()) <= 0
                            ? "LOW_STOCK" : "AVAILABLE";

            resourceItems.add(new MapItem(
                    "resource",
                    resource.getId(),
                    resource.getName(),
                    resource.getDescription(),
                    resource.getCategory(),
                    stockStatus,
                    null,
                    resource.getLocation(),
                    pin == null ? null : pin.getLatitude(),
                    pin == null ? null : pin.getLongitude(),
                    pin != null,
                    canPinResources,
                    false,
                    null,
                    quantity.stripTrailingZeros().toPlainString() + " " + resource.getUnit(),
                    resource.getUpdatedAt()
            ));
        }

        // Query only active facilities that can actually be plotted.
        List<MapItem> facilityItems = new ArrayList<>();
        for (Facility facility : facilities.findActiveForMap()) {
            String phone = facility.getPhone() == null || facility.getPhone().isBlank()
                    ? null : facility.getPhone();

            facilityItems.add(new MapItem(
                    "facility",
                    facility.getId(),
                    facility.getName(),
                    facility.getDescription(),
                    facility.getCategory(),
                    facility.isVerified() ? "VERIFIED" : "UNVERIFIED",
                    null,
                    facility.getAddress(),
                    facility.getLatitude(),
                    facility.getLongitude(),
                    true,
                    false,
                    false,
                    null,
                    phone,
                    facility.getUpdatedAt()
            ));
        }

        // Batch-load users and volunteer profiles to avoid N+1 database queries.
        LocalDateTime liveSince = LocalDateTime.now().minusHours(LIVE_WINDOW_HOURS);
        List<UserLiveLocation> liveLocations =
                live.findBySharingTrueAndUpdatedAtAfter(liveSince);

        Set<Long> liveUserIds = liveLocations.stream()
                .map(UserLiveLocation::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> usersById = new HashMap<>();
        Map<Long, Volunteer> volunteersByUserId = new HashMap<>();

        if (!liveUserIds.isEmpty()) {
            users.findAllById(liveUserIds).forEach(u -> usersById.put(u.getId(), u));
            volunteers.findAllWithUserByUserIds(liveUserIds).forEach(profile -> {
                if (profile.getUser() != null) {
                    volunteersByUserId.put(profile.getUser().getId(), profile);
                }
            });
        }

        List<VolunteerPin> volunteerPins = new ArrayList<>();
        for (UserLiveLocation location : liveLocations) {
            User volunteerUser = usersById.get(location.getUserId());
            if (volunteerUser == null || !hasRole(volunteerUser, LIVE_LOCATION_ROLES)) {
                continue;
            }

            Volunteer profile = volunteersByUserId.get(volunteerUser.getId());
            if (profile != null && !profile.isAvailable()) {
                continue;
            }

            volunteerPins.add(new VolunteerPin(
                    volunteerUser.getId(),
                    volunteerUser.getName(),
                    profile == null ? null : profile.getSkills(),
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy(),
                    location.getUpdatedAt()
            ));
        }

        boolean sharing = live.findById(me.getId())
                .map(UserLiveLocation::isSharing)
                .orElse(false);

        return new Overview(
                helpItems,
                eventItems,
                resourceItems,
                facilityItems,
                volunteerPins,
                sharing
        );
    }

    private void authorise(User me, String type, Long id) {
        switch (type) {
            case HELP -> {
                HelpRequest request = requests.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Help request not found"));
                boolean owner = request.getCreatedBy() != null
                        && request.getCreatedBy().getId().equals(me.getId());
                if (!owner && !hasRole(me, PIN_ADMIN_ROLES)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Only the requester or a moderator can set this location"
                    );
                }
            }
            case EVENT -> {
                CommunityEvent event = events.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Event not found"));
                boolean owner = event.getOrganizer() != null
                        && event.getOrganizer().getId().equals(me.getId());
                if (!owner && !hasRole(me, EVENT_PIN_ROLES)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Only the organizer can set this location"
                    );
                }
            }
            case RESOURCE -> {
                if (!resources.existsById(id)) {
                    throw new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Resource not found");
                }
                if (!hasRole(me, RESOURCE_PIN_ROLES)) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "You cannot set inventory locations"
                    );
                }
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unknown location type");
        }
    }

    @Transactional
    public void pin(String email, String type, Long id, LocationUpdate body) {
        authorise(user(email), type, id);

        MapLocation location = locations.findByEntityTypeAndEntityId(type, id)
                .orElseGet(MapLocation::new);
        location.setEntityType(type);
        location.setEntityId(id);
        location.setLatitude(body.latitude());
        location.setLongitude(body.longitude());
        locations.save(location);
    }

    @Transactional
    public void unpin(String email, String type, Long id) {
        authorise(user(email), type, id);
        locations.findByEntityTypeAndEntityId(type, id).ifPresent(locations::delete);
    }

    @Transactional
    public void shareLiveLocation(String email, LiveLocationUpdate body) {
        User me = user(email);
        if (!hasRole(me, LIVE_LOCATION_ROLES)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only volunteers and responders can share a live location"
            );
        }

        UserLiveLocation location = live.findById(me.getId())
                .orElseGet(UserLiveLocation::new);
        location.setUserId(me.getId());
        location.setLatitude(body.latitude());
        location.setLongitude(body.longitude());
        location.setAccuracy(body.accuracy());
        location.setSharing(true);
        live.save(location);
    }

    @Transactional
    public void stopSharing(String email) {
        User me = user(email);
        live.findById(me.getId()).ifPresent(live::delete);
    }
}
