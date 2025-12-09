package com.alemandan.crm.controller;

import com.alemandan.crm.model.PasswordResetToken;
import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.PasswordResetTokenRepository;
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
    private PasswordResetTokenRepository tokenRepo;
    @Autowired
    private PasswordResetService passwordResetService;
    @Autowired
    private MailService mailService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${app.base.url:http://localhost:8080}")
    private String appBaseUrl;
    
    @Value("${security.password.reset.token.expiration-minutes:60}")
    private int tokenExpirationMinutes;
    
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

        // Generate token using service (supports permanent or expirable tokens based on config)
        String token = passwordResetService.createTokenForEmail(email, usuario.getId());

        // Enviar correo con enlace de recuperación - wrapped to prevent SMTP failures from blocking
        try {
            String link = appBaseUrl + "/password-reset.html?token=" + token;
            // Log the password reset link for Railway debugging (in case SMTP is blocked)
            logger.info("Password reset link generated for {}: {}", email, link);
            mailService.enviarCorreoRecuperarPassword(email, usuario.getNombre(), link);
            logger.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            // Log error but continue - token is already saved
            logger.error("Failed to send password recovery email to {}, but token was created", email, e);
        }

        model.addAttribute("recuperarMensaje", "Se ha enviado un enlace de recuperación a tu correo.");
        // Mantener el slide de recuperar activo
        model.addAttribute("showRecuperar", true);
        return "login";
    }

    @GetMapping("/reset-password")
    public String mostrarFormularioReset(@RequestParam String token, Model model) {
        if (!passwordResetService.validateToken(token)) {
            PasswordResetToken expiredToken = tokenRepo.findByToken(token);
            if (expiredToken != null && expiredToken.getUsed()) {
                model.addAttribute("error", "Este enlace ya fue utilizado. Por favor, solicita un nuevo enlace de recuperación.");
            } else {
                model.addAttribute("error", "El enlace es inválido o ha expirado. Por favor, solicita un nuevo enlace de recuperación.");
            }
            return "reset_password";
        }
        
        model.addAttribute("token", token);
        return "reset_password";
    }

    @PostMapping("/reset-password")
    public String procesarReset(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                Model model) {
        // Validate token first
        if (!passwordResetService.validateToken(token)) {
            PasswordResetToken expiredToken = tokenRepo.findByToken(token);
            if (expiredToken != null && expiredToken.getUsed()) {
                model.addAttribute("error", "Este enlace ya fue utilizado. Por favor, solicita un nuevo enlace de recuperación.");
            } else {
                model.addAttribute("error", "El enlace es inválido o ha expirado. Por favor, solicita un nuevo enlace de recuperación.");
            }
            return "reset_password";
        }
        
        // Get the valid token
        PasswordResetToken resetToken = tokenRepo.findByToken(token);
        
        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            model.addAttribute("token", token);
            return "reset_password";
        }
        
        // Validate password strength
        String passwordError = validatePasswordStrength(password);
        if (passwordError != null) {
            model.addAttribute("error", passwordError);
            model.addAttribute("token", token);
            return "reset_password";
        }
        
        // Update user password
        Usuario usuario = usuarioRepo.findByEmail(resetToken.getEmail());
        if (usuario == null) {
            logger.error("User not found for email: {}", resetToken.getEmail());
            model.addAttribute("error", "Error al procesar la solicitud. Por favor, intenta de nuevo.");
            return "reset_password";
        }
        
        usuario.setPassword(passwordEncoder.encode(password));
        usuarioRepo.save(usuario);
        
        // Mark token as used
        passwordResetService.consumeToken(token);
        
        logger.info("Password successfully reset for user: {}", resetToken.getEmail());

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