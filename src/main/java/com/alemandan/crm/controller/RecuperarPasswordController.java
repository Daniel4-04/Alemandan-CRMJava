package com.alemandan.crm.controller;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.UsuarioRepository;
import com.alemandan.crm.service.MailService;
import com.alemandan.crm.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.regex.Pattern;

@Controller
public class RecuperarPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(RecuperarPasswordController.class);
    
    // Password validation patterns
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[a-zA-Z].*");

    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private MailService mailService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PasswordResetService passwordResetService;
    
    @Value("${app.base.url:http://localhost:8080}")
    private String appBaseUrl;
    
    @Value("${security.password.reset.min-password-length:8}")
    private int minPasswordLength;
    
    @Value("${security.password.reset.session-timeout-minutes:30}")
    private int sessionTimeoutMinutes;

    @GetMapping("/recuperar")
    public String mostrarFormularioRecuperar(Model model) {
        // Solo mostrar el formulario integrado en login.html
        return "login";
    }

    @PostMapping("/recuperar")
    public String procesarRecuperar(@RequestParam String email, 
                                   HttpSession session,
                                   Model model) {
        Usuario usuario = usuarioRepo.findByEmail(email);
        if (usuario == null) {
            model.addAttribute("recuperarError", "No existe usuario con ese correo.");
            // Mantener el slide de recuperar activo
            model.addAttribute("showRecuperar", true);
            return "login";
        }

        // Send simple admin contact email (no token/link)
        try {
            logger.info("Iniciando envío de correo de recuperación a: {}", email);
            
            mailService.enviarCorreoContactoAdminRecuperacion(email);
            
            logger.info("Correo de recuperación enviado exitosamente a: {}", email);
            model.addAttribute("recuperarMensaje", "Correo enviado. Revise su bandeja.");
        } catch (Exception e) {
            logger.error("Error al enviar correo de recuperación a: {}", email, e);
            model.addAttribute("recuperarError", "Error al enviar el correo. Por favor, intente nuevamente más tarde.");
        }
        
        // Mantener el slide de recuperar activo
        model.addAttribute("showRecuperar", true);
        return "login";
    }

    @GetMapping("/reset-password")
    public String mostrarFormularioReset(HttpSession session, Model model) {
        // Check if user has initiated password reset flow (has valid session)
        String authorizedEmail = (String) session.getAttribute("resetAuthorizedEmail");
        Long authorizedTime = (Long) session.getAttribute("resetAuthorizedTime");
        
        // For user convenience, allow access to the form even without session
        // The POST endpoint will validate session/email match
        // This allows users to bookmark the page and use it later
        
        // If session exists, pre-fill the email (convenience)
        if (authorizedEmail != null) {
            model.addAttribute("prefilledEmail", authorizedEmail);
        }
        
        return "reset_password";
    }

    @PostMapping("/reset-password")
    public String procesarReset(@RequestParam String email,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                HttpSession session,
                                Model model) {
        // Check if there's an authorized session for password reset
        String authorizedEmail = (String) session.getAttribute("resetAuthorizedEmail");
        Long authorizedTime = (Long) session.getAttribute("resetAuthorizedTime");
        
        // Validate session authorization
        if (authorizedEmail == null || authorizedTime == null) {
            model.addAttribute("error", "Sesión expirada o inválida. Por favor, solicita un nuevo enlace de recuperación desde la página de inicio de sesión.");
            logger.warn("Password reset attempted without valid session for email: {}", email);
            return "reset_password";
        }
        
        // Check session hasn't expired (configurable timeout)
        long sessionAge = System.currentTimeMillis() - authorizedTime;
        long maxAge = sessionTimeoutMinutes * 60 * 1000; // Convert minutes to milliseconds
        if (sessionAge > maxAge) {
            session.removeAttribute("resetAuthorizedEmail");
            session.removeAttribute("resetAuthorizedTime");
            model.addAttribute("error", "El enlace ha expirado. Por favor, solicita un nuevo enlace de recuperación.");
            logger.warn("Password reset attempted with expired session for email: {}", email);
            return "reset_password";
        }
        
        // Validate email matches authorized email
        if (!email.equals(authorizedEmail)) {
            model.addAttribute("error", "El correo electrónico no coincide con la solicitud de recuperación.");
            logger.warn("Password reset attempted for email {} but session authorized for {}", email, authorizedEmail);
            return "reset_password";
        }
        
        // Find user by email
        Usuario usuario = usuarioRepo.findByEmail(email);
        if (usuario == null) {
            // Use generic error message to prevent email enumeration
            model.addAttribute("error", "Los datos ingresados no son válidos. Por favor, verifica e intenta de nuevo.");
            logger.warn("Password reset attempted for non-existent email: {}", email);
            return "reset_password";
        }
        
        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "reset_password";
        }
        
        // Validate password strength
        String passwordError = validatePasswordStrength(password);
        if (passwordError != null) {
            model.addAttribute("error", passwordError);
            return "reset_password";
        }
        
        // Update user password
        usuario.setPassword(passwordEncoder.encode(password));
        usuarioRepo.save(usuario);
        
        // Clear session attributes
        session.removeAttribute("resetAuthorizedEmail");
        session.removeAttribute("resetAuthorizedTime");
        
        logger.info("Password successfully reset for user: {}", email);

        model.addAttribute("mensaje", "La contraseña fue actualizada correctamente. Ya puedes iniciar sesión con tu nueva contraseña.");
        return "login";
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
}