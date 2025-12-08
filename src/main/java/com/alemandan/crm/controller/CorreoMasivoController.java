package com.alemandan.crm.controller;

import com.alemandan.crm.model.Usuario;
import com.alemandan.crm.repository.UsuarioRepository;
import com.alemandan.crm.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/correos-masivos")
public class CorreoMasivoController {

    private static final Logger logger = LoggerFactory.getLogger(CorreoMasivoController.class);

    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private MailService mailService;

    @GetMapping
    public String mostrarFormulario(Model model) {
        List<Usuario> usuarios = usuarioRepo.findAll();
        model.addAttribute("usuarios", usuarios);
        return "correos_masivos";
    }

    @PostMapping
    public String enviarCorreos(@RequestParam(required = false) List<Long> destinatarios,
                                @RequestParam String asunto,
                                @RequestParam String mensaje,
                                Model model) {
        if (destinatarios != null && !destinatarios.isEmpty()) {
            List<Usuario> usuarios = usuarioRepo.findAllById(destinatarios);
            int enviados = 0;
            int fallidos = 0;
            
            for (Usuario usuario : usuarios) {
                try {
                    mailService.enviarCorreoGenerico(usuario.getEmail(), asunto, mensaje);
                    enviados++;
                } catch (Exception e) {
                    // Log but continue with other recipients
                    logger.error("Failed to send email to {}: {}", usuario.getEmail(), e.getMessage());
                    fallidos++;
                }
            }
            
            if (fallidos > 0) {
                model.addAttribute("mensaje", String.format("Correos enviados: %d exitosos, %d fallidos. Revisa los logs para detalles.", enviados, fallidos));
            } else {
                model.addAttribute("mensaje", "Correos enviados correctamente.");
            }
        } else {
            model.addAttribute("error", "Debes seleccionar al menos un destinatario.");
        }
        model.addAttribute("usuarios", usuarioRepo.findAll());
        return "correos_masivos";
    }
}