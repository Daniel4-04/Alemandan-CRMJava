package com.alemandan.crm.controller;

import com.alemandan.crm.model.PasswordResetToken;
import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.PasswordResetTokenRepository;
import com.alemandan.crm.repository.UsuarioRepository;
import com.alemandan.crm.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * REST API controller for password reset functionality.
 * Provides endpoints for the static password-reset.html page.
 */
@RestController
@RequestMapping("/api/password-reset")
public class PasswordResetApiController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetApiController.class);
    
    // Password validation patterns
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[a-zA-Z].*");

    @Autowired
    private UsuarioRepository usuarioRepo;
    
    @Autowired
    private PasswordResetTokenRepository tokenRepo;
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${security.password.reset.min-password-length:8}")
    private int minPasswordLength;

    /**
     * Validate a password reset token.
     * 
     * @param token The token to validate
     * @return JSON response indicating if token is valid
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isValid = passwordResetService.validateToken(token);
            
            if (isValid) {
                response.put("valid", true);
                response.put("message", "Token válido");
                logger.debug("Token validation successful for token: {}", token);
                return ResponseEntity.ok(response);
            } else {
                PasswordResetToken resetToken = tokenRepo.findByToken(token);
                if (resetToken != null && resetToken.getUsed()) {
                    response.put("valid", false);
                    response.put("message", "Este enlace ya fue utilizado. Por favor, solicita un nuevo enlace de recuperación.");
                } else {
                    response.put("valid", false);
                    response.put("message", "El enlace es inválido o ha expirado. Por favor, solicita un nuevo enlace de recuperación.");
                }
                logger.debug("Token validation failed for token: {}", token);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Error validating token", e);
            response.put("valid", false);
            response.put("message", "Error al validar el enlace. Por favor, intenta de nuevo.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Reset password using a valid token.
     * 
     * @param request The password reset request containing token, password, and confirmPassword
     * @return JSON response indicating success or failure
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody PasswordResetRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate token first
            if (!passwordResetService.validateToken(request.getToken())) {
                PasswordResetToken expiredToken = tokenRepo.findByToken(request.getToken());
                if (expiredToken != null && expiredToken.getUsed()) {
                    response.put("success", false);
                    response.put("message", "Este enlace ya fue utilizado. Por favor, solicita un nuevo enlace de recuperación.");
                } else {
                    response.put("success", false);
                    response.put("message", "El enlace es inválido o ha expirado. Por favor, solicita un nuevo enlace de recuperación.");
                }
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get the valid token
            PasswordResetToken resetToken = tokenRepo.findByToken(request.getToken());
            
            // Validate passwords match
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                response.put("success", false);
                response.put("message", "Las contraseñas no coinciden.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate password strength
            String passwordError = validatePasswordStrength(request.getPassword());
            if (passwordError != null) {
                response.put("success", false);
                response.put("message", passwordError);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Update user password
            Usuario usuario = usuarioRepo.findByEmail(resetToken.getEmail());
            if (usuario == null) {
                logger.error("User not found for email: {}", resetToken.getEmail());
                response.put("success", false);
                response.put("message", "Error al procesar la solicitud. Por favor, intenta de nuevo.");
                return ResponseEntity.badRequest().body(response);
            }
            
            usuario.setPassword(passwordEncoder.encode(request.getPassword()));
            usuarioRepo.save(usuario);
            
            // Mark token as used
            passwordResetService.consumeToken(request.getToken());
            
            logger.info("Password successfully reset for user: {}", resetToken.getEmail());
            
            response.put("success", true);
            response.put("message", "La contraseña fue actualizada correctamente. Ya puedes iniciar sesión con tu nueva contraseña.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error resetting password", e);
            response.put("success", false);
            response.put("message", "Error al procesar la solicitud. Por favor, intenta de nuevo.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Validates password strength according to security requirements.
     * 
     * @param password The password to validate
     * @return Error message if validation fails, null if password is valid
     */
    private String validatePasswordStrength(String password) {
        if (password == null || password.length() < minPasswordLength) {
            return "La contraseña debe tener al menos " + minPasswordLength + " caracteres.";
        }
        
        if (!LETTER_PATTERN.matcher(password).matches()) {
            return "La contraseña debe contener al menos una letra.";
        }
        
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            return "La contraseña debe contener al menos un número.";
        }
        
        return null; // Password is valid
    }
    
    /**
     * Request DTO for password reset
     */
    public static class PasswordResetRequest {
        private String token;
        private String password;
        private String confirmPassword;
        
        // Getters and setters
        public String getToken() {
            return token;
        }
        
        public void setToken(String token) {
            this.token = token;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getConfirmPassword() {
            return confirmPassword;
        }
        
        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}
