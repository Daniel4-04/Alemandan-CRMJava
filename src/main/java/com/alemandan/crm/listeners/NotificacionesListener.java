package com.alemandan.crm.listeners;

import com.alemandan.crm.events.UsuarioRegistradoEvent;
import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.service.MailService;
import com.alemandan.crm.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Optional;

/**
 * Listener for user registration events.
 * Sends approval emails asynchronously after transaction commit to prevent SMTP failures from blocking the response.
 */
@Component
public class NotificacionesListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificacionesListener.class);

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MailService mailService;

    /**
     * Handle user registration event asynchronously after transaction commit.
     * Uses mailExecutor thread pool to prevent blocking the main transaction.
     *
     * @param event UsuarioRegistradoEvent containing the user ID
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("mailExecutor")
    public void handleUsuarioRegistrado(UsuarioRegistradoEvent event) {
        logger.info("Processing user registration event for user ID: {}", event.getUsuarioId());
        
        try {
            Optional<Usuario> usuarioOpt = usuarioService.getUsuarioById(event.getUsuarioId());
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                // Build HTML body for approval email
                String htmlBody = "<html><body>" +
                        "<h2>¡Bienvenido a AlemandanPOS!</h2>" +
                        "<p>Hola " + usuario.getNombre() + ",</p>" +
                        "<p>Tu acceso ha sido aprobado. Puedes ingresar al sistema con tu usuario: <strong>" + usuario.getEmail() + "</strong></p>" +
                        "<p>Recuerda cambiar tu contraseña en el primer ingreso.</p>" +
                        "<p>Saludos,<br/>Equipo AlemandanPOS</p>" +
                        "</body></html>";
                
                mailService.enviarAprobacion(
                    usuario.getEmail(),
                    "Acceso aprobado - AlemandanPOS",
                    htmlBody
                );
                
                logger.info("User registration notification sent for user ID: {}", event.getUsuarioId());
            } else {
                logger.warn("User not found for ID: {} during event processing", event.getUsuarioId());
            }
        } catch (Exception e) {
            // Log but don't rethrow - email failures should not crash the event processing
            logger.error("Failed to process user registration event for user ID: {}. Error: {}", 
                event.getUsuarioId(), e.getMessage(), e);
        }
    }
}
