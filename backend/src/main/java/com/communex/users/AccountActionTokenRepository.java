package com.communex.users;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountActionTokenRepository extends JpaRepository<AccountActionToken, Long> {
    Optional<AccountActionToken> findByTokenHashAndPurposeAndConsumedAtIsNull(String tokenHash, AccountTokenPurpose purpose);
    List<AccountActionToken> findByUserIdAndPurposeAndConsumedAtIsNull(Long userId, AccountTokenPurpose purpose);
}
