
package com.communex.community;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.communex.common.ActivityLogService;
import com.communex.community.dto.AnnouncementRequest;
import com.communex.community.dto.AnnouncementResponse;
import com.communex.users.User;
import com.communex.users.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final CommunityAnnouncementRepository announcements;
    private final UserRepository users;
    private final ActivityLogService activity;

    public AnnouncementController(
            CommunityAnnouncementRepository announcements,
            UserRepository users,
            ActivityLogService activity
    ) {
        this.announcements = announcements;
        this.users = users;
        this.activity = activity;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list() {

        return announcements
                .findTop100ByPublishedTrueOrderByCreatedAtDesc()
                .stream()
                .map(AnnouncementResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Transactional
    public AnnouncementResponse create(
            Authentication auth,
            @Valid @RequestBody AnnouncementRequest request
    ) {

        User user = (User) auth.getPrincipal();

        CommunityAnnouncement announcement = new CommunityAnnouncement();

        announcement.setTitle(request.title().trim());
        announcement.setBody(request.body().trim());

        announcement.setCategory(
                request.category() == null || request.category().isBlank()
                        ? "GENERAL"
                        : request.category().trim()
        );

        announcement.setCreatedBy(user);

        announcement = announcements.save(announcement);

        activity.record(
                user.getEmail(),
                "ANNOUNCEMENT_CREATED",
                "ANNOUNCEMENT",
                announcement.getId(),
                announcement.getTitle()
        );

        return AnnouncementResponse.from(announcement);
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public AnnouncementResponse publish(
            @PathVariable Long id,
            @RequestParam boolean published,
            Authentication auth
    ) {

        CommunityAnnouncement announcement = announcements
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Announcement not found"
                        )
                );

        announcement.setPublished(published);
        announcement = announcements.save(announcement);

        User actor = (User) auth.getPrincipal();

        activity.record(
                actor.getEmail(),
                published
                        ? "ANNOUNCEMENT_PUBLISHED"
                        : "ANNOUNCEMENT_UNPUBLISHED",
                "ANNOUNCEMENT",
                id,
                announcement.getTitle()
        );

        return AnnouncementResponse.from(announcement);
    }
}
