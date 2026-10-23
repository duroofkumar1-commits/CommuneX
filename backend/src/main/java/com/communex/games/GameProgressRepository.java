package com.communex.games;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GameProgressRepository extends JpaRepository<GameProgress, Long> {
    List<GameProgress> findAllByUserEmailOrderByGameAsc(String email);
    Optional<GameProgress> findByUserEmailAndGame(String email, String game);
    long deleteByUserEmail(String email);
}
