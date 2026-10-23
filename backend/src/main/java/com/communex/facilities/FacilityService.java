
package com.communex.facilities;

import com.communex.common.ActivityLogService;
import com.communex.facilities.dto.FacilityRequest;
import com.communex.facilities.dto.FacilityResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FacilityService {
    private final FacilityRepository repo;
    private final ActivityLogService activity;

    public FacilityService(FacilityRepository repo, ActivityLogService activity) {
        this.repo = repo;
        this.activity = activity;
    }

    @Transactional(readOnly = true)
    public List<FacilityResponse> search(String q, String category) {
        // Native query ke liye sirf clean text bhejein (wildcards SQL khud sambhal lega)
        String cleanedQ = clean(q);
        String cleanedCategory = clean(category);

        return repo.search(cleanedQ, cleanedCategory)
                   .stream()
                   .map(FacilityResponse::from)
                   .toList();
    }

    @Transactional
    public FacilityResponse create(String email, FacilityRequest r) {
        Facility f = new Facility();
        apply(f, r, false);
        f = repo.save(f);
        activity.record(email, "FACILITY_CREATED", "FACILITY", f.getId(), f.getName());
        return FacilityResponse.from(f);
    }

    @Transactional
    public FacilityResponse update(Long id, String email, FacilityRequest r) {
        Facility f = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
        apply(f, r, true);
        f = repo.save(f);
        activity.record(email, "FACILITY_UPDATED", "FACILITY", f.getId(), f.getName());
        return FacilityResponse.from(f);
    }

    @Transactional
    public void deactivate(Long id, String email) {
        Facility f = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facility not found"));
        f.setActive(false);
        repo.save(f);
        activity.record(email, "FACILITY_ARCHIVED", "FACILITY", id, f.getName());
    }

    private void apply(Facility f, FacilityRequest r, boolean update) {
        f.setName(r.name().trim());
        f.setCategory(r.category().trim());
        f.setDescription(trim(r.description()));
        f.setAddress(trim(r.address()));
        f.setLatitude(r.latitude());
        f.setLongitude(r.longitude());
        f.setPhone(trim(r.phone()));
        f.setWebsite(trim(r.website()));
        f.setOpeningInformation(trim(r.openingInformation()));
        if (!update) f.setVerified(false);
    }

    private String clean(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private String trim(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}