package com.alemandan.crm.repository;

import com.alemandan.crm.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    PasswordResetToken findByToken(String token);
    
    @Transactional
    void deleteByEmail(String email);
    
    /**
     * Find a valid token: exists, not expired, and not used
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token AND t.expiryDate > :now AND t.used = false")
    Optional<PasswordResetToken> findValidToken(String token, LocalDateTime now);
    
    /**
     * Delete expired tokens for cleanup
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now")
    void deleteExpiredTokens(LocalDateTime now);
}