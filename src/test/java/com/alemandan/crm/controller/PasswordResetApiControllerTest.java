package com.alemandan.crm.controller;

import com.alemandan.crm.model.PasswordResetToken;
import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.PasswordResetTokenRepository;
import com.alemandan.crm.repository.UsuarioRepository;
import com.alemandan.crm.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PasswordResetApiController
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private PasswordResetTokenRepository tokenRepo;

    @MockBean
    private UsuarioRepository usuarioRepo;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private PasswordResetToken validToken;
    private Usuario testUser;

    @BeforeEach
    void setUp() {
        // Setup valid token
        validToken = new PasswordResetToken();
        validToken.setId(1L);
        validToken.setToken("valid-token-123");
        validToken.setEmail("test@example.com");
        validToken.setUsed(false);
        validToken.setCreatedAt(LocalDateTime.now());
        validToken.setExpiryDate(null); // Permanent token

        // Setup test user
        testUser = new Usuario();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setNombre("Test User");
        testUser.setPassword("oldPassword123");
    }

    @Test
    void testValidateToken_ValidToken() throws Exception {
        when(passwordResetService.validateToken("valid-token-123")).thenReturn(true);

        mockMvc.perform(get("/api/password-reset/validate")
                .param("token", "valid-token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Token válido"));

        verify(passwordResetService, times(1)).validateToken("valid-token-123");
    }

    @Test
    void testValidateToken_InvalidToken() throws Exception {
        when(passwordResetService.validateToken("invalid-token")).thenReturn(false);
        when(tokenRepo.findByToken("invalid-token")).thenReturn(null);

        mockMvc.perform(get("/api/password-reset/validate")
                .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(passwordResetService, times(1)).validateToken("invalid-token");
    }

    @Test
    void testValidateToken_UsedToken() throws Exception {
        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setToken("used-token");
        usedToken.setUsed(true);

        when(passwordResetService.validateToken("used-token")).thenReturn(false);
        when(tokenRepo.findByToken("used-token")).thenReturn(usedToken);

        mockMvc.perform(get("/api/password-reset/validate")
                .param("token", "used-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Este enlace ya fue utilizado. Por favor, solicita un nuevo enlace de recuperación."));
    }

    @Test
    void testResetPassword_Success() throws Exception {
        when(passwordResetService.validateToken("valid-token-123")).thenReturn(true);
        when(tokenRepo.findByToken("valid-token-123")).thenReturn(validToken);
        when(usuarioRepo.findByEmail("test@example.com")).thenReturn(testUser);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedPassword123");
        when(usuarioRepo.save(any(Usuario.class))).thenReturn(testUser);
        when(passwordResetService.consumeToken("valid-token-123")).thenReturn(true);

        PasswordResetApiController.PasswordResetRequest request = new PasswordResetApiController.PasswordResetRequest();
        request.setToken("valid-token-123");
        request.setPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        mockMvc.perform(post("/api/password-reset/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());

        // Verify password was encoded and saved
        ArgumentCaptor<Usuario> userCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(userCaptor.capture());
        assertEquals("encodedPassword123", userCaptor.getValue().getPassword());

        // Verify token was consumed
        verify(passwordResetService).consumeToken("valid-token-123");
    }

    @Test
    void testResetPassword_PasswordMismatch() throws Exception {
        when(passwordResetService.validateToken("valid-token-123")).thenReturn(true);
        when(tokenRepo.findByToken("valid-token-123")).thenReturn(validToken);

        PasswordResetApiController.PasswordResetRequest request = new PasswordResetApiController.PasswordResetRequest();
        request.setToken("valid-token-123");
        request.setPassword("newPassword123");
        request.setConfirmPassword("differentPassword123");

        mockMvc.perform(post("/api/password-reset/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Las contraseñas no coinciden."));

        verify(usuarioRepo, never()).save(any());
        verify(passwordResetService, never()).consumeToken(any());
    }

    @Test
    void testResetPassword_WeakPassword_TooShort() throws Exception {
        when(passwordResetService.validateToken("valid-token-123")).thenReturn(true);
        when(tokenRepo.findByToken("valid-token-123")).thenReturn(validToken);

        PasswordResetApiController.PasswordResetRequest request = new PasswordResetApiController.PasswordResetRequest();
        request.setToken("valid-token-123");
        request.setPassword("weak1");
        request.setConfirmPassword("weak1");

        mockMvc.perform(post("/api/password-reset/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("La contraseña debe tener al menos 8 caracteres."));
    }

    @Test
    void testResetPassword_WeakPassword_NoLetter() throws Exception {
        when(passwordResetService.validateToken("valid-token-123")).thenReturn(true);
        when(tokenRepo.findByToken("valid-token-123")).thenReturn(validToken);

        PasswordResetApiController.PasswordResetRequest request = new PasswordResetApiController.PasswordResetRequest();
        request.setToken("valid-token-123");
        request.setPassword("12345678");
        request.setConfirmPassword("12345678");

        mockMvc.perform(post("/api/password-reset/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("La contraseña debe contener al menos una letra."));
    }

    @Test
    void testResetPassword_WeakPassword_NoDigit() throws Exception {
        when(passwordResetService.validateToken("valid-token-123")).thenReturn(true);
        when(tokenRepo.findByToken("valid-token-123")).thenReturn(validToken);

        PasswordResetApiController.PasswordResetRequest request = new PasswordResetApiController.PasswordResetRequest();
        request.setToken("valid-token-123");
        request.setPassword("abcdefgh");
        request.setConfirmPassword("abcdefgh");

        mockMvc.perform(post("/api/password-reset/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("La contraseña debe contener al menos un número."));
    }

    @Test
    void testResetPassword_InvalidToken() throws Exception {
        when(passwordResetService.validateToken("invalid-token")).thenReturn(false);
        when(tokenRepo.findByToken("invalid-token")).thenReturn(null);

        PasswordResetApiController.PasswordResetRequest request = new PasswordResetApiController.PasswordResetRequest();
        request.setToken("invalid-token");
        request.setPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        mockMvc.perform(post("/api/password-reset/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());

        verify(usuarioRepo, never()).save(any());
    }

    @Test
    void testResetPassword_UserNotFound() throws Exception {
        when(passwordResetService.validateToken("valid-token-123")).thenReturn(true);
        when(tokenRepo.findByToken("valid-token-123")).thenReturn(validToken);
        when(usuarioRepo.findByEmail("test@example.com")).thenReturn(null);

        PasswordResetApiController.PasswordResetRequest request = new PasswordResetApiController.PasswordResetRequest();
        request.setToken("valid-token-123");
        request.setPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        mockMvc.perform(post("/api/password-reset/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Error al procesar la solicitud. Por favor, intenta de nuevo."));

        verify(passwordResetService, never()).consumeToken(any());
    }
}
