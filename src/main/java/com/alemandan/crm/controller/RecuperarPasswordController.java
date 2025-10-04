package com.alemandan.crm.controller;

import com.alemandan.crm.model.PasswordResetToken;
import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.PasswordResetTokenRepository;
import com.alemandan.crm.repository.UsuarioRepository;
import com.alemandan.crm.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class RecuperarPasswordController {

    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private PasswordResetTokenRepository tokenRepo;
    @Autowired
    private MailService mailService;
    @Autowired
    private PasswordEncoder passwordEncoder;

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

        // Generar token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(1)); // 1 hora de validez
        tokenRepo.deleteByEmail(email); // Borra tokens anteriores
        tokenRepo.save(resetToken);

        // Enviar correo con enlace de recuperación
        String link = "http://localhost:8080/reset-password?token=" + token;
        mailService.enviarCorreoRecuperarPassword(email, usuario.getNombre(), link);

        model.addAttribute("recuperarMensaje", "Se ha enviado un enlace de recuperación a tu correo.");
        // Mantener el slide de recuperar activo
        model.addAttribute("showRecuperar", true);
        return "login";
    }

    @GetMapping("/reset-password")
    public String mostrarFormularioReset(@RequestParam String token, Model model) {
        PasswordResetToken resetToken = tokenRepo.findByToken(token);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "El enlace es inválido o ha expirado.");
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
        PasswordResetToken resetToken = tokenRepo.findByToken(token);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "El enlace es inválido o ha expirado.");
            return "reset_password";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            model.addAttribute("token", token);
            return "reset_password";
        }
        Usuario usuario = usuarioRepo.findByEmail(resetToken.getEmail());
        usuario.setPassword(passwordEncoder.encode(password));
        usuarioRepo.save(usuario);

        tokenRepo.delete(resetToken);

        model.addAttribute("mensaje", "La contraseña fue actualizada correctamente.");
        return "login";
    }
}