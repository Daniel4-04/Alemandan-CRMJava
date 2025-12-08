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

/**
 * Controller for testing email sending functionality.
 * Provides an internal endpoint to trigger test emails via MailService.
 */
@RestController
@RequestMapping("/internal")
public class SendgridTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(SendgridTestController.class);
    
    @Autowired
    private MailService mailService;
    
    /**
     * Send a test email to the specified address.
     * 
     * Usage: POST /internal/test-mail?to=user@example.com
     * 
     * @param to Email address to send the test email to
     * @return ResponseEntity with success or error message
     */
    @PostMapping("/test-mail")
    public ResponseEntity<String> testMail(@RequestParam("to") String to) {
        try {
            logger.info("Test email requested for: {}", to);
            
            String subject = "Test Email - AlemandanPOS";
            String body = "Este es un correo de prueba enviado desde AlemandanPOS.\n\n"
                    + "Si recibes este mensaje, significa que el sistema de correo está funcionando correctamente.\n\n"
                    + "Saludos,\nEquipo AlemandanPOS";
            
            mailService.enviarCorreoGenerico(to, subject, body);
            
            logger.info("Test email sent successfully to: {}", to);
            return ResponseEntity.ok("Test email sent successfully to: " + to);
            
        } catch (Exception e) {
            logger.error("Failed to send test email to: {}. Error: {}", to, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send test email: " + e.getMessage());
        }
    }
}
