package com.alemandan.crm.service;

import com.alemandan.crm.model.PasswordResetToken;
import com.alemandan.crm.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PasswordResetService
 */
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateTokenForEmail_PermanentToken() {
        // Setup: Configure service for permanent tokens
        ReflectionTestUtils.setField(passwordResetService, "permanentTokens", true);
        ReflectionTestUtils.setField(passwordResetService, "tokenExpirationMinutes", 60);

        String email = "test@example.com";
        Long userId = 123L;

        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Execute
        String token = passwordResetService.createTokenForEmail(email, userId);

        // Verify
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        verify(tokenRepository, times(1)).deleteByEmail(email);
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        
        // Verify the saved token
        verify(tokenRepository).save(argThat(savedToken -> 
            savedToken.getEmail().equals(email) &&
            savedToken.getUserId().equals(userId) &&
            savedToken.getExpiryDate() == null && // Permanent token has null expiry
            !savedToken.getUsed()
        ));
    }

    @Test
    void testCreateTokenForEmail_ExpirableToken() {
        // Setup: Configure service for expirable tokens
        ReflectionTestUtils.setField(passwordResetService, "permanentTokens", false);
        ReflectionTestUtils.setField(passwordResetService, "tokenExpirationMinutes", 60);

        String email = "test@example.com";
        Long userId = 123L;

        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Execute
        String token = passwordResetService.createTokenForEmail(email, userId);

        // Verify
        assertNotNull(token);
        
        // Verify the saved token has expiration date
        verify(tokenRepository).save(argThat(savedToken -> 
            savedToken.getEmail().equals(email) &&
            savedToken.getExpiryDate() != null && // Expirable token has expiry date
            savedToken.getExpiryDate().isAfter(LocalDateTime.now())
        ));
    }

    @Test
    void testValidateToken_ValidPermanentToken() {
        // Setup
        String tokenString = "valid-token-123";
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenString);
        token.setEmail("test@example.com");
        token.setUsed(false);
        token.setExpiryDate(null); // Permanent token
        token.setCreatedAt(LocalDateTime.now());

        when(tokenRepository.findByToken(tokenString)).thenReturn(token);

        // Execute
        boolean isValid = passwordResetService.validateToken(tokenString);

        // Verify
        assertTrue(isValid);
        verify(tokenRepository, times(1)).findByToken(tokenString);
    }

    @Test
    void testValidateToken_ValidExpirableToken() {
        // Setup
        String tokenString = "valid-token-123";
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenString);
        token.setEmail("test@example.com");
        token.setUsed(false);
        token.setExpiryDate(LocalDateTime.now().plusHours(1)); // Not expired
        token.setCreatedAt(LocalDateTime.now());

        when(tokenRepository.findByToken(tokenString)).thenReturn(token);

        // Execute
        boolean isValid = passwordResetService.validateToken(tokenString);

        // Verify
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_TokenNotFound() {
        // Setup
        String tokenString = "non-existent-token";
        when(tokenRepository.findByToken(tokenString)).thenReturn(null);

        // Execute
        boolean isValid = passwordResetService.validateToken(tokenString);

        // Verify
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_TokenAlreadyUsed() {
        // Setup
        String tokenString = "used-token";
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenString);
        token.setEmail("test@example.com");
        token.setUsed(true); // Already used
        token.setExpiryDate(null);
        token.setCreatedAt(LocalDateTime.now());

        when(tokenRepository.findByToken(tokenString)).thenReturn(token);

        // Execute
        boolean isValid = passwordResetService.validateToken(tokenString);

        // Verify
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_TokenExpired() {
        // Setup
        String tokenString = "expired-token";
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenString);
        token.setEmail("test@example.com");
        token.setUsed(false);
        token.setExpiryDate(LocalDateTime.now().minusHours(1)); // Expired
        token.setCreatedAt(LocalDateTime.now().minusHours(2));

        when(tokenRepository.findByToken(tokenString)).thenReturn(token);

        // Execute
        boolean isValid = passwordResetService.validateToken(tokenString);

        // Verify
        assertFalse(isValid);
    }

    @Test
    void testConsumeToken_Success() {
        // Setup
        String tokenString = "valid-token";
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenString);
        token.setEmail("test@example.com");
        token.setUsed(false);

        when(tokenRepository.findByToken(tokenString)).thenReturn(token);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenReturn(token);

        // Execute
        boolean consumed = passwordResetService.consumeToken(tokenString);

        // Verify
        assertTrue(consumed);
        verify(tokenRepository).save(argThat(savedToken -> savedToken.getUsed()));
    }

    @Test
    void testConsumeToken_TokenNotFound() {
        // Setup
        String tokenString = "non-existent-token";
        when(tokenRepository.findByToken(tokenString)).thenReturn(null);

        // Execute
        boolean consumed = passwordResetService.consumeToken(tokenString);

        // Verify
        assertFalse(consumed);
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void testGetTokenByString() {
        // Setup
        String tokenString = "test-token";
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(tokenString);
        when(tokenRepository.findByToken(tokenString)).thenReturn(token);

        // Execute
        Optional<PasswordResetToken> result = passwordResetService.getTokenByString(tokenString);

        // Verify
        assertTrue(result.isPresent());
        assertEquals(tokenString, result.get().getToken());
    }

    @Test
    void testRevokeTokensForEmail() {
        // Setup
        String email = "test@example.com";

        // Execute
        passwordResetService.revokeTokensForEmail(email);

        // Verify
        verify(tokenRepository, times(1)).deleteByEmail(email);
    }

    @Test
    void testCleanupExpiredTokens_WithExpirableTokens() {
        // Setup: Configure for expirable tokens
        ReflectionTestUtils.setField(passwordResetService, "permanentTokens", false);

        // Execute
        passwordResetService.cleanupExpiredTokens();

        // Verify
        verify(tokenRepository, times(1)).deleteExpiredTokens(any(LocalDateTime.class));
    }

    @Test
    void testCleanupExpiredTokens_WithPermanentTokens() {
        // Setup: Configure for permanent tokens
        ReflectionTestUtils.setField(passwordResetService, "permanentTokens", true);

        // Execute
        passwordResetService.cleanupExpiredTokens();

        // Verify - should NOT call deleteExpiredTokens when using permanent tokens
        verify(tokenRepository, never()).deleteExpiredTokens(any());
    }
}
