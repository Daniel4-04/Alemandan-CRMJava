package com.alemandan.crm.controller;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.UsuarioRepository;
import com.alemandan.crm.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    
    @Value("${app.base.url:http://localhost:8080}")
    private String appBaseUrl;
    
    @Value("${security.password.reset.min-password-length:8}")
    private int minPasswordLength;

    @GetMapping("/recuperar")
    public String mostrarFormularioRecuperar(Model model) {
        // Solo mostrar el formulario integrado en login.html
        return "login";
    }

    @PostMapping("/recuperar")
    public String procesarRecuperar(@RequestParam String email, Model model) {
        Usuario usuario = usuarioRepo.findByEmail(email);
        if (usuario == null) {
            model.addAttribute("recuperarError", "No existe usuario con ese correo.");
            // Mantener el slide de recuperar activo
            model.addAttribute("showRecuperar", true);
            return "login";
        }

        // Enviar correo con enlace de recuperación - wrapped to prevent SMTP failures from blocking
        try {
            String link = appBaseUrl + "/reset-password";
            // Log the password reset link for Railway debugging (when SMTP is blocked)
            logger.info("Password reset link generated for {}: {}", email, link);
            mailService.enviarCorreoRecuperarPassword(email, usuario.getNombre(), link);
            logger.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            // Log error but continue
            logger.error("Failed to send password recovery email to {}", email, e);
        }

        model.addAttribute("recuperarMensaje", "Se ha enviado un enlace de recuperación a tu correo.");
        // Mantener el slide de recuperar activo
        model.addAttribute("showRecuperar", true);
        return "login";
    }

    @GetMapping("/reset-password")
    public String mostrarFormularioReset(Model model) {
        // Show the permanent password reset form (no token required)
        return "reset_password";
    }

    @PostMapping("/reset-password")
    public String procesarReset(@RequestParam String email,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                Model model) {
        // Find user by email
        Usuario usuario = usuarioRepo.findByEmail(email);
        if (usuario == null) {
            model.addAttribute("error", "No existe usuario con ese correo electrónico.");
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