package com.mobila.shop;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByTokenHash(String tokenHash);

    /** Consumă token-ul atomic: întoarce 1 doar pentru prima cerere (a doua, paralelă, primește 0). */
    @Transactional
    long deleteByTokenHash(String tokenHash);

    @Transactional
    long deleteByUserId(Long userId);
}
