package com.alemandan.crm.controller;

import com.alemandan.crm.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * Controller for testing email sending functionality.
 * Provides an internal endpoint to trigger test emails via MailService.
 * 
 * SECURITY NOTE: This endpoint should be secured in production environments
 * using Spring Security to restrict access to administrators only, or by
 * implementing IP whitelisting to allow only internal/trusted addresses.
 */
@RestController
@RequestMapping("/internal")
public class SendgridTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(SendgridTestController.class);
    
    // Basic email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    @Autowired
    private MailService mailService;
    
    /**
     * Send a test email to the specified address.
     * 
     * Usage: POST /internal/test-mail?to=user@example.com
     * 
     * IMPORTANT: Secure this endpoint in production to prevent abuse.
     * 
     * @param to Email address to send the test email to
     * @return ResponseEntity with success or error message
     */
    @PostMapping("/test-mail")
    public ResponseEntity<String> testMail(@RequestParam("to") String to) {
        try {
            // Validate email format
            if (to == null || to.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Email address is required");
            }
            
            String email = to.trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                return ResponseEntity.badRequest()
                        .body("Invalid email format: " + email);
            }
            
            logger.info("Test email requested for: {}", email);
            
            String subject = "Test Email - AlemandanPOS";
            String body = "Este es un correo de prueba enviado desde AlemandanPOS.\n\n"
                    + "Si recibes este mensaje, significa que el sistema de correo está funcionando correctamente.\n\n"
                    + "Saludos,\nEquipo AlemandanPOS";
            
            mailService.enviarCorreoGenerico(email, subject, body);
            
            logger.info("Test email sent successfully to: {}", email);
            return ResponseEntity.ok("Test email sent successfully to: " + email);
            
        } catch (Exception e) {
            logger.error("Failed to send test email to: {}. Error: {}", to, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send test email. Check server logs for details.");
        }
    }
}
