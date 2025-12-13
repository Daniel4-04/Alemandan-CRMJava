package com.alemandan.crm.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;

/**
 * Centralized email service with exception handling to prevent SMTP failures from crashing the app.
 * All mail sending methods catch and log exceptions without rethrowing.
 * 
 * Uses SendGrid API when SENDGRID_API_KEY is configured, falls back to JavaMailSender (SMTP) otherwise.
 */
@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${sendgrid.api.key:}")
    private String sendGridApiKey;
    
    @Value("${sendgrid.sender.email:noreply@alemandanpos.com}")
    private String senderEmail;
    
    @Value("${spring.mail.host:}")
    private String smtpHost;
    
    @Value("${spring.mail.port:0}")
    private int smtpPort;
    
    @Value("${spring.mail.username:}")
    private String smtpUsername;
    
    @Value("${spring.mail.password:}")
    private String smtpPassword;
    
    /**
     * Validate mail configuration on startup and log warnings if incomplete.
     * This helps diagnose email issues early rather than failing silently at runtime.
     */
    @PostConstruct
    public void validateMailConfiguration() {
        boolean sendGridConfigured = StringUtils.hasText(sendGridApiKey);
        boolean smtpConfigured = StringUtils.hasText(smtpHost) && 
                                 smtpPort > 0 && 
                                 StringUtils.hasText(smtpUsername) &&
                                 StringUtils.hasText(smtpPassword);
        
        if (!sendGridConfigured && !smtpConfigured) {
            logger.warn("=================================================================");
            logger.warn("WARNING: No email service configured!");
            logger.warn("Neither SendGrid API nor SMTP settings are properly configured.");
            logger.warn("Email sending will fail. To fix this:");
            logger.warn("1. Configure SendGrid: Set SENDGRID_API_KEY and SENDER_EMAIL");
            logger.warn("2. Or configure SMTP: Set SPRING_MAIL_HOST, SPRING_MAIL_PORT,");
            logger.warn("   SPRING_MAIL_USERNAME, and SPRING_MAIL_PASSWORD");
            logger.warn("See docs/DEPLOYMENT.md for Railway setup instructions.");
            logger.warn("=================================================================");
        } else if (sendGridConfigured) {
            logger.info("Email service configured: SendGrid API (primary)");
            if (smtpConfigured) {
                logger.info("Email service configured: SMTP (fallback)");
            }
        } else if (smtpConfigured) {
            logger.info("Email service configured: SMTP only");
            logger.info("Note: Railway may block SMTP ports. Consider using SendGrid API.");
        }
    }

    /**
     * Send approval email with HTML body support.
     * Catches all exceptions to prevent SMTP timeouts from propagating.
     * 
     * Uses SendGrid API if SENDGRID_API_KEY is configured, otherwise falls back to SMTP.
     *
     * @param to Email recipient
     * @param subject Email subject
     * @param htmlBody HTML content for email body
     */
    public void enviarAprobacion(String to, String subject, String htmlBody) {
        // Try SendGrid API first if configured
        if (StringUtils.hasText(sendGridApiKey)) {
            if (enviarConSendGrid(to, subject, htmlBody)) {
                return; // Success with SendGrid
            }
            logger.warn("SendGrid failed, falling back to SMTP for: {}", to);
        }
        
        // Fallback to SMTP (JavaMailSender)
        enviarConSMTP(to, subject, htmlBody);
    }
    
    /**
     * Send email using SendGrid API.
     * @return true if successful, false otherwise
     */
    private boolean enviarConSendGrid(String to, String subject, String htmlBody) {
        try {
            Email from = new Email(senderEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/html", htmlBody);
            Mail mail = new Mail(from, subject, toEmail, content);
            
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("Email sent successfully via SendGrid to: {} (status: {})", to, response.getStatusCode());
                return true;
            } else {
                logger.error("SendGrid API returned error status {} for: {}. Body: {}", 
                    response.getStatusCode(), to, response.getBody());
                return false;
            }
        } catch (IOException e) {
            logger.error("SendGrid API IO error sending to: {}. Error: {}", to, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error with SendGrid for: {}. Error: {}", to, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send email using SMTP (JavaMailSender).
     */
    private void enviarConSMTP(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML format
            mailSender.send(message);
            logger.info("Email sent successfully via SMTP to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to create email message for: {}. Error: {}", to, e.getMessage(), e);
        } catch (MailException e) {
            logger.error("Failed to send email to: {}. SMTP error: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    public void enviarBienvenida(String to, String nombre) {
        String subject = "Bienvenido a AlemandanPOS";
        String body = "Hola " + nombre + ",\n\n"
                + "Gracias por solicitar acceso a nuestra plataforma AlemandanPOS.\n"
                + "Tu registro fue recibido y está pendiente de aprobación por el administrador.\n"
                + "Recibirás otro correo cuando tu acceso sea aprobado.\n\n"
                + "Saludos,\nEquipo AlemandanPOS";
        
        // Try SendGrid API first if configured
        if (StringUtils.hasText(sendGridApiKey)) {
            if (enviarConSendGridTexto(to, subject, body)) {
                return; // Success with SendGrid
            }
            logger.warn("SendGrid failed, falling back to SMTP for: {}", to);
        }
        
        // Fallback to SMTP
        enviarConSMTPTexto(to, subject, body);
    }

    public void enviarRechazo(String to, String nombre) {
        String subject = "Solicitud rechazada - AlemandanPOS";
        String body = "Hola " + nombre + ",\n\n"
                + "Tu solicitud de acceso fue rechazada por el administrador.\n"
                + "Si crees que esto es un error, comunícate con la empresa.\n\n"
                + "Saludos,\nEquipo AlemandanPOS";
        
        // Try SendGrid API first if configured
        if (StringUtils.hasText(sendGridApiKey)) {
            if (enviarConSendGridTexto(to, subject, body)) {
                return; // Success with SendGrid
            }
            logger.warn("SendGrid failed, falling back to SMTP for: {}", to);
        }
        
        // Fallback to SMTP
        enviarConSMTPTexto(to, subject, body);
    }

    /**
     * Envía correo simple de recuperación de contraseña indicando que el usuario debe contactar al administrador.
     * No genera ni envía tokens/enlaces.
     * 
     * @param to Email del destinatario
     * @throws Exception si el envío falla (propagada para manejo en el controlador)
     */
    public void enviarCorreoContactoAdminRecuperacion(String to) throws Exception {
        String subject = "Recuperación de contraseña - Alemandan CRM";
        String body = "Para recuperación de contraseña porfavor contactar con el administrador\nContacto: 3216397497";
        
        logger.info("Enviando email de recuperación sencillo a {}", to);
        
        // Try SendGrid API first if configured
        if (StringUtils.hasText(sendGridApiKey)) {
            try {
                if (enviarConSendGridTextoConExcepcion(to, subject, body)) {
                    return; // Success with SendGrid
                }
            } catch (Exception e) {
                logger.warn("SendGrid failed, falling back to SMTP for: {}", to);
            }
        }
        
        // Fallback to SMTP - but throw exception if it fails
        enviarConSMTPTextoConExcepcion(to, subject, body);
    }

    public void enviarCorreoRecuperarPassword(String to, String nombre, String link) {
        String subject = "Recuperar contraseña - AlemandanPOS";
        String body = "Hola " + nombre + ",\n\n"
                + "Haz clic en el siguiente enlace para restablecer tu contraseña:\n"
                + link + "\n\n"
                + "Si no solicitaste este cambio, ignora este mensaje.\n\n"
                + "Saludos,\nEquipo AlemandanPOS";
        
        // Log the password reset link (useful for Railway debugging when SMTP is blocked)
        // SECURITY NOTE: This logs the token for admin debugging. Ensure server logs are properly secured.
        logger.info("Sending password reset email to {} with link: {}", to, link);
        
        // Try SendGrid API first if configured
        if (StringUtils.hasText(sendGridApiKey)) {
            if (enviarConSendGridTexto(to, subject, body)) {
                return; // Success with SendGrid
            }
            logger.warn("SendGrid failed, falling back to SMTP for: {}", to);
        }
        
        // Fallback to SMTP
        enviarConSMTPTexto(to, subject, body);
    }

    public void enviarCorreoGenerico(String to, String asunto, String mensajeTexto) {
        // Try SendGrid API first if configured
        if (StringUtils.hasText(sendGridApiKey)) {
            if (enviarConSendGridTexto(to, asunto, mensajeTexto)) {
                return; // Success with SendGrid
            }
            logger.warn("SendGrid failed, falling back to SMTP for: {}", to);
        }
        
        // Fallback to SMTP
        enviarConSMTPTexto(to, asunto, mensajeTexto);
    }
    
    /**
     * Send plain text email using SendGrid API.
     * @return true if successful, false otherwise
     */
    private boolean enviarConSendGridTexto(String to, String subject, String textBody) {
        try {
            Email from = new Email(senderEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/plain", textBody);
            Mail mail = new Mail(from, subject, toEmail, content);
            
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("Email sent successfully via SendGrid to: {} (status: {})", to, response.getStatusCode());
                return true;
            } else {
                logger.error("SendGrid API returned error status {} for: {}. Body: {}", 
                    response.getStatusCode(), to, response.getBody());
                return false;
            }
        } catch (IOException e) {
            logger.error("SendGrid API IO error sending to: {}. Error: {}", to, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error with SendGrid for: {}. Error: {}", to, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send plain text email using SendGrid API with exception propagation.
     * @return true if successful
     * @throws Exception if sending fails
     */
    private boolean enviarConSendGridTextoConExcepcion(String to, String subject, String textBody) throws Exception {
        try {
            Email from = new Email(senderEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/plain", textBody);
            Mail mail = new Mail(from, subject, toEmail, content);
            
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("Email sent successfully via SendGrid to: {} (status: {})", to, response.getStatusCode());
                return true;
            } else {
                logger.error("SendGrid API returned error status {} for: {}. Body: {}", 
                    response.getStatusCode(), to, response.getBody());
                throw new IOException("SendGrid API error: " + response.getStatusCode());
            }
        } catch (IOException e) {
            logger.error("SendGrid API IO error sending to: {}. Error: {}", to, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error with SendGrid for: {}. Error: {}", to, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Send plain text email using SMTP (JavaMailSender).
     */
    private void enviarConSMTPTexto(String to, String subject, String textBody) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(to);
            mensaje.setSubject(subject);
            mensaje.setText(textBody);
            mailSender.send(mensaje);
            logger.info("Email sent successfully via SMTP to: {}", to);
        } catch (MailException e) {
            logger.error("Failed to send email to: {}. SMTP error: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }
    
    /**
     * Send plain text email using SMTP (JavaMailSender) with exception propagation.
     * This version throws exceptions for explicit error handling in the caller.
     */
    private void enviarConSMTPTextoConExcepcion(String to, String subject, String textBody) throws Exception {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(to);
            mensaje.setSubject(subject);
            mensaje.setText(textBody);
            mailSender.send(mensaje);
            logger.info("Email sent successfully via SMTP to: {}", to);
        } catch (MailException e) {
            logger.error("Failed to send email to: {}. SMTP error: {}", to, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error sending email to: {}. Error: {}", to, e.getMessage(), e);
            throw e;
        }
    }
}