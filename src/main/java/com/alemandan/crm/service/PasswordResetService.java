package com.alemandan.crm.service;

import com.alemandan.crm.model.PasswordResetToken;
import com.alemandan.crm.repository.PasswordResetTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing password reset tokens.
 * Supports both permanent (non-expiring) and expirable tokens based on configuration.
 */
@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Value("${security.password.reset.permanent:true}")
    private boolean permanentTokens;

    @Value("${security.password.reset.token.expiration-minutes:60}")
    private int tokenExpirationMinutes;

    /**
     * Creates a new password reset token for the given email.
     * If permanentTokens is true, the token never expires (expiresAt = null).
     * Otherwise, token expires after configured minutes.
     *
     * @param email User's email address
     * @param userId Optional user ID (can be null)
     * @return The generated token string
     */
    @Transactional
    public String createTokenForEmail(String email, Long userId) {
        // Delete any existing tokens for this email
        tokenRepository.deleteByEmail(email);

        // Generate secure token
        String token = UUID.randomUUID().toString();

        // Create token entity
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setUserId(userId);
        resetToken.setCreatedAt(LocalDateTime.now());
        resetToken.setUsed(false);

        // Set expiration based on configuration
        if (permanentTokens) {
            resetToken.setExpiryDate(null); // Permanent token
            logger.debug("Created permanent password reset token for email: {}", email);
        } else {
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(tokenExpirationMinutes));
            logger.debug("Created expirable password reset token for email: {} (expires in {} minutes)", 
                       email, tokenExpirationMinutes);
        }

        tokenRepository.save(resetToken);
        return token;
    }

    /**
     * Validates a password reset token.
     * Checks:
     * - Token exists
     * - Token is not marked as used
     * - Token is not expired (if expiration is enabled)
     *
     * @param token The token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token);

        if (resetToken == null) {
            logger.debug("Token not found: {}", token);
            return false;
        }

        if (resetToken.getUsed()) {
            logger.debug("Token already used: {}", token);
            return false;
        }

        // Check expiration only if expiryDate is set (non-permanent token)
        if (resetToken.getExpiryDate() != null && resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            logger.debug("Token expired: {}", token);
            return false;
        }

        logger.debug("Token validated successfully: {}", token);
        return true;
    }

    /**
     * Marks a token as used (consumed).
     * This prevents the token from being reused.
     *
     * @param token The token to consume
     * @return true if token was marked as used, false if token not found
     */
    @Transactional
    public boolean consumeToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token);

        if (resetToken == null) {
            logger.warn("Attempted to consume non-existent token");
            return false;
        }

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        logger.debug("Token consumed for email: {}", resetToken.getEmail());
        return true;
    }

    /**
     * Gets a token entity by token string.
     *
     * @param token The token string
     * @return Optional containing the token entity if found
     */
    public Optional<PasswordResetToken> getTokenByString(String token) {
        return Optional.ofNullable(tokenRepository.findByToken(token));
    }

    /**
     * Revokes all tokens for a given email address.
     * Useful for security purposes or when user requests to invalidate all reset links.
     *
     * @param email The email address
     */
    @Transactional
    public void revokeTokensForEmail(String email) {
        tokenRepository.deleteByEmail(email);
        logger.debug("Revoked all password reset tokens for email: {}", email);
    }

    /**
     * Cleanup expired tokens (optional maintenance operation).
     * Only relevant when using expirable tokens.
     */
    @Transactional
    public void cleanupExpiredTokens() {
        if (!permanentTokens) {
            tokenRepository.deleteExpiredTokens(LocalDateTime.now());
            logger.debug("Cleaned up expired password reset tokens");
        }
    }
}
