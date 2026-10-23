package com.communex.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserLiveLocationRepository
        extends JpaRepository<UserLiveLocation, Long> {

    List<UserLiveLocation> findBySharingTrueAndUpdatedAtAfter(
            LocalDateTime since
    );
}
