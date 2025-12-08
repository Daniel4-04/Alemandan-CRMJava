package com.alemandan.crm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Centralized email service with exception handling to prevent SMTP failures from crashing the app.
 * All mail sending methods catch and log exceptions without rethrowing.
 */
@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Send approval email with HTML body support.
     * Catches all exceptions to prevent SMTP timeouts from propagating.
     *
     * @param to Email recipient
     * @param subject Email subject
     * @param htmlBody HTML content for email body
     */
    public void enviarAprobacion(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML format
            mailSender.send(message);
            logger.info("Approval email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to create approval email message for: {}. Error: {}", to, e.getMessage(), e);
        } catch (MailException e) {
            logger.error("Failed to send approval email to: {}. SMTP error: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending approval email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    public void enviarBienvenida(String to, String nombre) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(to);
            mensaje.setSubject("Bienvenido a AlemandanPOS");
            mensaje.setText("Hola " + nombre + ",\n\n"
                    + "Gracias por solicitar acceso a nuestra plataforma AlemandanPOS.\n"
                    + "Tu registro fue recibido y está pendiente de aprobación por el administrador.\n"
                    + "Recibirás otro correo cuando tu acceso sea aprobado.\n\n"
                    + "Saludos,\nEquipo AlemandanPOS");
            mailSender.send(mensaje);
            logger.info("Welcome email sent successfully to: {}", to);
        } catch (MailException e) {
            logger.error("Failed to send welcome email to: {}. SMTP error: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending welcome email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    public void enviarRechazo(String to, String nombre) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(to);
            mensaje.setSubject("Solicitud rechazada - AlemandanPOS");
            mensaje.setText("Hola " + nombre + ",\n\n"
                    + "Tu solicitud de acceso fue rechazada por el administrador.\n"
                    + "Si crees que esto es un error, comunícate con la empresa.\n\n"
                    + "Saludos,\nEquipo AlemandanPOS");
            mailSender.send(mensaje);
            logger.info("Rejection email sent successfully to: {}", to);
        } catch (MailException e) {
            logger.error("Failed to send rejection email to: {}. SMTP error: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending rejection email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    public void enviarCorreoRecuperarPassword(String to, String nombre, String link) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(to);
            mensaje.setSubject("Recuperar contraseña - AlemandanPOS");
            mensaje.setText("Hola " + nombre + ",\n\n"
                    + "Haz clic en el siguiente enlace para restablecer tu contraseña:\n"
                    + link + "\n\n"
                    + "Si no solicitaste este cambio, ignora este mensaje.\n\n"
                    + "Saludos,\nEquipo AlemandanPOS");
            mailSender.send(mensaje);
            logger.info("Password recovery email sent successfully to: {}", to);
        } catch (MailException e) {
            logger.error("Failed to send password recovery email to: {}. SMTP error: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending password recovery email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }

    public void enviarCorreoGenerico(String to, String asunto, String mensajeTexto) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(to);
            mensaje.setSubject(asunto);
            mensaje.setText(mensajeTexto);
            mailSender.send(mensaje);
            logger.info("Generic email sent successfully to: {}", to);
        } catch (MailException e) {
            logger.error("Failed to send generic email to: {}. SMTP error: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error sending generic email to: {}. Error: {}", to, e.getMessage(), e);
        }
    }
}