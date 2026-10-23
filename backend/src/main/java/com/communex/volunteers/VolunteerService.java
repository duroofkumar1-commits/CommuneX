package com.communex.volunteers;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.communex.users.Role;
import com.communex.users.User;
import com.communex.users.UserRepository;
import com.communex.volunteers.dto.VolunteerRequest;
import com.communex.volunteers.dto.VolunteerResponse;
import com.communex.volunteers.dto.VolunteerUpdateRequest;

@Service
public class VolunteerService {

    private final VolunteerRepository volunteerRepository;
    private final UserRepository userRepository;

    public VolunteerService(
            VolunteerRepository volunteerRepository,
            UserRepository userRepository) {

        this.volunteerRepository = volunteerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public VolunteerResponse createVolunteer(
            String email,
            VolunteerRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (volunteerRepository.existsByUser(user)) {
            throw new IllegalArgumentException(
                    "Volunteer profile already exists");
        }

        user.setRole(Role.VOLUNTEER);
        userRepository.save(user);

        Volunteer volunteer = new Volunteer();
        volunteer.setUser(user);
        volunteer.setSkills(request.skills().trim());
        volunteer.setBio(
                request.bio() == null
                        ? null
                        : request.bio().trim()
        );

        return toResponse(volunteerRepository.save(volunteer));
    }

    public VolunteerResponse getMyProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Volunteer volunteer = volunteerRepository.findByUser(user)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Volunteer profile not found"));

        return toResponse(volunteer);
    }

    @Transactional
    public VolunteerResponse updateMyProfile(
            String email,
            VolunteerUpdateRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        Volunteer volunteer = volunteerRepository.findByUser(user)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Volunteer profile not found"));

        volunteer.setSkills(request.skills().trim());
        volunteer.setBio(
                request.bio() == null
                        ? null
                        : request.bio().trim()
        );
        volunteer.setAvailable(request.available());

        return toResponse(volunteerRepository.save(volunteer));
    }

    public List<VolunteerResponse> getAvailableVolunteers() {

        return volunteerRepository.findByAvailableTrueOrderByCreatedAtDesc(PageRequest.of(0, 40))
                .stream()
                .map(this::toResponse)
                .toList();
    }
    public List<VolunteerResponse> searchAvailableVolunteers(String skills) {
        return searchAvailableVolunteers(skills, 0, 40);
    }

    @Transactional(readOnly = true)
    public List<VolunteerResponse> searchAvailableVolunteers(
            String skills, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        List<Volunteer> results;
        if (skills == null || skills.isBlank()) {
            results = volunteerRepository.findByAvailableTrueOrderByCreatedAtDesc(pageable);
        } else {
            results = volunteerRepository
                    .findByAvailableTrueAndSkillsContainingIgnoreCaseOrderByCreatedAtDesc(
                            skills.trim(), pageable);
        }
        return results.stream().map(this::toResponse).toList();
    }

    private VolunteerResponse toResponse(Volunteer volunteer) {

        User user = volunteer.getUser();

        return new VolunteerResponse(
                volunteer.getId(),
                user.getId(),
                user.getName(),
                volunteer.getSkills(),
                volunteer.getBio(),
                volunteer.isAvailable()
        );
    }
}